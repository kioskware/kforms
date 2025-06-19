package kioskware.kforms.requirements

import kioskware.kforms.FieldValueException
import kioskware.kforms.InvalidFieldValueException
import kioskware.kforms.common.LogicOp
import kioskware.kforms.data.binary.BinarySource
import kioskware.kforms.data.binary.MimeType
import kioskware.kforms.declare.FieldPath

/**
 * Interface representing a requirement for a value.
 * @param T The type of the value.
 */
sealed interface ValueRequirement<T> {

    /**
     * Creates an opositive requirement that checks if the value does not meet the specified requirement.
     */
    data class Not<T>(
        val requirement: ValueRequirement<T>
    ) : ValueRequirement<T> {
        override fun checkValid(value: T): Boolean = !requirement.checkValid(value)
    }

    /**
     * Ensures that the provided value is one of the specified values.
     * @param values The list of valid values.
     * @param caseSensitive Whether the check should be case-sensitive. (applicable for String values only)
     */
    data class OneOf<T>(
        val values: List<T>,
        val caseSensitive: Boolean = true
    ) : ValueRequirement<T> {
        override fun checkValid(value: T): Boolean = value in values
    }

    /**
     * Ensures that the provided value is within a specified range.
     * @param min The minimum value of the range (inclusive).
     * @param max The maximum value of the range (inclusive).
     */
    data class IntegerRange(
        val min: Long,
        val max: Long
    ) : ValueRequirement<Long> {
        override fun checkValid(value: Long): Boolean = value in min..max
    }

    /**
     * Ensures that the provided value is a multiple of a specified number.
     * @param multiple The number that the value should be a multiple of.
     */
    data class MultipleOf(
        val multiple: Long
    ) : ValueRequirement<Long> {
        override fun checkValid(value: Long): Boolean = value % multiple == 0L
    }

    /**
     * Ensures that the provided value is within a specified range.
     * @param min The minimum value of the range (inclusive).
     * @param max The maximum value of the range (inclusive).
     */
    data class DecimalRange(
        val min: Double,
        val max: Double
    ) : ValueRequirement<Double> {
        override fun checkValid(value: Double): Boolean = value in min..max
    }

    /**
     * Ensures that the provided value is greater than or equal to a specified value.
     * @param value The value to compare against.
     */
    data class DecimalLowerThan(
        val value: Double
    ) : ValueRequirement<Double> {
        override fun checkValid(value: Double): Boolean = value < this.value
    }

    /**
     * Ensures that the provided value is less than or equal to a specified value.
     * @param value The value to compare against.
     */
    data class DecimalGreaterThan(
        val value: Double
    ) : ValueRequirement<Double> {
        override fun checkValid(value: Double): Boolean = value > this.value
    }

    /**
     * Ensures that the provided text value meets certain length requirements.
     * @param min The minimum length of the text (inclusive).
     * @param max The maximum length of the text (inclusive).
     */
    data class TextLengthRange(
        val min: Int,
        val max: Int
    ) : ValueRequirement<String> {
        override fun checkValid(value: String): Boolean = value.length in min..max
    }

    /**
     * Ensures that the provided text value matches a specific pattern.
     * @param pattern The regex pattern to match against.
     * @param caseSensitive Whether the check should be case-sensitive.
     */
    data class TextPattern(
        val pattern: Regex,
        val caseSensitive: Boolean = true,
        val patternDescription: CharSequence? = null
    ) : ValueRequirement<String> {
        override fun checkValid(value: String): Boolean = pattern.matches(value)
    }

    /**
     * Ensures that the provided binary data meets certain size requirements.
     * @param min The minimum size of the binary data (inclusive).
     * @param max The maximum size of the binary data (inclusive).
     */
    data class BinarySizeRange(
        val min: Int,
        val max: Int
    ) : ValueRequirement<BinarySource> {
        override fun checkValid(value: BinarySource): Boolean = value.size in min..max
    }

    /**
     * Ensures that the provided binary data matches one of the specified MIME types.
     * @param mimeTypes The list of valid MIME types.
     */
    data class BinaryOneOfMimeTypes(
        val mimeTypes: List<MimeType>
    ) : ValueRequirement<BinarySource> {
        override fun checkValid(value: BinarySource): Boolean {
            return mimeTypes.any { value.mimeType.matches(it) }
        }
    }

