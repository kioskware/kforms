package kioskware.kforms.example

import kioskware.kforms.build
import kioskware.kforms.copy
import kioskware.kforms.declare.KForm
import kioskware.kforms.declare.Option
import kioskware.kforms.declare.optionSpec

internal open class File : KForm() {

    val name by textField {
        name = "File Name"
        description = "Name of the file"
    }

    val size by intField {
        name = "File Size"
        description = "Size of the file in bytes"
    }

    val createdAt by intField {
        name = "Created At"
        description = "Creation date and time of the file"
    }

    val author by textField {
        name = "Author"
        description = "Author of the file"
    }

    enum class FileType : Option {

        TEXT {
            override val spec = optionSpec {
                name = "Text File"
                description = "A file containing plain text"
            }
        },

        IMAGE,

        VIDEO,

        AUDIO,

        DOCUMENT,

        OTHER
    }

}

internal class Directory : File() {

    val files by formListField<File> {
        name = "Files"
        description = "List of files in the directory"
    }

    val subdirectories by formListField<Directory> {
        name = "Subdirectories"
        description = "List of subdirectories in the directory"
    }

}

fun main() {

    val file = build<File> {
        key::name put "example.txt"
        key::size put 1024
        key::createdAt put 1633036800 // Example timestamp
        key::author put "John Doe"
    }.copy {
        key::name put "example_copy.txt"
    }

    println("File Name: ${file.name}")
}