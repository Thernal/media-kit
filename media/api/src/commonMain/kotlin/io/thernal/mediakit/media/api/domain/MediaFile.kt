package io.thernal.mediakit.media.api.domain

import androidx.compose.runtime.Immutable

/**
 * A picked or produced file, read into memory. [uri] points at where it came from (a content URI, a
 * file URL) when the platform gave one; [width] and [height] are known for images the kit produced.
 *
 * Equality is by [name] and content, so a copy with the same bytes is the same file.
 */
@Immutable
class MediaFile(
    val name: String,
    val bytes: ByteArray,
    val mimeType: String,
    val uri: String? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    val size: Long
        get() = bytes.size.toLong()

    /** The part of [name] after the last dot, lower-case; empty when there is none. */
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    fun copy(
        name: String = this.name,
        bytes: ByteArray = this.bytes,
        mimeType: String = this.mimeType,
        uri: String? = this.uri,
        width: Int? = this.width,
        height: Int? = this.height,
    ): MediaFile {
        return MediaFile(
            name = name,
            bytes = bytes,
            mimeType = mimeType,
            uri = uri,
            width = width,
            height = height,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is MediaFile && name == other.name && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return 31 * name.hashCode() + bytes.contentHashCode()
    }

    override fun toString(): String {
        return "MediaFile(name=$name, size=$size, mimeType=$mimeType, hasUri=${uri != null})"
    }
}
