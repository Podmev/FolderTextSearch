package impl.trigram.map

/**
 * Checking size of charTriplet string - should be exactly 3 characters
 * */
fun validateCharTriplet(charTriplet: String) {
    require(charTriplet.length == 3) { "Char triplet does have 3 characters, but ${charTriplet.length} $charTriplet" }
}