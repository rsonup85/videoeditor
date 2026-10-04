package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TimelineItemEntity

@Dao
interface TimelineItemDao {
    @Query("SELECT * FROM timeline_items WHERE projectId = :projectId ORDER BY timelineStartMs ASC")
    suspend fun getItemsForProject(projectId: String): List<TimelineItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TimelineItemEntity>)

    @Query("DELETE FROM timeline_items WHERE projectId = :projectId")
    suspend fun deleteItemsForProject(projectId: String)
}
