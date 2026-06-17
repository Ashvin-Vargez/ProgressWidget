package com.progresswidget

import android.graphics.*
import java.util.Calendar
import kotlin.math.*

object WidgetRenderer {

    // Colors
    private val COLOR_BG = Color.parseColor("#000000")
    private val COLOR_CARD = Color.parseColor("#141414")
    private val COLOR_DOT_LAND = Color.parseColor("#E8E8E8")
    private val COLOR_DOT_OCEAN = Color.parseColor("#3A3A3A")
    private val COLOR_DOT_SHADOW = Color.parseColor("#232323")
    private val COLOR_RED = Color.parseColor("#E24B4A")
    private val COLOR_DIM = Color.parseColor("#3A3A3A")
    private val COLOR_BRIGHT = Color.parseColor("#D8D8D8")
    private val COLOR_LABEL = Color.parseColor("#777777")
    private val COLOR_WHITE = Color.parseColor("#FFFFFF")
    private val COLOR_TODAY_BG = Color.parseColor("#E24B4A")
    private val COLOR_TODAY_TEXT = Color.parseColor("#501313")
    private val COLOR_PAST_DATE = Color.parseColor("#444444")

    fun render(
        widthPx: Int,
        heightPx: Int,
        wakeH: Int, wakeM: Int,
        sleepH: Int, sleepM: Int,
        pulsePhase: Float  // 0.0 to 1.0, drives the red arc fill animation
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon ... 7=Sat
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val month = now.get(Calendar.MONTH) // 0-indexed
        val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
        val maxDay = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val maxDayOfYear = now.getActualMaximum(Calendar.DAY_OF_YEAR)
        val weekOfYear = now.get(Calendar.WEEK_OF_YEAR)

        // Compute % REMAINING (100 = just started, 0 = done)
        val nowMins = hour * 60 + minute
        val wakeMins = wakeH * 60 + wakeM
        val sleepMins = sleepH * 60 + sleepM
        val totalAwakeMins = (sleepMins - wakeMins).coerceAtLeast(1)
        val dayPctRemaining = ((sleepMins - nowMins).toFloat() / totalAwakeMins * 100f)
            .coerceIn(0f, 100f)
        val dayElapsed = 1f - dayPctRemaining / 100f

        // Week: Mon=0 ... Sun=6 (convert from Calendar's Sun=1)
        val dow0 = (dayOfWeek + 5) % 7  // 0=Mon, 6=Sun
        val dayFraction = (hour * 60 + minute) / 1440f
        val weekElapsed = (dow0 + dayFraction) / 7f
        val weekPctRemaining = ((1f - weekElapsed) * 100f).coerceIn(0f, 100f)

        val monthElapsed = dayOfMonth.toFloat() / maxDay
        val monthPctRemaining = ((1f - monthElapsed) * 100f).coerceIn(0f, 100f)

        val yearElapsed = dayOfYear.toFloat() / maxDayOfYear
        val yearPctRemaining = ((1f - yearElapsed) * 100f).coerceIn(0f, 100f)

        // Layout constants (all in px, relative to widget size)
        val pad = (widthPx * 0.03f)
        val gap = (widthPx * 0.02f)
        val cornerR = (widthPx * 0.04f)

        // Globe card: left 40% of width
        val globeCardW = widthPx * 0.38f
        val globeCardH = heightPx - pad * 2
        val globeCardL = pad
        val globeCardT = pad

        // Right column
        val rightL = pad + globeCardW + gap
        val rightW = widthPx - rightL - pad

        // Week bar card: top portion of right column (~28% height)
        val weekCardH = heightPx * 0.28f
        val weekCardT = pad
        val weekCardL = rightL
        val weekCardW = rightW

        // Bottom two squares side by side
        val squareT = pad + weekCardH + gap
        val squareH = heightPx - squareT - pad
        val squareW = (rightW - gap) / 2f
        val calCardL = rightL
        val yearCardL = rightL + squareW + gap

        // --- Draw widget background ---
        paint.color = COLOR_BG
        canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), paint)

        // --- Draw globe card ---
        drawCard(canvas, paint, globeCardL, globeCardT, globeCardW, globeCardH, cornerR)
        drawGlobe(canvas, paint,
            globeCardL + globeCardW / 2f,
            globeCardT + globeCardH * 0.45f,
            minOf(globeCardW, globeCardH) * 0.43f,
            dayElapsed, pulsePhase)

        // Day % remaining label (small, below globe)
        paint.color = COLOR_LABEL
        paint.textSize = heightPx * 0.07f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText("${dayPctRemaining.toInt()}%", globeCardL + globeCardW / 2f,
            globeCardT + globeCardH * 0.88f, paint)
        paint.textSize = heightPx * 0.055f
        canvas.drawText("AWAKE", globeCardL + globeCardW / 2f,
            globeCardT + globeCardH * 0.96f, paint)

        // --- Draw week bar card ---
        drawCard(canvas, paint, weekCardL, weekCardT, weekCardW, weekCardH, cornerR)

        // W## label and % remaining
        paint.color = COLOR_LABEL
        paint.textSize = heightPx * 0.07f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("W$weekOfYear", weekCardL + gap, weekCardT + weekCardH * 0.38f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = COLOR_WHITE
        canvas.drawText("${weekPctRemaining.toInt()}%", weekCardL + weekCardW - gap,
            weekCardT + weekCardH * 0.38f, paint)

        // Week dot-font progress bar
        val weekStr = "MONTUEWEDTHUFRISATSUN"
        val wDotR = weekCardH * 0.062f
        val wStepX = weekCardH * 0.12f
        val wStepY = weekCardH * 0.10f
        val wCharGap = weekCardH * 0.055f
        val wBarW = DotFont.measureWidth(weekStr, wDotR, wStepX, wCharGap)
        val wScale = (weekCardW - gap * 2) / wBarW
        val wDotRs = wDotR * wScale
        val wStepXs = wStepX * wScale
        val wStepYs = wStepY * wScale
        val wCharGaps = wCharGap * wScale
        val wBarX = weekCardL + gap
        val wBarY = weekCardT + weekCardH * 0.52f

        // Each char index 0..20, week elapsed = weekElapsed (0..1)
        // charElapsed: how many "char units" have elapsed (21 total)
        val charElapsed = weekElapsed * 21f

        DotFont.draw(canvas, weekStr, wBarX, wBarY, wDotRs, wStepXs, wStepYs, wCharGaps, paint) { ci, globalDot ->
            val dotProgress = ci.toFloat() + (globalDot - ci)
            when {
                dotProgress < charElapsed - 0.5f -> COLOR_DIM          // fully elapsed
                dotProgress < charElapsed + 0.5f -> COLOR_RED           // current boundary (today accent)
                else -> COLOR_BRIGHT                                      // remaining
            }
        }

        // --- Draw mini calendar card ---
        drawCard(canvas, paint, calCardL, squareT, squareW, squareH, cornerR)
        drawCalendar(canvas, paint, calCardL, squareT, squareW, squareH, gap,
            dayOfMonth, month, maxDay, monthPctRemaining)

        // --- Draw year card ---
        drawCard(canvas, paint, yearCardL, squareT, squareW, squareH, cornerR)
        drawYearBar(canvas, paint, yearCardL, squareT, squareW, squareH, gap,
            yearPctRemaining, yearElapsed, month)

        return bmp
    }

    private fun drawCard(canvas: Canvas, paint: Paint, l: Float, t: Float, w: Float, h: Float, r: Float) {
        paint.color = COLOR_CARD
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(l, t, l + w, t + h, r, r, paint)
    }

    private fun drawGlobe(
        canvas: Canvas, paint: Paint,
        cx: Float, cy: Float, R: Float,
        dayElapsed: Float, pulsePhase: Float
    ) {
        // Shadow circle: at elapsed=0 center is far right (no shadow), at 1 center is far left (fully dark)
        val shadowR = R * 1.08f
        val shadowCx = (cx + R + shadowR) - dayElapsed * (2 * R + 2 * shadowR)
        val shadowCy = cy

        paint.style = Paint.Style.FILL

        // Draw all globe dots
        for (dot in GlobeDotMap.dots) {
            val px = cx + dot.normX * R
            val py = cy + dot.normY * R
            val sdx = px - shadowCx
            val sdy = py - shadowCy
            val inShadow = sdx * sdx + sdy * sdy < shadowR * shadowR

            paint.color = when {
                inShadow -> COLOR_DOT_SHADOW
                dot.isLand -> COLOR_DOT_LAND
                else -> COLOR_DOT_OCEAN
            }
            val dotR = R * 0.038f
            canvas.drawCircle(px, py, dotR, paint)
        }

        // Draw red terminator arc (pulsing fill)
        // Collect arc points, then fill progressively based on pulsePhase
        val arcPoints = mutableListOf<Pair<Float, Float>>()
        var angle = 0.0
        while (angle < 360.0) {
            val rad = Math.toRadians(angle)
            val tx = shadowCx + shadowR * cos(rad).toFloat()
            val ty = shadowCy + shadowR * sin(rad).toFloat()
            val dx = tx - cx; val dy = ty - cy
            if (dx * dx + dy * dy <= R * R) {
                arcPoints.add(Pair(tx, ty))
            }
            angle += 3.0
        }

        val fillCount = (arcPoints.size * pulsePhase).toInt()
        paint.color = COLOR_RED
        val arcDotR = R * 0.033f
        for (i in 0 until fillCount) {
            canvas.drawCircle(arcPoints[i].first, arcPoints[i].second, arcDotR, paint)
        }
    }

    private fun drawCalendar(
        canvas: Canvas, paint: Paint,
        l: Float, t: Float, w: Float, h: Float, gap: Float,
        dayOfMonth: Int, month: Int, maxDay: Int, monthPctRemaining: Float
    ) {
        val monthNames = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
            "JUL","AUG","SEP","OCT","NOV","DEC")

        // Month label in dot font (red)
        val mStr = monthNames[month]
        val mDotR = h * 0.030f
        val mStepX = h * 0.055f
        val mStepY = h * 0.052f
        val mCharGap = h * 0.025f
        val mBarX = l + gap
        val mBarY = t + gap * 0.8f
        DotFont.draw(canvas, mStr, mBarX, mBarY, mDotR, mStepX, mStepY, mCharGap, paint) { _, _ -> COLOR_RED }

        // Month % remaining (small, top right)
        paint.color = COLOR_WHITE
        paint.textSize = h * 0.10f
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText("${monthPctRemaining.toInt()}%", l + w - gap, t + gap + h * 0.10f, paint)

        // Day-of-week header
        val days = arrayOf("S","M","T","W","T","F","S")
        val headerY = t + h * 0.26f
        val cellW = (w - gap * 2) / 7f
        paint.textSize = h * 0.09f
        paint.textAlign = Paint.Align.CENTER
        days.forEachIndexed { i, d ->
            paint.color = COLOR_LABEL
            canvas.drawText(d, l + gap + cellW * i + cellW / 2f, headerY, paint)
        }

        // Day numbers
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sun
        val cellH = (h - headerY + t) / 6f
        val startRow = 0
        paint.textSize = h * 0.10f

        for (d in 1..maxDay) {
            val idx = firstDow + d - 1
            val col = idx % 7
            val row = idx / 7
            val cx2 = l + gap + cellW * col + cellW / 2f
            val cy2 = headerY + cellH * (row + 1)

            when {
                d == dayOfMonth -> {
                    // Today: red circle background
                    paint.color = COLOR_TODAY_BG
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx2, cy2 - h * 0.04f, cellW * 0.38f, paint)
                    paint.color = COLOR_TODAY_TEXT
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
                d < dayOfMonth -> {
                    paint.color = COLOR_PAST_DATE
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
                else -> {
                    paint.color = COLOR_WHITE
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
            }
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawYearBar(
        canvas: Canvas, paint: Paint,
        l: Float, t: Float, w: Float, h: Float, gap: Float,
        yearPctRemaining: Float, yearElapsed: Float, currentMonth: Int
    ) {
        // "2026" label top left, % top right
        val cal = Calendar.getInstance()
        paint.color = COLOR_LABEL
        paint.textSize = h * 0.10f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText(cal.get(Calendar.YEAR).toString(), l + gap, t + gap + h * 0.10f, paint)

        paint.color = COLOR_WHITE
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${yearPctRemaining.toInt()}%", l + w - gap, t + gap + h * 0.10f, paint)

        // 4-line dot-font year bar: JANFEBMAR / APRMAYJUN / JULAUGSEP / OCTNOVDEC
        val yearLines = arrayOf("JANFEBMAR", "APRMAYJUN", "JULAUGSEP", "OCTNOVDEC")
        val totalChars = 36  // 12 months x 3 chars
        val elapsedChars = yearElapsed * totalChars  // how many chars are "elapsed"

        // We want the remaining portion = from elapsedChars to end = white/red
        // Elapsed portion = dim gray
        // Current month partial = red on the remaining side (right side of boundary)

        val barAreaH = h - gap * 2 - h * 0.16f
        val lineH = barAreaH / 4f
        val barY0 = t + gap + h * 0.16f

        val dDotR: Float
        val dStepX: Float
        val dStepY: Float
        val dCharGap: Float

        // Scale dot font to fit line width
        val testStr = yearLines[0]
        val testDotR = lineH * 0.11f
        val testStepX = lineH * 0.20f
        val testStepY = lineH * 0.17f
        val testCharGap = lineH * 0.10f
        val naturalW = DotFont.measureWidth(testStr, testDotR, testStepX, testCharGap)
        val scale = (w - gap * 2) / naturalW
        dDotR = testDotR * scale
        dStepX = testStepX * scale
        dStepY = testStepY * scale
        dCharGap = testCharGap * scale

        yearLines.forEachIndexed { lineIdx, lineStr ->
            val lineCharStart = lineIdx * 9  // 9 chars per line (3 months)
            val lineY = barY0 + lineIdx * lineH

            DotFont.draw(canvas, lineStr, l + gap, lineY, dDotR, dStepX, dStepY, dCharGap, paint) { ci, _ ->
                val globalCharIdx = lineCharStart + ci
                val globalCharIdxF = globalCharIdx.toFloat()
                when {
                    globalCharIdxF + 1f <= elapsedChars -> COLOR_DIM   // fully elapsed
                    globalCharIdxF < elapsedChars -> COLOR_RED           // straddling boundary = red (current month partial)
                    else -> COLOR_BRIGHT                                  // remaining
                }
            }
        }
    }
}
