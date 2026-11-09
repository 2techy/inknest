package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ThirdPartyLicense

internal fun interface AcknowledgementsRepository {
    suspend fun read(license: ThirdPartyLicense): String
}
