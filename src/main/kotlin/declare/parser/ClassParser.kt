package declare.parser

import AbstractField
import AbstractForm
import declare.Form
import declare.requireHostField
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

private const val ACCESS_ERROR_MESSAGE =
    "Form fields can only be accessed from a non-abstract class with empty constructor"

internal fun Form.readFieldsFromClass(): List<AbstractField<*>> {
    this::class.let {
        if (
            it.isCompanion ||
            it.isAbstract ||
            it.objectInstance != null ||
            it.constructors.any { constructor -> constructor.parameters.isNotEmpty() }
        ) {
            throw IllegalStateException(ACCESS_ERROR_MESSAGE)
        }
    }
    val fields = this::class.memberProperties
        .filterIsInstance<KProperty1<AbstractForm, AbstractField<*>>>()
        .filter {
            it.isAccessible = true // Make the property accessible
            !it.returnType.isMarkedNullable // Must be non-nullable
                    && it.returnType.jvmErasure.isSubclassOf(AbstractField::class) // Must be of type Field
                    && it.isFinal // Must be final
                    && it.name.firstOrNull()
                ?.isLowerCase() ?: false // Must start with a lowercase letter
        }
        // Get property values
        .map { it.get(this) }

    val formFields = this::class.memberProperties
        .filterIsInstance<KProperty1<AbstractForm, AbstractForm>>()
        .filter {
            it.isAccessible = true // Make the property accessible
            !it.returnType.isMarkedNullable // Must be non-nullable
                    && it.returnType.jvmErasure.isSubclassOf(AbstractForm::class) // Must be of type Form
                    && it.isFinal // Must be final
                    && it.name.firstOrNull()
                ?.isLowerCase() ?: false // Must start with a lowercase letter
        }
        // Get property values
        .map { it.get(this).requireHostField }

    //Combine fields and forms
    return (fields + formFields).sortedByDescending { it.spec().orderKey }
}