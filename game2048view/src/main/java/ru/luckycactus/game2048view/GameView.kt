package ru.luckycactus.game2048view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnNextLayout
import ru.luckycactus.game.Game
import kotlin.properties.Delegates

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var game: Game? by Delegates.observable(null) { _, old, new ->
        gridBitmap = null
        positionInvalidated = true
        if (old?.rows != new?.rows || old?.columns != new?.columns) {
            invalidate()
            requestLayout()
        }
        doOnNextLayout {
            grid.game = new
        }
    }

    var spacing by Delegates.observable(dpF(8f)) { _, _, _ ->
        invalidate()
        requestLayout()
    }
    var gridBgColor: Int
        get() = gridBgPaint.color
        set(value) {
            gridBgPaint.color = value
            invalidate()
        }
    var gridCellColor: Int
        get() = gridCellPaint.color
        set(value) {
            gridCellPaint.color = value
            invalidate()
        }
    var gridCornerRadius by Delegates.observable(dpF(8f)) { _, _, _ ->
        invalidate()
    }
    var tileCornerRadius by Delegates.observable(dpF(8f)) { _, _, _ ->
        invalidate()
    }

    internal val transformer = GridTransformer()
    private val grid = Grid(this)
    private var positionInvalidated = true

    private val gridBgPaint = Paint()
    private val gridCellPaint = Paint()
    internal val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val tileTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private var gridBitmap: Bitmap? = null
    private val gridBitmapPaint = Paint()

    private var tileSize = 0f
    private var gridWidth = 0f
    private var gridHeight = 0f
    private var gridTop = 0f
    private var gridLeft = 0f

    private var lastTick: Long = -1
    private val tmpPointF = PointF()

    internal val colors = intArrayOf(
        ResourcesCompat.getColor(resources, R.color.color_tile_2, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_4, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_8, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_16, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_32, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_64, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_128, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_256, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_512, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_1024, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_2048, context.theme),
        ResourcesCompat.getColor(resources, R.color.color_tile_4096, context.theme)
    )
    internal var textColors: IntArray

    init {
        gridBgColor = ResourcesCompat.getColor(resources, R.color.color_grid_bg, context.theme)
        gridCellColor = ResourcesCompat.getColor(resources, R.color.color_cell, context.theme)

        val tc1 = ResourcesCompat.getColor(resources, R.color.color_text1, context.theme)
        val tc2 = ResourcesCompat.getColor(resources, R.color.color_text2, context.theme)
        textColors = intArrayOf(
            tc1, tc1, tc2, tc2, tc2, tc2, tc2, tc2, tc2, tc2, tc2, tc2
        )
    }

    // todo listener
    fun update() {
        updatePosition()
        grid.update()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        _measure(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(
            gridWidth.toInt() + paddingLeft + paddingRight,
            gridHeight.toInt() + paddingTop + paddingBottom
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != measuredWidth || h != measuredHeight) {
            _measure(w, h)
        }
        updatePosition()
    }

    private fun updatePosition() {
        if (positionInvalidated && width != 0 && height != 0) {
            gridTop = (height - gridHeight) / 2
            gridLeft = (width - gridWidth) / 2
            transformer.setDimensions(
                game?.rows ?: 0,
                game?.columns ?: 0,
                tileSize,
                gridLeft,
                gridTop,
                spacing
            )
            grid.refresh()
            positionInvalidated = false
        }
    }

    private fun _measure(width: Int, height: Int) {
        game?.let { game ->
            val verticalLinesWidth = (game.rows + 1) * spacing
            val horizontalLinesWidth = (game.columns + 1) * spacing
            val widthForCells = width - paddingLeft - paddingRight - verticalLinesWidth
            val heightForCells = height - paddingTop - paddingBottom - horizontalLinesWidth
            tileSize = minOf(
                widthForCells / game.columns,
                heightForCells / game.rows
            )
            gridWidth = tileSize * game.columns + horizontalLinesWidth
            gridHeight = tileSize * game.rows + verticalLinesWidth
        } ?: run {
            tileSize = 0f
            gridWidth = 0f
            gridHeight = 0f
            gridTop = 0f
            gridLeft = 0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (game == null || height == 0 || width == 0)
            return

        val time = System.currentTimeMillis()
        val dt = if (lastTick > 0) time - lastTick else 0
        lastTick = time
        prepareGridBitmap()
        canvas.drawBitmap(gridBitmap!!, gridLeft, gridTop, gridBitmapPaint)
        grid.tick(dt)
        grid.draw(canvas)
        if (grid.state == Grid.STATE_IDLE) {
            lastTick = -1
        } else {
            invalidate()
        }
    }

    private fun prepareGridBitmap() {
        var bitmap = gridBitmap
        if (bitmap == null || bitmap.width != gridWidth.toInt() || bitmap.height != gridHeight.toInt()) {
            bitmap?.recycle()
            bitmap =
                Bitmap.createBitmap(gridWidth.toInt(), gridHeight.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawGridBackground(canvas)
            drawGridCells(canvas)
            gridBitmap = bitmap
        }
    }

    private fun drawGridBackground(canvas: Canvas) {
        canvas.drawRoundRect(
            gridLeft,
            gridTop,
            gridLeft + gridWidth,
            gridTop + gridHeight,
            gridCornerRadius,
            gridCornerRadius,
            gridBgPaint
        )
    }

    private fun drawGridCells(canvas: Canvas) {
        for (row in 0 until game!!.rows) {
            for (column in 0 until game!!.columns) {
                transformer.transform(row, column, tmpPointF)
                canvas.drawRoundRect(
                    tmpPointF.x,
                    tmpPointF.y,
                    tmpPointF.x + transformer.tileSize,
                    tmpPointF.y + transformer.tileSize,
                    tileCornerRadius,
                    tileCornerRadius,
                    gridCellPaint
                )
            }
        }
    }

    companion object {
        const val ANIM_DURATION = 100L
    }
}