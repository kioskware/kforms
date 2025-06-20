package kioskware.kforms.data.binary

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun binarySourceFromBase64(input: String): BinarySource? {
    val base64Data: String
    val mimeType: String

    if (input.startsWith("data:")) {
        val base64Index = input.indexOf(";base64,")
        if (base64Index == -1) return null // not valid data URL format

        mimeType = input.substring(5, base64Index).ifBlank { "*/*" }
        base64Data = input.substring(base64Index + 8)
    } else {
        // Plain Base64, use default MIME type
        mimeType = "*/*"
        base64Data = input
    }
    return try {
        ArrayBinarySource(
            mimeType = MimeType.parse(mimeType),
            data = Base64.decode(base64Data)
        )
    } catch (e: IllegalArgumentException) {
        null // Invalid Base64
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun BinarySource.asBase64String() : String {
    return "data:$mimeType;base64," + inputStream.use { inputStream ->
        Base64.encode(inputStream.readAllBytes())
    }
}


