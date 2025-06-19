package kioskware.kforms

import kioskware.kforms.data.FormDataMap
import kioskware.kforms.data.ValidationConfig
import kioskware.kforms.declare.KForm
import kioskware.kforms.type.Type
import kioskware.kforms.type.classFormFactory
import kioskware.kforms.type.form
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

/**
 * Defines complex data tu\ype with a set of fields.
 */
interface AbstractForm {

    /**
     * Specification of the form.
     */
    fun spec(): FormSpec

    /**
     * Form internal use only. Do not use it directly.
     */
    fun initializer(): AbstractFormInitializer

    /**
     * Returns the data of the form as a map.
     * The map contains field IDs as keys and their values as values.
     * @return A map containing the data of the form.
     */
    fun data(): FormDataMap

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

abstract class AbstractFormInitializer {

    private var currentValidation: ValidationConfig? = null

    /**
     * Current validation configuration of the form.
     * If the form is not initialized yet, returns the default validation configuration.
     */
    val validationConfig: ValidationConfig
        get() = currentValidation ?: ValidationConfig.Default

    /**
     * Initializes the form with the given data.
     * This function should be called before accessing any fields of the form.
     * Method used only for internal purposes.
     * To build a form with data, use builder methods instead.
     * @param data the data to initialize the form with.
     * @param validationConfig the configuration for validation of the form data.
     */
    internal fun initialize(
        data: Map<String, *>,
        validationConfig: ValidationConfig
    ) {
        if (isInitialized) {
            throw IllegalStateException("Form is already initialized.")
        }
        onInitialize(data, validationConfig)
        currentValidation = validationConfig
    }

    /**
     * Performs actual initialization logic.
     * @see initialize
     */
    protected abstract fun onInitialize(
        data: Map<String, *>,
        validationConfig: ValidationConfig
    )

    /**
     * Performs actual destruction logic.
     */
    protected abstract fun onDestroy()

    /**
     * Checks if the form is initialized.
     */
    abstract val isInitialized: Boolean

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
val <T : AbstractForm> T.fields: List<AbstractField<*>>
    get() = spec().fields

/**
 * Shorthand for getting the type of the form.
 */
val <T : AbstractForm> T.type: Type.FormType<T>
    get() = form(classFormFactory(this::class))

/**
 * Builds a form of type [T] using the provided class and block.
 * The block is executed in the context of [KForm.BuilderScope].
 *
 * @param formClass the class of the form to build.
 * @param validationConfig the configuration for validation of the form data.
 * @param block the block to provide the form data.
 * @return the built form of type [T].
 */
fun <T : AbstractForm> build(
    formFactory: () -> T,
    validationConfig: ValidationConfig = ValidationConfig.Default,
    block: KForm.BuilderScope<T>.() -> Unit
): T = KForm.BuilderScope(formFactory, validationConfig).apply(block).build()

/**
 * Builds a form of type [T] using the provided class and block.
 * The block is executed in the context of [KForm.BuilderScope].
 *
 * @param formClass the class of the form to build.
 * @param validationConfig the configuration for validation of the form data.
 * @param block the block to provide the form data.
 * @return the built form of type [T].
 */
fun <T : AbstractForm> build(
    formClass: KClass<T>,
    validationConfig: ValidationConfig = ValidationConfig.Default,
    block: KForm.BuilderScope<T>.() -> Unit
): T = build(classFormFactory(formClass), validationConfig, block)

/**
 * Builds a form of type [T] using the provided block.
 * The block is executed in the context of [KForm.BuilderScope].
 *
 * @param block the block to configure the form.
 * @return the built form of type [T].
 */
inline fun <reified T : AbstractForm> build(
    validationConfig: ValidationConfig = ValidationConfig.Default,
    noinline block: KForm.BuilderScope<T>.() -> Unit
): T = build(classFormFactory(T::class), validationConfig, block)

/**
 * Builds a form of type [T] using the provided data map.
 * The data map is processed to match the form's fields and their types.
 *
 * @param formClass the class of the form
 * @param data the data map to initialize the form with.
 * @param validationConfig the configuration for validation of the form data.
 * @return the built form of type [T].
 */
inline fun <T : AbstractForm> build(
    formFactory: () -> T,
    data: Map<String, *>,
    validationConfig: ValidationConfig = ValidationConfig.Default
): T = build(formFactory(), data, validationConfig)

/**
 * Builds a form of type [T] using the provided class and data map.
 * The data map is processed to match the form's fields and their types.
 *
 * @param formClass the class of the form to build.
 * @param data the data map to initialize the form with.
 * @param validationConfig the configuration for validation of the form data.
 * @return the built form of type [T].
 */
fun <T : AbstractForm> build(
    formClass: KClass<T>,
    data: Map<String, *>,
    validationConfig: ValidationConfig = ValidationConfig.Default
): T = build(classFormFactory(formClass), data, validationConfig)

/**
 * Builds a form of type [T] using the provided data map.
 * The data map is processed to match the form's fields and their types.
 *
 * @param data the data map to initialize the form with.
 * @param validationConfig the configuration for validation of the form data.
 * @return the built form of type [T].
 */
inline fun <reified T : AbstractForm> build(
    data: Map<String, *>,
    validationConfig: ValidationConfig = ValidationConfig.Default
): T = build(T::class, data, validationConfig)

/**
 * Builds a form of type [T] using the provided form and data map.
 * The data map is processed to match the form's fields and their types.
 *
 * @param form the form to initialize with data.
 * @param initialData the data map to initialize the form with.
 * @param validationConfig the configuration for validation of the form data.
 * @return the initialized form of type [T].
 */
fun <T : AbstractForm> build(
    form: T,
    initialData: Map<String, *>,
    validationConfig: ValidationConfig = ValidationConfig.Default,
): T = form.apply {
    initializer().initialize(
        data = initialData,
        validationConfig = validationConfig
    )
}

/**
 * Copies the form with the provided data map.
 * The data map is processed to match the form's fields and their types.
 *
 * @param validationConfig the configuration for validation of the form data.
 * @return a new instance of the form with the provided data.
 */
inline fun <T : AbstractForm> T.copy(
    validationConfig: ValidationConfig = ValidationConfig.Default,
    crossinline block: KForm.BuilderScope<out T>.() -> Unit = {}
): T = build(this::class, validationConfig) {
    merge(this@copy)
    block(this)
}

