package kioskware.kforms.declare

import kioskware.kforms.AbstractSpec
import kotlin.experimental.ExperimentalTypeInference

interface Option {

    val spec: OptionSpec get() = OptionSpec.Default

}

/**
 * Represents a specification of an option in an enum field.
 */
data class OptionSpec(
    override val docName: CharSequence? = null,
    override val description: CharSequence? = null,
    override val descriptionDetailed: CharSequence? = null,
    override val orderKey: Int = 0,
    override val extras: Map<String, String>? = null
) : AbstractSpec {

    companion object {
        /**
         * Default option specification.
         */
        val Default = OptionSpec()
    }

    class BuilderScope internal constructor(
        var name: CharSequence? = null,
        var description: CharSequence? = null,
        var descriptionDetailed: CharSequence? = null,
        var orderKey: Int = 0
    ) {

        private val _extras: MutableMap<String, String> = mutableMapOf()

        /**
         * Puts an extra key-value pair into the option.
         * Extras are additional metadata for the option,
         * used for documentation or for AI models.
         */
        fun putExtra(key: String, value: String) {
            _extras[key] = value
        }

        /**
         * Sets the extras for the option.
         * Extras are additional metadata for the option, used for documentation or for AI models.
         */
        @OptIn(ExperimentalTypeInference::class)
        @KFormDsl
        fun extras(@BuilderInference builderAction: MutableMap<String, String>.() -> Unit) =
            buildMap(builderAction).let { _extras.putAll(it) }

        internal fun build(): OptionSpec {
            return OptionSpec(name, description, descriptionDetailed, orderKey, _extras.toMap())
        }

    }

}

/**
 * Creates an [OptionSpec] using the provided block.
 * The block is executed in the context of [OptionSpec.BuilderScope].
 *
 * Should be used to define the specification of an option in an enum field.
 *
 * Take a look at an example:
 * ```kotlin
 * enum class ColorOption : Option {
 *     RED {
 *        override val spec = optionSpec {
 *          name = "Red Color"
 *          description = "Represents the color red."
 *          descriptionDetailed = "This option represents the color red, which is often associated with passion and energy."
 *          orderKey = 1
 *          putExtra("hex", "#FF0000")
 *        }
 *     }
 *    (... other options)
 * }
 * ```
 * This is useful to provide additional information about enum constant,
 * for example, when generating documentation, UI or for AI models.
 *
 *
 * @param block The block to configure the option specification.
 * @return A new instance of [OptionSpec].
 */
fun optionSpec(block: OptionSpec.BuilderScope.() -> Unit): OptionSpec {
    val builder = OptionSpec.BuilderScope()
    builder.block()
    return builder.build()
}