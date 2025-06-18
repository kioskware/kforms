@file:Suppress("ClassName", "UNCHECKED_CAST")

package type

import AbstractForm
import FormDeclarationException
import data.binary.BinarySource
import requirements.ValueRequirement
import kotlin.reflect.KClass

/**
 * ## Types
 *
 * KWForms provides a set of predefined types that can be used to define the value types of arguments.
 * These types are used to specify the expected data format for each argument in a form.
 *
 * Each type has 4 common properties:
 * - `kClass`: The class of the value type.
 * - `typeId`: The ID of the type, which is a unique identifier for the type.
 * Used for type serialization and deserialization.
 * - `requirement`: The requirement for the value type,
 * which is used to specify additional constraints or validation rules for the argument.
 * - `preProcessor`:
 * An optional pre-processor function that can be applied to the value before it is validated or used.
 * This can be used, for example, to trim strings or round numbers.
 *
 * KW forms supports variations of the following types:
 *
 * | Name | typeId | Kotlin Type | Description | Additional Properties |
 * | --- | --- | --- | --- | --- |
 * | Bool | 1 | Boolean | Represents a boolean value (true/false) | - |
 * | Integer | 2 | Long | Represents an integer value | - |
 * | Decimal | 3 | Double | Represents a decimal (floating point) value | - |
 * | Text | 4 | String | Represents a string value | - |
 * | Binary | 5 | BinarySource | Represents a binary value (e.g., file content) | - |
 * | EnumType | 6 | Enum&lt;T&gt; | Represents an enum value of type T | Enum class |
 * | ListType | 7 | List&lt;T&gt; | Represents a list of values of type T | Element type |
 * | FormType | 8 | FormData&lt;T&gt; | Represents a form of type T | Form factory |
 * | MapType | 9 | Map&lt;K, V&gt; | Represents a map with keys of type K and values of type V | Key and value types |
 * | Nullable | n+10 | T? | Represents a nullable type of T | Wrapped type |
 *
 * To declare a type for an argument, you can use the predefined types or create your own custom types.
 * You can also use the `nullable` property to create a nullable version of any type.
 * ### Example
 * ```kotlin
 * val myArgumentType: Type<String> = Type.Text(
 *     preProcessor = { it.trim() }, // Optional pre-processor to trim whitespace
 *     requirement = isLengthInRange(1..100) // Optional requirement for length
 * )
 * ```
 * You can also use the provided functions to create types with specific requirements:
 * ```kotlin
 * val myIntegerType: Type<Long> = integer(
 *    preProcessor = { it.coerceIn(0, 100) }, // Optional pre-processor to limit range
 *    requirement = isInRange(0..100) // Optional requirement for range
 * )
 * ```
 *
 * @param T The type corresponding to the value type of the argument.
 */
sealed interface Type<T> {

    /**
     * The class of the value type.
     */
    val kClass: KClass<*>

    /**
     * The ID of the type.
     */
    val typeId: Byte

    /**
     * The requirement for the value type.
     * This is used to specify additional constraints or validation rules for the argument.
     */
    val requirement: ValueRequirement<T>? get() = null

    /**
     * Optional pre-processor function that can be applied to the value before it is validated or used.
     * This can be used, for example, to trim strings or round numbers.
     */
    val preProcessor: ((T) -> T)? get() = null

    /**
     * Kind of type that stores a single value instance.
     */
    sealed interface Single<T> : Type<T>

    /**
     * Kind of type that stores zero or more value instances.
     * This can be a list, set, or any other collection type.
     */
    sealed interface Multi<T> : Complex<T>

    /**
     * Kind of type that stores a basic value instance without nested structures.
     */
    sealed interface Basic<T> : Single<T>

    /**
     * Kind of type that stores a complex value instance, which may include nested structures.
     */
    sealed interface Complex<T> : Type<T>

    /**
     * Represents a boolean type of argument. (true/false)
     */
    data object Bool : Basic<Boolean> {
        override val kClass = Boolean::class
        override val typeId: Byte = 0x01
    }

    /**
     * Represents an integer type of argument.
     * @param preProcessor An optional pre-processor function that can be
     * applied to the integer value before it is validated or used.
     * @param requirement The value requirement for the integer data.
     */
    data class Integer(
        override val preProcessor: ((Long) -> Long)? = null,
        override val requirement: ValueRequirement<Long>? = null
    ) : Basic<Long> {
        override val kClass: KClass<Long> = Long::class
        override val typeId: Byte = 0x02
    }

    /**
     * Represents a decimal type of argument. (floating point number)
     * @param preProcessor An optional pre-processor function that can be
     * applied to the decimal value before it is validated or used.
     * @param requirement The value requirement for the decimal data.
     */
    data class Decimal(
        override val preProcessor: ((Double) -> Double)? = null,
        override val requirement: ValueRequirement<Double>? = null
    ) : Basic<Double> {
        override val kClass: KClass<Double> = Double::class
        override val typeId: Byte = 0x03
    }

