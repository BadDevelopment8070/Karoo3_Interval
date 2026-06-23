package com.bastiankahuna.karoosmartftp.engine

class PowerHistory(private val maxSize: Int = 90) {
    private val values = ArrayDeque<Int>()

    fun add(power: Int?) {
        if (power != null && power >= 0) {
            values.addLast(power)
            while (values.size > maxSize) values.removeFirst()
        }
    }

    fun snapshot(): List<Int> = values.toList()

    fun clear() = values.clear()
}
