package ru.luckycactus.game2048view

import android.graphics.Canvas
import ru.luckycactus.game.Cell
import ru.luckycactus.game.Game
import kotlin.properties.Delegates

internal class Grid(
    private val gameView: GameView
) {
    private val transformer: GridTransformer = gameView.transformer

    private lateinit var grid: Array<Array<Tile?>>
    private lateinit var prev: Array<Array<Tile?>>

    private val mergingTiles = mutableListOf<Tile>()
    private val spawningTiles = mutableListOf<Tile>()
    private val mergedTiles = mutableListOf<Tile>()

    var state = STATE_IDLE
        private set

    var game: Game? by Delegates.observable(null) { _, old, new ->
        refresh()
    }

    fun tick(dt: Long) {
        if (game == null)
            return

        var hasActiveTiles = false
        grid.forEach { row ->
            row.forEach { tile ->
                tile?.let {
                    it.tick(dt)
                    hasActiveTiles = hasActiveTiles || it.state != Tile.STATE_IDLE
                }
            }
        }
        mergingTiles.forEach {
            it.tick(dt)
            hasActiveTiles = hasActiveTiles || it.state != Tile.STATE_IDLE
        }
        if (state != STATE_IDLE && !hasActiveTiles) {
            nextState()
        }
    }

    fun update() {
        if (gameView.height == 0 || gameView.width == 0)
            return
        val game = game ?: return

        if (state != STATE_IDLE) {
            finishAnimations()
        }

        swapBuffers()

        game.traverseGrid { row, column, tile ->
            if (tile != null) {
                if (tile.previousRow >= 0 && tile.previousColumn >= 0) {
                    if (tile.row != tile.previousRow || tile.column != tile.previousColumn)
                        move(tile)
                    else {
                        pass(tile)
                    }
                } else if (tile.mergedFrom != null) {
                    merge(tile)
                } else {
                    spawn(tile)
                }
            } else {
                grid[row][column] = null
            }
        }
        state = STATE_MOVE
    }

    fun refresh() {
        reset()
        update()
    }

    private fun reset() {
        createBuffers()
        mergingTiles.clear()
        spawningTiles.clear()
        mergedTiles.clear()
        state = STATE_IDLE
    }

    private fun finishAnimations() {
        while (state != STATE_IDLE) {
            tick(GameView.ANIM_DURATION + 1)
        }
    }

    private fun nextState() {
        when (state) {
            STATE_MOVE -> {
                mergingTiles.clear()
                spawningTiles.forEach {
                    it.spawn(GameView.ANIM_DURATION)
                    grid[it.row][it.column] = it
                }
                mergedTiles.forEach {
                    it.merge(GameView.ANIM_DURATION)
                    grid[it.row][it.column] = it
                }
                if (spawningTiles.isNotEmpty() || mergedTiles.isNotEmpty()) {
                    state = STATE_SPAWN
                    spawningTiles.clear()
                    mergedTiles.clear()
                } else {
                    state = STATE_IDLE
                }
            }
            STATE_SPAWN -> {
                state = STATE_IDLE
            }
        }
    }

    fun draw(canvas: Canvas) {
        if (game == null)
            return
        grid.forEach { row ->
            row.forEach { tile ->
                tile?.run {
                    draw(canvas)
                }
            }
        }
        mergingTiles.forEach {
            it.draw(canvas)
        }
    }

    private fun move(cell: Cell) {
        val tile = prev[cell.previousRow][cell.previousColumn]
        if (tile == null || tile.value != cell.value) {
            spawn(cell)
        } else {
            tile.moveTo(cell.row, cell.column, GameView.ANIM_DURATION)
            grid[cell.row][cell.column] = tile
        }
    }

    private fun pass(cell: Cell) {
        val tile = prev[cell.row][cell.column]
        if (tile == null || tile.value != cell.value) {
            spawn(cell)
        } else {
            grid[cell.row][cell.column] = tile
        }
    }

    private fun merge(merged: Cell) {
        val tile = Tile(gameView, transformer, merged.row, merged.column, merged.value)
        mergedTiles.add(tile)

        val (from1, from2) = merged.mergedFrom!!
        val tile1 = prev[from1.y][from1.x]
        val tile2 = prev[from2.y][from2.x]

        if (tile1 != null && tile2 != null) {
            tile1.moveTo(merged.row, merged.column, GameView.ANIM_DURATION)
            tile2.moveTo(merged.row, merged.column, GameView.ANIM_DURATION)
            mergingTiles.add(tile2)
            mergingTiles.add(tile1)
        } else {
            spawn(merged)
        }
        grid[merged.row][merged.column] = null
    }

    private fun spawn(cell: Cell) {
        val tile = Tile(gameView, transformer, cell.row, cell.column, cell.value)
        spawningTiles.add(tile)
        grid[cell.row][cell.column] = null
    }

    private fun swapBuffers() {
        grid = prev.also { prev = grid }
    }

    private fun createBuffers() {
        val rows = game?.rows ?: 0
        val columns = game?.columns ?: 0
        grid = Array(rows) { Array(columns) { null } }
        prev = Array(rows) { Array(columns) { null } }
    }

    companion object {
        const val STATE_IDLE = 0
        const val STATE_MOVE = 1
        const val STATE_SPAWN = 2
    }
}