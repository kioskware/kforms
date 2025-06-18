package kioskware.kforms.requirements

import kioskware.kforms.AbstractField
import kioskware.kforms.common.LogicOp

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
 * Represents a collection of field requirements with a specified logical operation.
 * This class is used to group multiple field requirements together, allowing for complex validation logic.
 * @property requirements The list of field requirements to be evaluated.
 * @property logicOp The logical operation to be applied to the field requirements (default is AND).
 */
data class FieldRequirements(
    val requirements: List<FieldRequirement<*>>,
    val logicOp: LogicOp = LogicOp.And,
) {
    companion object {
        /**
         * Represents an empty set of field requirements.
         * This can be used when no requirements are needed.
         */
        val None = FieldRequirements(emptyList())
    }
}

/**
 * Creates a requirement for the value of the field.
 * @receiver the field that requires some value format.
 * @param requirement the value requirement for the field.
 */
infix fun <T> AbstractField<T>.require(
    requirement: ValueRequirement<T>
) = FieldRequirement(this, requirement)