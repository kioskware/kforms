import kotlin.reflect.KAnnotatedElement

/**
 * Defines complex data tu\ype with a set of fields.
 */
interface AbstractForm {

    /**
     * Specification of the form.
     */
    fun spec(): FormSpec

    /**
     * Returns the value of the field with the given ID.
     * @param fieldId The ID of the field to get the value of.
     * @return The value of the field.
     * @throws DataAccessException If the field with the given ID does not exist or access to it is forbidden.
     */
    @Throws(DataAccessException::class)
    operator fun get(fieldId: String): Any?

    /**
     * Returns the value of the field.
     * @param field The field to get the value of.
     * @return The value of the field.
     * @throws DataAccessException If the field does not exist or access to it is forbidden.
     */
    @Throws(DataAccessException::class)
    operator fun <T> get(field: AbstractField<T>): T

}

/**
 * Specification of the form.
 * @property fields List of fields in the form.
 */
interface FormSpec : KAnnotatedElement {

    /**
     * Fields inside this form.
     */
    val fields: List<AbstractField<*>>

}

/**
 * Shorthand for getting the specification of the form.
 * Equivalent to `form.spec()`.
 */
val <T : AbstractForm> T.spec: FormSpec
    get() = spec()

/**
 * Shorthand for getting fields of the form. Equivalent to `form.spec().fields`.
 */
val <T : AbstractForm> T.fields : List<AbstractField<*>>
    get() = spec().fields