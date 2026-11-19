package dev.twotechy.inknest.data.repository.sources

import java.io.IOException

internal class BookFolderConflictException : IOException("Folder overlaps an existing book folder")
