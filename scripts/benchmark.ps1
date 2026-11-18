param(
    [ValidateSet('Smoke', 'Measure', 'Profile', 'VerifyProfile')][string]$Mode = 'Smoke',
    [string]$Device = 'emulator-5554',
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label = 'local',
    [ValidatePattern('^[a-zA-Z0-9_.,#]*$')][string]$TestFilter = '',
    [switch]$SkipBuild,
    [switch]$AllowEmulator
)
$ErrorActionPreference = 'Stop'
function Write-AdbOutput {
    param([object]$Line, [bool]$Live)
    $text = $Line.ToString()
    if ($Live) { Write-Host $text }
    return $text
}

function Invoke-Adb {
    param([string[]]$AdbArguments, [switch]$StreamOutput)
    $previousPreference = $ErrorActionPreference
    try {
        # Windows PowerShell treats adb's normal stderr progress as NativeCommandError.
        $ErrorActionPreference = 'Continue'
        $result = & adb -s $Device @AdbArguments 2>&1 | ForEach-Object {
            Write-AdbOutput -Line $_ -Live $StreamOutput.IsPresent
        }
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0) { throw "adb failed: $result" }
    return $result | ForEach-Object { $_.ToString() }
}

function Write-AppProfiles {
    param([string]$ReportPath, [string]$Workspace)
    $profiles = Get-ChildItem (Join-Path $reportPath 'output') -Recurse -Filter '*baseline-prof.txt'
    $startup = Get-ChildItem (Join-Path $reportPath 'output') -Recurse -Filter '*startup-prof.txt'
    if (-not $profiles -or -not $startup) { throw 'Missing generated baseline/startup profile' }
    $sourcePath = Join-Path $workspace 'app/src/main/generated/baselineProfiles'
    New-Item -ItemType Directory -Force -Path $sourcePath | Out-Null
    $rules = @($profiles) + @($startup) | Get-Content | Where-Object { $_ -and -not $_.StartsWith('#') } | Sort-Object -Unique
    $startupRules = $startup | Get-Content | Where-Object { $_ -and -not $_.StartsWith('#') } | Sort-Object -Unique
    if (($rules -join "`n") -match '/benchmarkfixture/') { throw 'Fixture methods leaked into profiles' }
    [IO.File]::WriteAllLines((Join-Path $sourcePath 'baseline-prof.txt'), [string[]]$rules)
    [IO.File]::WriteAllLines((Join-Path $sourcePath 'startup-prof.txt'), [string[]]$startupRules)
}

function Build-PerformanceApks {
    param([string]$CapitalVariant)
    if (-not $SkipBuild) {
        & ./gradlew.bat ":app:assemble$capitalVariant" ":benchmark:assemble$capitalVariant" --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Performance APK build failed' }
    }
}

function Get-PerformanceTestClass {
    if ($TestFilter) { return $TestFilter }
    switch ($Mode) {
        'Smoke' { 'dev.twotechy.inknest.perf.FixtureSmokeTest' }
        'Measure' { 'dev.twotechy.inknest.perf.StartupBenchmark' }
        'Profile' { 'dev.twotechy.inknest.perf.BaselineProfileGenerator' }
        'VerifyProfile' { 'dev.twotechy.inknest.perf.ProfileInstallationTest' }
    }
}

function Get-PerformanceArguments {
    param([bool]$Emulator, [string]$RemoteOutput, [string]$TestPackage)
    $testClass = Get-PerformanceTestClass
    $arguments = @('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $testClass,
        '-e', 'additionalTestOutputDir', $remoteOutput)
    if ($emulator) { $arguments += @('-e', 'androidx.benchmark.suppressErrors', 'EMULATOR') }
    $arguments += "$testPackage/androidx.test.runner.AndroidJUnitRunner"
    return $arguments
}

function Export-PerformanceResults {
    param([string]$ReportPath, [string]$RemoteOutput, [string]$Workspace, [string]$TestPackage)
    if ($Mode -eq 'Smoke') {
        Invoke-Adb @('pull', "/sdcard/Android/data/$testPackage/files/screenshots", (Join-Path $reportPath 'screenshots'))
    }
    if ($Mode -ne 'Smoke') {
        Invoke-Adb @('pull', $remoteOutput, (Join-Path $reportPath 'output'))
        if ($Mode -eq 'Measure') {
            Invoke-Adb @('pull', "/sdcard/Android/data/$testPackage/files/memory", (Join-Path $reportPath 'memory'))
        }
    }
    if ($Mode -eq 'Profile') { Write-AppProfiles $reportPath $workspace }
}

function Invoke-PerformanceInstrumentation {
    param([bool]$Emulator, [string]$RemoteOutput, [string]$TestPackage, [string]$ReportPath)
    $arguments = Get-PerformanceArguments $emulator $remoteOutput $testPackage
    $result = Invoke-Adb -AdbArguments $arguments -StreamOutput |
        Tee-Object -FilePath (Join-Path $reportPath 'instrumentation.txt')
    if (($result -join "`n") -notmatch 'OK \([0-9]+ tests?\)') { throw 'Performance instrumentation failed' }
}

function Get-MeasureScenarios {
    if ($TestFilter) { return $TestFilter.Split(',') | ForEach-Object { $_.Split('#')[-1] } }
    return @('coldStartup', 'warmStartup', 'mainTabNavigation', 'homeReturnUnchanged', 'homeReturnChanged', 'libraryScanAndEnrich', 'libraryGridScroll')
}

