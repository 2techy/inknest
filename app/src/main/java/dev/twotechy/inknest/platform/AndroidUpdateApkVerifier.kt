package dev.twotechy.inknest.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dev.twotechy.inknest.model.AppRelease
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidUpdateApkVerifier(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UpdateApkVerifier {
    @Suppress("DEPRECATION")
    override suspend fun verify(file: File, release: AppRelease) = withContext(dispatcher) {
        val manager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val candidate = manager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw UpdateVerificationException()
        val installed = manager.getPackageInfo(context.packageName, flags)
        if (candidate.packageName != context.packageName ||
            candidate.versionName != release.version ||
            PackageInfoCompat.getLongVersionCode(candidate) <=
            PackageInfoCompat.getLongVersionCode(installed) ||
            (candidate.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE) > Build.VERSION.SDK_INT ||
            !signaturesCompatible(installed, candidate)
        ) {
            throw UpdateVerificationException()
        }
    }

    @Suppress("DEPRECATION")
    private fun signaturesCompatible(installed: PackageInfo, candidate: PackageInfo): Boolean {
        if (Build.VERSION.SDK_INT < 28) {
            val current = installed.signatures?.toSet().orEmpty()
            return current.isNotEmpty() && current == candidate.signatures?.toSet()
        }
        val current = installed.signingInfo ?: return false
        val next = candidate.signingInfo ?: return false
        val signers = current.apkContentsSigners.toSet()
        if (signers.isEmpty()) return false
        return if (current.hasMultipleSigners() || next.hasMultipleSigners()) {
            signers == next.apkContentsSigners.toSet()
        } else {
            next.signingCertificateHistory?.toSet().orEmpty().containsAll(signers)
        }
    }
}
