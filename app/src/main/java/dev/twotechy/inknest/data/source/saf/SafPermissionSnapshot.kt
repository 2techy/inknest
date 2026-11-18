package dev.twotechy.inknest.data.source.saf

import android.content.ContentResolver
import android.net.Uri
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext

internal class SafPermissionSnapshot(readableTrees: Set<String>) :
    AbstractCoroutineContextElement(Key) {
    private val trees = readableTrees.toSet()

    fun grants(tree: Uri): Boolean = tree.toString() in trees

    companion object Key : CoroutineContext.Key<SafPermissionSnapshot>
}

internal fun ContentResolver.readableTreeUris(): Set<String> = persistedUriPermissions
    .filter { it.isReadPermission }.map { it.uri.toString() }.toSet()

internal suspend fun hasPersistedReadGrant(tree: Uri, load: () -> Set<String>): Boolean =
    currentCoroutineContext()[SafPermissionSnapshot]?.grants(tree) ?: (tree.toString() in load())
