package utils

import java.util.logging.Logger
import kotlin.reflect.full.companionObject

/*
* Utils methods are taken from here
* https://stackoverflow.com/questions/34416869/idiomatic-way-of-logging-in-kotlin
* */

// Return logger for Java class, if companion object fix the name
fun <T : Any> logger(forClass: Class<T>): Logger {
    return Logger.getLogger(unwrapCompanionClass(forClass).name)
}

// unwrap companion class to enclosing class given a Java Class
fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}

// marker interface and related extension (remove extension for Any.logger() in favour of this)
interface Loggable

fun Loggable.logger(): Logger = logger(this.javaClass)

// abstract base class to provide logging, intended for companion objects more than classes but works for either
abstract class WithLogging : Loggable {
    val LOG = logger()
}