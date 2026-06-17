package com.progresswidget

import android.graphics.*
import java.util.Calendar
import kotlin.math.*

object WidgetRenderer {

    private val COLOR_BG = Color.parseColor("#000000")
    private val COLOR_CARD = Color.parseColor("#141414")
    private val COLOR_DOT_LAND = Color.parseColor("#E8E8E8")
    private val COLOR_DOT_OCEAN = Color.parseColor("#3A3A3A")
    private val COLOR_DOT_SHADOW = Color.parseColor("#222222")
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
        pulsePhase: Float
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val month = now.get(Calendar.MONTH)
        val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
        val maxDay = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val maxDayOfYear = now.getActualMaximum(Calendar.DAY_OF_YEAR)
        val weekOfYear = now.get(Calendar.WEEK_OF_YEAR)

        val nowMins = hour * 60 + minute
        val wakeMins = wakeH * 60 + wakeM
        val sleepMins = sleepH * 60 + sleepM
        val totalAwakeMins = (sleepMins - wakeMins).coerceAtLeast(1)

        // FIX: handle before-wake and after-sleep states
        val dayElapsed: Float = when {
            nowMins < wakeMins -> 0f          // before wake: globe fully lit
            nowMins >= sleepMins -> 1f        // after sleep: globe fully dark
            else -> (nowMins - wakeMins).toFloat() / totalAwakeMins
        }
        val dayPctRemaining = ((1f - dayElapsed) * 100f).toInt().coerceIn(0, 100)

        val dow0 = (dayOfWeek + 5) % 7
        val dayFraction = nowMins / 1440f
        val weekElapsed = (dow0 + dayFraction) / 7f
        val weekPctRemaining = ((1f - weekElapsed) * 100f).toInt().coerceIn(0, 100)

        val monthElapsed = dayOfMonth.toFloat() / maxDay
        val monthPctRemaining = ((1f - monthElapsed) * 100f).toInt().coerceIn(0, 100)

        val yearElapsed = dayOfYear.toFloat() / maxDayOfYear
        val yearPctRemaining = ((1f - yearElapsed) * 100f).toInt().coerceIn(0, 100)

        // FIX: no outer padding — cards go edge to edge, only gaps between them
        val gap = (minOf(widthPx, heightPx) * 0.025f)
        val cornerR = gap * 1.6f

        // Globe card: left ~39% of width, full height
        val globeCardW = widthPx * 0.39f
        val globeCardH = heightPx.toFloat()
        val globeCardL = 0f
        val globeCardT = 0f

        // Right column starts after globe card + gap
        val rightL = globeCardW + gap
        val rightW = widthPx - rightL

        // Week bar: top of right column, ~28% of height
        val weekCardH = heightPx * 0.29f
        val weekCardT = 0f
        val weekCardL = rightL
        val weekCardW = rightW

        // Bottom two squares
        val squareT = weekCardH + gap
        val squareH = heightPx - squareT
        val squareW = (rightW - gap) / 2f
        val calCardL = rightL
        val yearCardL = rightL + squareW + gap

        // Draw transparent background
        canvas.drawColor(Color.TRANSPARENT)

        // Globe card
        drawCard(canvas, paint, globeCardL, globeCardT, globeCardW, globeCardH, cornerR)
        drawGlobe(canvas, paint,
            globeCardL + globeCardW / 2f,
            globeCardT + globeCardH * 0.44f,
            minOf(globeCardW, globeCardH) * 0.42f,
            dayElapsed, pulsePhase)

        paint.color = COLOR_LABEL
        paint.textSize = heightPx * 0.065f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText("$dayPctRemaining%", globeCardL + globeCardW / 2f,
            globeCardT + globeCardH * 0.87f, paint)
        paint.textSize = heightPx * 0.052f
        canvas.drawText("AWAKE", globeCardL + globeCardW / 2f,
            globeCardT + globeCardH * 0.95f, paint)

        // Week card
        drawCard(canvas, paint, weekCardL, weekCardT, weekCardW, weekCardH, cornerR)