    /**
     * Ensures that the provided list meets certain size requirements.
     * @param min The minimum size of the list (inclusive).
     * @param max The maximum size of the list (inclusive).
     */
    data class ListSizeRange(
        val min: Int,
        val max: Int
    ) : ValueRequirement<List<*>> {
        override fun checkValid(value: List<*>): Boolean = value.size in min..max
    }

    /**
     * Ensures that the provided list contains unique items.
     * @return `true` if the list contains unique items, `false` otherwise.
     */
    data object ListUniqueItems : ValueRequirement<List<*>> {
        override fun checkValid(value: List<*>): Boolean {
            return value.size == value.toSet().size
        }
    }

//    /**
//     * Ensures that the provided list contains unique items based on specified fields.
//     * @param byFields The list of fields to check for uniqueness.
//     * If null, the entire item is considered for uniqueness.
//     * This is useful for lists of [AbstractForm] items.
//     */
//    data class ListUniqueItemsBy(
//        val byFields: List<String>
//    ) : ValueRequirement<List<AbstractForm>> {
//        override fun checkValid(value: List<AbstractForm>): Boolean {
//            value[0].get()
//        }
//    }

    /**
     * Ensures that the provided map contains unique values.
     * @return `true` if the map contains unique values, `false` otherwise.
     */
    data object MapUniqueValues : ValueRequirement<Map<*, *>> {
        override fun checkValid(value: Map<*, *>): Boolean {
            return value.size == value.values.toSet().size
        }
    }

    /**
     * Ensures that the provided map meets certain size requirements.
     * @param min The minimum size of the map (inclusive).
     * @param max The maximum size of the map (inclusive).
     */
    data class MapSizeRange(
        val min: Int,
        val max: Int
    ) : ValueRequirement<Map<*, *>> {
        override fun checkValid(value: Map<*, *>): Boolean = value.size in min..max
    }

    /**
     * Checks whether the provided value meets the requirement.
     * @param value The value to check.
     * @return `true` if the value meets the requirement, `false` otherwise.
     */
    fun checkValid(value: T): Boolean

}


/**
 * Ensures that the provided value meets the requirement.
 * If the value does not meet the requirement, a [FieldValueException] is thrown.
 * @param fieldPath Path of the field that is being checked. Used for error reporting.
 * @param value The value to check.
 * @param optimized Applies only for [ValueRequirements].
 * `true` means that the requirements are checked until the first one fails.
 * It will save performance in case of multiple requirements,
 * but may lead to less informative error messages, since only the first failing requirement will be reported.
 *
 * @throws InvalidFieldValueException if the value does not meet the requirement.
 */
@Throws(InvalidFieldValueException::class)
fun <T> ValueRequirement<T>.ensureValid(
    fieldPath: FieldPath?,
    value: T,
    optimized: Boolean = true
) {

    when (this) {

        is ValueRequirements -> {

            when (mode) {
                LogicOp.And -> {
                    if (optimized) {
                        requirements.firstOrNull { !it.checkValid(value) }
                            ?.ensureValid(fieldPath, value, true)
                    } else {
                        val violated = requirements.filterNot { it.checkValid(value) }
                        if (violated.isNotEmpty()) {
                            throw InvalidFieldValueException(
                                fieldPath,
                                ValueRequirements(violated, LogicOp.And)
                            )
                        }
                    }
                }

                LogicOp.Or -> {
                    val anyValid = requirements.any { it.checkValid(value) }
                    if (!anyValid) {
                        throw InvalidFieldValueException(fieldPath, this)
                    }
                }

                LogicOp.Xor -> {
                    val validCount = requirements.count { it.checkValid(value) }
                    if (validCount != 1) {
                        throw InvalidFieldValueException(fieldPath, this)
                    }
                }
            }
        }

        else -> {
            if (!checkValid(value)) {
                throw InvalidFieldValueException(fieldPath, this)
            }
        }

    }
}

/**
 * A composite value requirement that combines multiple value requirements using a logical operation.
 * @param T The type of the value.
 * @property requirements The list of value requirements to combine.
 * @property mode The logical operation to use for combining the requirements.
 * This can be [LogicOp.And], [LogicOp.Or], or [LogicOp.Xor].
 */
