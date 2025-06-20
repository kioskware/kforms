package kioskware.kforms.data

import kioskware.kforms.*
import kioskware.kforms.data.binary.*
import kioskware.kforms.declare.FieldPath
import kioskware.kforms.declare.plus
import kioskware.kforms.requirements.ensureValid
import kioskware.kforms.scopes.grantsAccessTo
import kioskware.kforms.type.Type
import kioskware.kforms.type.classFormFactory

/**
 * Stores form data in KForm Data format.
 * The map will only contain KForm supported [Type]s:
 *
 * - [Type.Bool] - kotlin [Boolean]
 * - [Type.Integer] - kotlin [Long]
 * - [Type.Decimal] - kotlin [Double]
 * - [Type.Text] - kotlin [String]
 * - [Type.EnumType] - enum constant of the specified enum class
 * - [Type.Binary] - [BinarySource] containing binary data
 * - [Type.FormType] - [AbstractForm] instance
 * - [Type.ListType] - [List] of values of the specified type
 * - [Type.MapType] - [Map] of values and keys of the specified types
 * - [Type.Nullable] - nullable type of any of the above types
 *
 */
interface FormDataMap : Map<String, Any?> {

    val form: AbstractForm

    val validationConfig: ValidationConfig

}

/**
 * Converts a [Map] to a [FormDataMap].
 * @param form the form to which this data map belongs.
 * Used to provide data format and validation rules.
 * @param validationConfig the configuration for validation of the form data.
 * @return a map containing data in [FormDataMap] format. See [FormDataMap] for details.
 */
fun <T : AbstractForm> Map<String, Any?>.toFormDataMap(
    form: T,
    validationConfig: ValidationConfig = ValidationConfig.Default
): FormDataMap {
    val sm: MutableMap<String, Any?> = this.toMutableMap()
    return object : FormDataMap, Map<String, Any?> by sm {

        init {
            if (validationConfig.params.mode != ValidationMode.None) {
                // Perform validations only if the mode is not None
                performValidations()
            }
        }

        private fun performValidations() {
            for (field in form.fields) {
                if (!validationConfig.params.accessScope.grantsAccessTo(field.spec.accessScope)) {
                    // If the field is not accessible in the given access scope, skip it and remove it from the map
                    sm.remove(field.id)
                    continue
                }

                val v = sm[field.id]
                if (v != null) {
                    sm[field.id] = field.spec.type.processValue(
                        value = v,
                        defaultValue = field.spec.defaultValue,
                        validationParams = validationConfig.params,
                        optimized = validationConfig.optimized,
                        lenientTypes = validationConfig.lenientTypes,
                        detailedLocation = validationConfig.detailedLocation,
                        currentPath = { validationConfig.parentPath + field }
                    )
                } else {
                    // If the field is required and no value is provided, throw an exception
                    if (field.isRequired && validationConfig.params.mode == ValidationMode.Create) {
                        throw MissingFieldValueException(
                            fieldPath = validationConfig.parentPath + field
                        )
                    }
                    // If the field is not required, we can skip it
                }

            }
        }

        override val form: T = form
        override val validationConfig: ValidationConfig = validationConfig
        override fun toString(): String = "FormDataMap(${this@toFormDataMap})"

    }
}


/**
 * Processes the value using `preProcessor` (if available) and then
 * validates the provided value against the requirement of the field.
 * If the value does not meet the requirement, a [FieldValueException] is thrown.
 * @param value The value to validate.
 * @return The validated value.
 * @throws FieldValueException if the value does not meet the requirement.
 */
