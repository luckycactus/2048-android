package ru.luckycactus.game2048.model

import android.graphics.Point

class Tile(
    var row: Int,
    var column: Int,
    var value: Int
) {
    var previousRow: Int = -1
    var previousColumn: Int = -1
    var mergedFrom: Pair<Point, Point>? = null

    fun toPoint() = Point(column, row)

    fun prevToPoint() = Point(previousColumn, previousRow)

    fun clearPrevious() {
        previousRow = -1
        previousColumn = -1
        mergedFrom = null
    }

    fun move(newRow: Int, newColumn: Int) {
        if (previousRow == -1) {
            previousRow = row
            previousColumn = column
        }
        row = newRow
        column = newColumn
        mergedFrom = null
    }

    fun merge(other: Tile) {
        if (other.value != value)
            throw IllegalArgumentException()
        value *= 2

        val cell1 = if (previousRow >= 0) prevToPoint() else toPoint()
        val cell2 = if (other.previousRow >= 0) other.prevToPoint() else other.toPoint()
        mergedFrom = cell1 to cell2
    }
}