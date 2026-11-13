package dev.twotechy.inknest.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateInstallerTest {
    @Test fun permissionReturnDoesNotInstallUntilUserRequestsAgain() {
        val platform = FakeInstallPlatform()
        val installer = AppUpdateInstaller(platform)
        assertEquals(UpdateInstallResult.PermissionRequired, installer.open("update.apk"))
        assertEquals(1, platform.permissionRequests)
        assertEquals(0, platform.installs)
        platform.allowed = true
        assertEquals(UpdateInstallResult.Opened, installer.open("update.apk"))
        assertEquals(1, platform.installs)
        // Android cancellation does not imply success or prevent a subsequent explicit attempt.
        assertEquals(UpdateInstallResult.Opened, installer.open("update.apk"))
        assertEquals(2, platform.installs)
    }

    @Test fun invalidTokensAndSecurityFailuresDoNotDispatchInstaller() {
        val platform = FakeInstallPlatform()
        val installer = AppUpdateInstaller(platform)
        assertEquals(UpdateInstallResult.Unavailable, installer.open("../private"))
        assertEquals(0, platform.permissionRequests)
        platform.denied = true
        assertEquals(UpdateInstallResult.Unavailable, installer.open("update.apk"))
        assertEquals(0, platform.installs)
    }

    private class FakeInstallPlatform : UpdateInstallPlatform {
        var allowed = false
        var denied = false
        var permissionRequests = 0
        var installs = 0
        override fun canInstall(): Boolean = allowed
        override fun requestPermission() {
            if (denied) throw SecurityException()
            permissionRequests++
        }
        override fun install(fileName: String) {
            installs++
        }
    }
}
