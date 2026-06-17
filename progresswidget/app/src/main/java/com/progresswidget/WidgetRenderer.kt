package com.progresswidget

import android.graphics.*
import java.util.Calendar
import kotlin.math.*

object WidgetRenderer {

    private val COLOR_CARD       = Color.parseColor("#1E1E1E")
    private val COLOR_DOT_LAND   = Color.parseColor("#E8E8E8")
    private val COLOR_DOT_OCEAN  = Color.parseColor("#3A3A3A")
    private val COLOR_DOT_SHADOW = Color.parseColor("#1A1A1A")
    private val COLOR_RED        = Color.parseColor("#E24B4A")
    private val COLOR_DIM        = Color.parseColor("#3A3A3A")
    private val COLOR_BRIGHT     = Color.parseColor("#D8D8D8")
    private val COLOR_LABEL      = Color.parseColor("#777777")
    private val COLOR_WHITE      = Color.parseColor("#FFFFFF")
    private val COLOR_PAST_DATE  = Color.parseColor("#444444")
    private val COLOR_SUN_RED    = Color.parseColor("#E24B4A")

    // Scale a dot-font to exactly fit within maxWidth x maxHeight,
    // returning (dotR, stepX, stepY, charGap) — all in pixels.
    // Base proportions: stepX = 2.6*dotR, stepY = 2.6*dotR, charGap = 2.0*dotR
    private fun scaleDotFont(text: String, maxWidth: Float, maxHeight: Float)
            : FloatArray {
        // Start with height-constrained size: 7 rows of dots fit in maxHeight
        // maxHeight = 7*stepY + 2*dotR = 7*(2.6r) + 2r = 20.2r → r = maxHeight/20.2
        var dotR    = maxHeight / 20.2f
        var stepX   = dotR * 2.6f
        var stepY   = dotR * 2.6f
        var charGap = dotR * 2.0f
        val natW    = DotFont.measureWidth(text, dotR, stepX, charGap)
        if (natW > maxWidth) {
            val s    = maxWidth / natW
            dotR    *= s; stepX *= s; stepY *= s; charGap *= s
        }
        return floatArrayOf(dotR, stepX, stepY, charGap)
    }

