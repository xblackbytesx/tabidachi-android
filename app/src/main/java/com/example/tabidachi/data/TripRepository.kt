package com.example.tabidachi.data

import com.example.tabidachi.network.ApiResult
import com.example.tabidachi.network.ApiTripData
import com.example.tabidachi.network.ApiTripDetail
import com.example.tabidachi.network.ApiTripSummary
import com.example.tabidachi.network.AppJson
import com.example.tabidachi.network.TabidachiApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

data class TripSummary(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val homeLocation: String?,
    val timezone: String?,
    val coverColor: String?,
    val coverImageUrl: String?,
    val coverImageCredit: String?,
    val legCount: Int,
    val updatedAt: String,
    val lastSyncedAt: Long,
    val hasDetail: Boolean,
    val isShared: Boolean = false,
    val sharedFromServerUrl: String? = null,
    val sharedToken: String? = null,
)

class TripRepository(
    private val api: TabidachiApi,
    private val dao: TripDao,
) {
    private val json = AppJson

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    fun observeTrips(): Flow<List<TripSummary>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toSummary() }
        }
    }

    fun observeTripDetail(id: String): Flow<Pair<TripSummary, ApiTripData>?> {
        return dao.observeById(id).map { entity ->
            if (entity == null) return@map null
            val detail = entity.detailJson?.let {
                try {
                    json.decodeFromString<ApiTripData>(it)
                } catch (_: Exception) {
                    null
                }
            }
            if (detail != null) {
                Pair(entity.toSummary(), detail)
            } else {
                null
            }
        }
    }

    suspend fun refreshTrips(): ApiResult<Unit> {
        _syncStatus.value = SyncStatus.Syncing
        return when (val result = api.listTrips()) {
            is ApiResult.Success -> {
                val now = System.currentTimeMillis()
                // Preserve existing detailJson when upserting from list endpoint
                val existingMap = dao.getAll().associateBy { it.id }
                val entities = result.data.map { summary ->
                    summary.toEntity(now, existingDetailJson = existingMap[summary.id]?.detailJson)
                }
                dao.upsertAll(entities)
                // Only delete owned trips not returned by the server; preserve shared/pinned trips
                dao.deleteOwnedNotIn(result.data.map { it.id })
                _syncStatus.value = SyncStatus.Idle
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> {
                _syncStatus.value = SyncStatus.Idle
                ApiResult.Error(result.message, result.code)
            }
        }
    }

    /** Refreshes a single trip. For shared/pinned trips uses the public share API; for owned trips uses the authenticated API. */
    suspend fun refreshTrip(id: String): ApiResult<Unit> {
        val entity = dao.getById(id)
        if (entity?.isShared == true) {
            val serverUrl = entity.sharedFromServerUrl ?: return ApiResult.Error("Missing server URL")
            val shareToken = entity.sharedToken ?: return ApiResult.Error("Missing share token")
            return when (val result = api.getSharedTrip(serverUrl, shareToken)) {
                is ApiResult.Success -> {
                    val detail = result.data
                    val detailJson = json.encodeToString(ApiTripData.serializer(), detail.data)
                    dao.upsert(detail.toSharedEntity(System.currentTimeMillis(), detailJson, serverUrl, shareToken))
                    ApiResult.Success(Unit)
                }
                is ApiResult.Error -> ApiResult.Error(result.message, result.code)
            }
        }
        return when (val result = api.getTrip(id)) {
            is ApiResult.Success -> {
                val detail = result.data
                val detailJson = json.encodeToString(ApiTripData.serializer(), detail.data)
                dao.upsert(detail.toEntity(System.currentTimeMillis(), detailJson))
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> ApiResult.Error(result.message, result.code)
        }
    }

    /** Re-fetches all pinned shared trips from their respective servers. Failures are silently ignored to preserve the offline cache. */
    suspend fun refreshSharedTrips() {
        val shared = dao.getSharedTrips()
        for (entity in shared) {
            val serverUrl = entity.sharedFromServerUrl ?: continue
            val shareToken = entity.sharedToken ?: continue
            when (val result = api.getSharedTrip(serverUrl, shareToken)) {
                is ApiResult.Success -> {
                    val detail = result.data
                    val detailJson = json.encodeToString(ApiTripData.serializer(), detail.data)
                    dao.upsert(detail.toSharedEntity(System.currentTimeMillis(), detailJson, serverUrl, shareToken))
                }
                is ApiResult.Error -> { /* Keep stale cache — offline access is the whole point */ }
            }
        }
    }

    /** Saves a shared trip to the local cache so it is available offline. */
    suspend fun pinSharedTrip(detail: ApiTripDetail, serverUrl: String, shareToken: String) {
        val detailJson = json.encodeToString(ApiTripData.serializer(), detail.data)
        dao.upsert(detail.toSharedEntity(System.currentTimeMillis(), detailJson, serverUrl, shareToken))
    }

    /** Removes a previously pinned shared trip from the local cache. */
    suspend fun removeSharedTrip(id: String) {
        dao.deleteSharedTrip(id)
    }

    suspend fun isSharedTripPinned(id: String): Boolean {
        return dao.getById(id)?.isShared == true
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    private fun ApiTripSummary.toEntity(syncedAt: Long, existingDetailJson: String? = null): TripEntity {
        return TripEntity(
            id = id,
            title = title,
            startDate = startDate,
            endDate = endDate,
            homeLocation = homeLocation,
            timezone = timezone,
            coverColor = coverColor,
            coverImageUrl = coverImageUrl,
            coverImageCredit = coverImageCredit,
            legCount = legCount,
            updatedAt = updatedAt,
            detailJson = existingDetailJson,
            lastSyncedAt = syncedAt,
        )
    }

    private fun ApiTripDetail.toEntity(syncedAt: Long, detailJson: String): TripEntity {
        return TripEntity(
            id = id,
            title = title,
            startDate = startDate,
            endDate = endDate,
            homeLocation = homeLocation,
            timezone = timezone,
            coverColor = coverColor,
            coverImageUrl = coverImageUrl,
            coverImageCredit = coverImageCredit,
            legCount = legCount,
            updatedAt = updatedAt,
            detailJson = detailJson,
            lastSyncedAt = syncedAt,
        )
    }

    private fun ApiTripDetail.toSharedEntity(syncedAt: Long, detailJson: String, serverUrl: String, shareToken: String): TripEntity {
        return TripEntity(
            id = id,
            title = title,
            startDate = startDate,
            endDate = endDate,
            homeLocation = homeLocation,
            timezone = timezone,
            coverColor = coverColor,
            coverImageUrl = coverImageUrl,
            coverImageCredit = coverImageCredit,
            legCount = legCount,
            updatedAt = updatedAt,
            detailJson = detailJson,
            lastSyncedAt = syncedAt,
            isShared = true,
            sharedFromServerUrl = serverUrl,
            sharedToken = shareToken,
        )
    }

    private fun TripEntity.toSummary(): TripSummary {
        return TripSummary(
            id = id,
            title = title,
            startDate = startDate,
            endDate = endDate,
            homeLocation = homeLocation,
            timezone = timezone,
            coverColor = coverColor,
            coverImageUrl = coverImageUrl,
            coverImageCredit = coverImageCredit,
            legCount = legCount,
            updatedAt = updatedAt,
            lastSyncedAt = lastSyncedAt,
            hasDetail = detailJson != null,
            isShared = isShared,
            sharedFromServerUrl = sharedFromServerUrl,
            sharedToken = sharedToken,
        )
    }
}
