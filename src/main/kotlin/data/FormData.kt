package data

import AbstractField
import AbstractForm
import data.FormData.BuilderScope
import declare.Form
import declare.requireHostField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import type.Type
import requirements.processValue
import spec

/**
 * Builds a [FormData] object.
 * @param block The block to build the form data.
 * @return A [FormData] object. The object is immutable, but not already committed.
 */
fun <FormType : AbstractForm> FormType.build(
    commit: Boolean = true,
    block: BuilderScope<FormType>.() -> Unit
) : FormData<FormType> {
    return BuilderScope(this).apply(block).fields.let {
        MutableFormDataImpl(it, this).apply {
            if (commit) { commit() }
        }
    }
}

inline fun <reified FormType : AbstractForm> build(
    commit: Boolean = true,
    noinline block: BuilderScope<FormType>.() -> Unit
) : FormData<FormType> {
    val instance = FormType::class.java.getDeclaredConstructor().newInstance() // Ensure the form is instantiated
    return instance.build(commit, block)
}

/**
 * Represents a form data.
 */
interface FormData<FormType : AbstractForm> {

    /**
     * The [BaseForm] of the data.
     * Data will be parsed, validated and formatted according to this form.
     */
    val form: FormType

    /**
     * All fields present in the form data.
     * Shorthand for `form.spec().fields`.
     */
    val fields: List<AbstractField<*>>
        get() = form.spec.fields

    /**
     * Takes a snapshot of a whole form data and writes it into a map.
     * Useful for formatting data for serialization or data transfer.
     *
     * Map values should be only in one of the following types:
     * - [String]
     * - [Long]
     * - [Double]
     * - [Boolean]
     * - [ByteArray]
     * - [Enum]s
     * - [Map] with String keys and values of the types on this list
     * - [List] of the above types
     *
     * @return A map containing the data of the form. The map is immutable.
     */
    fun snapshot(): Map<String, Any?>

    /**
     * Returns the value of the specified field.
     * @param field The field to get the value from.
     * @return The value of the field.
     * @throws UnexpectedFieldException if the provided field is not part of the form.
     */
    @Throws(UnexpectedFieldException::class)
    operator fun <T> get(field: AbstractField<T>): T

    /**
     * Checks whether all required fields are provided.
     * @return A list of fields which values are missing.
     * If all required fields are provided, an empty list is returned.
     */
    fun commit() : List<AbstractField<*>>

    /**
     * Checks whether this form data is equal to another object.
     * Two form data objects are considered equal if they have the same form and the same data.
     * @param other The object to compare with.
     */
    override fun equals(other: Any?) : Boolean

    /**
     * Returns the hash code of this form data.
     * The hash code is calculated based on the form and the data.
     * @return The hash code of this form data.
     */
    override fun hashCode() : Int

    /**
     * Returns the value of the specified field.
     * Block is used to conveniently access the field from the form.
     * @param block The block to access the field.
     * @return The value of the field.
     */
    fun <T> get(block: FormType.() -> AbstractField<T>) : T = get(form.block())

    /**
     * Returns the [FormData] of the specified form field.
     * This is used to access nested forms.
     * @param block The block to access the form fields conveniently.
     */
    fun <T : AbstractForm> getFormData(block: FormType.() -> T) : FormData<T> = get { block(form).requireHostField }

    /**
     * Returns the value of the specified field.
     * Block is used to conveniently access the field from the form.
     * This is shorthand for `get(form.block())`.
     * @param block The block to access the field.
     * @return The value of the field.
     */
    operator fun <T> invoke(block: FormType.() -> AbstractField<T>) = get(block)

    /**
     * Scope responsible for building a form data.
     */
    abstract class BuilderScope<FormType : AbstractForm> {

        abstract fun form(): FormType

        /**
         * Used to access the form fields to put values into.
         */
        val key: FormType
            get() = form()

        protected abstract fun <T> onPutValue(
            field: AbstractField<T>,
            value: T
        )

        protected abstract fun onCreateBuilderScope(
            form: FormType
        ): BuilderScope<FormType>

        /**
         * Puts a form data into the field.
         * @param block The block to build the form data.
         */
        fun <FormType : AbstractForm> AbstractField<FormData<FormType>>.put(
            block: BuilderScope<FormType>.() -> Unit
        )

        /**
         * Puts a form data into the field.
         * @param block The block to build the form data.
         */
        fun <FormType : Form> FormType.put(
            block: BuilderScope<FormType>.() -> Unit
        ) = this.requireHostField.put(block)

        /**
         * Puts form data into the field.
         * @param data The form data to put into the field.
         */
        infix fun <FormType : AbstractForm> FormType.put(
            data: FormData<FormType>
        ) = this.requireHostField.putInternal(data)

