package com.example.tabidachi.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: String): TripEntity?

    @Query("SELECT * FROM trips")
    suspend fun getAll(): List<TripEntity>

    @Upsert
    suspend fun upsertAll(trips: List<TripEntity>)

    @Upsert
    suspend fun upsert(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
