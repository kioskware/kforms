package declare

import AbstractField
import AbstractForm
import FormSpec
import data.FormData
import declare.parser.readFieldsFromClass
import kotlin.reflect.KClass

/**
 * Base class for forms.
 * @param name Default name of the form. Will be used when the `hostField` does not provide a name.
 * @param description Default description of the form. Will be used when the `hostField` does not provide a description.
 * @param descriptionDetailed Default detailed description of the form. Will be used when the `hostField` does not provide a detailed description.
 * @param annotations Additional annotations to be added to the form specification.
 * Will be merged with the annotations from the class. These annotations will be added to `hostField` specification.
 */
abstract class Form(
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    annotations: List<Annotation> = emptyList(),
) : AbstractForm {

    private companion object {
        /**
         * Stores the specifications of forms to avoid re-reading them from the class.
         *
         * We can cache the specs because by the convention,
         * specs are the same for each instance of the form with same class.
         */
        val specCaches = mutableMapOf<KClass<out Form>, FormSpec>()
    }

    private val _spec: FormSpec by lazy {
        val fields = readFieldsFromClass()
        specCaches.getOrPut(this::class) {
            object : FormSpec {
                override val name = name
                override val description = description
                override val descriptionDetailed = descriptionDetailed
                override val annotations: List<Annotation> = this@Form::class.annotations + annotations
                override val fields = fields
            }
        }
    }

    override fun spec(): FormSpec = _spec

    /**
     * Stores a reference to the field that hosts this form.
     */
    @PublishedApi
    internal var _hostField: AbstractField<*>? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("Host field is already set.")
            }
            field = value
        }

}

/**
 * Returns the host field of this [Form] instance.
 * @throws IllegalStateException if the host field is not set or is not of type [AbstractField<FormData<T>>].
 */
val <T : AbstractForm> T.requireHostField : AbstractField<FormData<T>>
    get() = hostField ?:
        throw IllegalStateException("Host field is not set or is not of type AbstractField<FormData<T>>")


@Suppress("UNCHECKED_CAST")
val <T : AbstractForm> T.hostField : AbstractField<FormData<T>>?
    get() = (this as? Form)?._hostField as? AbstractField<FormData<T>>