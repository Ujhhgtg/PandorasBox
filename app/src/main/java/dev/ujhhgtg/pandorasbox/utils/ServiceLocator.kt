package dev.ujhhgtg.pandorasbox.utils

import android.app.Service

object ServiceLocator {
    private val services = mutableMapOf<Class<*>, Service>()

    fun <T : Service> register(service: T) {
        services[service::class.java] = service
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Service> get(serviceClass: Class<T>): T? {
        val service = services[serviceClass]
        if (service == null) {
            return null
        }
        return service as T
    }
}
