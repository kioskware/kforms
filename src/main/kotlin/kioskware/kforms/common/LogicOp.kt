package kioskware.kforms.common

/**
 * Represents a logical operation for combining conditions.
 */
enum class LogicOp {
    /**
     * All conditions must be satisfied.
     */
    And,

    /**
     * At least one condition must be satisfied.
     */
    Or,

    /**
     * Conditions must be satisfied in an exclusive manner,
     * meaning exactly one condition must be true.
     */
    Xor
}