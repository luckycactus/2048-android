package ru.luckycactus.game2048.model

import android.graphics.Point
import kotlin.random.Random

class Game(
    private val rows: Int,
    private val columns: Int
) {
    val grid = Array(rows) { Array<Tile?>(columns) { null } }

    private var started = false
    private var slidableDirections = 0
    private val traverseLinesDeltas: Map<Int, Pair<Int, Int>>
    private val traverseLinesXRanges: Map<Int, IntRange>
    private val traverseLinesYRanges: Map<Int, IntRange>

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

    private fun slideLine(startRow: Int, startColumn: Int, dx: Int, dy: Int) {
        var emptyRow = -1
        var emptyColumn = -1
        var mergeCandidate: Tile? = null
        traverseLine(startRow, startColumn, dx, dy) { row, column, tile ->
            if (tile == null) {
                if (emptyRow < 0) {
                    emptyRow = row
                    emptyColumn = column
                }
            } else {
                tile.clearPrevious()
                if (mergeCandidate?.value == tile.value) {
                    mergeTiles(mergeCandidate!!, tile)
                    mergeCandidate = null
                    if (emptyRow < 0) {
                        emptyRow = row
                        emptyColumn = column
                    }
                } else {
                    if (emptyRow >= 0) {
                        moveTile(tile, emptyRow, emptyColumn)
                        emptyRow += -dy
                        emptyColumn += -dx
                    }
                    mergeCandidate = tile
                }
            }
        }
    }

    private fun moveTile(tile: Tile, newRow: Int, newColumn: Int) {
        tile.move(newRow, newColumn)
        grid[tile.previousRow][tile.previousColumn] = null
        grid[tile.row][tile.column] = tile
    }

    private fun mergeTiles(tile1: Tile, tile2: Tile) {
        tile1.merge(tile2)
        grid[tile2.row][tile2.column] = null
    }

    private fun spawn(count: Int = 1) {
        val freeCells = getFreeCells()
        if (count > freeCells.size)
            throw IllegalArgumentException("Can't spawn $count items when only ${freeCells.size} cells are available")

        for (i in 0 until count) {
            val cellIndex = Random.Default.nextInt(freeCells.size)
            val cell = freeCells[cellIndex]
            val value = generateTileValue()
            grid[cell.y][cell.x] = Tile(cell.y, cell.x, value)
            if (i < count - 1)
                freeCells.removeAt(cellIndex)
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

    private fun getFreeCells(): MutableList<Point> {
        val free = mutableListOf<Point>()
        grid.forEachIndexed { y, row ->
            row.forEachIndexed { x, tile ->
                if (tile == null) {
                    free.add(Point(x, y))
                }
            }
        }
        return free
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
        body: (row: Int, column: Int, tile: Tile?) -> Unit
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