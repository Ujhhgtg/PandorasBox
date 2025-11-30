package dev.ujhhgtg.pandorasbox.utils

import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
object ReflectUtils {
    inline fun <reified T : Any> getCompanionField(fieldName: String): Any? {
        val companionClass = T::class.companionObject ?: return null
        val companionInstance = T::class.companionObjectInstance ?: return null

        val property = companionClass.memberProperties
            .firstOrNull { it.name == fieldName } as? KProperty1<Any, *>

        return property?.get(companionInstance)
    }
}
