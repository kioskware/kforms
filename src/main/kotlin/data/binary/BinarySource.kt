@file:Suppress("NOTHING_TO_INLINE")

package data.binary

import common.parseBase64
import java.io.InputStream

/**
 * Represents a source of binary data.
 * Provides access to the MIME type, size, and an input stream to read the binary data.
 */
interface BinarySource {

    /**
     * The MIME type of the binary data.
     */
    val mimeType: MimeType

    /**
     * The size of the binary data in bytes or negative if unknown.
     */
    val size: Long
        get() = -1L

    /**
     * The input stream to read the binary data.
     * The stream should be closed after use.
     * Each call to this property should return a new instance of [InputStream].
     */
    val outputStream: InputStream

}

/**
 * Checks if the size of the binary data is known.
 * A size is considered known if it is non-negative.
 * @receiver the binary source to check.
 * @return true if the size is known, false otherwise.
 */
val BinarySource.isSizeKnown: Boolean
    get() = this.size >= 0

/**
 * A binary source that provides binary data from a byte array.
 * @property mimeType The MIME type of the binary data.
 * @property data The byte array containing the binary data.
 */
class ArrayBinarySource(
    override val mimeType: MimeType,
    private val data: ByteArray
) : BinarySource {

    companion object {
        /**
         * Creates an [ArrayBinarySource] from a base64 encoded string.
         * @param base64 The base64 encoded string representing the binary data.
         * @return An [ArrayBinarySource] if the base64 string is valid, null otherwise.
         */
        inline fun fromBase64(base64: String): ArrayBinarySource? = parseBase64(base64)
    }

    private val hashCode: Int by lazy {
        var result = mimeType.hashCode()
        result = 31 * result + data.contentHashCode()
        result
    }
    override val size: Long
        get() = data.size.toLong()

    override val outputStream: InputStream
        get() = data.inputStream()

    override fun toString(): String {
        return "ArrayBinarySource(mimeType=$mimeType, size=$size)"
    }

    override fun hashCode() = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayBinarySource) return false
        if (mimeType != other.mimeType) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }
}