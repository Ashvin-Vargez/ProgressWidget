package com.progresswidget

import android.content.Context
import android.graphics.*
import java.util.Calendar
import kotlin.math.*

object WidgetRenderer {

    private val COLOR_CARD      = Color.parseColor("#1E1E1E")
    private val COLOR_RED       = Color.parseColor("#E24B4A")
    private val COLOR_DIM       = Color.parseColor("#606060")
    private val COLOR_BRIGHT    = Color.parseColor("#D8D8D8")
    private val COLOR_LABEL     = Color.parseColor("#888888")
    private val COLOR_WHITE     = Color.parseColor("#FFFFFF")
    private val COLOR_PAST_DATE = Color.parseColor("#666666")
    private val COLOR_SUN_RED   = Color.parseColor("#E24B4A")

    private var globeBitmap: Bitmap? = null

    private fun scaleDotFont(text: String, maxWidth: Float, maxHeight: Float): FloatArray {
        var dotR    = maxHeight / 19.5f
        var stepX   = dotR * 2.5f
        var stepY   = dotR * 2.5f
        var charGap = dotR * 1.8f
        val natW    = DotFont.measureWidth(text, dotR, stepX, charGap)
        if (natW > maxWidth) {
            val s = maxWidth / natW
            dotR *= s; stepX *= s; stepY *= s; charGap *= s
        }
        return floatArrayOf(dotR, stepX, stepY, charGap)
    }

