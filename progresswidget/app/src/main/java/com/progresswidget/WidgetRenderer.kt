package com.progresswidget

import android.graphics.*
import java.util.Calendar
import kotlin.math.*

object WidgetRenderer {

    // Slightly lighter card to match NothingOS default widget grey
    private val COLOR_CARD       = Color.parseColor("#1E1E1E")
    private val COLOR_DOT_LAND   = Color.parseColor("#E8E8E8")
    private val COLOR_DOT_OCEAN  = Color.parseColor("#3A3A3A")
    private val COLOR_DOT_SHADOW = Color.parseColor("#1A1A1A")
    private val COLOR_RED        = Color.parseColor("#E24B4A")
    private val COLOR_DIM        = Color.parseColor("#3A3A3A")
    private val COLOR_BRIGHT     = Color.parseColor("#D8D8D8")
    private val COLOR_LABEL      = Color.parseColor("#777777")
    private val COLOR_WHITE      = Color.parseColor("#FFFFFF")
    private val COLOR_TODAY_TEXT = Color.parseColor("#FFFFFF")
    private val COLOR_PAST_DATE  = Color.parseColor("#444444")
    private val COLOR_SUN_RED    = Color.parseColor("#E24B4A")

    fun render(
        widthPx: Int, heightPx: Int,
        wakeH: Int, wakeM: Int,
        sleepH: Int, sleepM: Int,
        pulsePhase: Float,
        weekStartDay: Int  // 0=Sun, 1=Mon
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val now        = Calendar.getInstance()
        val hour       = now.get(Calendar.HOUR_OF_DAY)
        val minute     = now.get(Calendar.MINUTE)
        val dayOfWeek  = now.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val month      = now.get(Calendar.MONTH)
        val dayOfYear  = now.get(Calendar.DAY_OF_YEAR)
        val maxDay     = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val maxDOY     = now.getActualMaximum(Calendar.DAY_OF_YEAR)
        val weekOfYear = now.get(Calendar.WEEK_OF_YEAR)

        val nowMins    = hour * 60 + minute
        val wakeMins   = wakeH  * 60 + wakeM
        val sleepMins  = sleepH * 60 + sleepM
        val totalAwake = (sleepMins - wakeMins).coerceAtLeast(1)

        val dayElapsed: Float = when {
            nowMins < wakeMins   -> 0f
            nowMins >= sleepMins -> 1f
            else -> (nowMins - wakeMins).toFloat() / totalAwake
        }
        val dayPct  = ((1f - dayElapsed) * 100f).toInt().coerceIn(0, 100)

        // Week: how many full days + fraction into today
        // dow0 = days since week start (0 = first day of week)
        val dow0Sun = dayOfWeek - 1  // 0=Sun..6=Sat
        val dow0 = if (weekStartDay == 1) (dow0Sun + 6) % 7 else dow0Sun // shift for Mon start
        val weekElapsed  = (dow0 + nowMins / 1440f) / 7f
        val weekPct      = ((1f - weekElapsed) * 100f).toInt().coerceIn(0, 100)

        val monthElapsed = dayOfMonth.toFloat() / maxDay
        val monthPct     = ((1f - monthElapsed) * 100f).toInt().coerceIn(0, 100)

        val yearElapsed  = dayOfYear.toFloat() / maxDOY
        val yearPct      = ((1f - yearElapsed) * 100f).toInt().coerceIn(0, 100)

        // Layout — much larger corner radius to match NothingOS
        val gap     = minOf(widthPx, heightPx) * 0.025f
        val cornerR = minOf(widthPx, heightPx) * 0.10f  // bigger curves

        val globeW  = widthPx * 0.39f
        val globeH  = heightPx.toFloat()
        val rightL  = globeW + gap
        val rightW  = widthPx - rightL
        val weekH   = heightPx * 0.29f
        val sqT     = weekH + gap
        val sqH     = heightPx - sqT
        val sqW     = (rightW - gap) / 2f
        val calL    = rightL
        val yearL   = rightL + sqW + gap

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        drawCard(canvas, paint, 0f,    0f,  globeW, globeH, cornerR)
        drawCard(canvas, paint, rightL, 0f, rightW, weekH,  cornerR)
        drawCard(canvas, paint, calL,  sqT, sqW,    sqH,    cornerR)
        drawCard(canvas, paint, yearL, sqT, sqW,    sqH,    cornerR)

        // Globe
        val gcx = globeW / 2f
        val gcy = globeH * 0.44f
        val GR  = minOf(globeW, globeH) * 0.42f
        drawGlobe(canvas, paint, gcx, gcy, GR, dayElapsed, pulsePhase)

        // Day % in dot font, red, below globe
        val pctStr    = "$dayPct%"
        val pDotR     = globeH * 0.018f
        val pSX       = globeH * 0.034f; val pSY = globeH * 0.030f; val pCG = globeH * 0.014f
        val pW        = DotFont.measureWidth(pctStr, pDotR, pSX, pCG)
        DotFont.draw(canvas, pctStr, gcx - pW / 2f, globeH * 0.845f,
            pDotR, pSX, pSY, pCG, paint) { _, _ -> COLOR_RED }

        paint.typeface  = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.CENTER
        paint.color     = COLOR_LABEL
        paint.textSize  = heightPx * 0.050f
        canvas.drawText("AWAKE", gcx, globeH * 0.955f, paint)

        // Week bar: "W" in red dot font, "25" in white dot font
        drawWeekBar(canvas, paint, rightL, rightW, weekH, gap,
            weekOfYear, weekPct, weekElapsed, dow0)

        // Calendar
        drawCalendar(canvas, paint, calL, sqT, sqW, sqH, gap,
            dayOfMonth, month, maxDay, monthPct, weekStartDay)

        // Year bar
        drawYearBar(canvas, paint, yearL, sqT, sqW, sqH, gap,
            yearPct, yearElapsed, month)

        return bmp
    }

