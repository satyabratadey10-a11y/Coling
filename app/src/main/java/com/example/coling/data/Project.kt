package com.example.coling.data

import androidx.room.*

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val fps: Float,
    val creationTime: Long,
    val lastModifiedTime: Long
)

@Entity(
    tableName = "timeline_clips",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimelineClipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val type: String, // VIDEO, AUDIO, TITLE
    val startFrame: Int,
    val durationFrames: Int,
    val colorHex: String
)

@Entity(
    tableName = "color_nodes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ColorNodeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val nodeIndex: Int,
    val name: String,
    val type: String, // SERIAL, PARALLEL
    val isEnabled: Boolean,
    // Primaries wheel offsets
    val liftX: Float,
    val liftY: Float,
    val gammaX: Float,
    val gammaY: Float,
    val gainX: Float,
    val gainY: Float
)

@Entity(
    tableName = "media_assets",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val fileName: String,
    val filePath: String,
    val format: String,
    val duration: String,
    val size: String,
    val videoCodec: String,
    val audioCodec: String,
    val resolution: String
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModifiedTime DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface TimelineClipDao {
    @Query("SELECT * FROM timeline_clips WHERE projectId = :projectId ORDER BY startFrame ASC")
    suspend fun getClipsForProject(projectId: String): List<TimelineClipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<TimelineClipEntity>)

    @Query("DELETE FROM timeline_clips WHERE id = :clipId")
    suspend fun deleteClip(clipId: String)
}

@Dao
interface ColorNodeDao {
    @Query("SELECT * FROM color_nodes WHERE projectId = :projectId ORDER BY nodeIndex ASC")
    suspend fun getNodesForProject(projectId: String): List<ColorNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<ColorNodeEntity>)
}

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets WHERE projectId = :projectId")
    suspend fun getAssetsForProject(projectId: String): List<MediaAssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: MediaAssetEntity)

    @Query("DELETE FROM media_assets WHERE id = :assetId")
    suspend fun deleteAsset(assetId: String)
}