        /**
         * Puts a form data into the field.
         * @param block The block to build the form data.
         */
        infix fun <FormType : AbstractForm> AbstractField<FormData<FormType>>.put(
            data: FormData<FormType>
        ) = putInternal(data)

        /**
         * Puts a value into the field.
         * @param value The value to put into the field.
         */
        infix fun <T : List<*>> AbstractField<T>.put(value: T) = putInternal(value)

        /**
         * Puts a number into the [Type.Integer] field.
         * @param value The number to put into the field, will be converted to [Long].
         */
        infix fun AbstractField<Long>.put(value: Number) = putInternal(value.toLong())

        /**
         * Puts a number into the [Type.Decimal] field.
         * @param value The number to put into the field, will be converted to [Double].
         */
        infix fun AbstractField<Double>.put(value: Number) = putInternal(value.toDouble())

        /**
         * Puts a text into the [Type.Text] field.
         * @param value The text to put into the field, will be converted to [String].
         */
        infix fun AbstractField<String>.put(value: CharSequence) = putInternal(value.toString())

        /**
         * Puts a boolean value into the [Type.Bool] field.
         * @param value The boolean value to put into the field.
         */
        infix fun AbstractField<Boolean>.put(value: Boolean) = putInternal(value)

        /**
         * Puts a binary data into the [Type.Binary] field.
         * @param value The binary data to put into the field.
         */
        infix fun AbstractField<ByteArray>.put(value: ByteArray) = putInternal(value)

        /**
         * Puts a single value into the field.
         * @param value The value to put into the field.
         * This is a shorthand for `put(listOf(value))`.
         */
        @JvmName("put7")
        infix fun <T : List<*>?> AbstractField<T>.put(value: T) = putInternal(value)

        /**
         * Puts a number into the [Type.Integer] field.
         * @param value The number to put into the field, will be converted to [Long].
         */
        @JvmName("put8")
        infix fun AbstractField<Long?>.put(value: Number) = putInternal(value.toLong())

        /**
         * Puts a number into the [Type.Decimal] field.
         * @param value The number to put into the field, will be converted to [Double].
         */
        @JvmName("put9")
        infix fun AbstractField<Double?>.put(value: Number) = putInternal(value.toDouble())

        /**
         * Puts a text into the [Type.Text] field.
         * @param value The text to put into the field, will be converted to [String].
         */
        @JvmName("put10")
        infix fun AbstractField<String?>.put(value: CharSequence) = putInternal(value.toString())

        /**
         * Puts a boolean value into the [Type.Bool] field.
         * @param value The boolean value to put into the field.
         */
        @JvmName("put11")
        infix fun AbstractField<Boolean?>.put(value: Boolean) = putInternal(value)

        /**
         * Puts a binary data into the [Type.Binary] field.
         * @param value The binary data to put into the field.
         */
        @JvmName("put12")
        infix fun AbstractField<ByteArray?>.put(value: ByteArray) = putInternal(value)


        private fun <T> AbstractField<T>.putInternal(value: T) {
            onPutValue(this, processValue(value))
        }

    }

}

/**
 * Represents a mutable form data.
 */
interface MutableFormData<FormType : AbstractForm> : FormData<FormType> {

    /**
     * Puts a value into the field.
     * @param field The field to put the value into.
     * @param value The value to put into the field.
     */
    fun <V> put(value: V, field: AbstractField<V>)

    /**
     * Puts a value into the field.
     * @param value The value to put into the field.
     * @param block The block to access the field conveniently.
     */
    fun <V> put(
        value: V,
        block: FormType.() -> AbstractField<V>
    ) = put(value, form.block())

    /**
     * Puts a form data into the field.
     * @param data The form data to put into the field.
     * @param block The block to access the field conveniently.
     */
    fun <T : AbstractForm> put(
        data: FormData<T>,
        block: FormType.() -> T
    ) { put(data, form.block().requireHostField) }

}

/**
 * Converts a [MutableFormData] to an immutable [FormData].
 * Returned [FormData] is a read-only view of the mutable data, no [MutableFormData.commit] will be called.
 * @return An immutable [FormData] object.
 */
fun <FormType : AbstractForm> MutableFormData<FormType>.asImmutable()
= object : FormData<FormType> by this {}




class MutableFormDataImpl<T : AbstractForm>(
    override val form: T,
    private val data: MutableMap<String, Any?> = mutableMapOf()
) : MutableFormData<T> {

    override fun <V> put(value: V, field: AbstractField<V>) {
        TODO("Not yet implemented")
    }

    override fun snapshot(): Map<String, Any?> {
        TODO("Not yet implemented")
    }

    override fun <T> get(field: AbstractField<T>): T {
        TODO("Not yet implemented")
    }

    override fun commit(): List<AbstractField<*>> {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun hashCode(): Int
    = arrayOf<Any?>(form, snapshot()).contentDeepHashCode()

}

fun main() {

}