    private fun drawCard(canvas: Canvas, paint: Paint,
                         l: Float, t: Float, w: Float, h: Float, r: Float) {
        paint.color = COLOR_CARD; paint.style = Paint.Style.FILL
        canvas.drawRoundRect(l, t, l + w, t + h, r, r, paint)
    }

    private fun drawGlobe(canvas: Canvas, paint: Paint,
                          cx: Float, cy: Float, R: Float,
                          dayElapsed: Float, pulsePhase: Float) {
        // Correct terminator: normX < (2*elapsed-1) → in shadow
        val terminatorNormX = 2f * dayElapsed - 1f
        val dotR = R * 0.022f
        paint.style = Paint.Style.FILL

        for (dot in GlobeDotMap.dots) {
            val px = cx + dot.normX * R
            val py = cy + dot.normY * R
            paint.color = when {
                dot.normX < terminatorNormX -> COLOR_DOT_SHADOW
                dot.isLand                  -> COLOR_DOT_LAND
                else                        -> COLOR_DOT_OCEAN
            }
            canvas.drawCircle(px, py, dotR, paint)
        }

        // Red terminator: dots in narrow band just right of terminator, sorted top→bottom
        val band = 0.055f
        val arcPoints = GlobeDotMap.dots
            .filter { it.normX >= terminatorNormX && it.normX < terminatorNormX + band }
            .map { Pair(cx + it.normX * R, cy + it.normY * R) }
            .sortedBy { it.second }

        val fillCount = (arcPoints.size * pulsePhase).toInt()
        paint.color = COLOR_RED
        arcPoints.take(fillCount).forEach {
            canvas.drawCircle(it.first, it.second, dotR * 1.2f, paint)
        }
    }

