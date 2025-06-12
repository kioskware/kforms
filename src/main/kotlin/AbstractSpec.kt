interface AbstractSpec {

    val annotations: List<Annotation> get() = emptyList()

}

/**
 * Shorthand for getting the specification of the field.
 * Equivalent to `field.spec()`.
 */
val <T> AbstractField<T>.spec: FieldSpec<T>
    get() = spec()

/**
 * Shorthand for getting the ID of the field.
 */
val <T> AbstractField<T>.id: String
    get() = spec().id

/**
 * Shorthand for getting the specification of the form.
 * Equivalent to `form.spec()`.
 */
val <T : AbstractForm> T.spec: FormSpec
    get() = spec()

/**
 * Shorthand for getting fields of the form. Equivalent to `form.spec().fields`.
 */
val <T : AbstractForm> T.fields : List<AbstractField<*>>
    get() = spec().fields