package ru.luckycactus.game

class Cell(
    var row: Int,
    var column: Int,
    var value: Int
) {
    var previousRow: Int = -1
    var previousColumn: Int = -1
    var mergedFrom: Pair<Point, Point>? = null
        private set

    fun savePosition() {
        previousRow = row
        previousColumn = column
        mergedFrom = null
    }

    fun move(newRow: Int, newColumn: Int) {
        row = newRow
        column = newColumn
    }

    fun setMergedFrom(cell1: Cell, cell2: Cell) {
        mergedFrom = Point(cell1.previousColumn, cell1.previousRow) to
                Point(cell2.previousColumn, cell2.previousRow)
    }
}