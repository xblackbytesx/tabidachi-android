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
                dao.deleteNotIn(result.data.map { it.id })
                _syncStatus.value = SyncStatus.Idle
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> {
                _syncStatus.value = SyncStatus.Error(result.message)
                ApiResult.Error(result.message, result.code)
            }
        }
    }

    suspend fun refreshTrip(id: String): ApiResult<Unit> {
        return when (val result = api.getTrip(id)) {
            is ApiResult.Success -> {
                val detail = result.data
                val detailJson = json.encodeToString(ApiTripData.serializer(), detail.data)
                val now = System.currentTimeMillis()
                val entity = detail.toEntity(now, detailJson)
                dao.upsert(entity)
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> {
                ApiResult.Error(result.message, result.code)
            }
        }
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
            detailJson = existingDetailJson, // preserve cached detail from previous fetch
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
        )
    }
}
