package com.example.elementalsmash

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

import android.os.SystemClock
import kotlin.random.Random

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

    // --- Runes ---
    private val earthRuneBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.rune_earth)

    // Rune animation state
    private var activeHoleIndex: Int = -1
    private var phaseStartMs: Long = 0L

    private enum class RunePhase { GROW, HOLD, SHRINK, IDLE }
    private var runePhase: RunePhase = RunePhase.IDLE

    // Timing (ms)
    private val growDurationMs = 650L
    private val holdDurationMs = 600L
    private val shrinkDurationMs = 550L
    private val gapDurationMs = 350L


    // Target size relative to hole
    private val runeTargetScale = 0.85f  // 85% of hole diameter

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

    // --- Game state ---
    private enum class GameState { READY, PLAYING, WIN, LOSE }
    private var gameState: GameState = GameState.READY

    private var hits = 0
    private var spawns = 0
    private val hitsNeeded = 5
    private val maxSpawns = 10

    // Track rune visibility for hit detection
    private var currentRuneScale: Float = 0f

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
        startNextRune()

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

    private fun startNextRune() {
        if (holePositions.isEmpty()) return
        if (gameState != GameState.PLAYING) return

        spawns += 1
        if (spawns > maxSpawns) {
            gameState = GameState.LOSE
            invalidate()
            return
        }

        activeHoleIndex = Random.nextInt(holePositions.size)
        runePhase = RunePhase.GROW
        phaseStartMs = SystemClock.uptimeMillis()

        postInvalidateOnAnimation()
    }


    private fun advancePhase(next: RunePhase) {
        runePhase = next
        phaseStartMs = SystemClock.uptimeMillis()
    }

    private fun easeOut(t: Float): Float {
        // simple smooth ease (0..1 -> 0..1)
        val inv = 1f - t
        return 1f - inv * inv
    }

    private fun easeIn(t: Float): Float {
        return t * t
    }

    private fun resetGame() {
        hits = 0
        spawns = 0
        activeHoleIndex = -1
        runePhase = RunePhase.IDLE
        currentRuneScale = 0f
        gameState = GameState.READY
        invalidate()
    }

    private fun startGame() {
        hits = 0
        spawns = 0
        gameState = GameState.PLAYING
        startNextRune()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background stretched to screen
        canvas.drawBitmap(backgroundBitmap, null, backgroundDst, null)

        // Draw board
        canvas.drawBitmap(boardBitmap, null, boardDst, null)

        // Draw holes
        drawHoles(canvas)
        drawRune(canvas)
        drawOverlay(canvas)

    }

    private fun drawOverlay(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = width * 0.05f
            isAntiAlias = true
            setShadowLayer(8f, 0f, 0f, Color.BLACK)
        }

        val msg = when (gameState) {
            GameState.READY -> "Tap to Start"
            GameState.WIN -> "You Win!\nTap to Restart"
            GameState.LOSE -> "Game Over!\nTap to Restart"
            GameState.PLAYING -> "Hits: $hits/$hitsNeeded   Spawns: $spawns/$maxSpawns"
        }

        val lines = msg.split("\n")
        val startY = height * 0.12f
        for (i in lines.indices) {
            canvas.drawText(lines[i], width / 2f, startY + i * paint.textSize * 1.2f, paint)
        }
    }


    private fun drawRune(canvas: Canvas) {
        if (gameState != GameState.PLAYING) return

        if (activeHoleIndex < 0 || activeHoleIndex >= holePositions.size) return

        val now = SystemClock.uptimeMillis()
        val elapsed = now - phaseStartMs

        val holeCenter = holePositions[activeHoleIndex]

        // If currentHoleSizePx somehow isn't set yet, fall back to a sane size
        val holeSize = if (currentHoleSizePx > 0f) {
            currentHoleSizePx
        } else {
            // fallback: 1/6 of board width
            boardDst.width() / 6f
        }

        val targetSize = holeSize * runeTargetScale

        val scale: Float = when (runePhase) {
            RunePhase.GROW -> {
                val t = (elapsed / growDurationMs.toFloat()).coerceIn(0f, 1f)
                if (t >= 1f) advancePhase(RunePhase.HOLD)
                easeOut(t)
            }
            RunePhase.HOLD -> {
                if (elapsed >= holdDurationMs) advancePhase(RunePhase.SHRINK)
                1f
            }
            RunePhase.SHRINK -> {
                val t = (elapsed / shrinkDurationMs.toFloat()).coerceIn(0f, 1f)
                if (t >= 1f) advancePhase(RunePhase.IDLE)
                1f - easeIn(t)
            }
            RunePhase.IDLE -> {
                if (elapsed >= gapDurationMs) startNextRune()
                0f
            }
        }

        currentRuneScale = scale

        if (scale > 0f) {
            val size = targetSize * scale
            val half = size / 2f

            val dst = RectF(
                holeCenter.x - half,
                holeCenter.y - half,
                holeCenter.x + half,
                holeCenter.y + half
            )

            canvas.drawBitmap(earthRuneBitmap, null, dst, null)
        }

        // Keep animating
        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action != android.view.MotionEvent.ACTION_DOWN) return true

        when (gameState) {
            GameState.READY -> {
                startGame()
                return true
            }
            GameState.WIN, GameState.LOSE -> {
                resetGame()
                return true
            }
            GameState.PLAYING -> {
                trySmash(event.x, event.y)
                return true
            }
        }
    }

    private fun trySmash(x: Float, y: Float) {
        if (activeHoleIndex < 0) return
        if (currentRuneScale <= 0.2f) return  // only hittable when mostly visible

        val center = holePositions[activeHoleIndex]
        val holeRadius = currentHoleSizePx * 0.5f
        val dx = x - center.x
        val dy = y - center.y
        val distSq = dx*dx + dy*dy

        if (distSq <= holeRadius * holeRadius) {
            // Successful smash
            hits += 1

            if (hits >= hitsNeeded) {
                gameState = GameState.WIN
                runePhase = RunePhase.IDLE
                currentRuneScale = 0f
                invalidate()
                return
            }

            // Force rune to disappear immediately, then next one after gap
            advancePhase(RunePhase.IDLE)
            currentRuneScale = 0f
            postInvalidateOnAnimation()
        }
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
