package ru.luckycactus.game2048view

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import androidx.core.graphics.withScale

internal class Tile(
    private val gameView: GameView,
    private val transformer: GridTransformer,
    row: Int,
    column: Int,
    val value: Int
) {
    var row = row
        private set
    var column = column
        private set

    private val color: Int
    private val textColor: Int

    private val bounds = RectF()
    private var position = PointF()
    private var prevPosition = PointF()
    private var scale = 1f
    private var stateElapsedTime = 0L
    private var stateDuration = 0L

    private val textSize: Float
    private val textWidth: Float
    private val textScale: Float

    var state = STATE_IDLE
        private set

    init {
        require(isPowerOfTwo(value))
        val i = minOf(gameView.colors.size - 1, Integer.numberOfTrailingZeros(value) - 1)
        color = gameView.colors[i]
        textColor = gameView.textColors[i]
        transformer.transform(row, column, position)
        bounds.set(
            position.x,
            position.y,
            position.x + transformer.tileSize,
            position.y + transformer.tileSize
        )

        //todo should be cached for reuse between tiles
        textSize = bounds.height() * TEXT_HEIGHT_FRACTION
        gameView.tileTextPaint.textSize = textSize
        textWidth = gameView.tileTextPaint.measureText(value.toString())
        textScale = minOf(1f, (bounds.width() * TEXT_MAX_WIDTH_FRACTION) / textWidth)
    }

    fun moveTo(row: Int, column: Int, duration: Long) {
        _setState(STATE_MOVE, duration)
        this.row = row
        this.column = column
        prevPosition.x = bounds.left
        prevPosition.y = bounds.top
        transformer.transform(row, column, position)
    }

    fun spawn(duration: Long) {
        _setState(STATE_SPAWN, duration)
    }

    fun merge(duration: Long) {
        _setState(STATE_MERGE, duration)
    }

    fun tick(dt: Long) {
        if (stateElapsedTime >= stateDuration && state != STATE_IDLE) {
            state = STATE_IDLE
            return
        }

        stateElapsedTime = minOf(stateDuration, stateElapsedTime + dt)
        val fraction = stateElapsedTime.toFloat() / stateDuration
        when (state) {
            STATE_MOVE -> {
                val left = lerp(prevPosition.x, position.x, fraction)
                val top = lerp(prevPosition.y, position.y, fraction)
                bounds.offsetTo(left, top)
            }
            STATE_SPAWN -> {
                scale = fraction
            }
            STATE_MERGE -> {
                scale = if (fraction < 0.5f) {
                    lerp(1f, MERGE_SCALE, fraction / 0.5f)
                } else {
                    lerp(MERGE_SCALE, 1f, (fraction - 0.5f) / 0.5f)
                }
            }
        }
    }

    fun draw(canvas: Canvas) {
        canvas.withScale(
            x = scale,
            y = scale,
            pivotX = bounds.centerX(),
            pivotY = bounds.centerY()
        ) {
            gameView.tileBgPaint.color = color
            drawRoundRect(
                bounds,
                gameView.tileCornerRadius,
                gameView.tileCornerRadius,
                gameView.tileBgPaint
            )

            canvas.withScale(
                x = textScale,
                y = textScale,
                pivotX = bounds.centerX(),
                pivotY = bounds.centerY()
            ) {
                gameView.tileTextPaint.color = textColor
                drawText(
                    value.toString(),
                    bounds.centerX() - textWidth / 2,
                    bounds.centerY() + (gameView.tileTextPaint.textSize - gameView.tileTextPaint.descent()) / 2,
                    gameView.tileTextPaint
                )
            }
        }
    }

    private fun _setState(state: Int, duration: Long) {
        if (this.state == state)
            return
        this.state = state
        this.stateDuration = duration
        stateElapsedTime = 0L
        scale = if (state == STATE_SPAWN)
            0f
        else
            1f
    }

    companion object {
        const val STATE_IDLE = 0
        const val STATE_MOVE = 1
        const val STATE_SPAWN = 2
        const val STATE_MERGE = 3

        private const val MERGE_SCALE = 1.2f

        private const val TEXT_HEIGHT_FRACTION = 0.5f
        private const val TEXT_MAX_WIDTH_FRACTION = 0.8f
    }
}