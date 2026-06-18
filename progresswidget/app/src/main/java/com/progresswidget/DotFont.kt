package com.progresswidget

import android.graphics.Canvas
import android.graphics.Paint

object DotFont {

    private val GLYPHS = mapOf(
        'A' to arrayOf("01110","10001","10001","11111","10001","10001","10001"),
        'B' to arrayOf("11110","10001","10001","11110","10001","10001","11110"),
        'C' to arrayOf("01111","10000","10000","10000","10000","10000","01111"),
        'D' to arrayOf("11110","10001","10001","10001","10001","10001","11110"),
        'E' to arrayOf("11111","10000","10000","11110","10000","10000","11111"),
        'F' to arrayOf("11111","10000","10000","11110","10000","10000","10000"),
        'G' to arrayOf("01111","10000","10000","10111","10001","10001","01111"),
        'H' to arrayOf("10001","10001","10001","11111","10001","10001","10001"),
        'I' to arrayOf("11111","00100","00100","00100","00100","00100","11111"),
        'J' to arrayOf("00111","00010","00010","00010","00010","10010","01100"),
        'K' to arrayOf("10001","10010","10100","11000","10100","10010","10001"),
        'L' to arrayOf("10000","10000","10000","10000","10000","10000","11111"),
        'M' to arrayOf("10001","11011","10101","10101","10001","10001","10001"),
        'N' to arrayOf("10001","11001","10101","10101","10011","10001","10001"),
        'O' to arrayOf("01110","10001","10001","10001","10001","10001","01110"),
        'P' to arrayOf("11110","10001","10001","11110","10000","10000","10000"),
        'Q' to arrayOf("01110","10001","10001","10101","10010","01101","00000"),
        'R' to arrayOf("11110","10001","10001","11110","10100","10010","10001"),
        'S' to arrayOf("01111","10000","10000","01110","00001","00001","11110"),
        'T' to arrayOf("11111","00100","00100","00100","00100","00100","00100"),
        'U' to arrayOf("10001","10001","10001","10001","10001","10001","01110"),
        'V' to arrayOf("10001","10001","10001","10001","10001","01010","00100"),
        'W' to arrayOf("10001","10001","10001","10101","10101","11011","10001"),
        'X' to arrayOf("10001","10001","01010","00100","01010","10001","10001"),
        'Y' to arrayOf("10001","10001","01010","00100","00100","00100","00100"),
        'Z' to arrayOf("11111","00001","00010","00100","01000","10000","11111"),
        '0' to arrayOf("01110","10001","10011","10101","11001","10001","01110"),
        '1' to arrayOf("00100","01100","00100","00100","00100","00100","01110"),
        '2' to arrayOf("01110","10001","00001","00010","00100","01000","11111"),
        '3' to arrayOf("11110","00001","00001","00110","00001","00001","11110"),
        '4' to arrayOf("10001","10001","10001","11111","00001","00001","00001"),
        '5' to arrayOf("11111","10000","11110","00001","00001","10001","01110"),
        '6' to arrayOf("00110","01000","10000","11110","10001","10001","01110"),
        '7' to arrayOf("11111","00001","00010","00100","01000","01000","01000"),
        '8' to arrayOf("01110","10001","10001","01110","10001","10001","01110"),
        '9' to arrayOf("01110","10001","10001","01111","00001","00010","01100"),
        '%' to arrayOf("11001","11010","00100","01000","10011","00011","00000"),
        ' ' to arrayOf("00000","00000","00000","00000","00000","00000","00000")
    )

    /**
     * Draw dot-matrix text.
     * boldFactor: 1.0 = normal dot radius, 1.4 = bolder/blockier (dots overlap slightly)
     */
    fun draw(
        canvas: Canvas,
        text: String,
        startX: Float, startY: Float,
        dotRadius: Float,
        stepX: Float, stepY: Float,
        charGap: Float,
        paint: Paint,
        boldFactor: Float = 1.0f,
        getColor: (charIndex: Int, globalDotX: Float) -> Int
    ) {
        val drawR = dotRadius * boldFactor
        var xOffset = startX
        text.forEachIndexed { ci, ch ->
            val glyph = GLYPHS[ch] ?: GLYPHS[' ']!!
            for (row in 0..6) {
                for (col in 0..4) {
                    if (glyph[row][col] == '1') {
                        val px = xOffset + col * stepX + dotRadius
                        val py = startY + row * stepY + dotRadius
                        val globalDotX = ci.toFloat() + col / 4f
                        paint.color = getColor(ci, globalDotX)
                        canvas.drawCircle(px, py, drawR, paint)
                    }
                }
            }
            xOffset += 5 * stepX + charGap
        }
    }

    fun measureWidth(text: String, dotRadius: Float, stepX: Float, charGap: Float): Float {
        if (text.isEmpty()) return 0f
        return text.length * (5 * stepX) + (text.length - 1) * charGap + dotRadius * 2
    }

    fun measureHeight(dotRadius: Float, stepY: Float): Float = 7 * stepY + dotRadius * 2
}
