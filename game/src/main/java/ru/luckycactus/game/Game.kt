package ru.luckycactus.game

import kotlin.random.Random

class Game(
    val rows: Int,
    val columns: Int
) {
    val grid = Array(rows) { Array<Cell?>(columns) { null } }

    private var started = false
    private var slidableDirections = 0
    private val traverseLinesDeltas: Map<Int, Pair<Int, Int>>
    private val traverseLinesXRanges: Map<Int, IntRange>
    private val traverseLinesYRanges: Map<Int, IntRange>
    private val freeSpots = mutableListOf<Point>()

    init {
        traverseLinesDeltas = mapOf(
            LEFT to (-1 to 0),
            TOP to (0 to -1),
            RIGHT to (1 to 0),
            BOTTOM to (0 to 1)
        )
        traverseLinesXRanges = mapOf(
            LEFT to (0 until 1),
            TOP to (0 until columns),
            RIGHT to (columns - 1 until columns),
            BOTTOM to (0 until columns)
        )
        traverseLinesYRanges = mapOf(
            LEFT to (0 until rows),
            TOP to (0 until 1),
            RIGHT to (0 until rows),
            BOTTOM to (rows - 1 until rows)
        )
    }

    fun start() {
        if (started)
            throw IllegalStateException("Game already started!")
        started = true
        spawn(2)
        updateSlidableDirections()
    }

    fun slide(direction: Int) {
        checkDirection(direction)
        if (!isDirectionSlidable(direction))
            return
        traverseLines(direction) { row, column, dx, dy ->
            slideLine(row, column, dx, dy)
        }
        spawn()
        updateSlidableDirections()
    }

    fun isDirectionSlidable(direction: Int) = (slidableDirections and direction) != 0

    inline fun traverseGrid(body: (row: Int, column: Int, cell: Cell?) -> Unit) {
        grid.forEachIndexed { row, tilesRow ->
            tilesRow.forEachIndexed { column, tile ->
                body(row, column, tile)
            }
        }
    }

    fun reset() {
        traverseGrid { row, column, _ ->
            grid[row][column] = null
        }
        started = false
        freeSpots.clear()
        slidableDirections = 0
    }

    fun restart() {
        reset()
        start()
    }

    private fun slideLine(startRow: Int, startColumn: Int, dx: Int, dy: Int) {
        var emptyCellRow = -1
        var emptyCellColumn = -1
        var mergeCandidate: Cell? = null
        traverseLine(startRow, startColumn, dx, dy) { row, column, tile ->
            if (tile == null) {
                if (emptyCellRow < 0) {
                    emptyCellRow = row
                    emptyCellColumn = column
                }
            } else {
                tile.savePosition()
                if (mergeCandidate?.value == tile.value) {
                    mergeCells(mergeCandidate!!, tile)
                    mergeCandidate = null
                    if (emptyCellRow < 0) {
                        emptyCellRow = row
                        emptyCellColumn = column
                    }
                } else {
                    if (emptyCellRow >= 0) {
                        moveCell(tile, emptyCellRow, emptyCellColumn)
                        emptyCellRow += -dy
                        emptyCellColumn += -dx
                    }
                    mergeCandidate = tile
                }
            }
        }
    }

    private fun moveCell(cell: Cell, newRow: Int, newColumn: Int) {
        cell.move(newRow, newColumn)
        grid[cell.previousRow][cell.previousColumn] = null
        grid[cell.row][cell.column] = cell
    }

    private fun mergeCells(cell1: Cell, cell2: Cell) {
        val merged = Cell(cell1.row, cell1.column, cell1.value * 2).apply {
            setMergedFrom(cell1, cell2)
        }
        grid[cell1.row][cell1.column] = merged
        grid[cell2.row][cell2.column] = null
    }

    private fun spawn(count: Int = 1) {
        findFreeSpots()
        if (count > freeSpots.size)
            throw IllegalArgumentException("Can't spawn $count items when only ${freeSpots.size} cells are available")

        for (i in 0 until count) {
            val spotIndex = Random.Default.nextInt(freeSpots.size)
            val spot = freeSpots[spotIndex]
            val value = generateTileValue()
            grid[spot.y][spot.x] = Cell(spot.y, spot.x, value)
            if (i < count - 1)
                freeSpots.removeAt(spotIndex)
        }
    }

    private fun generateTileValue(): Int {
        val n = Random.Default.nextInt(100)
        return when {
            n < 15 -> 4
            else -> 2
        }
    }

    private fun updateSlidableDirections() {
        slidableDirections = 0
        slidableDirections = slidableDirections or
                checkIsDirectionSlidable(LEFT) or
                checkIsDirectionSlidable(RIGHT) or
                checkIsDirectionSlidable(TOP) or
                checkIsDirectionSlidable(BOTTOM)
    }

    private fun checkIsDirectionSlidable(direction: Int): Int {
        traverseLines(direction) { row, column, dx, dy ->
            if (isLineSlidable(row, column, dx, dy))
                return direction
        }
        return 0
    }

    private fun isLineSlidable(startRow: Int, startColumn: Int, dx: Int, dy: Int): Boolean {
        var lastValue = -1
        var emptyCellFound = false
        traverseLine(startRow, startColumn, dx, dy) { _, _, tile ->
            if (tile != null) {
                if (emptyCellFound || lastValue == tile.value)
                    return true
                lastValue = tile.value
            } else {
                emptyCellFound = true
            }
        }
        return false
    }

    private fun findFreeSpots() {
        freeSpots.clear()
        traverseGrid { row, column, cell ->
            if (cell == null) {
                freeSpots.add(Point(column, row))
            }
        }
    }

    private inline fun traverseLines(
        direction: Int,
        body: (row: Int, column: Int, dx: Int, dy: Int) -> Unit
    ) {
        checkDirection(direction)
        val (dx, dy) = traverseLinesDeltas.getValue(direction)
        val xRange = traverseLinesXRanges.getValue(direction)
        val yRange = traverseLinesYRanges.getValue(direction)
        for (row in yRange) {
            for (column in xRange) {
                body(row, column, dx, dy)
            }
        }
    }

    private inline fun traverseLine(
        startRow: Int,
        startColumn: Int,
        dx: Int,
        dy: Int,
        body: (row: Int, column: Int, cell: Cell?) -> Unit
    ) {
        var row = startRow
        var column = startColumn
        while (column in 0 until columns && row in 0 until rows) {
            val cell = grid[row][column]
            body(row, column, cell)
            row += -dy
            column += -dx
        }
    }

    private fun checkDirection(direction: Int) {
        if (direction != LEFT && direction != TOP && direction != RIGHT && direction != BOTTOM)
            throw IllegalArgumentException("Unknown direction!")
    }

    companion object {
        const val LEFT = 1
        const val TOP = 2
        const val RIGHT = 4
        const val BOTTOM = 8
    }
}