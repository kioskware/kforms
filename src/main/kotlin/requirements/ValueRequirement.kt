package requirements

import AbstractField
import common.LogicOp
import FieldValueException
import isOptional
import spec
import data.binary.BinarySource
import data.binary.MimeType

/**
 * Interface representing a requirement for a value.
 * @param T The type of the value.
 */
sealed interface ValueRequirement<T> {

    /**
     * Ensures that the provided value is not null.
     * @return `true` if the value is not null, `false` otherwise.
     */
    class NonNull<T> : ValueRequirement<T> {
        override fun checkValid(value: T): Boolean = value != null
    }

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

    /**
     * Ensures that the provided value meets the requirement.
     * If the value does not meet the requirement, a [FieldValueException] is thrown.
     * @param field The field associated with the value.
     * @param value The value to check.
     * @param optimized Applies only for [ValueRequirements].
     * `true` means that the requirements are checked until the first one fails.
     * It will save performance in case of multiple requirements,
     * but may lead to less informative error messages, since only the first failing requirement will be reported.
     *
     * @throws FieldValueException if the value does not meet the requirement.
     */
    @Throws(FieldValueException::class)
    fun ensureValid(field: AbstractField<T>, value: T, optimized: Boolean = true) {
        if (!checkValid(value)) {
            throw FieldValueException(listOf(FieldRequirement(field, this)))
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

    override fun ensureValid(field: AbstractField<T>, value: T, optimized: Boolean) {
        when(mode) {
            LogicOp.And -> {
                if (optimized) {
                    requirements.firstOrNull { !it.checkValid(value) }
                        ?.ensureValid(field, value, true)
                } else {
                    requirements.forEach { it.ensureValid(field, value, true) }
                }
            }

            LogicOp.Or -> {
                val anyValid = requirements.any { it.checkValid(value) }
                if (!anyValid) {
                    throw FieldValueException(listOf(FieldRequirement(field, this)))
                }
            }

            LogicOp.Xor -> {
                val validCount = requirements.count { it.checkValid(value) }
                if (validCount != 1) {
                    throw FieldValueException(listOf(FieldRequirement(field, this)))
                }
            }
        }
    }

}

/**
 * Processes the value using `preProcessor` (if available) and then
 * validates the provided value against the requirement of the field.
 * If the value does not meet the requirement, a [FieldValueException] is thrown.
 * @param value The value to validate.
 * @return The validated value.
 * @throws FieldValueException if the value does not meet the requirement.
 */
@Throws(FieldValueException::class)
fun <T> AbstractField<T>.processValue(
    value: T?,
    optimizedValidation: Boolean = true
): T {
    // Handle value nullability and default value
    if(value == null) {
        if(isOptional) {
            // It's guaranteed that the default value will meet the requirements,
            // so we can safely return it without further checks.
            @Suppress("UNCHECKED_CAST")
            return this.spec.defaultValue as T
        } else {
            // If the field is required and the value is null, throw an exception
            throw FieldValueException(FieldRequirement(this, ValueRequirement.NonNull()))
        }
    }
    // Check if the value with the correct type meets the requirements
    spec.type.let {
        val processed = it.preProcessor?.invoke(value) ?: value
        it.requirement?.ensureValid(this, processed, optimizedValidation)
    }
    return value
}


/*
 * PREDEFINED REQUIREMENTS
 */

/**
 * Creates a value requirement that checks if the value is equal to any of the provided values.
 * @param to The values to check against.
 * @return A [ValueRequirement] that checks if the value is equal to any of the provided values.
 */
fun <T> isEqual(vararg to: T) = ValueRequirement.OneOf(to.toList())

fun isMultipleOf(multiple: Long) = ValueRequirement.MultipleOf(multiple)

val isEven = isMultipleOf(2)

val isOdd = ValueRequirement.Not(isEven)

fun isInRange(range: LongRange) = ValueRequirement.IntegerRange(
    min = range.first,
    max = range.last
)

fun isInRange(range: IntRange) = ValueRequirement.IntegerRange(
    min = range.first.toLong(),
    max = range.last.toLong()
)

fun isInRange(range: ClosedFloatingPointRange<Double>) = ValueRequirement.DecimalRange(
    min = range.start,
    max = range.endInclusive
)

val isPositive = isInRange(1L..Long.MAX_VALUE)

val isNonPositive = isInRange(Long.MIN_VALUE..0L)

val isNegative = isInRange(Long.MIN_VALUE..-1L)

val isNonNegative = isInRange(0L..Long.MAX_VALUE)

fun isGreaterThan(value: Long) = ValueRequirement.IntegerRange(
    min = value + 1,
    max = Long.MAX_VALUE
)

fun isGreaterThan(value: Int) = ValueRequirement.IntegerRange(
    min = (value + 1).toLong(),
    max = Long.MAX_VALUE
)

fun isLessThan(value: Long) = ValueRequirement.IntegerRange(
    min = Long.MIN_VALUE,
    max = value - 1
)

fun isGreaterThan(value: Double) = ValueRequirement.DecimalLowerThan(value)

fun isLessThan(value: Double) = ValueRequirement.DecimalGreaterThan(value)

fun isLengthInRange(range: IntRange) = ValueRequirement.TextLengthRange(
    min = range.first,
    max = range.last
)

fun isPattern(pattern: Regex, caseSensitive: Boolean = true, patternDescription: CharSequence? = null) =
    ValueRequirement.TextPattern(pattern, caseSensitive, patternDescription)


fun isSizeInRange(range: IntRange) = ValueRequirement.BinarySizeRange(
    min = range.first,
    max = range.last
)

infix fun <T> ValueRequirement<T>.and(other: ValueRequirement<T>):
        ValueRequirement<T> = ValueRequirements(listOf(this, other), LogicOp.And)

infix fun <T> ValueRequirement<T>.or(other: ValueRequirement<T>):
        ValueRequirement<T> = ValueRequirements(listOf(this, other), LogicOp.Or)

infix fun <T> ValueRequirement<T>.xor(other: ValueRequirement<T>):
        ValueRequirement<T> = ValueRequirements(listOf(this, other), LogicOp.Xor)