    fun render(
        context: Context,
        widthPx: Int, heightPx: Int,
        wakeH: Int, wakeM: Int,
        sleepH: Int, sleepM: Int,
        pulsePhase: Float,
        weekStartDay: Int,
        monthOffset: Int = 0
    ): Bitmap {
        val bmp    = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Single paint object — never call paint.reset(), set properties explicitly each time
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        val now    = Calendar.getInstance()
        val calNav = Calendar.getInstance()
        if (monthOffset != 0) calNav.add(Calendar.MONTH, monthOffset)

        val hour       = now.get(Calendar.HOUR_OF_DAY)
        val minute     = now.get(Calendar.MINUTE)
        val dayOfWeek  = now.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val month      = calNav.get(Calendar.MONTH)
        val dayOfYear  = now.get(Calendar.DAY_OF_YEAR)
        val maxDay     = calNav.getActualMaximum(Calendar.DAY_OF_MONTH)
        val maxDOY     = now.getActualMaximum(Calendar.DAY_OF_YEAR)
        val weekOfYear = now.get(Calendar.WEEK_OF_YEAR)
        val calDayOfMonth = if (monthOffset == 0) dayOfMonth else -1

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

        val dow0Sun = dayOfWeek - 1
        val dow0    = if (weekStartDay == 1) (dow0Sun + 6) % 7 else dow0Sun
        val weekElapsed  = (dow0 + nowMins / 1440f) / 7f
        val weekPct      = ((1f - weekElapsed) * 100f).toInt().coerceIn(0, 100)

        // Month % uses calNav
        val calDayForPct  = if (monthOffset == 0) dayOfMonth else 1
        val monthElapsed  = calDayForPct.toFloat() / maxDay
        val monthPct      = ((1f - monthElapsed) * 100f).toInt().coerceIn(0, 100)

        val yearElapsed  = dayOfYear.toFloat() / maxDOY
        val yearPct      = ((1f - yearElapsed) * 100f).toInt().coerceIn(0, 100)

        // ── Layout ───────────────────────────────────────────────────────────
        val W = widthPx.toFloat(); val H = heightPx.toFloat()
        val gap     = minOf(W, H) * 0.030f
        val cornerR = minOf(W, H) * 0.10f
        val pad     = gap * 0.9f

        val globeW = W * 0.385f; val globeH = H
        val rightL = globeW + gap; val rightW = W - rightL
        val weekH  = H * 0.285f
        val sqT    = weekH + gap; val sqH = H - sqT
        val sqW    = (rightW - gap) / 2f
        val calL   = rightL; val yearL = rightL + sqW + gap

        // Transparent background
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Cards
        drawCard(canvas, paint, 0f,    0f,  globeW, globeH, cornerR)
        drawCard(canvas, paint, rightL, 0f, rightW, weekH,  cornerR)
        drawCard(canvas, paint, calL,  sqT, sqW,    sqH,    cornerR)
        drawCard(canvas, paint, yearL, sqT, sqW,    sqH,    cornerR)

        // ── Globe ────────────────────────────────────────────────────────────
        drawGlobeImage(context, canvas, paint, 0f, 0f, globeW, globeH, dayElapsed, pulsePhase)

        // Day % — reference size for all percentages
        val pStr   = "$dayPct%"
        val pScale = scaleDotFont(pStr, globeW * 0.55f, globeH * 0.068f)
        val pW     = DotFont.measureWidth(pStr, pScale[0], pScale[1], pScale[3])
        val pH     = DotFont.measureHeight(pScale[0], pScale[2])
        val refDotR = pScale[0]  // ALL other labels/percentages use this size

        DotFont.draw(canvas, pStr, globeW/2f - pW/2f, globeH * 0.855f,
            pScale[0], pScale[1], pScale[2], pScale[3], paint, 1.25f) { _, _ -> COLOR_RED }

        paint.typeface = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.CENTER
        paint.color = COLOR_LABEL
        paint.textSize = pH * 0.60f
        canvas.drawText("AWAKE", globeW/2f, globeH*0.855f + pH + pH*0.3f, paint)

        // ── Week bar ─────────────────────────────────────────────────────────
        val hdrStX = refDotR * 2.5f; val hdrStY = refDotR * 2.5f; val hdrCG = refDotR * 1.8f
        val hdrH   = DotFont.measureHeight(refDotR, hdrStY)
        val hdrY   = pad

        val wLabelW = DotFont.measureWidth("W", refDotR, hdrStX, hdrCG)
        DotFont.draw(canvas, "W", rightL + pad, hdrY,
            refDotR, hdrStX, hdrStY, hdrCG, paint, 1.25f) { _, _ -> COLOR_RED }
        DotFont.draw(canvas, weekOfYear.toString(), rightL + pad + wLabelW + hdrCG, hdrY,
            refDotR, hdrStX, hdrStY, hdrCG, paint, 1.25f) { _, _ -> COLOR_WHITE }

        val wPctStr = "$weekPct%"
        val wPctW   = DotFont.measureWidth(wPctStr, refDotR, hdrStX, hdrCG)
        DotFont.draw(canvas, wPctStr, rightL + rightW - pad - wPctW, hdrY,
            refDotR, hdrStX, hdrStY, hdrCG, paint, 1.25f) { _, _ -> COLOR_RED }

        val wStr    = "MONTUEWEDTHUFRISATSUN"
        val barMaxH = weekH - hdrH - pad * 2.8f
        val barMaxW = rightW - pad * 2f
        val wScale  = scaleDotFont(wStr, barMaxW, barMaxH)
        val barY    = hdrY + hdrH + pad * 0.8f
        val charElapsed  = weekElapsed * 21f
        val todayEndChar = (dow0 + 1) * 3f

        DotFont.draw(canvas, wStr, rightL + pad, barY,
            wScale[0], wScale[1], wScale[2], wScale[3], paint, 1.35f) { ci, _ ->
            val cf = ci.toFloat()
            when {
                cf + 1f <= charElapsed -> COLOR_DIM
                cf      <  todayEndChar -> COLOR_RED
                else                    -> COLOR_BRIGHT
            }
        }

        // ── Calendar ─────────────────────────────────────────────────────────
        drawCalendar(canvas, paint, calL, sqT, sqW, sqH, pad, refDotR,
            calDayOfMonth, month, maxDay, monthPct, weekStartDay, calNav, monthOffset)

        // ── Year bar ─────────────────────────────────────────────────────────
        drawYearBar(canvas, paint, yearL, sqT, sqW, sqH, pad, refDotR,
            yearPct, yearElapsed, now.get(Calendar.MONTH))

        return bmp
    }

    private fun drawCard(canvas: Canvas, paint: Paint,
                         l: Float, t: Float, w: Float, h: Float, r: Float) {
        paint.color = COLOR_CARD; paint.style = Paint.Style.FILL
        paint.alpha = 255
        canvas.drawRoundRect(l, t, l + w, t + h, r, r, paint)
    }

