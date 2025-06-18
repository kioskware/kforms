package kioskware.kforms.common

/**
 * Performs a deep copy of a [Map] with [String] keys and nullable [Any] values.
 *
 * This function recursively copies all nested maps and lists, ensuring that
 * the returned map and its contents are independent of the original.
 *
 * @param original The map to be deeply copied.
 * @return A deep copy of the original map.
 */
fun Map<String, Any?>.deepCopy(): Map<String, Any?> {
    return mapValues { (_, value) ->
        when (value) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (value as Map<String, Any?>).deepCopy()
            }

            is List<*> -> {
                value.deepCopy()
            }

            else -> value
        }
    }
}

/**
 * Performs a deep copy of a [List] containing any type of elements.
 *
 * This function recursively copies all nested maps and lists, ensuring that
 * the returned list and its contents are independent of the original.
 *
 * @param original The list to be deeply copied.
 * @return A deep copy of the original list.
 */
fun List<*>.deepCopy(): List<Any?> {
    return map { item ->
        when (item) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (item as Map<String, Any?>).deepCopy()
            }

            is List<*> -> item.deepCopy()
            else -> item
        }
    }
}
