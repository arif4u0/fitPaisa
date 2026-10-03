package com.fitpaisa.app.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Placeholder entity for M0. M1 replaces this with the real step-cache tables
 * (daily step snapshots, sync queue) that back the Health Connect pipeline.
 * The server ledger in Postgres remains the source of truth — Room is a
 * read-only cache and offline queue only.
 */
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val key: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface PendingSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PendingSyncEntity)

    @Query("SELECT * FROM pending_sync ORDER BY createdAt DESC")
    suspend fun getAll(): List<PendingSyncEntity>

    @Query("DELETE FROM pending_sync WHERE `key` = :key")
    suspend fun delete(key: String)
}
