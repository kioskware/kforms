package data

import AbstractField
import requirements.FieldRequirement

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
) : Exception()

/**
 * Exception thrown on an attempt to access the data inappropriately.
 * For example, when trying to access a field that is not present in the form.
 */
abstract class DataAccessException : Exception()

/**
 * Exception thrown when trying to access a field that is not present in the form.
 * @property field The field that is not present in the form.
 */
class UnexpectedFieldException(
    val field: AbstractField<*>,
) : DataAccessException()