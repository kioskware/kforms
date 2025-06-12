import requirements.FieldRequirements
import type.Type
import kotlin.reflect.KAnnotatedElement

/**
 * Represents a field in a form.
 *
 */
interface AbstractField<T> {

    /**
     * Specification of this field including its type, default value,
     * enable rules etc.
     * @return the specification of the field.
     */
    fun spec(): FieldSpec<T>

}

/**
 * Specification of the field.
 * @param T Type of the field value
 * @property type Type of the field
 * @property defaultValue Default value of the field
 * @property id ID of the field or empty string to use property or class name for as
 * an ID
 * @property name Name of the field
 * @property description Description of the field
 * @property descriptionDetailed Detailed description of the field
 * @property orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * if the field is a single value or a list of values
 * @property enabledRules Rules that enable or disable the field
 *
 */
interface FieldSpec<T> : KAnnotatedElement {
    val id: String
    val type: Type<T>
    val name: CharSequence? get() = null
    val description: CharSequence? get() = null
    val descriptionDetailed: CharSequence? get() = null
    val defaultValue: T? get() = null
    val orderKey: Int get() = 0
    val enabledRules: FieldRequirements get() = FieldRequirements.None
}

/**
 * Checks whether this field is optional or not.
 * Optional fields come with a default value or have a nullable type.
 */
val AbstractField<*>.isOptional: Boolean get() =
    spec().defaultValue != null || spec().type is Type.Nullable<*>

/**
 * Checks whether this field is required or not.
 * Required fields do not come with a default value.
 */
val AbstractField<*>.isRequired: Boolean get() = !isOptional

/**
 * Shorthand for getting the specification of the field.
 * Equivalent to `field.spec()`.
 */
val <T> AbstractField<T>.spec: FieldSpec<T>
    get() = spec()

/**
 * Shorthand for getting the ID of the field.
 */
val <T> AbstractField<T>.id: String
    get() = spec().id