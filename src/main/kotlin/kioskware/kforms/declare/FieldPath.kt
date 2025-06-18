package kioskware.kforms.declare

import kioskware.kforms.AbstractField
import kioskware.kforms.declare.FieldPath.Segment
import kioskware.kforms.id

/**
 * Stores a path to some field value.
 */
class FieldPath(
    val segments: List<Segment>
) {

    private val _toStringCache: String by lazy {
        segments.joinToString(separator = ".") { segment ->
            when (segment) {
                is Segment.Field -> segment.field.id
                is Segment.Index -> "#" + segment.index.toString()
                is Segment.MapKey -> segment.key.toString().limitAndEllipsize(300)
            }
        }
    }

    /**
     * Returns the last field in the path, if any.
     *
     * @return The last field in the path, or null if there are no fields in the path.
     */
    val lastField: AbstractField<*>? by lazy {
        segments.findLast { it is Segment.Field }?.let { (it as Segment.Field).field }
    }

    /**
     * Returns the last segment in the path, if any.
     *
     * @return The last segment in the path, or null if there are no segments in the path.
     */
    val lastSegment: Segment? by lazy {
        segments.lastOrNull()
    }

    sealed class Segment {

        data class Field(val field: AbstractField<*>) : Segment()

        data class Index(val index: Int) : Segment()

        data class MapKey(val key: Any?, val valueTarget: Boolean) : Segment()

    }

    override fun toString(): String = _toStringCache

    private fun String.limitAndEllipsize(maxLength: Int): String =
        if (this.length > maxLength) this.take(maxLength) + "..." else this

}


infix operator fun FieldPath?.plus(other: FieldPath): FieldPath {
    if (this == null) {
        return other
    }
    return FieldPath(segments + other.segments)
}

infix operator fun FieldPath?.plus(segment: Segment): FieldPath = plus(FieldPath(listOf(segment)))

infix operator fun FieldPath?.plus(field: AbstractField<*>): FieldPath = plus(Segment.Field(field))

infix operator fun FieldPath?.plus(index: Int): FieldPath = plus(Segment.Index(index))
