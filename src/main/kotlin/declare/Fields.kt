package declare

import AbstractField
import AbstractForm
import common.ValueProvider
import FieldSpec
import common.LogicOp
import data.binary.BinarySource
import data.FormData
import requirements.FieldRequirement
import requirements.FieldRequirements
import requirements.ValueRequirement
import type.*
import kotlin.reflect.KProperty

/**
 * Declares single value field of type [ValueType] with the given parameters.
 * @param type Type of the field
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 */
fun <ValueType> Form.field(
    type: Type<ValueType>,
    defaultValue: ValueType? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None
) = Form.Field(
    fieldSpec = object : FieldSpec<ValueType> {
        override val type: Type<ValueType> = type
        override val defaultValue: ValueType? = defaultValue
        override val name: CharSequence? = name
        override val description: CharSequence? = description
        override val descriptionDetailed: CharSequence? = descriptionDetailed
        override val orderKey: Int = orderKey
        override val id: String = id
        override val enabledRules: FieldRequirements = enabledRules
    }
)

/**
 * Declares multi-value field of type [ValueType] with the given parameters.
 * @param elementType Type of the field
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param listRequirement The value requirement for the list field.
 * @param enabledRules Rules that enable or disable the field
 */
fun <ValueType> Form.listField(
    elementType: Type<ValueType>,
    defaultValue: List<ValueType>? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    listPreProcessor: ((List<ValueType>) -> List<ValueType>)? = null,
    listRequirement: ValueRequirement<List<ValueType>>? = null,
    enabledRules: FieldRequirements = FieldRequirements.None
) = field(
    type = list(
        elementType = elementType,
        preProcessor = listPreProcessor,
        requirement = listRequirement
    ),
    defaultValue = defaultValue,
    name = name,
    description = description,
    descriptionDetailed = descriptionDetailed,
    orderKey = orderKey,
    id = id,
    enabledRules = enabledRules
)

/**
 * Nullable version of [listField].
 * This field can hold a null value, which is useful for optional lists.
 */
fun <ValueType> Form.nullableListField(
    elementType: Type<ValueType>,
    defaultValue: List<ValueType>? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    listPreProcessor: ((List<ValueType>) -> List<ValueType>)? = null,
    listRequirement: ValueRequirement<List<ValueType>>? = null,
    enabledRules: FieldRequirements = FieldRequirements.None
) = field(
    type = list(
        elementType = elementType,
        preProcessor = listPreProcessor,
        requirement = listRequirement
    ).nullable,
    defaultValue = defaultValue,
    name = name,
    description = description,
    descriptionDetailed = descriptionDetailed,
    orderKey = orderKey,
    id = id,
    enabledRules = enabledRules
)

