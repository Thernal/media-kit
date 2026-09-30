package io.thernal.mediakit.media.api.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MediaTypesTest {
    @Test
    fun `a family pattern matches its members and nothing else`() {
        assertTrue(MimeType.matches(mimeType = "image/png", allowed = listOf(MimeType.Image.ALL)))
        assertFalse(MimeType.matches(mimeType = "video/mp4", allowed = listOf(MimeType.Image.ALL)))
    }

    @Test
    fun `an empty list or the wildcard allows everything`() {
        assertTrue(MimeType.matches(mimeType = "application/zip", allowed = emptyList()))
        assertTrue(MimeType.matches(mimeType = "application/zip", allowed = listOf(MimeType.ALL)))
    }

    @Test
    fun `an exact type matches regardless of case`() {
        assertTrue(MimeType.matches(mimeType = "Application/PDF", allowed = listOf(MimeType.Application.PDF)))
    }

    @Test
    fun `files are equal by name and content`() {
        val a = MediaFile(name = "a.jpg", bytes = byteArrayOf(1, 2), mimeType = MimeType.Image.JPEG)

        assertEquals(a, a.copy(bytes = byteArrayOf(1, 2), uri = "elsewhere"))
        assertNotEquals(a, a.copy(bytes = byteArrayOf(1, 3)))
    }

    @Test
    fun `the extension is the lower-case part after the last dot`() {
        assertEquals("jpeg", MediaFile(name = "My.Photo.JPEG", bytes = ByteArray(0), mimeType = "").extension)
        assertEquals("", MediaFile(name = "README", bytes = ByteArray(0), mimeType = "").extension)
    }

    @Test
    fun `crop options refuse an impossible quality or size`() {
        assertFailsWith<IllegalArgumentException> { CropOptions(jpegQuality = 101) }
        assertFailsWith<IllegalArgumentException> { CropOptions(maxOutputSize = 0) }
    }

    @Test
    fun `a ratio is labelled by its two numbers`() {
        assertEquals("4:5", AspectRatio.R4_5.label)
        assertEquals(0.8f, AspectRatio.R4_5.value)
    }
}