    private fun drawWeekBar(canvas: Canvas, paint: Paint,
                            rightL: Float, rightW: Float, weekH: Float, gap: Float,
                            weekOfYear: Int, weekPct: Int,
                            weekElapsed: Float, dow0: Int) {
        // "W" in red dot font + week number in white dot font on same baseline
        val hdrDotR = weekH * 0.055f
        val hdrSX   = weekH * 0.105f; val hdrSY = weekH * 0.090f; val hdrCG = weekH * 0.045f
        val labelY  = weekH * 0.08f

        // "W" red
        val wW = DotFont.measureWidth("W", hdrDotR, hdrSX, hdrCG)
        DotFont.draw(canvas, "W", rightL + gap, labelY,
            hdrDotR, hdrSX, hdrSY, hdrCG, paint) { _, _ -> COLOR_RED }

        // week number white
        val wnStr = weekOfYear.toString()
        DotFont.draw(canvas, wnStr, rightL + gap + wW + hdrCG, labelY,
            hdrDotR, hdrSX, hdrSY, hdrCG, paint) { _, _ -> COLOR_WHITE }

        // "%" remaining in red dot font, top right
        val pctStr   = "$weekPct%"
        val pctNatW  = DotFont.measureWidth(pctStr, hdrDotR, hdrSX, hdrCG)
        DotFont.draw(canvas, pctStr, rightL + rightW - gap - pctNatW, labelY,
            hdrDotR, hdrSX, hdrSY, hdrCG, paint) { _, _ -> COLOR_RED }

        // Week progress bar (MONTUEWEDTHUFRISATSUN or SUNMONTUEWEDTHUFRISAT)
        val wStr  = "MONTUEWEDTHUFRISATSUN"
        val wDotR = weekH * 0.060f
        val wSX   = weekH * 0.115f; val wSY = weekH * 0.098f; val wCG = weekH * 0.050f
        val natW  = DotFont.measureWidth(wStr, wDotR, wSX, wCG)
        val scl   = (rightW - gap * 2f) / natW
        val wDR   = wDotR * scl; val wStX = wSX * scl
        val wStY  = wSY  * scl; val wChG = wCG * scl

        val charElapsed  = weekElapsed * 21f
        val todayEndChar = (dow0 + 1) * 3f

        DotFont.draw(canvas, wStr, rightL + gap, weekH * 0.46f,
            wDR, wStX, wStY, wChG, paint) { ci, _ ->
            val cf = ci.toFloat()
            when {
                cf + 1f <= charElapsed -> COLOR_DIM
                cf      <  todayEndChar -> COLOR_RED
                else                   -> COLOR_BRIGHT
            }
        }
    }

    private fun drawCalendar(canvas: Canvas, paint: Paint,
                             l: Float, t: Float, w: Float, h: Float, gap: Float,
                             dayOfMonth: Int, month: Int, maxDay: Int,
                             monthPct: Int, weekStartDay: Int) {
        val monthNames = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
            "JUL","AUG","SEP","OCT","NOV","DEC")

        // Month label in red dot font, capped width
        val mStr   = monthNames[month]
        val maxLW  = w * 0.48f
        val mDotR  = h * 0.018f
        val mSX    = h * 0.034f; val mSY = h * 0.032f; val mCG = h * 0.015f
        val natW   = DotFont.measureWidth(mStr, mDotR, mSX, mCG)
        val mScl   = if (natW > maxLW) maxLW / natW else 1f
        val mDR    = mDotR * mScl; val mStX = mSX * mScl
        val mStY   = mSY   * mScl; val mChG = mCG * mScl
        val labelH = DotFont.measureHeight(mDR, mStY)
        DotFont.draw(canvas, mStr, l + gap, t + gap,
            mDR, mStX, mStY, mChG, paint) { _, _ -> COLOR_RED }

        // Month % in red dot font, top right
        val pStr  = "$monthPct%"
        val pDR   = mDR; val pSX = mStX; val pSY = mStY; val pCG = mChG
        val pNatW = DotFont.measureWidth(pStr, pDR, pSX, pCG)
        DotFont.draw(canvas, pStr, l + w - gap - pNatW, t + gap,
            pDR, pSX, pSY, pCG, paint) { _, _ -> COLOR_RED }

        // Day-of-week header — Sunday column in red
        // Build header order based on weekStartDay
        val allDays    = if (weekStartDay == 1)
            arrayOf("M","T","W","T","F","S","S")  // Mon start, Sun is last
        else
            arrayOf("S","M","T","W","T","F","S")  // Sun start

        val sunColIndex = if (weekStartDay == 1) 6 else 0

        val headerY = t + gap + labelH + h * 0.09f
        val cellW   = (w - gap * 2f) / 7f
        paint.textSize = h * 0.082f; paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.MONOSPACE
        allDays.forEachIndexed { i, d ->
            paint.color = if (i == sunColIndex) COLOR_SUN_RED else COLOR_LABEL
            canvas.drawText(d, l + gap + cellW * i + cellW / 2f, headerY, paint)
        }

