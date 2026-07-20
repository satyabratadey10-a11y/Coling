package com.example.coling.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ColingDatabase.getDatabase(application)
    private val projectDao = database.projectDao()
    private val timelineClipDao = database.timelineClipDao()
    private val colorNodeDao = database.colorNodeDao()
    private val mediaAssetDao = database.mediaAssetDao()

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    private val _mediaAssets = MutableStateFlow<List<MediaAssetEntity>>(emptyList())
    val mediaAssets: StateFlow<List<MediaAssetEntity>> = _mediaAssets.asStateFlow()

    private val _timelineClips = MutableStateFlow<List<TimelineClipEntity>>(emptyList())
    val timelineClips: StateFlow<List<TimelineClipEntity>> = _timelineClips.asStateFlow()

    private val _colorNodes = MutableStateFlow<List<ColorNodeEntity>>(emptyList())
    val colorNodes: StateFlow<List<ColorNodeEntity>> = _colorNodes.asStateFlow()

    init {
        // Initialize default project on startup
        viewModelScope.launch {
            val projects = projectDao.getAllProjects()
            val project = if (projects.isEmpty()) {
                val newProject = ProjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Demo Project",
                    width = 1920,
                    height = 1080,
                    fps = 30.0f,
                    creationTime = System.currentTimeMillis(),
                    lastModifiedTime = System.currentTimeMillis()
                )
                projectDao.insertProject(newProject)
                newProject
            } else {
                projects.first()
            }
            _activeProject.value = project
            loadProjectData(project.id)
        }
    }

    private suspend fun loadProjectData(projectId: String) {
        _mediaAssets.value = mediaAssetDao.getAssetsForProject(projectId)
        _timelineClips.value = timelineClipDao.getClipsForProject(projectId)
        _colorNodes.value = colorNodeDao.getNodesForProject(projectId)

        // Seed default timeline clips & color nodes if empty
        if (_timelineClips.value.isEmpty()) {
            val defaultClips = listOf(
                TimelineClipEntity(UUID.randomUUID().toString(), projectId, "Intro Sunset.mp4", "VIDEO", 0, 180, "#38BDF8"),
                TimelineClipEntity(UUID.randomUUID().toString(), projectId, "Landscape Grade.mp4", "VIDEO", 180, 270, "#0EA5E9"),
                TimelineClipEntity(UUID.randomUUID().toString(), projectId, "Ambient Mix.wav", "AUDIO", 0, 450, "#A78BFA"),
                TimelineClipEntity(UUID.randomUUID().toString(), projectId, "VIBRANT SUNSET", "TITLE", 45, 120, "#F59E0B")
            )
            timelineClipDao.insertClips(defaultClips)
            _timelineClips.value = defaultClips
        }

        if (_colorNodes.value.isEmpty()) {
            val defaultNodes = listOf(
                ColorNodeEntity("node1", projectId, 0, "Primaries (CDL)", "SERIAL", true, 0f, 0f, 0f, 0f, 0f, 0f),
                ColorNodeEntity("node2", projectId, 1, "Curves", "SERIAL", true, 0f, 0f, 0f, 0f, 0f, 0f),
                ColorNodeEntity("node3", projectId, 2, "Custom LUT", "SERIAL", true, 0f, 0f, 0f, 0f, 0f, 0f)
            )
            colorNodeDao.insertNodes(defaultNodes)
            _colorNodes.value = defaultNodes
        } else {
            val node3 = _colorNodes.value.find { it.id == "node3" }
            if (node3 != null && (!node3.isEnabled || node3.name != "Custom LUT")) {
                val updatedNode3 = node3.copy(name = "Custom LUT", isEnabled = true)
                colorNodeDao.insertNodes(listOf(updatedNode3))
                _colorNodes.value = _colorNodes.value.map { if (it.id == "node3") updatedNode3 else it }
            }
        }
    }

    fun importMedia(fileName: String, filePath: String, format: String, duration: String, size: String, videoCodec: String, audioCodec: String, resolution: String) {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            val newAsset = MediaAssetEntity(
                id = UUID.randomUUID().toString(),
                projectId = project.id,
                fileName = fileName,
                filePath = filePath,
                format = format,
                duration = duration,
                size = size,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                resolution = resolution
            )
            mediaAssetDao.insertAsset(newAsset)
            _mediaAssets.value = _mediaAssets.value + newAsset
        }
    }

    fun deleteMedia(assetId: String) {
        viewModelScope.launch {
            mediaAssetDao.deleteAsset(assetId)
            _mediaAssets.value = _mediaAssets.value.filter { it.id != assetId }
        }
    }

    fun addClipToTimeline(name: String, type: String, durationFrames: Int, colorHex: String) {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            val newClip = TimelineClipEntity(
                id = UUID.randomUUID().toString(),
                projectId = project.id,
                name = name,
                type = type,
                startFrame = 0,
                durationFrames = durationFrames,
                colorHex = colorHex
            )
            timelineClipDao.insertClips(listOf(newClip))
            _timelineClips.value = _timelineClips.value + newClip
        }
    }

    fun deleteClipFromTimeline(clipId: String) {
        viewModelScope.launch {
            timelineClipDao.deleteClip(clipId)
            _timelineClips.value = _timelineClips.value.filter { it.id != clipId }
        }
    }

    fun updateClipPosition(clipId: String, startFrame: Int, durationFrames: Int) {
        val currentClip = _timelineClips.value.find { it.id == clipId } ?: return
        viewModelScope.launch {
            val updated = currentClip.copy(startFrame = startFrame, durationFrames = durationFrames)
            timelineClipDao.insertClips(listOf(updated))
            _timelineClips.value = _timelineClips.value.map { if (it.id == clipId) updated else it }
        }
    }

    fun updateColorNodeOffset(nodeId: String, liftX: Float, liftY: Float, gammaX: Float, gammaY: Float, gainX: Float, gainY: Float) {
        val currentNode = _colorNodes.value.find { it.id == nodeId } ?: return
        viewModelScope.launch {
            val updated = currentNode.copy(
                liftX = liftX, liftY = liftY,
                gammaX = gammaX, gammaY = gammaY,
                gainX = gainX, gainY = gainY
            )
            colorNodeDao.insertNodes(listOf(updated))
            _colorNodes.value = _colorNodes.value.map { if (it.id == nodeId) updated else it }
        }
    }
}
