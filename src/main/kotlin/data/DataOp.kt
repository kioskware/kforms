package data

object DataOp {



    enum class Mode {
        /**
         * Overrides field value with the provided value.
         */
        Override,
        /**
         * Behaves differently depending on the value type:<br></br>
         *
         *
         * **For maps:** Overrides only provided keys with the provided values.
         * Adds new keys if not present.<br></br>
         * **For lists and sets:** Adds the provided values to the list.<br></br>
         *
         */
        Merge,
        /**
         * Removes the provided keys from the map or values from the list.
         */
        Remove,
        /**
         * Removes following key in the map.
         */
        RemoveAll
    }

}