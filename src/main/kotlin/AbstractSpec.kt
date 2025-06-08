interface AbstractSpec {

    val name: CharSequence? get() = null
    val description: CharSequence? get() = null
    val descriptionDetailed: CharSequence? get() = null
    val annotations: List<Annotation> get() = emptyList()

}

/**
 * Shorthand for getting the specification of the field.
 * Equivalent to `field.spec()`.
 */
val <T> AbstractField<T>.spec: FieldSpec<T>
    get() = spec()

/**
 * Shorthand for getting the specification of the form.
 * Equivalent to `form.spec()`.
 */
val <T : AbstractForm> T.spec: FormSpec
    get() = spec()