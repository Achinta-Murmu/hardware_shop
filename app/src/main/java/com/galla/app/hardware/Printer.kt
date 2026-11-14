package com.galla.app.hardware

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.print.PrintHelper
import com.galla.app.core.util.Codes
import com.galla.app.core.util.Dates
import com.galla.app.core.util.Money
import com.galla.app.core.util.Qty
import com.galla.app.data.local.db.ItemEntity
import com.galla.app.data.local.db.SaleEntity
import com.galla.app.data.local.db.SaleLineEntity

/**
 * Stickers and receipts are drawn as pictures and handed to Android's own print screen,
 * which works with any printer the phone knows (Wi-Fi, Bluetooth, or "Save as PDF").
 */
object Printer {

    private fun textPaint(size: Float, bold: Boolean = false): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun fit(text: String, paint: TextPaint, width: Float): String =
        TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END).toString()

    /** A 50 x 30 mm sticker: QR code on the left, name, code and price on the right. */
    fun labelBitmap(shopName: String, item: ItemEntity): Bitmap {
        val width = 600
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val qr = QrCodes.bitmap(Codes.qrPayload(item.code), 320)
        canvas.drawBitmap(qr, 16f, (height - qr.height) / 2f, null)

        val left = 16f + qr.width + 12f
        val room = width - left - 16f
        val small = textPaint(22f)
        val name = textPaint(30f, bold = true)
        val price = textPaint(44f, bold = true)

        var y = 60f
        if (shopName.isNotBlank()) {
            canvas.drawText(fit(shopName.uppercase(), small, room), left, y, small)
            y += 42f
        }
        // The name can take two lines.
        val words = item.name.split(" ")
        val firstLine = StringBuilder()
        var index = 0
        while (index < words.size && name.measureText(firstLine.toString() + " " + words[index]) <= room) {
            if (firstLine.isNotEmpty()) firstLine.append(' ')
            firstLine.append(words[index])
            index++
        }
        if (firstLine.isEmpty() && words.isNotEmpty()) {
            firstLine.append(words[0])
            index = 1
        }
        canvas.drawText(fit(firstLine.toString(), name, room), left, y, name)
        y += 38f
        val rest = words.drop(index).joinToString(" ")
        if (rest.isNotEmpty()) {
            canvas.drawText(fit(rest, name, room), left, y, name)
            y += 38f
        }
        y += 22f
        canvas.drawText(fit(Money.format(item.pricePaise), price, room), left, y, price)
        canvas.drawText(item.code, left, height - 34f, small)
        return bitmap
    }

    /** A receipt for an 80 mm roll printer. It gets taller with every line. */
    fun receiptBitmap(shopName: String, sale: SaleEntity, lines: List<SaleLineEntity>): Bitmap {
        val width = 576
        val pad = 24f
        val height = 300 + lines.size * 78 + 130
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val title = textPaint(40f, bold = true).apply { textAlign = Paint.Align.CENTER }
        val centered = textPaint(24f).apply { textAlign = Paint.Align.CENTER }
        val body = textPaint(26f)
        val bold = textPaint(26f, bold = true)
        val right = textPaint(26f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val total = textPaint(40f, bold = true)
        val totalRight = textPaint(40f, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val rule = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
        }

        var y = 70f
        canvas.drawText(fit(shopName.ifBlank { "Receipt" }, title, width - pad * 2), width / 2f, y, title)
        y += 44f
        canvas.drawText("Bill no. ${sale.id}", width / 2f, y, centered)
        y += 34f
        canvas.drawText(Dates.full(sale.at), width / 2f, y, centered)
        y += 30f
        canvas.drawLine(pad, y, width - pad, y, rule)
        y += 46f

        lines.forEach { line ->
            canvas.drawText(fit(line.name, bold, width - pad * 2), pad, y, bold)
            y += 34f
            canvas.drawText("${Qty.withUnit(line.qtyMilli, line.unit)} x ${Money.format(line.pricePaise)}", pad, y, body)
            canvas.drawText(Money.format(line.totalPaise), width - pad, y, right)
            y += 44f
        }

        canvas.drawLine(pad, y - 14f, width - pad, y - 14f, rule)
        y += 44f
        canvas.drawText("Total", pad, y, total)
        canvas.drawText(Money.format(sale.totalPaise), width - pad, y, totalRight)
        y += 60f
        canvas.drawText("Thank you", width / 2f, y, centered)
        return bitmap
    }

    /** The same receipt as plain text, for sending by message. */
    fun receiptText(shopName: String, sale: SaleEntity, lines: List<SaleLineEntity>): String = buildString {
        appendLine(shopName.ifBlank { "Receipt" })
        appendLine("Bill no. ${sale.id} · ${Dates.full(sale.at)}")
        appendLine()
        lines.forEach { line ->
            appendLine(line.name)
            appendLine("  ${Qty.withUnit(line.qtyMilli, line.unit)} x ${Money.format(line.pricePaise)} = ${Money.format(line.totalPaise)}")
        }
        appendLine()
        appendLine("Total: ${Money.format(sale.totalPaise)}")
        append("Thank you")
    }

    /** Opens Android's print screen with this picture. Returns false if the phone cannot print. */
    fun print(context: Context, jobName: String, bitmap: Bitmap): Boolean {
        if (!PrintHelper.systemSupportsPrint()) return false
        return try {
            PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }.printBitmap(jobName, bitmap)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Opens Android's share sheet with some text (a receipt, a restock list). */
    fun shareText(context: Context, title: String, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(send, title)) }
    }
}
