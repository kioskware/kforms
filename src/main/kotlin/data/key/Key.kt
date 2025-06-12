package data.key

import AbstractField
import AbstractForm
import Directory

fun main() {

    Directory().key {
        root.files
    }

}






fun <T : AbstractForm> T.key(
    block: KeyBuilderScope<T>.() -> AbstractField<*>
) {

}

class KeyBuilderScope<T : AbstractForm> {

    val root: T
        get() = TODO("Implement root field access")

    operator fun <T : AbstractField<List<*>>> T.get(index: Int): T {
        TODO("Implement key access for list index")
    }

}