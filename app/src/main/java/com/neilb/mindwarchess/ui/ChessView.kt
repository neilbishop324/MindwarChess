package com.neilb.mindwarchess.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.neilb.mindwarchess.R
import com.neilb.mindwarchess.game.PositionList
import com.neilb.mindwarchess.model.PieceInPosition

class ChessView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        const val lightType = 0
        const val darkType = 1
        const val selectedType = 2
        const val enemyType = 3
        const val mateType = 4
    }

    private var darkColor: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    private var lightColor: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    private val selectedColor = Color.BLUE
    private val enemyColor = Color.RED
    private val mateColor = Color.YELLOW

    private var paint: Paint? = null

    var colorTypeList =
        Array(8) { y -> IntArray(8) { x -> if ((x + y) % 2 == 0) lightType else darkType } }

    var positionList = ArrayList(PositionList.map { ArrayList(it.toMutableList()) }.toMutableList())
        set(value) {
            field = value
            invalidate()
        }

    init {
        paint = Paint()

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ChessView,
            0, 0
        ).apply {

            try {
                darkColor = getInteger(R.styleable.ChessView_darkColor, Color.BLACK)
                lightColor = getInteger(R.styleable.ChessView_lightColor, Color.WHITE)
            } finally {
                recycle()
            }
        }
    }

    private val leftPadding = 50
    private val topPadding = 50

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var left = leftPadding
        var top = topPadding
        val boardWidth = if (this.measuredWidth > this.measuredHeight) this.measuredHeight else this.measuredWidth
        val size = (boardWidth - 100) / 8
        for (y in 0..7) {
            for (x in 0..7) {
                val color = getColorFromType(colorTypeList[y][x])
                paint!!.color = color
                canvas.apply {
                    drawRect(
                        left.toFloat(), top.toFloat(),
                        (left + size).toFloat(), (top + size).toFloat(), paint!!
                    )

                    if (positionList[y][x].status == PieceInPosition.isWhite
                        || positionList[y][x].status == PieceInPosition.isBlack
                    ) {
                        drawBitmap(
                            getPieceBitmapFromResource(
                                positionList[y][x].status,
                                positionList[y][x].piece!!,
                                size
                            ), left.toFloat(), top.toFloat(), paint
                        )
                    } else if (positionList[y][x].status == PieceInPosition.isTarget) {
                        paint?.color = Color.GREEN

                        val centerX = left + size / 2
                        val centerY = top + size / 2
                        val radius = size / 2 - 36

                        drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), paint!!)
                    }
                }
                left += size
            }
            top += size
            left = 50
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val cellWidth = (width - 100) / 8
        val numRows = 8
        val padding = 50
        val totalHeight = numRows * cellWidth + 2 * padding
        val measuredWidth = resolveSize(width, widthMeasureSpec)
        val measuredHeight = resolveSize(totalHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    fun changeColorType(type: Int, x: Int, y: Int) {
        colorTypeList[y][x] = type
        invalidate()
    }

    fun changePositionData(position: PieceInPosition, x: Int, y: Int) {
        positionList[y][x] = position
        invalidate()
    }

    private fun getColorFromType(type: Int?): Int {
        return when (type) {
            lightType -> lightColor!!
            darkType -> darkColor!!
            selectedType -> selectedColor
            enemyType -> enemyColor
            mateType -> mateColor
            else -> lightColor!!
        }
    }

    val getBitmap = { res: Int -> BitmapFactory.decodeResource(resources, res) }

    private fun getPieceBitmapFromResource(color: Int, piece: Int, size: Int): Bitmap {
        val bitmap = if (color == PieceInPosition.isWhite) {
            when (piece) {
                PieceInPosition.king.id -> getBitmap(R.drawable.white_king)
                PieceInPosition.queen.id -> getBitmap(R.drawable.white_queen)
                PieceInPosition.rook.id -> getBitmap(R.drawable.white_rook)
                PieceInPosition.knight.id -> getBitmap(R.drawable.white_knight)
                PieceInPosition.bishop.id -> getBitmap(R.drawable.white_bishop)
                else -> getBitmap(R.drawable.white_pawn)
            }
        } else {
            when (piece) {
                PieceInPosition.king.id -> getBitmap(R.drawable.black_king)
                PieceInPosition.queen.id -> getBitmap(R.drawable.black_queen)
                PieceInPosition.rook.id -> getBitmap(R.drawable.black_rook)
                PieceInPosition.knight.id -> getBitmap(R.drawable.black_knight)
                PieceInPosition.bishop.id -> getBitmap(R.drawable.black_bishop)
                else -> getBitmap(R.drawable.black_pawn)
            }
        }
        return Bitmap.createScaledBitmap(bitmap, size, size, false)
    }

    fun onClick(x: Int, y: Int, handle: (x: Int, y: Int) -> Unit) {
        if (x > leftPadding
            && x < this.measuredWidth - leftPadding
            && y > topPadding
            && y < this.measuredWidth - topPadding) {
            val elementSize = (this.measuredWidth - 100) / 8
            val col = (x - leftPadding) / elementSize //x
            val row = (y - topPadding) / elementSize //y
            handle(col, row)
        }
    }

    fun clearColorList() {
        colorTypeList =
            Array(8) { y -> IntArray(8) { x -> if ((x + y) % 2 == 0) lightType else darkType } }
        positionList.forEachIndexed { y, list ->
            list.forEachIndexed { x, pieceInPosition ->
                if (pieceInPosition.status == PieceInPosition.isTarget) {
                    positionList[y][x].status = PieceInPosition.isBlank
                }
            }
        }
        invalidate()
    }

}