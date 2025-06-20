package kioskware.kforms

import kioskware.kforms.requirements.FieldRequirements
import kioskware.kforms.scopes.AccessScope
import kioskware.kforms.type.Type
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
 * @property docName Name of the field.
 * @property description Description of the field.
 * @property descriptionDetailed Detailed description of the field.
 * @property orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * if the field is a single value or a list of values
 * @property enabledRules Rules that enable or disable the field
 * @property accessScope Access scope of the field, defines who can access the field.
 * @property owner The form that owns this field
 * @property sensitive Whether the field may hold sensitive data or not.
 * @property examples Example values of the field, used for documentation purposes or for AI models.
 * @property extras Additional metadata for the field, used for documentation purposes or for AI models.
 *
 */
interface FieldSpec<T> : KAnnotatedElement, AbstractSpec {
    val id: String
    val type: Type<T>
    val owner: AbstractForm
    val sensitive: Boolean get() = false
    val examples: List<T>? get() = null
    val defaultValue: T? get() = null
    val enabledRules: FieldRequirements get() = FieldRequirements.None
    val accessScope: AccessScope? get() = AccessScope.None
}

/**
 * Checks whether this field is optional or not.
 * Optional fields come with a default value or have a nullable type.
 */
val AbstractField<*>.isOptional: Boolean
    get() =
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

/**
 * Shorthand for getting the type of the field.
 */
val <T> AbstractField<T>.type: Type<T>
    get() = spec().type