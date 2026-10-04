package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MediaAssetEntity

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets WHERE projectId = :projectId")
    suspend fun getAssetsForProject(projectId: String): List<MediaAssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<MediaAssetEntity>)

    @Query("DELETE FROM media_assets WHERE projectId = :projectId")
    suspend fun deleteAssetsForProject(projectId: String)
}
