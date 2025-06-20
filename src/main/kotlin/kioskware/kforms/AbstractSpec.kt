package kioskware.kforms


interface AbstractSpec {

    companion object {
        val Default: AbstractSpec = object : AbstractSpec {}
    }

    val docName: CharSequence? get() = null
    val description: CharSequence? get() = null
    val descriptionDetailed: CharSequence? get() = null
    val orderKey: Int get() = 0
    val extras: Map<String, String>? get() = null

}