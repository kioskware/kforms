package declare

import AbstractField
import AbstractForm
import FieldSpec
import FormSpec
import data.DataAccessException
import data.FieldNotFoundException
import data.UnexpectedFieldException
import declare.Form.Field
import fields
import id
import requirements.ValueRequirement
import requirements.processValue
import requirements.require
import spec
import type.Type
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.getExtensionDelegate
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * # Forms
 *
 * Forms are used to define complex data structures
 * with a set of fields with their validation and metadata.
 *
 * To create a form:
 *
 * 1. Extend the [Form] class.
 * Remember, it must be a regular class, not an `object` or `interface`.
 * Class may be open if you want to allow inheritance.
 *
 * 2. Declare fields using the provided field declaration functions as a `val` delegated property:
 *     - [boolField] - for boolean fields
 *     - [intField] - for integer fields
 *     - [decimalField] - for decimal fields (floating-point numbers)
 *     - [textField] - for text fields (strings)
 *     - [enumField] - for enumerated types (enums)
 *     - [formField] - for nested forms
 *     - [listField] - for lists of values
 *     - [mapField] - for maps of key-value pairs
 *     - [formListField] - for lists of nested forms
 *
 *     **Note:** To declare nullable field, use `nullable` variation of the field declaration functions,
 *     e.g. [nullableTextField], [nullableIntField], etc.
 *
 *
 *
 *
 * Example usage:
 * ```kotlin
 *
 * open class File : Form() {
 *
 *     val name by textField(
 *         name = "File Name",
 *         description = "Name of the file"
 *     )
 *
 *     val size by intField(
 *         name = "File Size",
 *         description = "Size of the file in bytes"
 *     )
 *
 *     val createdAt by intField(
 *         name = "Created At",
 *         description = "Creation date and time of the file"
 *     )
 *
 * }
 *
 * class Directory : File() {
 *
 *     val files by formListField<File>(
 *         name = "Files",
 *         description = "List of files in the directory"
 *     )
 *
 *     val subdirectories by formListField<Directory>(
 *         name = "Subdirectories",
 *         description = "List of subdirectories in the directory"
 *     )
 *
 * }
 *
 * ```
 *
 * @param name Default name of the form. Will be used when the `hostField` does not provide a name.
 * @param description Default description of the form. Will be used when the `hostField` does not provide a description.
 * @param descriptionDetailed Default detailed description of the form. Will be used when the `hostField` does not provide a detailed description.
 * @param annotations Additional annotations to be added to the form specification.
 * Will be merged with the annotations from the class. These annotations will be added to `hostField` specification.
 */
abstract class Form : AbstractForm {

    private companion object {
        /**
         * Stores the specifications of forms to avoid re-reading them from the class.
         *
         * We can cache the specs because by the convention,
         * specs are the same for each instance of the form with same class.
         */
        val specCaches = mutableMapOf<KClass<out Form>, FormSpec>()
    }

    private val data: MutableMap<String, Any?> = mutableMapOf()

