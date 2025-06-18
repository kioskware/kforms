import data.ValidationConfig
import data.ValidationMode
import data.ValidationParams
import declare.*
import requirements.*
import type.*
import kotlin.random.Random
import kotlin.random.nextInt

data class FileData(
    val name: String,
    val description: String,
    val defaultValue: Any? = null,
    val enabledRules: List<String> = emptyList(),
    val requirement: String? = null
)

data class DirectoryData(
    val name: String,
    val size: Int,
    val createdAt: Int,
    val lastModifiedAt: Int,
    val lastAccessedAt: Int,
    val files: List<FileData> = emptyList(),
    val subdirectories: List<DirectoryData> = emptyList()
)


suspend fun main() {

    val i = 1

    val directory: Directory = build(
        validationConfig = ValidationConfig(
            params = ValidationParams(
                mode = ValidationMode.Full
            )
        )
    ) {
        val inte = Random.nextInt(1..10000)
        key::createdAt put 1633036800 // Unix timestamp
        key::lastModifiedAt put 1633123200 // Unix timestamp
        key::lastAccessedAt put 1633209600 // Unix timestamp
        key::name put "example_directory_$i"
        key::size put inte
        key::files put listOf(
            build {
                key::name put "file1.txt"
                key::size put 2048
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
            },
            build {
                key::name put "file2.txt"
                key::size put 4096
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
            },
            build {
                key::name put "file1.txt"
                key::size put 2048
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
            },
            build {
                key::name put "file2.txt"
                key::size put 4096
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
            },
            build {
                key::name put "file1.txt"
                key::size put 2048
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
            },
            build {
                key::name put "file2.txt"
                key::size put 4096
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
            }
        )
        key::subdirectories put listOf(
            build {
                key::name put "subdir1"
                key::size put 8192
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
                key::files put listOf(
                    build {
                        key::name put "subfile1.txt"
                        key::size put 512
                        key::createdAt put 1633036800 // Unix timestamp
                        key::lastModifiedAt put 1633123200 // Unix timestamp
                        key::lastAccessedAt put 1633209600 // Unix timestamp
                    }
                )
            }
        )
    }

    test1()

    println("Directory: ${directory}")

}

fun test1(n: Int = 5) {

    for (j in 1..n) {
        val rootStart = System.currentTimeMillis()
        for (i in 1..1_000_000) {
            val start = System.currentTimeMillis()

            val directory: Directory = build {
                val inte = (1).coerceAtMost(14_444)
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
                key::name put "example_directory_$i"
                key::size put inte // Increment size for each directory
                key::files put listOf(
                    build {
                        key::name put "file1.txt"
                        key::size put 2048
                        key::createdAt put 1633036800 // Unix timestamp
                        key::lastModifiedAt put 1633123200 // Unix timestamp
                        key::lastAccessedAt put 1633209600 // Unix timestamp
                    },
                    build {
                        key::name put "file2.txt"
                        key::size put 4096
                        key::createdAt put 1633036800 // Unix timestamp
                        key::lastModifiedAt put 1633123200 // Unix timestamp
                        key::lastAccessedAt put 1633209600 // Unix timestamp
                    },
                    build {
                        key::name put "file1.txt"
                        key::size put 2048
                        key::createdAt put 1633036800 // Unix timestamp
                        key::lastModifiedAt put 1633123200 // Unix timestamp
                        key::lastAccessedAt put 1633209600 // Unix timestamp
                    },
                    build {
                        key::name put "file2.txt"
                        key::size put 4096
                        key::createdAt put 1633036800 // Unix timestamp
                        key::lastModifiedAt put 1633123200 // Unix timestamp
                        key::lastAccessedAt put 1633209600 // Unix timestamp
                    },
                    build {
                        key::name put "file1.txt"
                        key::size put 2048
                        key::createdAt put 1633036800 // Unix timestamp
                        key::lastModifiedAt put 1633123200 // Unix timestamp
                        key::lastAccessedAt put 1633209600 // Unix timestamp
                    },
                    build {
                        key::name put "file2.txt"
                        key::size put 4096
                        key::createdAt put 1633036800 // Unix timestamp
                        key::lastModifiedAt put 1633123200 // Unix timestamp
                        key::lastAccessedAt put 1633209600 // Unix timestamp
                    }
                )
                key::subdirectories put listOf(
                    build {
                        key::name put "subdir1"
                        key::size put 8192
                        key::createdAt put 1633036800 // Unix timestamp
                        key::lastModifiedAt put 1633123200 // Unix timestamp
                        key::lastAccessedAt put 1633209600 // Unix timestamp
                        key::files put listOf(
                            build {
                                key::name put "subfile1.txt"
                                key::size put -1
                                key::createdAt put 1633036800 // Unix timestamp
                                key::lastModifiedAt put 1633123200 // Unix timestamp
                                key::lastAccessedAt put 1633209600 // Unix timestamp
                            }
                        )
                    }
                )
            }

            val file: File = build {
                key::name put "example.txt"
                key::size put 1024
                key::createdAt put 1633036800 // Unix timestamp
                key::lastModifiedAt put 1633123200 // Unix timestamp
                key::lastAccessedAt put 1633209600 // Unix timestamp
            }


            directory.toString()
            file.toString()

//            println("Printing..")
//            println("Directory: $directory")
//            println("Done in ${System.currentTimeMillis() - start} ms")
        }
        println("Root done in ${System.currentTimeMillis() - rootStart} ms")
    }


}

// 1. Defining template
class Appearance : Form() {

    val enabled by integer().field()

    // 1. Declaring basic field of name "language"
    val language by nullableTextField {
        defaultValue = "en"
        name = "Language"
        description = "Language of the application"
    }

    val theme by enumField<Theme>()

    // 2. Declaring basic list field of name "customColors"
    val customColors by integer.listField {
        defaultValue = emptyList()
        name = "Custom Colors"
        description = "List of custom colors"
        enabledRules = rule(::theme require (isEqual(Theme.Custom)))
    }

    val address by formField<Address>()
    val size by intField(
        require = isEven
    )

    enum class Theme {
        Light, Dark, Custom
    }

}

class Address : Form() {
    val street by textField()
    val city by textField()
    val zipCode by formField<ZipCode>()
}

class ZipCode : Form() {

    val part1 by intField(
        require = isInRange(100..999) and isEven
    )
    val part2 by intField(
        require = isInRange(10..99) and isOdd
    )

}


//// 2. Filling template with data
//val appearanceData = Appearance {
//    key.language - "en"
//    Appearance.Theme {
//        key.isDark - true
//        key.customColors - listOf(1, 2, 3)
//    }
//}

// 3. Accessing data


open class File : Form() {

    val name by textField {
        name = "File Name"
        description = "Name of the file"
    }

    val size by intField(
        require = isNonNegative
    ) {
        name = "File Size"
        description = "Size of the file in bytes"
    }

    val createdAt by intField {
        name = "Created At"
        description = "Creation date and time of the file"
    }

    val lastModifiedAt by intField {
        name = "Last Modified At"
        description = "Last modification date and time of the file"
    }

    val lastAccessedAt by intField {
        name = "Last Accessed At"
        description = "Last access date and time of the file"
    }

}

class Directory : File() {

    val files by formListField<File> {
        name = "Files"
        description = "List of files in the directory"
        defaultValue = emptyList()
    }

    val subdirectories by formListField<Directory> {
        name = "Subdirectories"
        description = "List of subdirectories in the directory"
        defaultValue = emptyList()
    }

}