        // Build calendar grid respecting weekStartDay
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDow1 = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat
        // Column of day 1 in our grid
        val firstCol = if (weekStartDay == 1) (firstDow1 + 6) % 7 else firstDow1

        val gridTop = headerY + h * 0.05f
        val cellH   = (h - (gridTop - t) - gap) / 6f
        paint.textSize = h * 0.090f

        for (d in 1..maxDay) {
            val idx     = firstCol + d - 1
            val col     = idx % 7
            val row     = idx / 7
            val cx2     = l + gap + cellW * col + cellW / 2f
            val cy2     = gridTop + cellH * row + cellH * 0.75f
            // Is this date in the Sunday column?
            val isSunCol = col == sunColIndex

            when {
                d == dayOfMonth -> {
                    // Option 2: red circle, white text, slightly bigger+bold
                    paint.color = COLOR_RED; paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx2, cy2 - h * 0.035f, cellW * 0.38f, paint)
                    paint.color    = COLOR_TODAY_TEXT
                    paint.textAlign = Paint.Align.CENTER
                    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    paint.textSize = h * 0.105f
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                    paint.typeface = Typeface.MONOSPACE
                    paint.textSize = h * 0.090f
                }
                d < dayOfMonth -> {
                    paint.color     = if (isSunCol) COLOR_SUN_RED.withAlpha(120) else COLOR_PAST_DATE
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
                else -> {
                    paint.color     = if (isSunCol) COLOR_SUN_RED else COLOR_WHITE
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
            }
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawYearBar(canvas: Canvas, paint: Paint,
                            l: Float, t: Float, w: Float, h: Float, gap: Float,
                            yearPct: Int, yearElapsed: Float, currentMonth: Int) {
        val cal = Calendar.getInstance()

        // Year label in red dot font + year number in white dot font
        val hdrDotR = h * 0.048f
        val hdrSX   = h * 0.090f; val hdrSY = h * 0.078f; val hdrCG = h * 0.038f
        val labelY  = t + gap

        // Year number in red dot font
        val yrStr = cal.get(Calendar.YEAR).toString()
        DotFont.draw(canvas, yrStr, l + gap, labelY,
            hdrDotR, hdrSX, hdrSY, hdrCG, paint) { _, _ -> COLOR_RED }

        // Year % in red dot font, top right
        val pStr  = "$yearPct%"
        val pNatW = DotFont.measureWidth(pStr, hdrDotR, hdrSX, hdrCG)
        DotFont.draw(canvas, pStr, l + w - gap - pNatW, labelY,
            hdrDotR, hdrSX, hdrSY, hdrCG, paint) { _, _ -> COLOR_RED }

        // Month name progress bar
        val yearLines    = arrayOf("JANFEBMAR", "APRMAYJUN", "JULAUGSEP", "OCTNOVDEC")
        val elapsedChars = yearElapsed * 36f
        val monthEndChar = (currentMonth + 1) * 3f

        val hdrH       = DotFont.measureHeight(hdrDotR, hdrSY)
        val barAreaH   = h - gap * 2f - hdrH - gap * 0.5f
        val lineH      = barAreaH / 4f
        val barY0      = t + gap + hdrH + gap * 0.5f

        val testStr = yearLines[0]
        val tDotR   = lineH * 0.105f; val tSX = lineH * 0.192f
        val tSY     = lineH * 0.162f; val tCG = lineH * 0.095f
        val natW    = DotFont.measureWidth(testStr, tDotR, tSX, tCG)
        val scl     = (w - gap * 2f) / natW
        val dDR     = tDotR * scl; val dSX = tSX * scl
        val dSY     = tSY   * scl; val dCG = tCG * scl

        yearLines.forEachIndexed { li, lineStr ->
            val lineCharStart = li * 9
            DotFont.draw(canvas, lineStr, l + gap, barY0 + li * lineH,
                dDR, dSX, dSY, dCG, paint) { ci, _ ->
                val g = (lineCharStart + ci).toFloat()
                when {
                    g + 1f <= elapsedChars -> COLOR_DIM
                    g      <  monthEndChar -> COLOR_RED
                    else                   -> COLOR_BRIGHT
                }
            }
        }
    }

    // Extension to set alpha on a color int
    private fun Int.withAlpha(alpha: Int): Int =
        (this and 0x00FFFFFF) or (alpha shl 24)
}