    private fun drawGlobeImage(
        context: Context, canvas: Canvas, paint: Paint,
        cardL: Float, cardT: Float, cardW: Float, cardH: Float,
        dayElapsed: Float, pulsePhase: Float
    ) {
        // Safe bitmap load using R.drawable directly
        if (globeBitmap == null || globeBitmap!!.isRecycled) {
            try {
                globeBitmap = BitmapFactory.decodeResource(
                    context.resources, R.drawable.globe_white)
            } catch (e: Exception) {
                globeBitmap = null
            }
        }
        val gb = globeBitmap ?: return

        val size = minOf(cardW, cardH) * 0.82f
        val left = cardL + (cardW - size) / 2f
        val top  = cardT + cardH * 0.04f
        val dst  = RectF(left, top, left + size, top + size)

        // Draw white globe image
        paint.alpha = 255; paint.style = Paint.Style.FILL
        canvas.drawBitmap(gb, null, dst, paint)

        // Dark shadow sweeping left→right
        if (dayElapsed > 0.01f) {
            val terminatorX = left + size * dayElapsed
            paint.color = Color.argb(210, 0, 0, 0)
            canvas.drawRect(left, top, minOf(terminatorX, left + size), top + size, paint)
        }

        // Red terminator bezier arc (D-shape: bows right)
        if (dayElapsed > 0.02f && dayElapsed < 0.98f) {
            val terminatorX = left + size * dayElapsed
            val cy  = top + size / 2f
            val bow = size * 0.08f
            val arcPath = Path()
            arcPath.moveTo(terminatorX, top + size * 0.05f)
            arcPath.cubicTo(
                terminatorX + bow, cy - size * 0.25f,
                terminatorX + bow, cy + size * 0.25f,
                terminatorX, top + size * 0.95f
            )
            val pulseAlpha = (80 + 175 * sin(pulsePhase * Math.PI).toFloat())
                .toInt().coerceIn(80, 255)
            paint.style      = Paint.Style.STROKE
            paint.strokeWidth = size * 0.022f
            paint.strokeCap  = Paint.Cap.ROUND
            paint.color      = Color.argb(pulseAlpha, 226, 75, 74)
            canvas.drawPath(arcPath, paint)
            // Reset stroke
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
        }
        paint.alpha = 255
    }

    private fun drawCalendar(
        canvas: Canvas, paint: Paint,
        l: Float, t: Float, w: Float, h: Float, pad: Float,
        refDotR: Float,
        dayOfMonth: Int, month: Int, maxDay: Int,
        monthPct: Int, weekStartDay: Int,
        calNav: Calendar, monthOffset: Int
    ) {
        val monthNames = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
            "JUL","AUG","SEP","OCT","NOV","DEC")
        val hdrStX = refDotR * 2.5f; val hdrStY = refDotR * 2.5f; val hdrCG = refDotR * 1.8f
        val hdrH   = DotFont.measureHeight(refDotR, hdrStY)

        // Month name red dot font
        val mStr   = monthNames[month]
        val mScale = scaleDotFont(mStr, w * 0.50f, hdrH * 1.1f)
        DotFont.draw(canvas, mStr, l + pad, t + pad,
            mScale[0], mScale[1], mScale[2], mScale[3], paint, 1.25f) { _, _ -> COLOR_RED }

        // Month % right-aligned
        val pStr = "$monthPct%"
        val pW   = DotFont.measureWidth(pStr, refDotR, hdrStX, hdrCG)
        DotFont.draw(canvas, pStr, l + w - pad - pW, t + pad,
            refDotR, hdrStX, hdrStY, hdrCG, paint, 1.25f) { _, _ -> COLOR_RED }

        // Nav arrows ‹ › next to the %
        paint.typeface  = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize  = hdrH * 0.85f
        paint.alpha     = 180
        paint.color     = COLOR_RED
        val arrowX = l + w - pad - pW - refDotR * 1.5f
        canvas.drawText("‹", arrowX, t + pad + hdrH * 0.85f, paint)
        if (monthOffset < 0) {
            canvas.drawText("›", arrowX + refDotR * 3f, t + pad + hdrH * 0.85f, paint)
        }
        paint.alpha = 255

