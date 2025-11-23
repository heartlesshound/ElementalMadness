package com.example.elementalsmash

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // --- Load level ---
    private val level: LevelDefinition =
        LevelLoader.load(context, "levels/level1.json")

    // --- Bitmaps ---
    private val backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.background_1)

    private val boardBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.board_rect)

    private val holeBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.hole)

    // Dest rects for scaling
    private val backgroundDst = Rect()
    private val boardDst = Rect()

    // Inner bounding box (padded board area)
    private var innerBoardRect: Rect? = null

    // Computed hole positions (screen coords)
    private var holePositions: List<PointF> = emptyList()

    // Current hole size in pixels (computed from grid)
    private var currentHoleSizePx: Float = 0f

    // Board area as percentages of the screen (tweak later to match mockup perfectly)
    private val boardLeftPercent = 0.15f
    private val boardRightPercent = 0.85f
    private val boardTopPercent = 0.90f
    private val boardBottomPercent = 0.15f

    // Padding INSIDE the board so holes don't sit on the frame
    private val boardPaddingXPercent = 0.08f
    private val boardPaddingYPercent = 0.08f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Background covers whole screen
        backgroundDst.set(0, 0, w, h)

        // Board rectangle
        val left = (boardLeftPercent * w).toInt()
        val right = (boardRightPercent * w).toInt()
        val bottom = (boardBottomPercent * h).toInt()
        val top = (boardTopPercent * h).toInt()

        boardDst.set(left, h - top, right, h - bottom)

        // Create inner bounding box (like CSS padding)
        val padX = (boardDst.width() * boardPaddingXPercent).toInt()
        val padY = (boardDst.height() * boardPaddingYPercent).toInt()

        val inner = Rect(
            boardDst.left + padX,
            boardDst.top + padY,
            boardDst.right - padX,
            boardDst.bottom - padY
        )

        innerBoardRect = inner

        // Compute holes using the inner box
        holePositions = computeHolePositions(inner, level)
    }

    /**
     * Centered master-grid placement with gutter.
     * Works for even/odd rows/cols and guarantees spacing.
     */
    private fun computeHolePositions(boardRect: Rect, level: LevelDefinition): List<PointF> {
        val boardW = boardRect.width().toFloat()
        val boardH = boardRect.height().toFloat()

        val cols = level.gridCols
        val rows = level.gridRows

        // How much of each cell the HOLE should occupy (rest becomes gutter)
        val holeScaleInCell = 0.65f  // smaller = more breathing room

        // Base cell size from available space
        val cellW = boardW / cols
        val cellH = boardH / rows
        val cellSize = minOf(cellW, cellH)

        // Hole size derived from cell size
        val holeSize = cellSize * holeScaleInCell
        currentHoleSizePx = holeSize

        // Actual grid width/height using cellSize (not holeSize)
        val gridW = cols * cellSize
        val gridH = rows * cellSize

        // Center the whole grid in the boardRect (even or odd is fine)
        val startX = boardRect.left + (boardW - gridW) / 2f
        val startY = boardRect.top + (boardH - gridH) / 2f

        val points = mutableListOf<PointF>()
        for (cell in level.holeCells) {
            val cx = startX + (cell.col + 0.5f) * cellSize
            val cy = startY + (cell.row + 0.5f) * cellSize
            points += PointF(cx, cy)
        }

        return points
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background stretched to screen
        canvas.drawBitmap(backgroundBitmap, null, backgroundDst, null)

        // Draw board
        canvas.drawBitmap(boardBitmap, null, boardDst, null)

        // Draw holes
        drawHoles(canvas)
    }

    private fun drawHoles(canvas: Canvas) {
        if (holePositions.isEmpty()) return

        val holeSize = currentHoleSizePx.takeIf { it > 0f }
            ?: (minOf(boardDst.width(), boardDst.height()) * 0.1f)

        val half = holeSize / 2f

        val dst = Rect()
        for (p in holePositions) {
            dst.set(
                (p.x - half).toInt(),
                (p.y - half).toInt(),
                (p.x + half).toInt(),
                (p.y + half).toInt()
            )
            canvas.drawBitmap(holeBitmap, null, dst, null)
        }
    }
}
