package requirements

import AbstractField

/**
 * Pair of a field and its requirement.
 * Stores a value requirement associated with a field.
 * @property field The field associated with the requirement.
 * @property requirement The value requirement for the field, or null if there is no requirement.
 */
data class FieldRequirement<T>(
    val field: AbstractField<T>,
    val requirement: ValueRequirement<T>?
)

/**
 * Creates a requirement for the value of the field.
 * @receiver the field that requires some value format.
 * @param requirement the value requirement for the field.
 */
infix fun <T> AbstractField<T>.require(
    requirement: ValueRequirement<T>
) = FieldRequirement(this, requirement)