        // Day-of-week header
        val sunCol  = if (weekStartDay == 1) 6 else 0
        val dayHdrs = if (weekStartDay == 1)
            arrayOf("M","T","W","T","F","S","S") else arrayOf("S","M","T","W","T","F","S")
        val cellW   = (w - pad * 2f) / 7f
        val headerY = t + pad + hdrH * 1.3f
        val cellH   = (h - (headerY - t) - pad) / 7.2f

        paint.typeface = Typeface.MONOSPACE; paint.textAlign = Paint.Align.CENTER
        paint.textSize = cellH * 0.72f
        dayHdrs.forEachIndexed { i, d ->
            paint.color = if (i == sunCol) COLOR_SUN_RED else COLOR_LABEL
            canvas.drawText(d, l + pad + cellW * i + cellW / 2f, headerY, paint)
        }

        // Date grid — use calNav for first day of navigated month
        calNav.set(Calendar.DAY_OF_MONTH, 1)
        val fd1      = calNav.get(Calendar.DAY_OF_WEEK) - 1
        val firstCol = if (weekStartDay == 1) (fd1 + 6) % 7 else fd1
        val gridTop  = headerY + cellH * 0.85f
        paint.textSize = cellH * 0.72f

        for (d in 1..maxDay) {
            val idx  = firstCol + d - 1
            val col  = idx % 7; val row = idx / 7
            val cx2  = l + pad + cellW * col + cellW / 2f
            val cy2  = gridTop + cellH * row + cellH * 0.72f
            val isSun = col == sunCol

            when {
                d == dayOfMonth -> {
                    paint.color = COLOR_RED; paint.style = Paint.Style.FILL; paint.alpha = 255
                    canvas.drawCircle(cx2, cy2 - cellH * 0.33f, cellW * 0.37f, paint)
                    paint.color    = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    paint.textSize = cellH * 0.82f
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                    paint.typeface = Typeface.MONOSPACE; paint.textSize = cellH * 0.72f
                }
                d < dayOfMonth && monthOffset == 0 -> {
                    paint.color = if (isSun) Color.argb(160, 226, 75, 74) else COLOR_PAST_DATE
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
                else -> {
                    paint.color = if (isSun) COLOR_SUN_RED else COLOR_WHITE
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(d.toString(), cx2, cy2, paint)
                }
            }
            paint.style = Paint.Style.FILL; paint.alpha = 255
        }
    }

    private fun drawYearBar(
        canvas: Canvas, paint: Paint,
        l: Float, t: Float, w: Float, h: Float, pad: Float,
        refDotR: Float,
        yearPct: Int, yearElapsed: Float, currentMonth: Int
    ) {
        val cal    = Calendar.getInstance()
        val hdrStX = refDotR * 2.5f; val hdrStY = refDotR * 2.5f; val hdrCG = refDotR * 1.8f
        val hdrH   = DotFont.measureHeight(refDotR, hdrStY)

        val yrStr = cal.get(Calendar.YEAR).toString()
        DotFont.draw(canvas, yrStr, l + pad, t + pad,
            refDotR, hdrStX, hdrStY, hdrCG, paint, 1.25f) { _, _ -> COLOR_RED }

        val pStr = "$yearPct%"
        val pW   = DotFont.measureWidth(pStr, refDotR, hdrStX, hdrCG)
        DotFont.draw(canvas, pStr, l + w - pad - pW, t + pad,
            refDotR, hdrStX, hdrStY, hdrCG, paint, 1.25f) { _, _ -> COLOR_RED }

        val yearLines    = arrayOf("JANFEBMAR","APRMAYJUN","JULAUGSEP","OCTNOVDEC")
        val elapsedChars = yearElapsed * 36f
        val monthEndChar = (currentMonth + 1) * 3f

        val barAreaH = h - pad - hdrH * 1.3f - pad
        val lineH    = barAreaH / 4f
        val barY0    = t + pad + hdrH * 1.3f
        val barW     = w - pad * 2f

        yearLines.forEachIndexed { li, lineStr ->
            val s = scaleDotFont(lineStr, barW, lineH * 0.82f)
            val lineCharStart = li * 9
            DotFont.draw(canvas, lineStr, l + pad, barY0 + li * lineH,
                s[0], s[1], s[2], s[3], paint, 1.35f) { ci, _ ->
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
