package kioskware.kforms.declare


import kioskware.kforms.*
import kioskware.kforms.common.LogicOp
import kioskware.kforms.data.FormDataMap
import kioskware.kforms.data.ValidationConfig
import kioskware.kforms.data.binary.BinarySource
import kioskware.kforms.data.toFormDataMap
import kioskware.kforms.declare.KForm.Field
import kioskware.kforms.requirements.FieldRequirement
import kioskware.kforms.requirements.FieldRequirements
import kioskware.kforms.requirements.ValueRequirement
import kioskware.kforms.requirements.require
import kioskware.kforms.scopes.AccessScope
import kioskware.kforms.scopes.grantsAccessTo
import kioskware.kforms.type.*
import kotlin.experimental.ExperimentalTypeInference
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
 * 1. Extend the [KForm] class.
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
 * open class File : KForm() {
 *
 *     val name by textField {
 *         name = "File Name"
 *         description = "Name of the file"
 *     }
 *
 *     val size by intField {
 *         name = "File Size"
 *         description = "Size of the file in bytes"
 *     }
 *
 *     val createdAt by intField {
 *         name = "Created At"
 *         description = "Creation date and time of the file"
 *     }
 *
 * }
 *
 * class Directory : File() {
 *
 *     val files by formListField<File> {
 *         name = "Files"
 *         description = "List of files in the directory"
 *     }
 *
 *     val subdirectories by formListField<Directory> {
 *         name = "Subdirectories"
 *         description = "List of subdirectories in the directory"
 *     }
 *
 * }
 *
 * ```
 *
 */
@Suppress("NOTHING_TO_INLINE")
abstract class KForm : AbstractForm {

    companion object {
        /**
         * Stores the specifications of forms to avoid re-reading them from the class.
         *
         * We can cache the specs because by the convention,
         * specs are the same for each instance of the form with same class.
         */
        private val _specCaches = mutableMapOf<KClass<out KForm>, FormSpec>()
    }

    private var _data: FormDataMap? = null
    private var _fieldN: Int = 0
    private val _spec: FormSpec by lazy { _specCaches.getOrPut(this::class) { resolveSpec() } }
    private var _hashCode: Int? = null
    private var _afterFirstInit = false

    private val _initializer = object : AbstractFormInitializer() {
        override fun onInitialize(data: Map<String, *>, validationConfig: ValidationConfig) {
            // Ensure spec is resolved before initializing data
            _spec // Accessing spec to ensure it is resolved
            _afterFirstInit = true // Mark that the form has been initialized at least once
            this@KForm._data = data.toFormDataMap(this@KForm, validationConfig)
            this@KForm._hashCode = null // Reset hash code to ensure it is recalculated
        }

        override fun onDestroy() {
            _data = null
        }

        override val isInitialized: Boolean
            get() = _data != null
    }

    /**
     * For internal use only. Do not use directly.
     */
    override fun initializer() = _initializer

    override fun data() = _data ?: throw IllegalStateException("Form is not initialized.")

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

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: AbstractField<T>): T {
        ensureInitialized
        // Check if the field belongs to this form
        // compare by classes not instances, because
        // fields are reused across instances of the same form class
        if (field.spec.owner::class != this::class) {
            throw UnexpectedFieldException(field)
        }
        // Check if the field is accessible
        // with current validation config
        if (!initializer().validationConfig.params.accessScope.grantsAccessTo(field.spec.accessScope)) {
            throw ForbiddenFieldAccessException(field.id, this::class)
        }
        return data().let {
            it[field.id] as T
        }
    }

    //region Any Methods

    /**
     * Returns a string representation of the form data.
     * This is useful for debugging purposes.
     */
    override fun toString(): String {
        val className = this::class.simpleName ?: "Form"
        val fieldsString = _data?.entries?.joinToString(", ") { (key, value) ->
            "$key=$value"
        } ?: "<uninitialized>"
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
        if (other !is KForm) return false
        if (this::class != other::class) return false
        return _data == other._data
    }

    /**
     * Returns the hash code of the form.
     * The hash code is computed based on the class and the data of the form.
     * @return the hash code of the form.
     */
    override fun hashCode(): Int {
        return _hashCode?.let {
            return it
        } ?: (31 * this::class.hashCode() + _data.hashCode()).also {
            _hashCode = it
        }
    }

    //endregion

    //region Field Builder Scope

    /**
     * Declares single value field of type [T] with the given parameters.
     * @param type Type of the field
     * @param defaultValue Default value of the field
     * @param name Name of the field
     * @param description Description of the field
     * @param descriptionDetailed Detailed description of the field
     * @param orderKey Order key of the field. Defines the order of the field in the form.
     * higher numbers are positioned first, default is 0
     * @param id ID of the field or empty string to use property or class name for as an ID
     * @param enabledRules Rules that enable or disable the field
     * @param accessScope Access scope required to access the field.
     * @param annotations Additional annotations to be added to the field specification.
     * @param examples Example values of the field, used for documentation purposes or for AI models.
     * @param sensitive Whether the field may hold sensitive data or not.
     */
    @KFormDsl
    inner class FieldBuilderScope<T> internal constructor(
        val type: Type<T>,
        private val n: Int,
        @KFormDsl var defaultValue: T? = null,
        @KFormDsl var name: CharSequence? = null,
        @KFormDsl var description: CharSequence? = null,
        @KFormDsl var descriptionDetailed: CharSequence? = null,
        @KFormDsl var orderKey: Int = 0,
        @KFormDsl var id: String = "",
        @KFormDsl var enabledRules: FieldRequirements = FieldRequirements.None,
        @KFormDsl var accessScope: AccessScope? = AccessScope.None,
        @KFormDsl var annotations: List<Annotation> = emptyList(),
        @KFormDsl var examples: List<T>? = null,
        @KFormDsl var sensitive: Boolean = false
    ) {

        private val _extras: MutableMap<String, String> = mutableMapOf()

        /**
         * Puts an extra key-value pair into the field.
         * Extras are additional metadata for the field,
         * used for documentation or for AI models.
         */
        fun putExtra(key: String, value: String) {
            _extras[key] = value
        }

        /**
         * Sets the extras for the field.
         * Extras are additional metadata for the field, used for documentation purposes or for AI models.
         */
        @OptIn(ExperimentalTypeInference::class)
        @KFormDsl
        fun extras(@BuilderInference builderAction: MutableMap<String, String>.() -> Unit) =
            buildMap(builderAction).let { _extras.putAll(it) }

        /**
         * Builds the field with the specified parameters.
         * @return the created field.
         */
        internal fun build(): Field<T> = Field(
            type = type,
            defaultValue = defaultValue,
            name = name,
            description = description,
            descriptionDetailed = descriptionDetailed,
            orderKey = orderKey,
            id = id,
            enabledRules = enabledRules,
            annotations = annotations,
            accessScope = accessScope,
            examples = examples,
            sensitive = sensitive,
            extras = _extras.toMap(),
            n = n
        )

    }

    //endregion

    /**
     * Declares a field of type [ValueType] with the given parameters.
     * This function is used to create a field of a specific type
     *
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected fun <ValueType> Type<ValueType>.field(
        block: FieldBuilderScope<ValueType>.() -> Unit = {}
    ): Field<ValueType> {
        if (_afterFirstInit) {
            throw FormDeclarationException("Cannot declare fields after the form is initialized.")
        }
        val n = _fieldN++
        //Check if the field is already cached in the spec cache
        @Suppress("UNCHECKED_CAST")
        val cachedField =
            _specCaches[this@KForm::class]?.fields?.find { (it as? Field<*>)?.n == n } as? Field<ValueType>
        if (cachedField != null) {
            return cachedField
        }
        // Create a new field if not cached
        return FieldBuilderScope(this, n)
            .apply(block).build()
    }

    // region Field declaration extensions

    /**
     * Declares a multi-value field of type [ElementType] with the given parameters.
     * This function is used to create a field that holds a list of elements of a specific type.
     *
     * @param listPreProcessor Optional lambda to preprocess the list before validation or usage.
     * @param requireList Optional value requirement for the list field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <ElementType> Type<ElementType>.listField(
        noinline listPreProcessor: ((List<ElementType>) -> List<ElementType>)? = null,
        requireList: ValueRequirement<List<ElementType>>? = null,
        noinline block: FieldBuilderScope<List<ElementType>>.() -> Unit = {}
    ) = list(
        elementType = this,
        preProcessor = listPreProcessor,
        requirement = requireList
    ).field(block)

    /**
     * Declares a nullable multi-value field of type [ElementType] with the given parameters.
     * This function is used to create a field that holds a nullable list of elements of a specific type.
     *
     * @param listPreProcessor Optional lambda to preprocess the list before validation or usage.
     * @param requireList Optional value requirement for the list field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <ElementType> Type<ElementType>.nullableListField(
        noinline listPreProcessor: ((List<ElementType>) -> List<ElementType>)? = null,
        requireList: ValueRequirement<List<ElementType>>? = null,
        noinline block: FieldBuilderScope<List<ElementType>?>.() -> Unit = {}
    ) = list(
        elementType = this,
        preProcessor = listPreProcessor,
        requirement = requireList
    ).nullable.field(block)

    /**
     * Declares a boolean field with the given parameters.
     * This function is used to create a field that holds a boolean value (true or false).
     *
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun boolField(
        noinline block: FieldBuilderScope<Boolean>.() -> Unit = {}
    ) = bool.field(block)

    /**
     * Declares a nullable boolean field with the given parameters.
     * This function is used to create a field that holds a nullable boolean value.
     *
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun nullableBoolField(
        noinline block: FieldBuilderScope<Boolean?>.() -> Unit = {}
    ) = Type.Bool.nullable.field(block)

    /**
     * Declares an enum field of type [T] with the given parameters.
     * This function is used to create a field that holds a value of a specific enum type.
     *
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <reified T : Enum<T>> enumField(
        noinline block: FieldBuilderScope<T>.() -> Unit = {}
    ) = Type.EnumType(T::class).field(block)

    /**
     * Declares a nullable enum field of type [T] with the given parameters.
     * This function is used to create a field that holds a nullable value of a specific enum type.
     *
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <reified T : Enum<T>> nullableEnumField(
        noinline block: FieldBuilderScope<T?>.() -> Unit = {}
    ) = Type.EnumType(T::class).nullable.field(block)

    /**
     * Declares an integer field with the given parameters.
     * This function is used to create a field that holds an integer value.
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the integer field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun intField(
        noinline preProcessor: ((Long) -> Long)? = null,
        require: ValueRequirement<Long>? = null,
        noinline block: FieldBuilderScope<Long>.() -> Unit = {}
    ) = Type.Integer(preProcessor, require).field(block)

    /**
     * Declares a nullable integer field with the given parameters.
     * This function is used to create a field that holds a nullable integer value.
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the integer field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun nullableIntField(
        noinline preProcessor: ((Long) -> Long)? = null,
        require: ValueRequirement<Long>? = null,
        noinline block: FieldBuilderScope<Long?>.() -> Unit = {}
    ) = Type.Integer(preProcessor, require).nullable.field(block)

    /**
     * Declares a decimal field with the given parameters.
     * This function is used to create a field that holds a double value (e.g., price or measurement).
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the decimal field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun decimalField(
        noinline preProcessor: ((Double) -> Double)? = null,
        require: ValueRequirement<Double>? = null,
        noinline block: FieldBuilderScope<Double>.() -> Unit = {}
    ) = Type.Decimal(preProcessor, require).field(block)

    /**
     * Declares a nullable decimal field with the given parameters.
     * This function is used to create a field that holds a nullable double value.
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the decimal field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun nullableDecimalField(
        noinline preProcessor: ((Double) -> Double)? = null,
        require: ValueRequirement<Double>? = null,
        noinline block: FieldBuilderScope<Double?>.() -> Unit = {}
    ) = Type.Decimal(preProcessor, require).nullable.field(block)

    /**
     * Declares a text field with the given parameters.
     * This function is used to create a field that holds a string value, such as a name or description.
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the text field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun textField(
        noinline preProcessor: ((String) -> String)? = null,
        require: ValueRequirement<String>? = null,
        noinline block: FieldBuilderScope<String>.() -> Unit = {}
    ) = Type.Text(preProcessor, require).field(block)

    /**
     * Declares a nullable text field with the given parameters.
     * This function is used to create a field that holds a nullable string value.
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the text field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun nullableTextField(
        noinline preProcessor: ((String) -> String)? = null,
        require: ValueRequirement<String>? = null,
        noinline block: FieldBuilderScope<String?>.() -> Unit = {}
    ) = Type.Text(preProcessor, require).nullable.field(block)

    /**
     * Declares a binary field with the given parameters.
     * This function is used to create a field that holds binary data, such as files or images.
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the binary field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun binaryField(
        noinline preProcessor: ((BinarySource) -> BinarySource)? = null,
        require: ValueRequirement<BinarySource>? = null,
        noinline block: FieldBuilderScope<BinarySource>.() -> Unit = {}
    ) = Type.Binary(preProcessor, require).field(block)

    /**
     * Declares a nullable binary field with the given parameters.
     * This function is used to create a field that holds a nullable binary value.
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the binary field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun nullableBinaryField(
        noinline preProcessor: ((BinarySource) -> BinarySource)? = null,
        require: ValueRequirement<BinarySource>? = null,
        noinline block: FieldBuilderScope<BinarySource?>.() -> Unit = {}
    ) = Type.Binary(preProcessor, require).nullable.field(block)

    /**
     * Declares a form list field with the given parameters.
     * This function is used to create a field that holds a list of forms of type [T].
     *
     * @param elementPreprocessor Optional lambda to preprocess each form before validation or usage.
     * @param requireElement Optional value requirement for each form in the list.
     * @param listPreProcessor Optional lambda to preprocess the list before validation or usage.
     * @param requireList Optional value requirement for the list field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <reified T : AbstractForm> formListField(
        noinline elementPreprocessor: ((T) -> T)? = null,
        requireElement: ValueRequirement<T>? = null,
        noinline listPreProcessor: ((List<T>) -> List<T>)? = null,
        requireList: ValueRequirement<List<T>>? = null,
        noinline block: FieldBuilderScope<List<T>>.() -> Unit = {}
    ) = form(
        classFormFactory(T::class),
        elementPreprocessor,
        requireElement
    ).listField(
        listPreProcessor = listPreProcessor,
        requireList = requireList,
        block = block
    )

    /**
     * Declares a nullable form list field with the given parameters.
     * This function is used to create a field that holds a nullable list of forms of type [T].
     *
     * @param elementPreProcessor Optional lambda to preprocess each form before validation or usage.
     * @param requireElement Optional value requirement for each form in the list.
     * @param listPreProcessor Optional lambda to preprocess the list before validation or usage.
     * @param requireList Optional value requirement for the list field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <reified T : AbstractForm> nullableFormListField(
        noinline elementPreProcessor: ((T) -> T)? = null,
        requireElement: ValueRequirement<T>? = null,
        noinline listPreProcessor: ((List<T>) -> List<T>)? = null,
        requireList: ValueRequirement<List<T>>? = null,
        noinline block: FieldBuilderScope<List<T>?>.() -> Unit = {}
    ) = form(
        formFactory = classFormFactory(T::class),
        preProcessor = elementPreProcessor,
        requirement = requireElement
    ).nullableListField(
        listPreProcessor = listPreProcessor,
        requireList = requireList,
        block = block
    )

    /**
     * Declares a form field with the given parameters.
     * This function is used to create a field that holds a nested form of type [T].
     *
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the form field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <reified T : KForm> formField(
        noinline preProcessor: ((T) -> T)? = null,
        require: ValueRequirement<T>? = null,
        noinline block: FieldBuilderScope<T>.() -> Unit = {}
    ) = form(
        classFormFactory(T::class),
        preProcessor,
        require
    ).field(block)

    /**
     * Declares a map field with the given parameters.
     * This function is used to create a field that holds a map of key-value pairs.
     *
     * @param keyType Type of the keys in the map.
     * @param valueType Type of the values in the map.
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the map field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <K, V> mapField(
        keyType: Type<K>,
        valueType: Type<V>,
        noinline preProcessor: ((Map<K, V>) -> Map<K, V>)? = null,
        require: ValueRequirement<Map<K, V>>? = null,
        noinline block: FieldBuilderScope<Map<K, V>>.() -> Unit = {}
    ) = map(keyType, valueType, preProcessor, require).field(block)

    /**
     * Declares a nullable map field with the given parameters.
     * This function is used to create a field that holds a nullable map of key-value pairs.
     *
     * @param keyType Type of the keys in the map.
     * @param valueType Type of the values in the map.
     * @param preProcessor Optional lambda to preprocess the value before validation or usage.
     * @param require Optional value requirement for the map field.
     * @param block A lambda that configures the field using [FieldBuilderScope].
     * @return the created field.
     */
    protected inline fun <K, V> nullableMapField(
        keyType: Type<K>,
        valueType: Type<V>,
        noinline preProcessor: ((Map<K, V>) -> Map<K, V>)? = null,
        require: ValueRequirement<Map<K, V>>? = null,
        noinline block: FieldBuilderScope<Map<K, V>?>.() -> Unit = {}
    ) = map(keyType, valueType, preProcessor, require).nullable.field(block)

    //endregion

    //region Enabled Rules declaration

    /**
     * Creates a [FieldRequirements] instance with the provided field requirement with a logical AND operation.
     * May be used to define `enabledRules` for a field.
     * @param rules The field requirements to include in the [FieldRequirements].
     */
    protected inline fun rules(vararg rules: FieldRequirement<*>) = FieldRequirements(rules.toList(), LogicOp.And)

    /**
     * Creates a [FieldRequirements] instance with the provided field requirements with a logical OR operation.
     * May be used to define `enabledRules` for a field.
     * @param rules The field requirements to include in the [FieldRequirements].
     */
    protected inline fun orRules(vararg rules: FieldRequirement<*>) = FieldRequirements(rules.toList(), LogicOp.Or)

    /**
     * Creates a [FieldRequirements] instance with the provided field requirements with a logical XOR operation.
     * May be used to define `enabledRules` for a field.
     * @param rules The field requirements to include in the [FieldRequirements].
     */
    protected inline fun xorRules(vararg rules: FieldRequirement<*>) = FieldRequirements(rules.toList(), LogicOp.Xor)

    /**
     * Creates a [FieldRequirements] instance with a single field requirement with a logical AND operation.
     * May be used to define `enabledRules` for a field.
     * @param rule The field requirement to include in the [FieldRequirements].
     */
    protected inline fun rule(rule: FieldRequirement<*>) = rules(rule)

    /**
     * Creates a requirement for the value of the field.
     * @receiver the property of the field that requires some value format.
     * @param requirement the value requirement for the field.
     */
    protected inline infix fun <T> KProperty<T>.require(
        requirement: ValueRequirement<T>
    ) = name.require(requirement)

    //endregion

    /**
     * Internal function that reads the specification of the form from the class.
     */
    private fun resolveSpec() = object : FormSpec {
        override val annotations: List<Annotation> = this@KForm::class.annotations
        override val fields by lazy {
            this@KForm::class.memberProperties
                .filterIsInstance<KProperty1<KForm, *>>()
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
                    (property.getDelegate(this@KForm) as? Field<*>).also {
                        // If the field is found, set its property reference
                        it?.property = property
                    }
                }
        }
    }

    //region Field class

    /**
     * Field of the form.
     */
    open inner class Field<T> internal constructor(
        internal val n: Int,
        type: Type<T>,
        defaultValue: T?,
        name: CharSequence?,
        description: CharSequence?,
        descriptionDetailed: CharSequence?,
        orderKey: Int,
        id: String,
        enabledRules: FieldRequirements,
        annotations: List<Annotation>,
        accessScope: AccessScope?,
        sensitive: Boolean,
        examples: List<T>?,
        extras: Map<String, String>?
    ) : AbstractField<T> {

        internal var property: KProperty<*>? = null

        private val _fieldSpec by lazy {
            object : FieldSpec<T> {
                override val id by lazy { resolveFieldId(id, property) }
                override val type get() = type
                override val owner get() = this@KForm
                override val defaultValue get() = defaultValue
                override val docName get() = name
                override val description get() = description
                override val descriptionDetailed get() = descriptionDetailed
                override val orderKey get() = orderKey
                override val enabledRules get() = enabledRules
                override val annotations get() = annotations + (property?.annotations ?: emptyList())
                override val accessScope: AccessScope? get() = accessScope
                override val sensitive get() = sensitive
                override val examples get() = examples
                override val extras get() = extras
            }
        }

        operator fun getValue(thisRef: KForm, property: KProperty<*>): T {
            if (this.property == null) {
                // If the property is not set, set it to the current property
                this.property = property
            } else if (this.property != property) {
                // If the property is already set, but it's different from the current one,
                // throw an exception
                throw FormDeclarationException(
                    "Field '${this.id}' is already declared with a different property: " +
                            "${this.property?.name} vs ${property.name}"
                )
            }
            return thisRef[this@Field]
        }

        override fun spec(): FieldSpec<T> = _fieldSpec

        private inline fun resolveFieldId(id: String, property: KProperty<*>?): String {
            return id.ifEmpty {
                // When id is not provided, use the property name
                property?.name ?: throw FormDeclarationException(
                    "Cannot resolve field ID: no property name available."
                )
            }
        }
    }

    //endregion

    //region Form Builder

    /**
     * Builder scope for creating forms.
     */
    class BuilderScope<T : AbstractForm>
    @PublishedApi
    internal constructor(
        formFactory: () -> T,
        val validationConfig: ValidationConfig,
        initialData: MutableMap<String, Any?>? = null
    ) {

        private val _form = formFactory().ensureNotInitialized
        private val _data: MutableMap<String, Any?> = initialData ?: LinkedHashMap(_form.fields.size)

        /**
         * Reference to the form being built.
         *
         * Should be used to access the form's properties and put values into them.
         *
         * Example:
         *
         * ```kotlin
         * val key: MyForm = build {
         *     key::exampleField put "example value"
         * }
         * ```
         */
        val key: T = _form

        /**
         * Puts all fields from another form into this form's data map.
         * This operation is lenient what means that it does not check
         * if the fields from the other form are compatible with this form.
         *
         * Possible exceptions will be thrown after calling [build] method if data is not compatible.
         */
        fun merge(other: AbstractForm): Unit = merge(other.data())

        /**
         * Puts all fields from another map into this form's data map.
         * This operation is lenient what means that it does not check
         * if the fields from the other map are compatible with this form.
         *
         * Possible exceptions will be thrown after calling [build] method if data is not compatible.
         */
        fun merge(other: Map<String, *>) {
            _data.putAll(other)
        }

        /**
         * Puts a value into the form's data map.
         * @receiver the key of the form to put the value into.
         * @param value the value to put into the form's data map.
         */
        infix fun String.put(value: Any?) {
            _data[this] = value
        }

        @JvmName("p1")
        infix fun KProperty<Long>.put(value: Number?) = name put value?.toLong()

        @JvmName("p1n")
        infix fun KProperty<Long?>.put(value: Number?) = name put value?.toLong()

        @JvmName("p2")
        infix fun KProperty<Double>.put(value: Number?) = name put value?.toDouble()

        @JvmName("p2n")
        infix fun KProperty<Double?>.put(value: Number?) = name put value?.toDouble()

        @JvmName("p3")
        inline infix fun KProperty<String>.put(value: String?) = name put value

        @JvmName("p3n")
        infix fun KProperty<String?>.put(value: String?) = name put value

        @JvmName("p4")
        inline infix fun KProperty<Boolean>.put(value: Boolean?) = name put value

        @JvmName("p4n")
        inline infix fun KProperty<Boolean?>.put(value: Boolean?) = name put value

        @JvmName("p5")
        inline infix fun KProperty<BinarySource>.put(value: BinarySource?) = name put value

        @JvmName("p5n")
        inline infix fun KProperty<BinarySource?>.put(value: BinarySource?) = name put value

        @JvmName("p6")
        inline infix fun <T : Enum<T>> KProperty<Enum<T>>.put(value: T?) = name put value

        @JvmName("p6n")
        inline infix fun <T : Enum<T>> KProperty<Enum<T>?>.put(value: T?) = name put value

        @JvmName("p7")
        inline infix fun <T : AbstractForm> KProperty<T>.putForm(value: T?) = name put value

        @JvmName("p7n")
        inline infix fun <T : AbstractForm> KProperty<T?>.putForm(value: T?) = name put value

        @JvmName("p8")
        inline infix fun <K, V> KProperty<Map<K, V>>.put(value: Map<K, V>?) = name put value

        @JvmName("p8n")
        inline infix fun <K, V> KProperty<Map<K, V>?>.put(value: Map<K, V>?) = name put value

        @JvmName("p9")
        inline infix fun <T> KProperty<List<T>>.put(value: List<T>?) = name put value

        @JvmName("p9n")
        inline infix fun <T> KProperty<List<T>?>.put(value: List<T>?) = name put value

        /**
         * Puts a field into the form's data map.
         * @receiver the property of the form to put the field into.
         * @param validationConfig the configuration for validation of the field.
         * @param block the block to configure the field.
         */
        inline fun <reified T : AbstractForm> KProperty<T>.put(
            validationConfig: ValidationConfig = this@BuilderScope.validationConfig,
            noinline block: BuilderScope<T>.() -> Unit
        ) = name.put(build<T>(validationConfig, block))

        /**
         * Builds the form with the provided data and validation configuration.
         * This method should be called after all fields are set.
         * @return the built form of type [T].
         */
        fun build(): T = _form.apply {
            initializer().initialize(
                data = _data,
                validationConfig = validationConfig
            )
        }

    }

    //endregion

}

/**
 * Gets the [KForm.Field] by its ID.
 * @param id the ID of the field.
 * @return the field if found, or null if not found.
 */
fun KForm.getFieldById(id: String): AbstractField<*>? = ensureInitialized.fields.find { it.id == id }

/**
 * Gets the [KForm.Field] by its property.
 * @param property the property of the field.
 * @return the field if found, or null if not found.
 */
@Suppress("UNCHECKED_CAST")
fun <T> KForm.getFieldByProperty(property: KProperty<T>): AbstractField<T>? = fields
    .mapNotNull { it as? Field<T> }
    .find { it.property?.name == property.name }


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