        paint.color = COLOR_LABEL
        paint.textSize = heightPx * 0.07f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("W$weekOfYear", weekCardL + gap, weekCardT + weekCardH * 0.36f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = COLOR_WHITE
        canvas.drawText("$weekPctRemaining%", weekCardL + weekCardW - gap,
            weekCardT + weekCardH * 0.36f, paint)

        val weekStr = "MONTUEWEDTHUFRISATSUN"
        val wDotR = weekCardH * 0.060f
        val wStepX = weekCardH * 0.115f
        val wStepY = weekCardH * 0.098f
        val wCharGap = weekCardH * 0.050f
        val wBarW = DotFont.measureWidth(weekStr, wDotR, wStepX, wCharGap)
        val wScale = (weekCardW - gap * 2) / wBarW
        val wDotRs = wDotR * wScale
        val wStepXs = wStepX * wScale
        val wStepYs = wStepY * wScale
        val wCharGaps = wCharGap * wScale
        val wBarX = weekCardL + gap
        val wBarY = weekCardT + weekCardH * 0.50f
        val charElapsed = weekElapsed * 21f

        DotFont.draw(canvas, weekStr, wBarX, wBarY, wDotRs, wStepXs, wStepYs, wCharGaps, paint) { ci, globalDot ->
            val dotProgress = ci.toFloat() + (globalDot - ci)
            when {
                dotProgress < charElapsed - 0.5f -> COLOR_DIM
                dotProgress < charElapsed + 0.5f -> COLOR_RED
                else -> COLOR_BRIGHT
            }
        }

        // Calendar card
        drawCard(canvas, paint, calCardL, squareT, squareW, squareH, cornerR)
        drawCalendar(canvas, paint, calCardL, squareT, squareW, squareH, gap,
            dayOfMonth, month, maxDay, monthPctRemaining)

        // Year card
        drawCard(canvas, paint, yearCardL, squareT, squareW, squareH, cornerR)
        drawYearBar(canvas, paint, yearCardL, squareT, squareW, squareH, gap,
            yearPctRemaining, yearElapsed)

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
        // FIX: shadow sweeps LEFT to RIGHT correctly.
        // dayElapsed=0 (just woke) → shadow fully OFF to the LEFT (globe lit)
        // dayElapsed=1 (bedtime)   → shadow fully covers globe from the LEFT
        //
        // Shadow circle center starts far LEFT (off-screen), sweeps RIGHT across globe.
        // When shadowCx is far left of globe, the shadow circle (radius shadowR >> R)
        // covers the LEFT portion of the globe.
        // At elapsed=0: shadowCx = cx - R - shadowR*2  → no overlap, globe fully lit
        // At elapsed=1: shadowCx = cx + R + shadowR*2  → full overlap, globe fully dark
        //
        // BUT the terminator arc should face RIGHT (like letter D) meaning:
        // the dark half is on the LEFT, lit half on the RIGHT.
        // Shadow circle covers LEFT side → its RIGHT edge is the terminator (faces right ✓)

        val shadowR = R * 1.10f
        // At elapsed=0: center far right of globe (shadow doesn't touch globe from left)
        // Wait — to make left side dark: shadow center must be on the LEFT
        // shadow covers pts where dist(pt, shadowCx) < shadowR
        // If shadowCx is far LEFT, the right edge of shadow sweeps across globe left→right
        // At elapsed=0: no shadow = shadowCx far LEFT such that globe is outside shadow
        //   → shadowCx = cx - R - shadowR  (just touching left edge, no overlap)
        // At elapsed=1: full shadow = shadowCx moved right until globe fully inside
        //   → shadowCx = cx + R + shadowR  (shadow fully covers globe)
        // So shadowCx moves from (cx - R - shadowR) [lit] to (cx + R + shadowR) [dark]
        // The terminator arc (right edge of shadow circle) faces RIGHT = letter D ✓

        val shadowCx = (cx - R - shadowR) + dayElapsed * (2f * R + 2f * shadowR)
        val shadowCy = cy

        paint.style = Paint.Style.FILL

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

        // Red terminator arc: right edge of shadow circle, clipped to globe
        // Collect arc points sorted top→bottom for progressive fill pulse
        val arcPoints = mutableListOf<Pair<Float, Float>>()
        var angle = -90.0  // start from top
        while (angle < 270.0) {
            val rad = Math.toRadians(angle)
            val tx = shadowCx + shadowR * cos(rad).toFloat()
            val ty = shadowCy + shadowR * sin(rad).toFloat()
            val dx = tx - cx; val dy = ty - cy
            if (dx * dx + dy * dy <= R * R) {
                arcPoints.add(Pair(tx, ty))
            }
            angle += 2.5
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
        dayOfMonth: Int, month: Int, maxDay: Int, monthPctRemaining: Int
    ) {
        val monthNames = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
            "JUL","AUG","SEP","OCT","NOV","DEC")

        // FIX: month label much smaller — constrain to ~40% of card width, one row only
        val mStr = monthNames[month]
        val maxLabelW = w * 0.50f
        val mDotR = h * 0.018f
        val mStepX = h * 0.034f
        val mStepY = h * 0.032f
        val mCharGap = h * 0.015f
        val naturalLabelW = DotFont.measureWidth(mStr, mDotR, mStepX, mCharGap)
        val mScale = if (naturalLabelW > maxLabelW) maxLabelW / naturalLabelW else 1f
        val mDotRs = mDotR * mScale
        val mStepXs = mStepX * mScale
        val mStepYs = mStepY * mScale
        val mCharGaps = mCharGap * mScale
        val labelH = DotFont.measureHeight(mDotRs, mStepYs)

        DotFont.draw(canvas, mStr, l + gap, t + gap, mDotRs, mStepXs, mStepYs, mCharGaps, paint) { _, _ -> COLOR_RED }

        // % remaining top right
        paint.color = COLOR_WHITE
        paint.textSize = h * 0.09f
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText("$monthPctRemaining%", l + w - gap, t + gap + labelH, paint)

        // Day-of-week header row
        val days = arrayOf("S","M","T","W","T","F","S")
        val headerY = t + gap + labelH + h * 0.10f
        val cellW = (w - gap * 2) / 7f
        paint.textSize = h * 0.085f
        paint.textAlign = Paint.Align.CENTER

        days.forEachIndexed { i, d ->
            paint.color = COLOR_LABEL
            canvas.drawText(d, l + gap + cellW * i + cellW / 2f, headerY, paint)
        }

        // Day number grid
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sun
        val gridTop = headerY + h * 0.04f
        val gridH = h - (gridTop - t) - gap
        val cellH = gridH / 6f
        paint.textSize = h * 0.092f

        for (d in 1..maxDay) {
            val idx = firstDow + d - 1
            val col = idx % 7
            val row = idx / 7
            val cx2 = l + gap + cellW * col + cellW / 2f
            val cy2 = gridTop + cellH * row + cellH * 0.75f

            when {
                d == dayOfMonth -> {
                    paint.color = COLOR_TODAY_BG
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx2, cy2 - h * 0.038f, cellW * 0.36f, paint)
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
        yearPctRemaining: Int, yearElapsed: Float
    ) {
        val cal = Calendar.getInstance()
        paint.color = COLOR_LABEL
        paint.textSize = h * 0.09f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText(cal.get(Calendar.YEAR).toString(), l + gap, t + gap + h * 0.09f, paint)

        paint.color = COLOR_WHITE
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$yearPctRemaining%", l + w - gap, t + gap + h * 0.09f, paint)

        val yearLines = arrayOf("JANFEBMAR", "APRMAYJUN", "JULAUGSEP", "OCTNOVDEC")
        val totalChars = 36f
        val elapsedChars = yearElapsed * totalChars

        val barAreaH = h - gap * 2 - h * 0.14f
        val lineH = barAreaH / 4f
        val barY0 = t + gap + h * 0.14f

        val testStr = yearLines[0]
        val testDotR = lineH * 0.105f
        val testStepX = lineH * 0.192f
        val testStepY = lineH * 0.162f
        val testCharGap = lineH * 0.095f
        val naturalW = DotFont.measureWidth(testStr, testDotR, testStepX, testCharGap)
        val scale = (w - gap * 2) / naturalW
        val dDotR = testDotR * scale
        val dStepX = testStepX * scale
        val dStepY = testStepY * scale
        val dCharGap = testCharGap * scale

        yearLines.forEachIndexed { lineIdx, lineStr ->
            val lineCharStart = lineIdx * 9
            val lineY = barY0 + lineIdx * lineH

            DotFont.draw(canvas, lineStr, l + gap, lineY, dDotR, dStepX, dStepY, dCharGap, paint) { ci, _ ->
                val g = (lineCharStart + ci).toFloat()
                when {
                    g + 1f <= elapsedChars -> COLOR_DIM
                    g < elapsedChars -> COLOR_RED
                    else -> COLOR_BRIGHT
                }
            }
        }
    }
}
