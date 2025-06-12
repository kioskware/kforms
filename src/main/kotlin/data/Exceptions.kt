package data

import AbstractField
import AbstractForm
import requirements.FieldRequirement
import kotlin.reflect.KClass

/**
 * Exception thrown when there is an error in the form declaration.
 * This can happen if the form is not properly declared or if there are issues with the form's structure.
 * @property message The detail message of the exception.
 * @property cause The cause of the exception, if any.
 */
class FormDeclarationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a field value does not satisfy the requirements.
 * @property unsatisfiedRequirements The requirements that are not satisfied.
 */
data class FieldValueException(
    val unsatisfiedRequirements: List<FieldRequirement<*>>
) : Exception() {
    /**
     * Constructs a FieldValueException with a single unsatisfied requirement.
     * @param unsatisfiedRequirement The unsatisfied requirement.
     */
    constructor(unsatisfiedRequirement: FieldRequirement<*>) : this(listOf(unsatisfiedRequirement))
}

/**
 * Exception thrown on an attempt to access the data inappropriately.
 * For example, when trying to access a field that is not present in the form.
 */
abstract class DataAccessException : Exception()

/**
 * Exception thrown when trying to access a field by non-existing ID.
 * @property fieldId The ID of the field that was not found.
 */
data class FieldNotFoundException(
    val fieldId: String,
    val formClass: KClass<out AbstractForm>
) : DataAccessException() {
    override val message: String
        get() = "Field with ID '$fieldId' not found in the form: '${formClass.qualifiedName}'"
}

/**
 * Exception thrown when trying to access a field that is not present in the form.
 * @property field The field that is not present in the form.
 */
data class UnexpectedFieldException(
    val field: AbstractField<*>,
) : DataAccessException()