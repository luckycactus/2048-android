package ru.luckycactus.game2048.model

import java.util.*

open class ObjectPool<T>(
    private val factory: () -> T
) {
    private val free = LinkedList<T>()

    fun acquire(): T {
        return if (free.isEmpty()) {
            factory()
        } else {
            free.pop()
        }
    }

    fun release(o: T) {
        free.push(o)
    }
}