data class ValueRequirements<T>(
    val requirements: List<ValueRequirement<T>>,
    val mode: LogicOp = LogicOp.And
) : ValueRequirement<T> {

    override fun checkValid(value: T): Boolean {
        return when (mode) {
            LogicOp.And -> {
                requirements.all { it.checkValid(value) }
            }

            LogicOp.Or -> {
                requirements.any { it.checkValid(value) }
            }

            LogicOp.Xor -> {
                requirements.count { it.checkValid(value) } == 1
            }
        }
    }
}


/*
 * PREDEFINED REQUIREMENTS
 */

/**
 * Requires the value to be one of the specified values.
 * @param to The values that the value should be equal to.
 */
fun <T> isEqual(vararg to: T) = ValueRequirement.OneOf(to.toList())

/**
 * Requires the value to be equal to the specified value.
 * @param to The value that the value should be equal to.
 * @return A [ValueRequirement] that checks if the value is equal to the specified value.
 */
val isTrue = isEqual(true)

/**
 * Requires the value to be equal to the specified value.
 * @param to The value that the value should be equal to.
 * @return A [ValueRequirement] that checks if the value is equal to the specified value.
 */
val isFalse = isEqual(false)

/**
 * Requires the value to be a multiple of a specified number.
 * @param multiple The number that the value should be a multiple of.
 * @return A [ValueRequirement] that checks if the value is a multiple of the specified number.
 */
fun isMultipleOf(multiple: Long) = ValueRequirement.MultipleOf(multiple)

/**
 * Requires the value to be even.
 * This is a shorthand for `isMultipleOf(2)`.
 *
 * @return A [ValueRequirement] that checks if the value is even.
 */
val isEven = isMultipleOf(2)

/**
 * Requires the value to be odd.
 * This is a shorthand for `Not(isEven)`.
 * @return A [ValueRequirement] that checks if the value is odd.
 */
val isOdd = ValueRequirement.Not(isEven)

/**
 * Requires the value to be in a specified range.
 * @param range The range to check against.
 * @return A [ValueRequirement] that checks if the value is within the specified range.
 */
fun isInRange(range: LongRange) = ValueRequirement.IntegerRange(
    min = range.first,
    max = range.last
)

/**
 * Requires the value to be in a specified range.
 * @param range The range to check against.
 * @return A [ValueRequirement] that checks if the value is within the specified range.
 */
fun isInRange(range: IntRange) = ValueRequirement.IntegerRange(
    min = range.first.toLong(),
    max = range.last.toLong()
)

/**
 * Requires the value to be in a specified range (for Double).
 * @param range The range to check against.
 * @return A [ValueRequirement] that checks if the value is within the specified range.
 */
fun isInRange(range: ClosedFloatingPointRange<Double>) = ValueRequirement.DecimalRange(
    min = range.start,
    max = range.endInclusive
)

/**
 * Requires the value to be positive (> 0).
 * @return A [ValueRequirement] that checks if the value is positive.
 */
val isPositive = isInRange(1L..Long.MAX_VALUE)

/**
 * Requires the value to be non-positive (<= 0).
 * @return A [ValueRequirement] that checks if the value is non-positive.
 */
val isNonPositive = isInRange(Long.MIN_VALUE..0L)

/**
 * Requires the value to be negative (< 0).
 * @return A [ValueRequirement] that checks if the value is negative.
 */
val isNegative = isInRange(Long.MIN_VALUE..-1L)

/**
 * Requires the value to be non-negative (>= 0).
 * @return A [ValueRequirement] that checks if the value is non-negative.
 */
val isNonNegative = isInRange(0L..Long.MAX_VALUE)

/**
 * Requires the value to be greater than the specified value.
 * @param value The lower bound (exclusive).
 * @return A [ValueRequirement] that checks if the value is greater than the specified value.
 */
fun isGreaterThan(value: Long) = ValueRequirement.IntegerRange(
    min = value + 1,
    max = Long.MAX_VALUE
)

/**
 * Requires the value to be greater than the specified value.
 * @param value The lower bound (exclusive).
 * @return A [ValueRequirement] that checks if the value is greater than the specified value.
 */
