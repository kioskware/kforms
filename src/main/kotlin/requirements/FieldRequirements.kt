package requirements

import common.LogicOp

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