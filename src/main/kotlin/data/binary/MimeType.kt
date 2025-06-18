package data.binary

/**
 * Represents a MIME type with primary type, subtype, and optional parameters.
 *
 * Examples:
 * - text/plain
 * - application/json; charset=utf-8
 * - image/jpeg
 * - multipart/form-data; boundary=something
 **/
data class MimeType(
    val primaryType: String,
    val subType: String,
    val parameters: Map<String, String> = emptyMap()
) {

    init {
        require(primaryType.isNotBlank()) { "Primary type cannot be blank" }
        require(subType.isNotBlank()) { "Sub type cannot be blank" }
        require(isValidToken(primaryType)) { "Invalid primary type: $primaryType" }
        require(isValidToken(subType)) { "Invalid sub type: $subType" }

        parameters.forEach { (name, value) ->
            require(name.isNotBlank()) { "Parameter name cannot be blank" }
            require(isValidToken(name)) { "Invalid parameter name: $name" }
        }
    }

    /**
     * Returns the base type without parameters (e.g., "text/plain")
     */
    val baseType: String
        get() = "$primaryType/$subType"

    /**
     * Returns the full MIME type string with parameters
     */
    override fun toString(): String {
        val base = baseType
        return if (parameters.isEmpty()) {
            base
        } else {
            val params = parameters.entries.joinToString("; ") { (name, value) ->
                if (needsQuoting(value)) {
                    "$name=\"${escapeQuoted(value)}\""
                } else {
                    "$name=$value"
                }
            }
            "$base; $params"
        }
    }

    /**
     * Checks if this MIME type matches another, ignoring parameters
     */
    fun matches(other: MimeType): Boolean {
        return primaryType.equals(other.primaryType, ignoreCase = true) &&
                subType.equals(other.subType, ignoreCase = true)
    }


    fun matchesPattern(pattern: String): Boolean {
        val patternMime = try {
            parse(pattern)
        } catch (e: Exception) {
            return false
        }

        val primaryMatches = patternMime.primaryType == "*" ||
                primaryType.equals(patternMime.primaryType, ignoreCase = true)
        val subMatches = patternMime.subType == "*" ||
                subType.equals(patternMime.subType, ignoreCase = true)

        return primaryMatches && subMatches
    }

    /**
     * Returns a new data.binary.MimeType with the specified parameter added or updated
     */
    fun withParameter(name: String, value: String): MimeType {
        require(name.isNotBlank()) { "Parameter name cannot be blank" }
        require(isValidToken(name)) { "Invalid parameter name: $name" }

        val newParams = parameters.toMutableMap()
        newParams[name.lowercase()] = value
        return copy(parameters = newParams)
    }

    /**
     * Returns a new data.binary.MimeType with the specified parameter removed
     */
    fun withoutParameter(name: String): MimeType {
        val newParams = parameters.toMutableMap()
        newParams.remove(name.lowercase())
        return copy(parameters = newParams)
    }

    /**
     * Gets a parameter value by name (case-insensitive)
     */
    fun getParameter(name: String): String? {
        return parameters[name.lowercase()]
    }

    /**
     * Gets the charset parameter if present
     */
    val charset: String?
        get() = getParameter("charset")

    /**
     * Gets the boundary parameter if present (common in multipart types)
     */
    val boundary: String?
        get() = getParameter("boundary")

    /**
     * Checks if this is a text type
     */
    val isText: Boolean
        get() = primaryType.equals("text", ignoreCase = true)

    /**
     * Checks if this is an image type
     */
    val isImage: Boolean
        get() = primaryType.equals("image", ignoreCase = true)

    /**
     * Checks if this is an audio type
     */
    val isAudio: Boolean
        get() = primaryType.equals("audio", ignoreCase = true)

    /**
     * Checks if this is a video type
     */
    val isVideo: Boolean
        get() = primaryType.equals("video", ignoreCase = true)

    /**
     * Checks if this is an application type
     */
    val isApplication: Boolean
        get() = primaryType.equals("application", ignoreCase = true)

    /**
     * Checks if this is a multipart type
     */
    val isMultipart: Boolean
        get() = primaryType.equals("multipart", ignoreCase = true)

    companion object {
        // Common MIME types as constants
        val ALL = MimeType("*", "*")
        val TEXT_PLAIN = MimeType("text", "plain")
        val TEXT_HTML = MimeType("text", "html")
        val TEXT_CSS = MimeType("text", "css")
        val TEXT_JAVASCRIPT = MimeType("text", "javascript")
        val APPLICATION_JSON = MimeType("application", "json")
        val APPLICATION_XML = MimeType("application", "xml")
        val APPLICATION_PDF = MimeType("application", "pdf")
        val APPLICATION_OCTET_STREAM = MimeType("application", "octet-stream")
        val APPLICATION_FORM_URLENCODED = MimeType("application", "x-www-form-urlencoded")
        val MULTIPART_FORM_DATA = MimeType("multipart", "form-data")
        val IMAGE_JPEG = MimeType("image", "jpeg")
        val IMAGE_PNG = MimeType("image", "png")
        val IMAGE_GIF = MimeType("image", "gif")
        val IMAGE_SVG = MimeType("image", "svg+xml")

        /**
         * Parses a MIME type string into a data.binary.MimeType object
         *
         * @param mimeTypeString The MIME type string to parse
         * @return data.binary.MimeType object
         * @throws IllegalArgumentException if the string is invalid
         */
        fun parse(mimeTypeString: String): MimeType {
            require(mimeTypeString.isNotBlank()) { "MIME type string cannot be blank" }

            val trimmed = mimeTypeString.trim()
            val parts = trimmed.split(';')

            // Parse the main type/subtype part
            val mainPart = parts[0].trim()
            val typeParts = mainPart.split('/')

            require(typeParts.size == 2) { "Invalid MIME type format: $mimeTypeString" }

            val primaryType = typeParts[0].trim().lowercase()
            val subType = typeParts[1].trim().lowercase()

            // Parse parameters
            val parameters = mutableMapOf<String, String>()

            for (i in 1 until parts.size) {
                val paramPart = parts[i].trim()
                val equalIndex = paramPart.indexOf('=')

                require(equalIndex > 0) { "Invalid parameter format: $paramPart" }

                val paramName = paramPart.substring(0, equalIndex).trim().lowercase()
                var paramValue = paramPart.substring(equalIndex + 1).trim()

                // Handle quoted values
                if (paramValue.startsWith('"') && paramValue.endsWith('"') && paramValue.length >= 2) {
                    paramValue = unescapeQuoted(paramValue.substring(1, paramValue.length - 1))
                }

                parameters[paramName] = paramValue
            }

            return MimeType(primaryType, subType, parameters)
        }

        /**
         * Tries to parse a MIME type string, returning null if invalid
         */
        fun parseOrNull(mimeTypeString: String?): MimeType? {
            return try {
                if (mimeTypeString == null) null else parse(mimeTypeString)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Checks if a string is a valid token according to RFC 2045
         */
        private fun isValidToken(token: String): Boolean {
            if (token.isEmpty()) return false

            return token.all { char ->
                char.code in 33..126 && // printable ASCII
                        char !in "()<>@,;:\\\"/[]?={} \t" // tspecials and whitespace
            }
        }

        /**
         * Checks if a parameter value needs to be quoted
         */
        private fun needsQuoting(value: String): Boolean {
            return !isValidToken(value) || value.isEmpty()
        }

        /**
         * Escapes a quoted string value
         */
        private fun escapeQuoted(value: String): String {
            return value.replace("\\", "\\\\").replace("\"", "\\\"")
        }

        /**
         * Unescapes a quoted string value
         */
        private fun unescapeQuoted(value: String): String {
            val result = StringBuilder()
            var i = 0
            while (i < value.length) {
                val char = value[i]
                if (char == '\\' && i + 1 < value.length) {
                    result.append(value[i + 1])
                    i += 2
                } else {
                    result.append(char)
                    i++
                }
            }
            return result.toString()
        }
    }
}

// Extension functions for common operations
/**
 * Extension function to check if a string represents a specific MIME type
 */
fun String.isMimeType(mimeType: MimeType): Boolean {
    return try {
        val parsed = MimeType.parse(this)
        parsed.matches(mimeType)
    } catch (e: Exception) {
        false
    }
}

/**
 * Extension function to check if a string matches a MIME type pattern
 */
fun String.matchesMimePattern(pattern: String): Boolean {
    return try {
        val parsed = MimeType.parse(this)
        parsed.matchesPattern(pattern)
    } catch (e: Exception) {
        false
    }
}