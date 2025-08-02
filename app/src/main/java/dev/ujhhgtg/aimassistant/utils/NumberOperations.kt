package dev.ujhhgtg.aimassistant.utils

class NumberOperations {
    companion object {
        infix fun Number.add(other: Number): Number {
            return when (this) {
                is Int -> this + other.toInt()
                is Long -> this + other.toLong()
                is Float -> this + other.toFloat()
                is Double -> this + other.toDouble()
                is Short -> (this + other.toShort()).toShort()
                is Byte -> (this + other.toByte()).toByte()
                else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
            }
        }

        infix fun Number.subtract(other: Number): Number {
            return when (this) {
                is Int -> this - other.toInt()
                is Long -> this - other.toLong()
                is Float -> this - other.toFloat()
                is Double -> this - other.toDouble()
                is Short -> (this - other.toShort()).toShort()
                is Byte -> (this - other.toByte()).toByte()
                else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
            }
        }

        infix fun Number.multiply(other: Number): Number {
            return when (this) {
                is Int -> this * other.toInt()
                is Long -> this * other.toLong()
                is Float -> this * other.toFloat()
                is Double -> this * other.toDouble()
                is Short -> (this * other.toShort()).toShort()
                is Byte -> (this * other.toByte()).toByte()
                else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
            }
        }

        infix fun Number.divide(other: Number): Number {
            return when (this) {
                is Int -> this / other.toInt()
                is Long -> this / other.toLong()
                is Float -> this / other.toFloat()
                is Double -> this / other.toDouble()
                is Short -> (this / other.toShort()).toShort()
                is Byte -> (this / other.toByte()).toByte()
                else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
            }
        }

        fun Number.coerceIn(min: Number, max: Number): Number {
            require(min.toDouble() <= max.toDouble()) {
                "Cannot coerce to an empty range: maximum $max is less than minimum $min."
            }

            val value = this.toDouble()
            val coerced = when {
                value < min.toDouble() -> min.toDouble()
                value > max.toDouble() -> max.toDouble()
                else -> value
            }

            return when (this) {
                is Int -> coerced.toInt()
                is Long -> coerced.toLong()
                is Float -> coerced.toFloat()
                is Double -> coerced
                is Short -> coerced.toInt().toShort()
                is Byte -> coerced.toInt().toByte()
                else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
            }
        }

        operator fun Number.compareTo(other: Number): Int {
            return this.toDouble().compareTo(other.toDouble())
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Number> Number.toGeneric(clazz: Class<T>): T {
            return when (clazz) {
                Int::class.java -> this.toInt() as T
                Long::class.java -> this.toLong() as T
                Float::class.java -> this.toFloat() as T
                Double::class.java -> this.toDouble() as T
                Short::class.java -> this.toShort() as T
                Byte::class.java -> this.toByte() as T
                else -> throw IllegalArgumentException("Unsupported type: $clazz")
            }
        }
    }
}