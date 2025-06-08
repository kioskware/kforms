package common

import kotlin.reflect.KProperty

/**
 * common.Creator of the field instance.
 */
interface ValueProvider<T> {
    /**
     * Initializes the field lazily and returns it.
     */
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T

}