    /**
     * Represents a string type of argument.
     * @param preProcessor An optional pre-processor function that can be
     * applied to the string before it is validated or used.
     * @param requirement The value requirement for the string data.
     */
    data class Text(
        override val preProcessor: ((String) -> String)? = null,
        override val requirement: ValueRequirement<String>? = null
    ) : Basic<String> {
        override val kClass = String::class
        override val typeId: Byte = 0x04
    }

    /**
     * Represents a binary type of argument.
     * @param preProcessor An optional pre-processor function that can be
     * applied to the binary data before it is validated or used.
     * @param requirement The value requirement for the binary data.
     */
    data class Binary(
        override val preProcessor: ((BinarySource) -> BinarySource)? = null,
        override val requirement: ValueRequirement<BinarySource>? = null
    ) : Basic<BinarySource> {
        override val kClass = ByteArray::class
        override val typeId: Byte = 0x06
    }

    /**
     * Represents an enum type of argument.
     * @param kClass The enum class that the argument represents.
     */
    data class EnumType<T : Enum<T>>(
        override val kClass: KClass<T>,
    ) : Basic<T> {
        override val typeId: Byte = 0x05
        override val requirement: ValueRequirement<T>? = null
    }

    /**
     * Represents a list type of argument.
     * @param T The type of the elements in the list.
     * @param elementType The type of the elements in the list.
     * @param requirement The value requirement for the list data.
     */
    data class ListType<T>(
        val elementType: Type<T>,
        override val preProcessor: ((List<T>) -> List<T>)? = null,
        override val requirement: ValueRequirement<List<T>>? = null
    ) : Multi<List<T>> {
        override val kClass: KClass<List<T>> get() = List::class as KClass<List<T>>
        override val typeId: Byte get() = 0x07
    }

    /**
     * Represents a form type of argument.
     * @param T The type of the form.
     * @param formFactory factory function that creates an instance of the form.
     * @param preProcessor An optional pre-processor function that can be
     * applied to the form data before it is validated or used.
     * @param requirement The value requirement for the form data.
     */
    data class FormType<T : AbstractForm>(
        val formFactory : () -> T,
        override val preProcessor: ((T) -> T)? = null,
        override val requirement: ValueRequirement<T>? = null
    ) : Complex<T> {
        override val kClass: KClass<T> by lazy { formFactory()::class as KClass<T> }
        override val typeId: Byte = 0x08
    }

    /**
     * Represents a map type of argument.
     * @param K The type of the keys in the map.
     * @param V The type of the values in the map.
     * @param keyType The type of the keys in the map.
     * @param valueType The type of the values in the map.
     * @param preProcessor An optional pre-processor function that can be
     * applied to the map data before it is validated or used.
     * @param requirement The value requirement for the map data.
     */
    data class MapType<K, V>(
        val keyType: Type<K>,
        val valueType: Type<V>,
        override val preProcessor: ((Map<K, V>) -> Map<K, V>)? = null,
        override val requirement: ValueRequirement<Map<K, V>>? = null
    ) : Multi<Map<K, V>> {
        override val kClass: KClass<Map<K, V>> get() = Map::class as KClass<Map<K, V>>
        override val typeId: Byte get() = 0x09
    }

    /**
     * Represents a nullable type of argument.
     * This is used to indicate that the value can be null.
     * @param T The type of the value, which can be null.
     */
    data class Nullable<T : Any>(
        val type: Type<T>
    ) : Type<T?> {
        override val kClass = type.kClass
        override val typeId: Byte = (type.typeId + 0x0a).toByte() // Offset to differentiate nullable types
        override val requirement: ValueRequirement<T?>? = null
    }

}

/**
 * Boolean type of argument.
 * This is a shorthand for `Type.Bool`.
 */
val bool get() = Type.Bool

/**
 * Integer type of argument.
 * This is a shorthand for `Type.Integer`.
 */
val integer get() = Type.Integer()

/**
 * Decimal type of argument.
 * This is a shorthand for `Type.Decimal`.
 */
val decimal get() = Type.Decimal()

/**
 * Enum type of argument.
 * This is a shorthand for `Type.Enum`.
 */
val text get() = Type.Text()

/**
 * Form type of argument.
 * This is a shorthand for `Type.Form`.
 */
val binary get() = Type.Binary()

/**
 * Integer type of argument with a specific requirement.
 * @param preProcessor An optional pre-processor function that can be
 * applied to the integer value before it is validated or used.
 * @param requirement The value requirement for the integer type.
 */
fun integer(
    preProcessor: ((Long) -> Long)? = null,
    requirement: ValueRequirement<Long>? = null
) = Type.Integer(preProcessor, requirement)

