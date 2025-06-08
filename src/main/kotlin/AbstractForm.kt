/**
 * Defines complex data tu\ype with a set of fields.
 */
interface AbstractForm {

    /**
     * Specification of the form.
     */
    fun spec(): FormSpec

}

/**
 * Specification of the form.
 * @property fields List of fields in the form.
 */
interface FormSpec : AbstractSpec {

    /**
     * Fields inside this form.
     */
    val fields: List<AbstractField<*>>

}