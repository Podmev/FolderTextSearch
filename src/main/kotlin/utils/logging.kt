package utils

import java.util.logging.Logger
import kotlin.reflect.full.companionObject

/*
* Utils methods are taken from here
* https://stackoverflow.com/questions/34416869/idiomatic-way-of-logging-in-kotlin
* */

/**
 * Returns logger for Java class, if companion object fix the name
 * */
fun <T : Any> logger(forClass: Class<T>): Logger =
    Logger.getLogger(unwrapCompanionClass(forClass).name)

/**
 *
 * Unwraps companion class to enclosing class given a Java Class
 * */
fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> =
    ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass

/**
 * Marker interface and related extension (remove extension for Any.logger() in favour of this)
 * */
interface Loggable

/**
 * Extension function for Logger interface to get instance of logger for this class
 * */
fun Loggable.logger(): Logger = logger(this.javaClass)

/**
 * Abstract base class to provide logging, intended for companion objects more than classes but works for either
 * */
abstract class WithLogging : Loggable {
    /**
     * field of logger, which automatic appears in class, which inherits this abstract class
     * */
    @Suppress("PropertyName")
    val LOG = logger()
}