/**
 * Decimal type of argument with a specific requirement.
 * @param preProcessor An optional pre-processor function that can be
 * applied to the decimal value before it is validated or used.
 * @param requirement The value requirement for the decimal type.
 */
fun decimal(
    preProcessor: ((Double) -> Double)? = null,
    requirement: ValueRequirement<Double>? = null
) = Type.Decimal(preProcessor, requirement)

/**
 * String type of argument with a specific requirement.
 * @param preProcessor An optional pre-processor function that can be
 * applied to the string value before it is validated or used.
 * @param requirement The value requirement for the string type.
 */
fun text(
    preProcessor: ((String) -> String)? = null,
    requirement: ValueRequirement<String>? = null
) = Type.Text(preProcessor, requirement)

/**
 * Enum type of argument with a specific requirement.
 * @param enumClass The enum class that the argument represents.
 */
fun <T : Enum<T>> enum(
    enumClass: KClass<T>
) = Type.EnumType(enumClass)

inline fun <reified T : Enum<T>> enum() = Type.EnumType(T::class)

/**
 * Binary type of argument with a specific MIME type and requirement.
 * @param preProcessor An optional pre-processor function that can be
 * applied to the binary data before it is validated or used.
 * @param requirement The value requirement for the binary type.
 */
fun binary(
    preProcessor: ((BinarySource) -> BinarySource)? = null,
    requirement: ValueRequirement<BinarySource>? = null
) = Type.Binary(preProcessor, requirement)

/**
 * Form type of argument with a specific requirement.
 * @param formClass The KClass of the form class that the argument represents.
 * @param preProcessor An optional pre-processor function that can be
 * applied to the form data before it is validated or used.
 * @param requirement The value requirement for the form data.
 */
fun <T : AbstractForm> form(
    formFactory: () -> T,
    preProcessor: ((T) -> T)? = null,
    requirement: ValueRequirement<T>? = null
): Type.FormType<T> = Type.FormType(formFactory, preProcessor, requirement)

/**
 * Form type of argument with a specific form class and requirement.
 * This is shorthand for creating a form type using a class factory.
 * @param T The type of the form class.
 * @param preProcessor An optional pre-processor function that can be
 * applied to the form data before it is validated or used.
 * @param requirement The value requirement for the form data.
 * @return A [Type.FormType] object representing the form type.
 */
inline fun <reified T : AbstractForm> form(
    noinline preProcessor: ((T) -> T)? = null,
    requirement: ValueRequirement<T>? = null
): Type.FormType<T> = Type.FormType(classFormFactory(T::class), preProcessor, requirement)

/**
 * List type of argument with a specific element type, size range, duplicates policy, and requirement.
 * @param elementType The type of the elements in the list.
 * @param preProcessor An optional pre-processor function that can be
 * applied to the list data before it is validated or used.
 * @param requirement The value requirement for the list data.
 * @return A [Type.ListType] object representing the list type.
 */
fun <T> list(
    elementType: Type<T>,
    preProcessor: ((List<T>) -> List<T>)? = null,
    requirement: ValueRequirement<List<T>>? = null
) = Type.ListType(elementType, preProcessor, requirement)

/**
 * Map type of argument with specific key and value types, and an optional requirement.
 * @param K The type of the keys in the map.
 * @param V The type of the values in the map.
 * @param keyType The type of the keys in the map.
 * @param valueType The type of the values in the map.
 * @param preProcessor An optional pre-processor function that can be
 * applied to the map data before it is validated or used.
 * @param requirement The value requirement for the map data.
 * @return A [Type.MapType] object representing the map type.
 */
fun <K, V> map(
    keyType: Type<K>,
    valueType: Type<V>,
    preProcessor: ((Map<K, V>) -> Map<K, V>)? = null,
    requirement: ValueRequirement<Map<K, V>>? = null
) = Type.MapType(keyType, valueType, preProcessor, requirement)

/**
 * Extension property to create a nullable version of the type.
 * This is used to indicate that the value can be null.
 */
val <T : Any> Type<T>.nullable: Type<T?> get() = Type.Nullable(this)

/**
 * Extension property to create a non-nullable version of the type.
 * This is used to ensure that the value cannot be null.
 */
val <T : Any> Type<T?>.nonNull: Type<T>
    get() = when (this) {
        is Type.Nullable<*> -> this.type as Type<T>
        else -> this as Type<T>
    }

/**
 * Form factory function that creates an instance of the form class.
 *
 * @param T The type of the form class.
 * @param formClass The KClass of the form class.
 * @return A function that creates an instance of the form class.
 */
fun <T : AbstractForm> classFormFactory(
    formClass: KClass<T>
): () -> T = {
    formClass.constructors.firstOrNull()?.call() ?: throw FormDeclarationException(
        "Form class must have a no-arg constructor"
    )
}