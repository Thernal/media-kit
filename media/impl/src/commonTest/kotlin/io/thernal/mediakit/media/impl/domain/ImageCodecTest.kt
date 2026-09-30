package io.thernal.mediakit.media.impl.domain

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.domain.Rejection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private class FakeCodec(
    private val jpeg: ByteArray? = byteArrayOf(9, 9),
) : ImageCodec {
    var normalized = 0

    override suspend fun transcodeToJpeg(bytes: ByteArray): ByteArray? {
        return jpeg
    }

    override suspend fun normalizeOrientation(bytes: ByteArray): ByteArray {
        normalized++
        return bytes
    }
}

private fun file(
    name: String,
    mimeType: String,
    size: Int = 4,
): MediaFile {
    return MediaFile(name = name, bytes = ByteArray(size), mimeType = mimeType, uri = "content://$name")
}

class ImagePipelineTest {
    @Test
    fun `a HEIC becomes a JPEG without the original uri`() {
        runTest {
            val item = prepareImage(file(name = "photo.heic", mimeType = MimeType.Image.HEIC), null, FakeCodec())

            val accepted = assertIs<PickedItem.Accepted>(item)
            assertEquals("photo.jpg", accepted.file.name)
            assertEquals(MimeType.Image.JPEG, accepted.file.mimeType)
            assertEquals(2L, accepted.file.size)
            assertNull(accepted.file.uri)
        }
    }

    @Test
    fun `a HEIC the device cannot decode is refused as unsupported`() {
        runTest {
            val item = prepareImage(
                file(name = "photo.heif", mimeType = MimeType.Image.HEIF),
                null,
                FakeCodec(jpeg = null),
            )

            assertEquals(PickedItem.Refused(Rejection.UnsupportedType("photo.heif", MimeType.Image.HEIF)), item)
        }
    }

    @Test
    fun `only a JPEG is redrawn upright`() {
        runTest {
            val codec = FakeCodec()

            prepareImage(file(name = "a.jpg", mimeType = MimeType.Image.JPEG), null, codec)
            prepareImage(file(name = "b.png", mimeType = MimeType.Image.PNG), null, codec)

            assertEquals(1, codec.normalized)
        }
    }

    @Test
    fun `an image the cropper cannot decode is refused at pick time`() {
        runTest {
            val item = prepareImage(file(name = "logo.svg", mimeType = "image/svg+xml"), null, FakeCodec())

            assertEquals(PickedItem.Refused(Rejection.UnsupportedType("logo.svg", "image/svg+xml")), item)
        }
    }

    @Test
    fun `the size limit applies to the final bytes`() {
        runTest {
            val item = prepareImage(file(name = "big.png", mimeType = MimeType.Image.PNG, size = 10), 8, FakeCodec())

            assertEquals(
                PickedItem.Refused(Rejection.TooLarge(name = "big.png", limitBytes = 8, actualBytes = 10)),
                item,
            )
        }
    }
}

class PickChecksTest {
    @Test
    fun `type is checked before size`() {
        val rejection = checkFile(
            name = "a.pdf",
            mimeType = MimeType.Application.PDF,
            size = 100,
            allowedMimeTypes = listOf(MimeType.Image.ALL),
            maxFileSizeBytes = 1,
        )

        assertEquals(Rejection.UnsupportedType(name = "a.pdf", mimeType = MimeType.Application.PDF), rejection)
    }

    @Test
    fun `an unknown size passes until the bytes are read`() {
        assertNull(
            checkFile(
                name = "a",
                mimeType = MimeType.Image.PNG,
                size = null,
                allowedMimeTypes = emptyList(),
                maxFileSizeBytes = 1,
            ),
        )
    }

    @Test
    fun `extensions map to MIME types and unknown ones to octet-stream`() {
        assertEquals(MimeType.Image.JPEG, mimeTypeForExtension("JPEG"))
        assertEquals(MimeType.Video.QUICKTIME, mimeTypeForExtension("mov"))
        assertEquals(MimeType.Application.OCTET_STREAM, mimeTypeForExtension("xyz"))
    }

    @Test
    fun `accepted files arrive with the rejected ones beside them`() {
        val good = file(name = "a.jpg", mimeType = MimeType.Image.JPEG)
        val bad = Rejection.Unreadable("b.jpg")

        val result = listOf(PickedItem.Accepted(good), PickedItem.Refused(bad)).toPickResult()

        assertEquals(PickResult.Picked(files = listOf(good), rejected = listOf(bad)), result)
    }

    @Test
    fun `nothing accepted is rejected and nothing at all is a cancel`() {
        val bad = Rejection.Unreadable("b.jpg")

        assertEquals(PickResult.Rejected(listOf(bad)), listOf(PickedItem.Refused(bad)).toPickResult())
        assertEquals(PickResult.Cancelled, emptyList<PickedItem>().toPickResult())
    }
}
