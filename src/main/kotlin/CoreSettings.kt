import declare.*
import requirements.*
import type.*

fun main() {

    val file : File = build {
        key::name put "example.txt"
        key::size put 1024
        key::createdAt put 1633036800 // Unix timestamp
        key::lastModifiedAt put 1633123200 // Unix timestamp
        key::lastAccessedAt put 1633209600 // Unix timestamp
    }

    println("File: $file")

}


// 1. Defining template
class Appearance : Form() {

    // 1. Declaring basic field of name "language"
    val language by nullableTextField(
        defaultValue = "en",
        name = "Language",
        description = "Language of the application"
    )

    val theme by enumField<Theme>()

    // 2. Declaring basic list field of name "customColors"
    val customColors by integer.listField(
        defaultValue = emptyList(),
        name = "Custom Colors",
        description = "List of custom colors",
        enabledRules = rule(::theme require (isEqual(Theme.Custom)))
    )

    val address by formField<Address>()
    val size by intField(
        requirement = isEven
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
        requirement = isInRange(100..999) and isEven
    )
    val part2 by intField(
        requirement = isInRange(10..99) and isOdd
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

    val name by textField(
        name = "File Name",
        description = "Name of the file"
    )

    val size by intField(
        name = "File Size",
        description = "Size of the file in bytes"
    )

    val createdAt by intField(
        name = "Created At",
        description = "Creation date and time of the file"
    )

    val lastModifiedAt by intField(
        name = "Last Modified At",
        description = "Last modification date and time of the file"
    )

    val lastAccessedAt by intField(
        name = "Last Accessed At",
        description = "Last access date and time of the file"
    )

}

class Directory : File() {

    val files by formListField<File>(
        name = "Files",
        description = "List of files in the directory"
    )

    val subdirectories by formListField<Directory>(
        name = "Subdirectories",
        description = "List of subdirectories in the directory"
    )

}