@Suppress("UNCHECKED_CAST")
@Throws(FieldValueException::class)
private fun <T> Type<T>.processValue(
    value: Any?,
    defaultValue: Any?,
    validationParams: ValidationParams,
    optimized: Boolean,
    lenientTypes: Boolean,
    detailedLocation: Boolean,
    currentPath: () -> FieldPath?
): T {

    fun isOptional() = defaultValue != null || this is Type.Nullable<*>

    // 1. Handle value nullability and default value
    if (value == null) {
        if (isOptional()) {
            // It's guaranteed that the default value will meet the requirements,
            // so we can safely return it without further checks.
            return defaultValue as T
        } else {
            // If the field is required and the value is null, throw an exception
            throw MissingFieldValueException(currentPath())
        }
    }

    // 2. Perform value type checks and casting
    val castedValue = if (!kClass.isInstance(value)) {
        if (lenientTypes) {
            // Try casting the value to the expected type
            castValue(
                value = value,
                validationParams = validationParams,
                optimized = optimized,
                detailedLocation = detailedLocation,
                currentPath = currentPath
            )
        } else {
            // If the value is not of the correct type and lenient types are not allowed,
            // throw an exception
            throw FieldValueTypeMismatchException(currentPath(), kClass, this)
        }
    } else {
        // If the value is of the correct type - nothing to do
        value
    } as T

    // 3. Process the value with preProcessor if available
    val processed = preProcessor?.invoke(castedValue) ?: castedValue

    // 4. Check if the value with the correct type meets the requirements
    requirement?.ensureValid(currentPath(), processed, optimized)

    // 5. Return the processed value
    return processed
}

private fun <T> Type<T>.castValue(
    value: Any,
    validationParams: ValidationParams,
    optimized: Boolean,
    detailedLocation: Boolean,
    currentPath: () -> FieldPath?
): T {

    fun typeMismatch() = FieldValueTypeMismatchException(
        fieldPath = currentPath(),
        valueType = value::class,
        expectedType = this
    )

    @Suppress("UNCHECKED_CAST")
    return when (this) {

        is Type.Bool -> this.toString().toBoolean()
        is Type.Integer -> try {
            this.toString().toLong()
        } catch (e: Exception) {
            throw typeMismatch()
        }

        is Type.Decimal -> try {
            this.toString().toDouble()
        } catch (e: Exception) {
            typeMismatch()
        }

        is Type.Text -> this.toString()
        is Type.EnumType -> {
            try {
                val enumConst = this.kClass.java.enumConstants
                if (value is Number) {
                    enumConst[value.toInt()]
                } else {
                    val enumValue = value.toString()
                    enumConst.first { it.toString() == enumValue }
                }
            } catch (e: Exception) {
                throw typeMismatch()
            }
        }

        is Type.Binary -> {
            if (value is ByteArray) {
                ArrayBinarySource(MimeType.APPLICATION_OCTET_STREAM, value)
            } else {
                // If the value is not a ByteArray, we assume it's a base64 encoded string
                binarySourceFromBase64(this.toString()) ?: throw typeMismatch()
            }
        }

        is Type.FormType<*> -> if (value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            (build(
                formFactory = classFormFactory(kClass),
                data = value as (Map<String, *>),
                validationConfig = ValidationConfig(
                    params = validationParams,
                    optimized = optimized,
                    detailedLocation = detailedLocation,
                    parentPath = currentPath()
                )
            ))
        } else {
            throw typeMismatch()
        }

        is Type.ListType<*> -> if (value is Collection<*>) {
            value.mapIndexed { index, it ->
                elementType.processValue(
                    value = it,
                    defaultValue = null,
                    validationParams = validationParams,
                    optimized = optimized,
                    detailedLocation = detailedLocation,
                    lenientTypes = true,
                    currentPath = { currentPath() + index }
                )
            }
        } else {
            throw typeMismatch()
        }

        is Type.MapType<*, *> -> if (value is Map<*, *>) {
            @Suppress("RemoveExplicitTypeArguments")
            (buildMap<Any?, Any?> {
        for ((key, v) in value) {
            put(
                keyType.processValue(
                    value = key,
                    defaultValue = null,
                    validationParams = validationParams,
                    optimized = optimized,
                    detailedLocation = detailedLocation,
                    lenientTypes = true,
                    currentPath = { currentPath() + FieldPath.Segment.MapKey(key, false) }
                ),
                valueType.processValue(
                    value = v,
                    defaultValue = null,
                    validationParams = validationParams,
                    optimized = optimized,
                    detailedLocation = detailedLocation,
                    lenientTypes = true,
                    currentPath = { currentPath() + FieldPath.Segment.MapKey(key, true) }
                )
            )
        }
    })
        } else {
            throw typeMismatch()
        }

        is Type.Nullable<*> -> type.castValue(
            value = value,
            validationParams = validationParams,
            optimized = optimized,
            detailedLocation = detailedLocation,
            currentPath = currentPath
        )

    } as T

}