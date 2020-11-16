package ru.luckycactus.game2048view

import android.graphics.PointF

internal class GridTransformer {

    private var rows = 0
    private var columns = 0
    var tileSize = 0f
        private set

    private var gridLeft = 0f
    private var gridTop = 0f
    private var spacing = 0f

    private var invalidated = true

    private lateinit var cellsPositions: Array<Array<PointF>>

    fun setDimensions(
        rows: Int,
        columns: Int,
        tileSize: Float,
        gridLeft: Float,
        gridTop: Float,
        spacing: Float
    ) {
        if (this.rows != rows || this.columns != columns) {
            this.rows = rows
            this.columns = columns
            invalidated = true
        }
        if (this.tileSize != tileSize
            || this.gridLeft != gridLeft
            || this.gridTop != gridTop
            || this.spacing != spacing
        ) {
            invalidated = true
            this.tileSize = tileSize
            this.gridLeft = gridLeft
            this.gridTop = gridTop
            this.spacing = spacing
        }
    }

    fun transform(row: Int, column: Int, cell: PointF) {
        cell.set(getPosition(row, column))
    }

    private fun getPosition(row: Int, column: Int): PointF {
        if (invalidated) {
            createArray()
            calculatePositions()
            invalidated = false
        }
        return cellsPositions[row][column]
    }

    private fun createArray() {
        cellsPositions = Array(rows) { Array(columns) { PointF() } }
    }

    private fun calculatePositions() {
        var top = gridTop + spacing
        for (row in 0 until rows) {
            var left = gridLeft + spacing
            for (column in 0 until columns) {
                cellsPositions[row][column].set(left, top)
                left += (tileSize + spacing)
            }
            top += (tileSize + spacing)
        }
    }
}