function Get-MeasureIterations {
    if ($TestFilter -match 'RefreshBenchmark') { return 1 }
    return 5
}

function Write-PerformanceMetadata {
    param([string]$ReportPath, [string]$Variant, [string]$OriginalHome, [hashtable]$Outcome)
    $metadata = [ordered]@{
        commit = (& git rev-parse HEAD).Trim()
        workingTreeDirty = [bool](& git status --porcelain)
        targetApkSha256 = (Get-FileHash "app/build/outputs/apk/$variant/app-$variant.apk" -Algorithm SHA256).Hash
        mode = $Mode
        runStatus = $Outcome.status
        testFilter = $TestFilter
        variant = $variant
        serial = $Device
        emulator = $Outcome.emulator
        model = (Invoke-Adb @('shell', 'getprop', 'ro.product.model')).ToString().Trim()
        build = (Invoke-Adb @('shell', 'getprop', 'ro.build.display.id')).ToString().Trim()
        display = (Invoke-Adb @('shell', 'wm', 'size')) -join '; '
        density = (Invoke-Adb @('shell', 'wm', 'density')) -join '; '
        fontScale = (Invoke-Adb @('shell', 'settings', 'get', 'system', 'font_scale')) -join ''
        battery = (Invoke-Adb @('shell', 'dumpsys', 'battery')) -join "`n"
        powerSaver = (Invoke-Adb @('shell', 'settings', 'get', 'global', 'low_power')) -join ''
        dataset = (Invoke-Adb @('shell', 'cat', '/sdcard/Download/InkNestBenchmark/dataset-version')) -join ''
        homeReturn = 'Explicit HOME intent to benchmark MainActivity; not a physical HOME key press.'
        scenarios = if ($Mode -eq 'Measure') { Get-MeasureScenarios } else { @() }
        iterations = if ($Mode -eq 'Profile') { 3 } elseif ($Mode -eq 'Measure') { Get-MeasureIterations } else { 1 }
        originalHome = $originalHome
        limitation = 'PSS/RSS checkpoints and emulator timings are sampled observations, not physical peak guarantees.'
    }
    $metadata | ConvertTo-Json | Set-Content (Join-Path $reportPath 'metadata.json')
}

function Invoke-AndExportPerformanceRun {
    param([hashtable]$Run)
    $instrumentationFailure = $null
    try {
        Invoke-PerformanceInstrumentation $Run.emulator $Run.remoteOutput $Run.testPackage $Run.reportPath
    } catch {
        $instrumentationFailure = $_
    }
    $outcome = @{
        emulator = $Run.emulator
        status = if ($instrumentationFailure) { 'failed' } else { 'passed' }
    }
    try {
        Export-PerformanceResults $Run.reportPath $Run.remoteOutput $Run.workspace $Run.testPackage
        Write-PerformanceMetadata $Run.reportPath $Run.variant $Run.originalHome $outcome
    } catch {
        if (-not $instrumentationFailure) { throw }
        Write-Warning "Partial report export failed: $_"
    }
    if ($instrumentationFailure) { throw $instrumentationFailure }
}

function Invoke-PerformanceRun {
    $workspace = Split-Path -Parent $PSScriptRoot
    $variant = if ($Mode -eq 'Profile') { 'profile' } else { 'benchmark' }
    $capitalVariant = (Get-Culture).TextInfo.ToTitleCase($variant)
    $targetPackage = 'dev.twotechy.inknest.benchmark'
    $testPackage = 'dev.twotechy.inknest.perf'
    $remoteOutput = "/sdcard/Android/media/$testPackage/benchmarks/$Label-$Mode"
    $reportPath = Join-Path $workspace "benchmark/build/reports/$Label-$Mode"
    New-Item -ItemType Directory -Force -Path $reportPath | Out-Null

    $originalHome = $null
    $changedHome = $false
    Push-Location $workspace
    try {
        Build-PerformanceApks $capitalVariant
        Invoke-Adb @('install', '-r', "app/build/outputs/apk/$variant/app-$variant.apk")
        Invoke-Adb @('install', '-r', "benchmark/build/outputs/apk/$variant/benchmark-$variant.apk")
        $emulator = ((Invoke-Adb @('shell', 'getprop', 'ro.kernel.qemu')) -join '').Trim() -eq '1'
        if ($emulator -and -not $AllowEmulator) {
            throw 'Pass -AllowEmulator for functional measurements; these do not certify physical performance.'
        }
        $originalHome = ((Invoke-Adb @('shell', 'cmd', 'role', 'get-role-holders', 'android.app.role.HOME')) -join '').Trim()
        if ($emulator -and $originalHome -in @('dev.twotechy.inknest', $targetPackage)) {
            Invoke-Adb @('shell', 'cmd', 'role', 'add-role-holder', 'android.app.role.HOME', 'com.google.android.apps.nexuslauncher')
            $changedHome = $true
        }
        Invoke-Adb @('shell', 'mkdir', '-p', $remoteOutput)
        Invoke-AndExportPerformanceRun @{
            emulator = $emulator
            remoteOutput = $remoteOutput
            testPackage = $testPackage
            reportPath = $reportPath
            workspace = $workspace
            variant = $variant
            originalHome = $originalHome
        }
    } finally {
        if ($changedHome) {
            Invoke-Adb @('shell', 'cmd', 'role', 'add-role-holder', 'android.app.role.HOME', $originalHome) | Out-Null
        }
        Pop-Location
    }
}

Invoke-PerformanceRun
