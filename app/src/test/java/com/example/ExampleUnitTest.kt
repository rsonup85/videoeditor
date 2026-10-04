package com.example

import com.example.common.TimeUtils
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.EditorFont
import com.example.domain.model.ItemType
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TimelineItem
import com.example.domain.model.TransitionConfig
import com.example.domain.model.TransitionType
import com.example.editor.undo.UndoRedoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTimeUtilsFormatting() {
        assertEquals("00:05", TimeUtils.formatTimeShort(5000L))
        assertEquals("01:23", TimeUtils.formatTimeShort(83000L))
        assertEquals("00:01.24", TimeUtils.formatTimeDetailed(1240L))
        assertEquals("00:00.00", TimeUtils.formatTimeDetailed(0L))
        assertEquals("45s", TimeUtils.formatDurationHuman(45000L))
        assertEquals("1m 15s", TimeUtils.formatDurationHuman(75000L))
    }

    @Test
    fun testProjectDurationCalculation() {
        val clip1 = TimelineItem(
            id = "c1",
            trackId = "track_video_1",
            type = ItemType.VIDEO,
            timelineStartMs = 0L,
            durationMs = 5000L,
            sourceStartMs = 0L,
            sourceDurationMs = 5000L
        )
        val clip2 = TimelineItem(
            id = "c2",
            trackId = "track_video_1",
            type = ItemType.VIDEO,
            timelineStartMs = 5000L,
            durationMs = 3000L,
            sourceStartMs = 0L,
            sourceDurationMs = 3000L
        )

        val project = Project(
            id = "p1",
            name = "Test Project",
            items = listOf(clip1, clip2)
        )

        assertEquals(8000L, project.totalDurationMs)
        assertEquals(2, project.videoClips.size)
    }

    @Test
    fun testCustomSpeedCalculation() {
        val sourceDuration = 10000L
        val customSpeed = 0.83f
        val calculatedDuration = (sourceDuration / customSpeed).toLong()

        val clip = TimelineItem(
            id = "speed_clip",
            trackId = "track_video_1",
            type = ItemType.VIDEO,
            sourceStartMs = 0L,
            sourceDurationMs = sourceDuration,
            speed = customSpeed,
            durationMs = calculatedDuration
        )

        assertEquals(0.83f, clip.speed)
        assertEquals(12048L, clip.durationMs)
    }

    @Test
    fun testTransitionsAndFonts() {
        val transition = TransitionConfig(
            type = TransitionType.FADE,
            durationMs = 800L
        )
        assertEquals(TransitionType.FADE, transition.type)
        assertEquals(800L, transition.durationMs)

        assertEquals(EditorFont.SANS, EditorFont.fromId("sans"))
        assertEquals(EditorFont.SERIF, EditorFont.fromId("serif"))
        assertEquals(EditorFont.MONO, EditorFont.fromId("mono"))
        assertEquals(EditorFont.CURSIVE, EditorFont.fromId("cursive"))
        assertEquals(EditorFont.BOLD, EditorFont.fromId("bold"))
    }

    @Test
    fun testTextLayerProperties() {
        val textProps = TextLayerProperties(
            text = "Vistara Cinematic",
            fontFamily = "serif",
            fontSizeSp = 32f,
            colorHex = "#38BDF8",
            backgroundColorHex = "#99000000",
            hasShadow = true
        )
        assertEquals("Vistara Cinematic", textProps.text)
        assertEquals("serif", textProps.fontFamily)
        assertEquals(32f, textProps.fontSizeSp)
        assertEquals("#38BDF8", textProps.colorHex)
        assertTrue(textProps.hasShadow)
    }

    @Test
    fun testUndoRedoStackOperations() {
        val undoRedo = UndoRedoManager()
        val p1 = Project(id = "1", name = "State 1")
        val p2 = Project(id = "1", name = "State 2")
        val p3 = Project(id = "1", name = "State 3")

        assertFalse(undoRedo.canUndo())
        assertFalse(undoRedo.canRedo())

        undoRedo.pushState(p1)
        assertTrue(undoRedo.canUndo())

        undoRedo.pushState(p2)
        val undone = undoRedo.undo(p3)
        assertEquals("State 2", undone?.name)
        assertTrue(undoRedo.canRedo())

        val redone = undoRedo.redo(undone!!)
        assertEquals("State 3", redone?.name)
    }

    @Test
    fun testCanvasAspectRatios() {
        assertEquals("9:16", CanvasAspectRatio.RATIO_9_16.label)
        assertEquals(9f / 16f, CanvasAspectRatio.RATIO_9_16.ratio)
        assertEquals("16:9", CanvasAspectRatio.RATIO_16_9.label)
        assertEquals(16f / 9f, CanvasAspectRatio.RATIO_16_9.ratio)
        assertEquals(CanvasAspectRatio.RATIO_1_1, CanvasAspectRatio.fromName("RATIO_1_1"))
    }
}
