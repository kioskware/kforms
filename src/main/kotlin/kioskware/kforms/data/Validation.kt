package kioskware.kforms.data

import kioskware.kforms.declare.FieldPath
import kioskware.kforms.scopes.AccessScope

/**
 * Parameters for form validation.
 * @property mode the validation mode to use. Default is [ValidationMode.Full].
 * @property accessScope if provided, only fields accessible in this scope will be validated.
 */
data class ValidationParams(
    val mode: ValidationMode = ValidationMode.Full,
    val accessScope: AccessScope? = null
) {
    companion object {
        /**
         * Default validation parameters.
         */
        val Default = ValidationParams()
    }
}

/**
 * Configuration for form validation.
 * This configuration is used to specify how the form data should be validated.
 *
 * @property params the parameters for validation, such as validation mode and access scope.
 * @property optimized whether to use optimized validation. Default is true.
 * @property lenientTypes whether to allow lenient type casting. Default is true.
 * @property detailedLocation whether to include detailed location information in validation errors. Default is false.
 * This will include detailed location paths, but may affect performance.
 * In lenient mode, the validation processor will try to cast values to the field type even if they are not exactly matching.
 * @property parentPath applicable when detailed location is enabled. Path of the parent field. Next path segments will be appended to this path.
 */
data class ValidationConfig(
    val params: ValidationParams = ValidationParams(),
    val optimized: Boolean = true,
    val lenientTypes: Boolean = true,
    val detailedLocation: Boolean = false,
    val parentPath: FieldPath? = null
) {
    companion object {
        /**
         * Default validation configuration.
         */
        val Default = ValidationConfig()
    }
}

/**
 * Represents the validation mode for form data.
 */
enum class ValidationMode {

    /**
     * Data will be fully validated.
     * Checking for missing required fields and validating all values.
     */
    Full,

    /**
     * Data will be validated only for values that are set.
     * In this mode, missing required fields will not be reported as errors.
     */
    Provided,

    /**
     * Data will not be validated at all.
     * This mode is useful when you are sure that the data is already
     * valid or when you want to skip validation for performance reasons.
     */
    None;

}
