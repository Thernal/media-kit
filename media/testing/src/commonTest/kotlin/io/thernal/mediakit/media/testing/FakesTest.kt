package io.thernal.mediakit.media.testing

import io.thernal.mediakit.media.api.domain.ImageSource
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.presentation.FilePickerOptions
import io.thernal.mediakit.media.api.presentation.ImagePickerOptions
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakesTest {
    @Test
    fun `a pick is recorded and answered`() {
        val image = testImage()
        val fake = FakeMediaPickerProvider(answer = { PickResult.Picked(listOf(image)) })
        var received: PickResult? = null

        fake.imagePicker(
            options = ImagePickerOptions(maxItems = 3),
            onResult = { received = it },
        ).pick(ImageSource.Gallery)

        assertEquals(
            listOf<PickRequest>(PickRequest.Image(ImageSource.Gallery, ImagePickerOptions(maxItems = 3))),
            fake.requests,
        )
        assertEquals(PickResult.Picked(listOf(image)), received)
    }

    @Test
    fun `a file pick is cancelled by default`() {
        val fake = FakeMediaPickerProvider()
        var received: PickResult? = null

        fake.filePicker(options = FilePickerOptions(), onResult = { received = it }).pick()

        assertEquals(PickResult.Cancelled, received)
        assertEquals(listOf<PickRequest>(PickRequest.File(FilePickerOptions())), fake.requests)
    }

    @Test
    fun `the camera permission grants as told and counts requests`() {
        runTest {
            val permission = FakeCameraPermission(isGranted = false, willGrantOnRequest = false)

            assertFalse(permission.access.request())
            permission.willGrantOnRequest = true
            assertTrue(permission.access.request())
            assertEquals(2, permission.requests)
        }
    }
}
