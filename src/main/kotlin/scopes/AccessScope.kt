package scopes

/**
 * Defines a scope required for accessing certain fields or data.
 *
 * 1. Super classes are more restrictive than subclasses.
 * 2. Null grants/requires full access.
 * 3. Base [AccessScope] is empty and does not allow/require any access.
 *
 * Example hierarchy:
 *
 * ```kotlin
 *
 * interface User : AccessScope {}
 *
 * interface Admin : User {}
 *
 * interface SuperAdmin : Admin {}
 *
 * interface Owner : SuperAdmin {}
 *
 * ```
 *
 */
interface AccessScope {

    companion object {

        /**
         * An empty access scope that does not allow/require any access.
         */
        val None: AccessScope = object : AccessScope {}

    }

}

operator fun AccessScope?.compareTo(other: AccessScope?): Int {
    // Case 2: Null grants/requires full access
    if (this == null && other == null) return 0      // Equal if both are null
    if (this == null) return 1                       // this > other if this is null (this has full access)
    if (other == null) return -1                     // this < other if other is null (other has full access)

    // Case 3: Base AccessScope is empty and does not allow/require any access
    if (this === AccessScope.None && other === AccessScope.None) return 0
    if (this === AccessScope.None) return -1         // Empty scope is least restrictive
    if (other === AccessScope.None) return 1

    // Case 1: Super classes are more restrictive than subclasses
    return when {
        other::class.isInstance(this) -> -1          // this is a subclass of other (less restrictive)
        this::class.isInstance(other) -> 1           // this is a superclass of other (more restrictive)
        else -> 0                                    // Unrelated scopes are treated as equal
    }
}

/**
 * Checks whether this scope grants access to the specified required scope.
 *
 * A scope grants access to another scope if:
 * 1. This scope is null (null grants full access)
 * 2. The required scope is AccessScope.None (no access requirement)
 * 3. This scope is a superclass or the same class as the required scope
 * 4. Unrelated scopes do NOT grant access to each other
 *
 * @param requiredScope The scope that is required for access.
 * @return True if this scope grants access to the required scope, false otherwise.
 */
fun AccessScope?.grantsAccessTo(requiredScope: AccessScope?): Boolean {
    // First check for reference equality to avoid potential infinite recursion
    if (this === requiredScope) return true           // Same reference grants access

    // Handle null and None cases directly
    if (this == null) return true                     // Null grants full access
    if (requiredScope == null) return false           // Cannot grant access to null (full access)
    if (requiredScope === AccessScope.None) return true // No access requirement
    if (this === AccessScope.None) return false       // Empty scope grants no access

    // Handle class hierarchy - only grant access if this is a superclass or the same class
    return when {
        this::class == requiredScope::class -> true   // Same class
        this::class.isInstance(requiredScope) -> true // This is a superclass of requiredScope
        else -> false                                 // Unrelated or this is a subclass of requiredScope
    }
}