/**
 * Declares a boolean field with the given parameters.
 * This field can hold a boolean value, such as true or false.
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 */
fun Form.boolField(
    defaultValue: Boolean? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None
) = field(
    Type.Bool,
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Nullable version of [boolField].
 * This field can hold a null value, which is useful for optional boolean values.
 */
fun Form.nullableBoolField(
    defaultValue: Boolean? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None
) = field(
    Type.Bool.nullable,
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Declares an enum field with the given parameters.
 * This field can hold a value of type [T] which is an enum.
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 * @param requirement The value requirement for the enum data.
 */
inline fun <reified T : Enum<T>> Form.enumField(
    defaultValue: T? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None
) = field(
    Type.EnumType(T::class),
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Nullable version of [enumField].
 * This field can hold a null value, which is useful for optional enum values.
 */
inline fun <reified T : Enum<T>> Form.nullableEnumField(
    defaultValue: T? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None
) = field(
    Type.EnumType(T::class).nullable,
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Declares an integer field with the given parameters.
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 * @param requirement The value requirement for the integer field.
 */
fun Form.intField(
    defaultValue: Long? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    preProcessor: ((Long) -> Long)? = null,
    requirement: ValueRequirement<Long>? = null
) = field(
    Type.Integer(preProcessor, requirement),
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Nullable version of [intField].
 * This field can hold a null value, which is useful for optional integer values.
 */
fun Form.nullableIntField(
    defaultValue: Long? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    preProcessor: ((Long) -> Long)? = null,
    requirement: ValueRequirement<Long>? = null
) = field(
    Type.Integer(preProcessor, requirement).nullable,
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Declares a decimal field with the given parameters.
 * This field can hold a double value, such as a price or measurement.
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 * @param requirement The value requirement for the decimal data.
 */
fun Form.decimalField(
    defaultValue: Double? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    preProcessor: ((Double) -> Double)? = null,
    requirement: ValueRequirement<Double>? = null
) = field(
    Type.Decimal(preProcessor, requirement),
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Nullable version of [decimalField].
 * This field can hold a null value, which is useful for optional decimal values.
 */
fun Form.nullableDecimalField(
    defaultValue: Double? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    preProcessor: ((Double) -> Double)? = null,
    requirement: ValueRequirement<Double>? = null
) = field(
    Type.Decimal(preProcessor, requirement).nullable,
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Declares a text field with the given parameters.
 * This field can hold a string value, such as a name or description.
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 * @param requirement The value requirement for the text data.
 */
fun Form.textField(
    defaultValue: String? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    preProcessor: ((String) -> String)? = null,
    requirement: ValueRequirement<String>? = null
) = field(
    Type.Text(preProcessor, requirement),
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Nullable version of [textField].
 * This field can hold a null value, which is useful for optional text data.
 */
fun Form.nullableTextField(
    defaultValue: String? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    preProcessor: ((String) -> String)? = null,
    requirement: ValueRequirement<String>? = null
) = field(
    Type.Text(preProcessor, requirement).nullable,
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Declares a binary field with the given parameters.
 * This field can hold binary data, such as files or images.
 * @param mimeType MIME type of the binary data
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 * @param requirement The value requirement for the binary data.
 */
fun Form.binaryField(
    defaultValue: BinarySource? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    preProcessor: ((BinarySource) -> BinarySource)? = null,
    requirement: ValueRequirement<BinarySource>? = null
) = field(
    Type.Binary(preProcessor, requirement),
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Nullable version of [binaryField].
 * This field can hold a null value, which is useful for optional binary data.
 */
fun Form.nullableBinaryField(
    defaultValue: BinarySource? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    preProcessor: ((BinarySource) -> BinarySource)? = null,
    requirement: ValueRequirement<BinarySource>? = null
) = field(
    Type.Binary(preProcessor, requirement).nullable,
    defaultValue,
    name,
    description,
    descriptionDetailed,
    orderKey,
    id,
    enabledRules
)

/**
 * Declares a form list field with the given parameters.
 * This field can hold a list of forms of type [T].
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param elementRequirement The value requirement for each form data element.
 * @param listRequirement The value requirement for the list field.
 * @param enabledRules Rules that enable or disable the field
 */
inline fun <reified T : AbstractForm> Form.formListField(
    defaultValue: List<T>? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    noinline elementPreprocessor: ((T) -> T)? = null,
    elementRequirement: ValueRequirement<T>? = null,
    noinline listPreProcessor: ((List<T>) -> List<T>)? = null,
    listRequirement: ValueRequirement<List<T>>? = null,
    enabledRules: FieldRequirements = FieldRequirements.None,
) = listField(
    elementType = form(
        classFormFactory(T::class),
        elementPreprocessor,
        elementRequirement
    ),
    listPreProcessor = listPreProcessor,
    listRequirement = listRequirement,
    defaultValue = defaultValue,
    name = name,
    description = description,
    descriptionDetailed = descriptionDetailed,
    orderKey = orderKey,
    id = id,
    enabledRules = enabledRules
)

/**
 * Nullable version of [formListField].
 * This field can hold a list of nullable form data,
 * which is useful for optional lists of forms.
 */
inline fun <reified T : AbstractForm> Form.nullableFormListField(
    defaultValue: List<T>? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    noinline elementPreProcessor: ((T) -> T)? = null,
    elementRequirement: ValueRequirement<T>? = null,
    noinline listPreProcessor: ((List<T>) -> List<T>)? = null,
    listRequirement: ValueRequirement<List<T>>? = null,
    enabledRules: FieldRequirements = FieldRequirements.None
) = listField(
    elementType = form(
        formFactory = classFormFactory(T::class),
        preProcessor = elementPreProcessor,
        requirement = elementRequirement
    ),
    defaultValue = defaultValue,
    name = name,
    description = description,
    descriptionDetailed = descriptionDetailed,
    orderKey = orderKey,
    id = id,
    enabledRules = enabledRules,
    listPreProcessor = listPreProcessor,
    listRequirement = listRequirement
)

/**
 * Declares a form field with the given parameters.
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 * @param requirement The value requirement for the form data.
 */
inline fun <reified T : Form> Form.formField(
    defaultValue: T? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    enabledRules: FieldRequirements = FieldRequirements.None,
    noinline preProcessor: ((T) -> T)? = null,
    requirement: ValueRequirement<T>? = null
) = field(
    type = form(
        classFormFactory(T::class),
        preProcessor,
        requirement
    ),
    defaultValue = defaultValue,
    name = name,
    description = description,
    descriptionDetailed = descriptionDetailed,
    orderKey = orderKey,
    id = id,
    enabledRules = enabledRules
)

/**
 * Declares a map field with the given parameters.
 * @param keyType Type of the keys in the map
 * @param valueType Type of the values in the map
 * @param defaultValue Default value of the field
 * @param name Name of the field
 * @param description Description of the field
 * @param descriptionDetailed Detailed description of the field
 * @param orderKey Order key of the field. Defines the order of the field in the form.
 * higher numbers are positioned first, default is 0
 * @param id ID of the field or empty string to use property or class name for as an ID
 * @param enabledRules Rules that enable or disable the field
 */
fun <K, V> Form.mapField(
    keyType: Type<K>,
    valueType: Type<V>,
    defaultValue: Map<K, V>? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    preProcessor: ((Map<K, V>) -> Map<K, V>)? = null,
    requirement: ValueRequirement<Map<K, V>>? = null,
    enabledRules: FieldRequirements = FieldRequirements.None
) = field(
    type = map(keyType, valueType, preProcessor, requirement),
    defaultValue = defaultValue,
    name = name,
    description = description,
    descriptionDetailed = descriptionDetailed,
    orderKey = orderKey,
    id = id,
    enabledRules = enabledRules
)

/**
 * Nullable version of [mapField].
 * This field can hold a null value, which is useful for optional maps.
 */
fun <K, V> Form.nullableMapField(
    keyType: Type<K>,
    valueType: Type<V>,
    defaultValue: Map<K, V>? = null,
    name: CharSequence? = null,
    description: CharSequence? = null,
    descriptionDetailed: CharSequence? = null,
    orderKey: Int = 0,
    id: String = "",
    preProcessor: ((Map<K, V>) -> Map<K, V>)? = null,
    requirement: ValueRequirement<Map<K, V>>? = null,
    enabledRules: FieldRequirements = FieldRequirements.None
) = field(
    type = map(keyType, valueType, preProcessor, requirement).nullable,
    defaultValue = defaultValue,
    name = name,
    description = description,
    descriptionDetailed = descriptionDetailed,
    orderKey = orderKey,
    id = id,
    enabledRules = enabledRules
)

/**
 * Creates a [FieldRequirements] instance with the provided field requirement with a logical AND operation.
 * May be used to define `enabledRules` for a field.
 * @param rules The field requirements to include in the [FieldRequirements].
 */
fun rules(vararg rules: FieldRequirement<*>) = FieldRequirements(rules.toList(), LogicOp.And)

/**
 * Creates a [FieldRequirements] instance with the provided field requirements with a logical OR operation.
 * May be used to define `enabledRules` for a field.
 * @param rules The field requirements to include in the [FieldRequirements].
 */
fun orRules(vararg rules: FieldRequirement<*>) = FieldRequirements(rules.toList(), LogicOp.Or)

/**
 * Creates a [FieldRequirements] instance with the provided field requirements with a logical XOR operation.
 * May be used to define `enabledRules` for a field.
 * @param rules The field requirements to include in the [FieldRequirements].
 */
fun xorRules(vararg rules: FieldRequirement<*>) = FieldRequirements(rules.toList(), LogicOp.Xor)

/**
 * Creates a [FieldRequirements] instance with a single field requirement with a logical AND operation.
 * May be used to define `enabledRules` for a field.
 * @param rule The field requirement to include in the [FieldRequirements].
 */
fun rule(rule: FieldRequirement<*>) = rules(rule)