fun isGreaterThan(value: Int) = ValueRequirement.IntegerRange(
    min = (value + 1).toLong(),
    max = Long.MAX_VALUE
)

/**
 * Requires the value to be less than the specified value.
 * @param value The upper bound (exclusive).
 * @return A [ValueRequirement] that checks if the value is less than the specified value.
 */
fun isLessThan(value: Long) = ValueRequirement.IntegerRange(
    min = Long.MIN_VALUE,
    max = value - 1
)

/**
 * Requires the value to be greater than the specified value (for Double).
 * @param value The lower bound (exclusive).
 * @return A [ValueRequirement] that checks if the value is greater than the specified value.
 */
fun isGreaterThan(value: Double) = ValueRequirement.DecimalLowerThan(value)

/**
 * Requires the value to be less than the specified value (for Double).
 * @param value The upper bound (exclusive).
 * @return A [ValueRequirement] that checks if the value is less than the specified value.
 */
fun isLessThan(value: Double) = ValueRequirement.DecimalGreaterThan(value)

/**
 * Requires the length of the text to be within the specified range.
 * @param range The range of valid lengths.
 * @return A [ValueRequirement] that checks if the text length is within the specified range.
 */
fun isLengthInRange(range: IntRange) = ValueRequirement.TextLengthRange(
    min = range.first,
    max = range.last
)

/**
 * Requires the text value to match the specified pattern.
 * @param pattern The regex pattern to match against.
 * @param caseSensitive Whether the check should be case-sensitive.
 * @param patternDescription Optional description of the pattern for error messages.
 * @return A [ValueRequirement] that checks if the text matches the pattern.
 */
fun isPattern(pattern: Regex, caseSensitive: Boolean = true, patternDescription: CharSequence? = null) =
    ValueRequirement.TextPattern(pattern, caseSensitive, patternDescription)

/**
 * Requires the binary data size to be within the specified range.
 * @param range The range of valid sizes in bytes.
 * @return A [ValueRequirement] that checks if the binary size is within the specified range.
 */
fun isByteSizeInRange(range: IntRange) = ValueRequirement.BinarySizeRange(
    min = range.first,
    max = range.last
)

/**
 * Requires the list size to be within the specified range.
 * @param range The range of valid sizes.
 * @return A [ValueRequirement] that checks if the list size is within the specified range.
 */
fun isListSizeInRange(range: IntRange) = ValueRequirement.ListSizeRange(
    min = range.first,
    max = range.last
)

/**
 * Requires the map size to be within the specified range.
 * @param range The range of valid sizes.
 * @return A [ValueRequirement] that checks if the map size is within the specified range.
 */
fun isMapSizeInRange(range: IntRange) = ValueRequirement.MapSizeRange(
    min = range.first,
    max = range.last
)

/**
 * Requires the list to contain unique items.
 * @return A [ValueRequirement] that checks if the list contains unique items.
 */
val isUniqueItemsList: ValueRequirement<List<*>> = ValueRequirement.ListUniqueItems

/**
 * Requires the map to contain unique values.
 * @return A [ValueRequirement] that checks if the map contains unique values.
 */
val isUniqueValuesMap: ValueRequirement<Map<*, *>> = ValueRequirement.MapUniqueValues

/**
 * Requires the binary data to match at least one of the specified MIME types.
 * @param mimeTypes The list of valid MIME types.
 * @return A [ValueRequirement] that checks if the binary data matches one of the specified MIME types.
 */
fun isMimeTypeOneOf(vararg mimeTypes: MimeType): ValueRequirement<BinarySource> =
    ValueRequirement.BinaryOneOfMimeTypes(mimeTypes.toList())

operator fun <T> ValueRequirement<T>.not(): ValueRequirement<T> =
    ValueRequirement.Not(this)

infix fun <T> ValueRequirement<T>.and(other: ValueRequirement<T>):
        ValueRequirement<T> = ValueRequirements(listOf(this, other), LogicOp.And)

infix fun <T> ValueRequirement<T>.or(other: ValueRequirement<T>):
        ValueRequirement<T> = ValueRequirements(listOf(this, other), LogicOp.Or)

infix fun <T> ValueRequirement<T>.xor(other: ValueRequirement<T>):
        ValueRequirement<T> = ValueRequirements(listOf(this, other), LogicOp.Xor)