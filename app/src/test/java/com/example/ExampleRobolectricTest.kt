package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ExportSettings
import com.example.domain.model.ItemType
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.TimelineItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProjectRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vistara Edit", appName)
    }

    @Test
    fun `save and load project from repository`() = runBlocking {
        val projectId = UUID.randomUUID().toString()
        val assetId = UUID.randomUUID().toString()

        val asset = MediaAsset(
            id = assetId,
            uriString = "content://media/external/video/media/1",
            fileName = "sample_clip.mp4",
            mediaType = MediaType.VIDEO,
            durationMs = 6000L,
            width = 1920,
            height = 1080
        )

        val item = TimelineItem(
            id = UUID.randomUUID().toString(),
            trackId = "track_main_video",
            assetId = assetId,
            type = ItemType.VIDEO,
            timelineStartMs = 0L,
            durationMs = 6000L,
            sourceStartMs = 0L,
            sourceDurationMs = 6000L,
            speed = 1.0f,
            transform = ClipTransform(rotationDegrees = 90)
        )

        val project = Project(
            id = projectId,
            name = "Vistara Summer Reel",
            canvasRatio = CanvasAspectRatio.RATIO_9_16,
            exportSettings = ExportSettings(),
            items = listOf(item),
            assets = listOf(asset)
        )

        repository.saveProject(project)

        val loaded = repository.getProject(projectId)
        assertNotNull(loaded)
        assertEquals("Vistara Summer Reel", loaded?.name)
        assertEquals(CanvasAspectRatio.RATIO_9_16, loaded?.canvasRatio)
        assertEquals(1, loaded?.assets?.size)
        assertEquals(1, loaded?.items?.size)
        assertEquals(90, loaded?.items?.first()?.transform?.rotationDegrees)
        assertEquals(6000L, loaded?.totalDurationMs)
    }

    @Test
    fun `duplicate and delete project`() = runBlocking {
        val projectId = UUID.randomUUID().toString()
        val project = Project(
            id = projectId,
            name = "Reel 1"
        )
        repository.saveProject(project)

        val duplicated = repository.duplicateProject(projectId)
        assertNotNull(duplicated)
        assertEquals("Reel 1 (Copy)", duplicated?.name)

        repository.deleteProject(projectId)
        val loadedOriginal = repository.getProject(projectId)
        assertNull(loadedOriginal)

        val loadedCopy = repository.getProject(duplicated!!.id)
        assertNotNull(loadedCopy)
    }
}
