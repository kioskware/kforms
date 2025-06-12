package declare

import AbstractField
import AbstractForm
import Appearance
import FieldNotFoundException
import FieldSpec
import FormSpec
import UnexpectedFieldException
import declare.Form.Field
import fields
import id
import requirements.ValueRequirement
import requirements.require
import type.classFormFactory
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
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

    private lateinit var data: MutableMap<String, Any?>

    private val _spec: FormSpec by lazy { specCaches.getOrPut(this::class) { resolveSpec() } }

    /**
     * Initializes the form with the given data.
     * This function should be called before accessing any fields of the form.
     * Method used only for internal purposes.
     * To build a form with data, use builder methods instead.
     * @param data the data to initialize the form with.
     */
    internal fun initialize(data: MutableMap<String, Any?>) {
        this.data = data
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
     * Creates a requirement for the value of the field.
     * @receiver the property of the field that requires some value format.
     * @param requirement the value requirement for the field.
     */
    infix fun <T> KProperty<T>.require(
        requirement: ValueRequirement<T>
    ) = getFieldByProperty(this)!!.require(requirement)

    /**
     * Internal function that reads the specification of the form from the class.
     */
    private fun resolveSpec() = object : FormSpec {
        override val annotations: List<Annotation> = this@Form::class.annotations
        override val fields by lazy {
            this@Form::class.memberProperties
                .filterIsInstance<KProperty1<Form, *>>()
                .mapNotNull { property ->
                    // Make the property accessible
                    property.isAccessible = true
                    // Skip properties that are not fields
                    if (!property.isFinal || property.isAbstract ||
                        property.isConst || property.isLateinit ||
                        property.isSuspend
                    ) {
                        return@mapNotNull null
                    }
                    // Try to extract the field from the property,
                    // if it's not a field declaration,
                    // it will return null
                    (property.getDelegate(this@Form) as? Field<*>).also {
                        // If the field is found, set its property reference
                        it?.property = property
                    }
                }
        }
    }

    /**
     * Returns a string representation of the form data.
     * This is useful for debugging purposes.
     */
    override fun toString(): String {
        val className = this::class.simpleName ?: "Form"
        val fieldsString = data.entries.joinToString(", ") { (key, value) ->
            "$key=$value"
        }
        return "$className($fieldsString)"
    }

    /**
     * Compares this form with another object for equality.
     * Two forms are considered equal if they have the same class and the same data.
     * @param other the object to compare with.
     * @return true if the forms are equal, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Form) return false
        if (this::class != other::class) return false
        return data == other.data
    }

    /**
     * Returns the hash code of the form.
     * The hash code is computed based on the class and the data of the form.
     * @return the hash code of the form.
     */
    override fun hashCode(): Int {
        return 31 * this::class.hashCode() + data.hashCode()
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
            if (!thisRef::data.isInitialized) {
                throw IllegalStateException("Form data is not initialized. Make sure to call Form.initialize() before accessing fields.")
            }
            // Find the field by its id (property name)
            @Suppress("UNCHECKED_CAST")
            return thisRef.data[property.name] as? T
                ?: throw IllegalStateException("Field '${property.name}' is not set or has an incorrect type.")
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

    class BuilderScope<T : Form>
    @PublishedApi
    internal constructor(formClass: KClass<T>) {

        private val data: MutableMap<String, Any?> = mutableMapOf()

        val key: T = classFormFactory(formClass).invoke()

        infix fun <T> KProperty<T>.put(value: T?) {
            data[this.name] = value
        }

        inline infix fun <reified T : Form> KProperty<T>.put(block: BuilderScope<T>.() -> Unit)
        = put(build<T>(block))

        fun build(): T {
            key.initialize(data)
            return key
        }

    }


}

/**
 * Gets the [Form.Field] by its ID.
 * @param id the ID of the field.
 * @return the field if found, or null if not found.
 */
fun Form.getFieldById(id: String): AbstractField<*>? = fields.find { it.id == id }

/**
 * Gets the [Form.Field] by its property.
 * @param property the property of the field.
 * @return the field if found, or null if not found.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Form.getFieldByProperty(property: KProperty<T>): AbstractField<T>? {
    return fields
        .mapNotNull { it as? Field<T> }
        .find { it.property?.name == property.name }
}

/**
 * Builds a form of type [T] using the provided block.
 * The block is executed in the context of [Form.BuilderScope].
 *
 * @param block the block to configure the form.
 * @return the built form of type [T].
 */
inline fun <reified T : Form> build(block: Form.BuilderScope<T>.() -> Unit): T =
    Form.BuilderScope(T::class).apply(block).build()

fun main() {

    val appearance: Appearance = build {
        key::customColors put listOf(0xFF0000, 0x00FF00, 0x0000FF)
        key::address put {
            key::street put "123 Main St"
            key::city put "New York"
            key::zipCode put {
                key::part1 put 100
                key::part2 put 1
            }
        }
    }

    println(appearance)

}


//private val whitespaceRegex = "\\s+".toRegex()

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