    fun render(
        widthPx: Int, heightPx: Int,
        wakeH: Int, wakeM: Int,
        sleepH: Int, sleepM: Int,
        pulsePhase: Float,
        weekStartDay: Int   // 0=Sun, 1=Mon
    ): Bitmap {
        val bmp    = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

        val now        = Calendar.getInstance()
        val hour       = now.get(Calendar.HOUR_OF_DAY)
        val minute     = now.get(Calendar.MINUTE)
        val dayOfWeek  = now.get(Calendar.DAY_OF_WEEK)   // 1=Sun
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
        val dayPct = ((1f - dayElapsed) * 100f).toInt().coerceIn(0, 100)

        val dow0Sun  = dayOfWeek - 1
        val dow0     = if (weekStartDay == 1) (dow0Sun + 6) % 7 else dow0Sun
        val weekElapsed  = (dow0 + nowMins / 1440f) / 7f
        val weekPct      = ((1f - weekElapsed) * 100f).toInt().coerceIn(0, 100)
        val monthElapsed = dayOfMonth.toFloat() / maxDay
        val monthPct     = ((1f - monthElapsed) * 100f).toInt().coerceIn(0, 100)
        val yearElapsed  = dayOfYear.toFloat() / maxDOY
        val yearPct      = ((1f - yearElapsed) * 100f).toInt().coerceIn(0, 100)

        // ── Layout ────────────────────────────────────────────────────────────
        val W = widthPx.toFloat()
        val H = heightPx.toFloat()
        val gap     = minOf(W, H) * 0.030f
        val cornerR = minOf(W, H) * 0.10f

        val globeW = W * 0.385f;  val globeH = H
        val rightL = globeW + gap; val rightW = W - rightL
        val weekH  = H * 0.285f
        val sqT    = weekH + gap;  val sqH = H - sqT
        val sqW    = (rightW - gap) / 2f
        val calL   = rightL;       val yearL = rightL + sqW + gap

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawCard(canvas, paint, 0f,    0f,  globeW, globeH, cornerR)
        drawCard(canvas, paint, rightL, 0f, rightW, weekH,  cornerR)
        drawCard(canvas, paint, calL,  sqT, sqW,    sqH,    cornerR)
        drawCard(canvas, paint, yearL, sqT, sqW,    sqH,    cornerR)

        // ── Globe ─────────────────────────────────────────────────────────────
        val gcx = globeW / 2f
        val gcy = globeH * 0.43f
        val GR  = minOf(globeW * 0.82f, globeH * 0.40f)
        drawGlobe(canvas, paint, gcx, gcy, GR, dayElapsed, pulsePhase)

        // Day % below globe — fit in 60% of globe width, 8% of globe height
        val pStr   = "$dayPct%"
        val pScale = scaleDotFont(pStr, globeW * 0.60f, globeH * 0.075f)
        val pW     = DotFont.measureWidth(pStr, pScale[0], pScale[1], pScale[3])
        val pH     = DotFont.measureHeight(pScale[0], pScale[2])
        DotFont.draw(canvas, pStr, gcx - pW / 2f, globeH * 0.855f,
            pScale[0], pScale[1], pScale[2], pScale[3], paint) { _, _ -> COLOR_RED }

        paint.typeface  = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.CENTER
        paint.color     = COLOR_LABEL
        paint.textSize  = globeH * 0.040f
        canvas.drawText("AWAKE", gcx, globeH * 0.855f + pH + globeH * 0.025f, paint)

        // ── Week bar ──────────────────────────────────────────────────────────
        val innerPad = gap * 0.9f

        // Header row: "W25" (W=red, 25=white) left | "61%" (red) right
        // All fit in top 30% of weekH
        val hdrMaxH = weekH * 0.28f
        val hdrMaxW = rightW * 0.40f  // each side gets ~40%

        val wLabelScale = scaleDotFont("W", hdrMaxW * 0.25f, hdrMaxH)
        val wnStr  = weekOfYear.toString()
        val wnScale = scaleDotFont(wnStr, hdrMaxW * 0.70f, hdrMaxH)
        // Use the smaller of the two dotR so they match height
        val hdrDotR  = minOf(wLabelScale[0], wnScale[0])
        val hdrStepX = hdrDotR * 2.6f
        val hdrStepY = hdrDotR * 2.6f
        val hdrCG    = hdrDotR * 2.0f

        val hdrY     = innerPad
        val wLabelW  = DotFont.measureWidth("W", hdrDotR, hdrStepX, hdrCG)
        DotFont.draw(canvas, "W", rightL + innerPad, hdrY,
            hdrDotR, hdrStepX, hdrStepY, hdrCG, paint) { _, _ -> COLOR_RED }
        DotFont.draw(canvas, wnStr, rightL + innerPad + wLabelW + hdrCG, hdrY,
            hdrDotR, hdrStepX, hdrStepY, hdrCG, paint) { _, _ -> COLOR_WHITE }

        val pctStr = "$weekPct%"
        val pctScale = scaleDotFont(pctStr, rightW * 0.38f, hdrMaxH)
        val pctDotR  = minOf(pctScale[0], hdrDotR)
        val pctStX   = pctDotR * 2.6f; val pctStY = pctDotR * 2.6f; val pctCG = pctDotR * 2.0f
        val pctW2    = DotFont.measureWidth(pctStr, pctDotR, pctStX, pctCG)
        DotFont.draw(canvas, pctStr, rightL + rightW - innerPad - pctW2, hdrY,
            pctDotR, pctStX, pctStY, pctCG, paint) { _, _ -> COLOR_RED }

        // Week progress bar: MONTUEWEDTHUFRISATSUN fitted in remaining height
        val wStr     = "MONTUEWEDTHUFRISATSUN"
        val barMaxH  = weekH * 0.52f
        val barMaxW  = rightW - innerPad * 2f
        val wScale   = scaleDotFont(wStr, barMaxW, barMaxH)
        val hdrH2    = DotFont.measureHeight(hdrDotR, hdrStepY)
        val barY     = hdrY + hdrH2 + gap * 0.6f

        val charElapsed  = weekElapsed * 21f
        val todayEndChar = (dow0 + 1) * 3f
        DotFont.draw(canvas, wStr, rightL + innerPad, barY,
            wScale[0], wScale[1], wScale[2], wScale[3], paint) { ci, _ ->
            val cf = ci.toFloat()
            when {
                cf + 1f <= charElapsed -> COLOR_DIM
                cf      <  todayEndChar -> COLOR_RED
                else                    -> COLOR_BRIGHT
            }
        }

        // ── Calendar ──────────────────────────────────────────────────────────
        drawCalendar(canvas, paint, calL, sqT, sqW, sqH, innerPad,
            dayOfMonth, month, maxDay, monthPct, weekStartDay)

        // ── Year bar ──────────────────────────────────────────────────────────
        drawYearBar(canvas, paint, yearL, sqT, sqW, sqH, innerPad,
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
        val termNX = 2f * dayElapsed - 1f
        val dotR   = R * 0.022f
        paint.style = Paint.Style.FILL
        for (dot in GlobeDotMap.dots) {
            val px = cx + dot.normX * R
            val py = cy + dot.normY * R
            paint.color = when {
                dot.normX < termNX -> COLOR_DOT_SHADOW
                dot.isLand          -> COLOR_DOT_LAND
                else                -> COLOR_DOT_OCEAN
            }
            canvas.drawCircle(px, py, dotR, paint)
        }
        val band = 0.06f
        val arc  = GlobeDotMap.dots
            .filter { it.normX >= termNX && it.normX < termNX + band }
            .map { Pair(cx + it.normX * R, cy + it.normY * R) }
            .sortedBy { it.second }
        val fill = (arc.size * pulsePhase).toInt()
        paint.color = COLOR_RED
        arc.take(fill).forEach { canvas.drawCircle(it.first, it.second, dotR * 1.3f, paint) }
    }

    private fun drawCalendar(canvas: Canvas, paint: Paint,
                             l: Float, t: Float, w: Float, h: Float, pad: Float,
                             dayOfMonth: Int, month: Int, maxDay: Int,
                             monthPct: Int, weekStartDay: Int) {
        val monthNames = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
            "JUL","AUG","SEP","OCT","NOV","DEC")

        // Month label + pct on same row, sharing top 18% of card height
        val hdrH   = h * 0.16f
        val mStr   = monthNames[month]
        val mScale = scaleDotFont(mStr, w * 0.46f, hdrH)
        val mH     = DotFont.measureHeight(mScale[0], mScale[2])
        DotFont.draw(canvas, mStr, l + pad, t + pad,
            mScale[0], mScale[1], mScale[2], mScale[3], paint) { _, _ -> COLOR_RED }

        val pStr   = "$monthPct%"
        val pScale = scaleDotFont(pStr, w * 0.38f, hdrH)
        val pDotR  = minOf(mScale[0], pScale[0])
        val pStX   = pDotR * 2.6f; val pStY = pDotR * 2.6f; val pCG2 = pDotR * 2.0f
        val pW     = DotFont.measureWidth(pStr, pDotR, pStX, pCG2)
        DotFont.draw(canvas, pStr, l + w - pad - pW, t + pad,
            pDotR, pStX, pStY, pCG2, paint) { _, _ -> COLOR_RED }

        // Day-of-week header
        val sunCol  = if (weekStartDay == 1) 6 else 0
        val dayHdrs = if (weekStartDay == 1)
            arrayOf("M","T","W","T","F","S","S") else arrayOf("S","M","T","W","T","F","S")
        val headerY = t + pad + mH + h * 0.04f
        val cellW   = (w - pad * 2f) / 7f
        val cellH   = (h - (headerY - t) - pad * 0.5f - h * 0.04f) / 7f

        paint.typeface  = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize  = cellH * 0.72f
        dayHdrs.forEachIndexed { i, d ->
            paint.color = if (i == sunCol) COLOR_SUN_RED else COLOR_LABEL
            canvas.drawText(d, l + pad + cellW * i + cellW / 2f, headerY, paint)
        }

        // Date grid
        val cal      = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val fd1      = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sun
        val firstCol = if (weekStartDay == 1) (fd1 + 6) % 7 else fd1
        val gridTop  = headerY + cellH * 0.9f

        for (d in 1..maxDay) {
            val idx  = firstCol + d - 1
            val col  = idx % 7; val row = idx / 7
            val cx2  = l + pad + cellW * col + cellW / 2f
            val cy2  = gridTop + cellH * row + cellH * 0.72f
            val isSun = col == sunCol
            when {
                d == dayOfMonth -> {
                    paint.color = COLOR_RED; paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx2, cy2 - cellH * 0.35f, cellW * 0.37f, paint)
                    paint.color    = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    paint.textSize = cellH * 0.82f
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                    paint.typeface = Typeface.MONOSPACE
                    paint.textSize = cellH * 0.72f
                }
                d < dayOfMonth -> {
                    paint.color = if (isSun) 0x88E24B4A.toInt() else COLOR_PAST_DATE
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
                else -> {
                    paint.color = if (isSun) COLOR_SUN_RED else COLOR_WHITE
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
            }
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawYearBar(canvas: Canvas, paint: Paint,
                            l: Float, t: Float, w: Float, h: Float, pad: Float,
                            yearPct: Int, yearElapsed: Float, currentMonth: Int) {
        val cal = Calendar.getInstance()

        // Year number + pct in header row
        val hdrH   = h * 0.16f
        val yrStr  = cal.get(Calendar.YEAR).toString()
        val yScale = scaleDotFont(yrStr, w * 0.46f, hdrH)
        DotFont.draw(canvas, yrStr, l + pad, t + pad,
            yScale[0], yScale[1], yScale[2], yScale[3], paint) { _, _ -> COLOR_RED }

        val pStr   = "$yearPct%"
        val pDotR  = minOf(yScale[0], scaleDotFont(pStr, w * 0.38f, hdrH)[0])
        val pStX   = pDotR * 2.6f; val pStY = pDotR * 2.6f; val pCG = pDotR * 2.0f
        val pW     = DotFont.measureWidth(pStr, pDotR, pStX, pCG)
        DotFont.draw(canvas, pStr, l + w - pad - pW, t + pad,
            pDotR, pStX, pStY, pCG, paint) { _, _ -> COLOR_RED }

        // 4-line month progress bar fills remaining height
        val hdrH2    = DotFont.measureHeight(yScale[0], yScale[2])
        val barAreaH = h - pad - hdrH2 - pad * 1.5f
        val lineH    = barAreaH / 4f
        val barY0    = t + pad + hdrH2 + pad * 0.5f
        val barW     = w - pad * 2f

        val yearLines    = arrayOf("JANFEBMAR","APRMAYJUN","JULAUGSEP","OCTNOVDEC")
        val elapsedChars = yearElapsed * 36f
        val monthEndChar = (currentMonth + 1) * 3f

        yearLines.forEachIndexed { li, lineStr ->
            val s    = scaleDotFont(lineStr, barW, lineH * 0.85f)
            val lineCharStart = li * 9
            DotFont.draw(canvas, lineStr, l + pad, barY0 + li * lineH,
                s[0], s[1], s[2], s[3], paint) { ci, _ ->
                val g = (lineCharStart + ci).toFloat()
                when {
                    g + 1f <= elapsedChars -> COLOR_DIM
                    g      <  monthEndChar -> COLOR_RED
                    else                   -> COLOR_BRIGHT
                }
            }
        }
    }
}
