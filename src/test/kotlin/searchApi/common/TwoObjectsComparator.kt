package searchApi.common

import org.junit.jupiter.api.Assertions
import java.lang.UnsupportedOperationException

/**
 * Helper class to check correspondent fields of 2 objects of same type
 */
class TwoObjectsComparator<T>(
    private val obj1: T,
    private val obj2: T,
    private val obj1Str: String,
    private val obj2Str: String) {

    /**
     * Check correspondent fields of 2 objects of same type with provided:
     *  - operator: >, >=, <, <=, ==, !=
     *  - getter function
     *  - field name
     */
    fun <K:Comparable<K>> assert(operator: String, getter: (T) -> K, getterStr: String) {
        val message = "$obj1Str.$getterStr$operator$obj2Str.$getterStr"
        val field1: K = getter(obj1)
        val field2: K = getter(obj2)
        val condition: Boolean = when(operator){
            "<" -> field1 < field2
            "<=" -> field1 <= field2
            ">" -> field1 > field2
            ">=" -> field1 >= field2
            "==" -> field1 == field2
            "!=" -> field1 != field2
            else -> throw UnsupportedOperationException("Unsupported operation $operator")
        }
        Assertions.assertTrue(condition, message)
    }

}