    private val _spec: FormSpec by lazy {
        specCaches.getOrPut(this::class) {
            object : FormSpec {
                override val annotations: List<Annotation> = this@Form::class.annotations
                override val fields by lazy {
                    this@Form::class.memberProperties.mapNotNull { property ->
                        property.isAccessible = true // Make the property accessible
                        // Try extract the field from the property,
                        // if it's not a field declaration,
                        // it will return null
                        (property.getExtensionDelegate() as? Field<*>).also {
                            // If the field is found, set its property reference
                            it?.property = property
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the specification of this form.
     * The specification is cached to avoid re-reading it from the class,
     * so it can be accessed multiple times without a performance penalty.
     */
    override fun spec(): FormSpec = _spec

    override fun get(fieldId: String): Any? = getFieldById(fieldId).let {
        if (it == null) {
            throw FieldNotFoundException(fieldId, this::class)
        } else {
            get(it)
        }
    }

    override fun <T> get(field: AbstractField<T>): T {
        return spec().fields.find { it.id == field.id }
            ?.let {
                @Suppress("UNCHECKED_CAST")
                (it as Field<T>).getValue(
                    this, it.property ?:
                    // If the property is not set, it means the field was not initialized properly
                    // this should not happen, but we throw an exception to indicate that
                    throw IllegalStateException()
                )
            } ?: throw UnexpectedFieldException(field)
    }

    /**
     * Field of the form.
     */
    class Field<T> internal constructor(
        private val fieldSpec: FieldSpec<T>
    ) : AbstractField<T> {

        internal var property: KProperty<*>? = null

        operator fun getValue(
            thisRef: Form,
            property: KProperty<*>
        ): T {
            // Find the field by its id (property name)
            @Suppress("UNCHECKED_CAST")
            return thisRef.data[property.name] as? T
                ?: throw IllegalStateException("Field '${property.name}' is not set or has an incorrect type.")
        }

        operator fun setValue(
            thisRef: Form,
            property: KProperty<*>,
            value: T
        ) {
            thisRef.data[property.name] =
                processValue(value) // Perform validation and processing of the value
        }

        override fun spec(): FieldSpec<T> = object : FieldSpec<T> by fieldSpec {
            override val id by lazy { resolveFieldId(fieldSpec.id, property) }
        }

        private fun resolveFieldId(id: String, property: KProperty<*>?): String {
            return id.ifEmpty {
                // When id is not provided, use the property name
                property?.name ?: throw IllegalStateException()
            }
        }

    }

    /**
     * Creates a requirement for the value of the field.
     * @receiver the property of the field that requires some value format.
     * @param requirement the value requirement for the field.
     */
    infix fun <T> KProperty<T>.require(
        requirement: ValueRequirement<T>
    ) = getFieldByProperty(this)!!.require(requirement)

}

/**
 * Gets the [Form.Field] by its ID.
 * @param id the ID of the field.
 * @return the field if found, or null if not found.
 */
fun Form.getFieldById(id: String): AbstractField<*>? = spec().fields.find { it.id == id }

/**
 * Gets the [Form.Field] by its property.
 * @param property the property of the field.
 * @return the field if found, or null if not found.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Form.getFieldByProperty(property: KProperty<T>): AbstractField<T>? {
    return spec().fields
        .mapNotNull { it as? Field<T> }
        .find { it.property == property }
}


private val whitespaceRegex = "\\s+".toRegex()

///**
// * Finds a field in the form by its path.
// * The path is a dot-separated string representing the field's hierarchy.
// * For example, `address.zipCode.part1` will find the field with id `part1`
// * inside the `zipCode` field of the `address` form.
// * - If the field is not found, returns null.
// * - If the path is empty or consists only of whitespace, returns null.
// * - If the path is invalid (e.g., a field does not exist),
// * returns null.
// *
// * @param path the dot-separated path to the field.
// * @return the field if found, or null if not found or path is invalid.
// */
//fun AbstractForm.fieldAtPath(
//    path: String
//): AbstractField<*>? {
//    // Split the path by '.' and trim each part
//    val parts = path.split('.')
//        .map { it.replace(whitespaceRegex, "") } //Remove all whitespace
//        .filter { it.isNotBlank() }
//
//    if (parts.isEmpty()) return null
//    var currentField: AbstractField<*>? = null
//    var currentForm: AbstractForm? = this
//    for (part in parts) {
//        if (currentForm == null) {
//            // If the current form is null, we cannot proceed
//            return null
//        }
//        val field = currentForm.fields.find { it.id == part }
//        if (field == null) {
//            // If the field is not found, return null
//            return null
//        } else {
//            currentField = field
//            field.spec.type.let {
//                currentForm = if (it is Type.FormType<*>) {
//                    it.formFactory()
//                } else {
//                    null // If it's not a form type, we stop here
//                }
//            }
//        }
//    }
//    return currentField
//}