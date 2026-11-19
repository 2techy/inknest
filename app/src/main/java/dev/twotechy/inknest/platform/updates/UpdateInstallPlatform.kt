package dev.twotechy.inknest.platform.updates

internal interface UpdateInstallPlatform {
    fun canInstall(): Boolean
    fun requestPermission()
    fun install(fileName: String)
}
