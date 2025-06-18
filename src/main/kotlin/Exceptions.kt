import declare.FieldPath
import requirements.ValueRequirement
import type.Type
import kotlin.reflect.KClass

/**
 * Base class for all form-related exceptions.
 *
 * @property message The detail message of the exception.
 * @property cause The cause of the exception, if any.
 */
sealed class FormException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when there is an error in the form declaration.
 * This can happen if the form is not properly declared or if there are issues with the form's structure.
 * @property message The detail message of the exception.
 * @property cause The cause of the exception, if any.
 */
class FormDeclarationException(
    message: String,
    cause: Throwable? = null
) : FormException(message, cause)

/**
 * Base class for exceptions related to field values in a form.
 * @property fieldPath The path to the field in the form where the error occurred.
 */
sealed class FieldValueException(
    open val fieldPath: FieldPath?
) : FormException() {
    abstract override val message : String
}

/**
 * Exception thrown when a required field value is missing.
 * This can happen if the field is required but no value is provided.
 * @param fieldPath The path to the field in the form where the error occurred.
 */
class MissingFieldValueException(
    fieldPath: FieldPath? = null
) : FieldValueException(fieldPath) {
    override val message: String
        get() = "Missing required field value at path: '$fieldPath'"
}

/**
 * Exception thrown when a field value does not meet the specified requirements.
 * This can happen if the value is invalid or does not conform to the expected type.
 * @property fieldPath The path to the field in the form where the error occurred.
 * @property valueType The expected type of the field value.
 */
class FieldValueTypeMismatchException(
    fieldPath: FieldPath? = null,
    val valueType: KClass<*>? = null,
    val expectedType: Type<*>
) : FieldValueException(fieldPath) {
    override val message: String
        get() = "Field value at path '$fieldPath' does not match expected type '${expectedType}'" +
                (valueType?.let { ", but was of type '${it.simpleName}'" } ?: "")
}

/**
 * Exception thrown when a field value does not meet the specified requirement.
 * This can happen if the value is invalid, according to the requirement.
 * @property fieldPath The path to the field in the form where the error occurred.
 * @property requirement The requirement that the field value did not meet.
 */
class InvalidFieldValueException(
    fieldPath: FieldPath? = null,
    val requirement: ValueRequirement<*>
) : FieldValueException(fieldPath) {
    override val message: String
        get() = "Field value at path '$fieldPath' does not meet requirement '${requirement}'"}

/**
 * Exception thrown on an attempt to access the data inappropriately.
 * For example, when trying to access a field that is not present in the form.
 */
abstract class DataAccessException : FormException()

/**
 * Exception thrown when trying to access a field by non-existing ID.
 * @property fieldId The ID of the field that was not found.
 */
data class FieldNotFoundException(
    val fieldId: String,
    val formClass: KClass<out AbstractForm>
) : DataAccessException() {
    override val message: String
        get() = "Field with ID '$fieldId' not found in form '${formClass.simpleName}'"
}

/**
 * Exception thrown when trying to access a field that is not present in the form.
 * @property field The field that is not present in the form.
 */
data class UnexpectedFieldException(
    val field: AbstractField<*>,
) : DataAccessException() {
    override val message: String = "Unexpected field '${field.id}'"
}