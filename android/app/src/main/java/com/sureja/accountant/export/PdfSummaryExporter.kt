package com.sureja.accountant.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.sureja.accountant.data.local.TransactionListItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** A shareable household summary generated entirely from confirmed local transactions. */
@Singleton
class PdfSummaryExporter @Inject constructor(@ApplicationContext private val context: Context) {
    fun share(items: List<TransactionListItem>) {
        val document = PdfDocument()
        try {
            val months = items.groupBy { it.occurredAt.take(7) }.toSortedMap(reverseOrder())
            if (months.isEmpty()) drawMonth(document, "No expenses", emptyList(), 1)
            else months.entries.forEachIndexed { index, (month, transactions) ->
                val title = runCatching { YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)) }.getOrDefault(month)
                drawMonth(document, title, transactions, index + 1)
            }
            val directory = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(directory, "accountant-summary.pdf")
            file.outputStream().use(document::writeTo)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share expense summary").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } finally {
            document.close()
        }
    }

    private fun drawMonth(document: PdfDocument, title: String, items: List<TransactionListItem>, pageNumber: Int) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        val canvas = page.canvas
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 23, 42); textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 23, 42); textSize = 12f }
        val muted = Paint(body).apply { color = Color.rgb(100, 116, 139); textSize = 10f }
        val amount = Paint(body).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val line = Paint().apply { color = Color.rgb(226, 232, 240); strokeWidth = 1f }
        val margin = 44f
        var y = 54f
        canvas.drawText("Accountant", margin, y, heading)
        y += 28f
        canvas.drawText("Household expense summary · $title", margin, y, body)
        y += 20f
        canvas.drawLine(margin, y, 551f, y, line)
        y += 30f
        canvas.drawText("Total spending", margin, y, muted)
        canvas.drawText(formatMoney(items.sumOf { it.amountPaise }), 551f, y + 3f, amount)
        y += 23f
        canvas.drawText("${items.size} confirmed expense${if (items.size == 1) "" else "s"}", margin, y, muted)
        y += 36f
        fun section(label: String, rows: List<Pair<String, Long>>) {
            canvas.drawText(label, margin, y, heading.apply { textSize = 16f })
            y += 24f
            if (rows.isEmpty()) {
                canvas.drawText("No expenses", margin, y, muted)
                y += 24f
            }
            rows.forEach { (name, value) ->
                val clipped = if (name.length > 46) name.take(43) + "…" else name
                canvas.drawText(clipped, margin, y, body)
                canvas.drawText(formatMoney(value), 551f, y, amount)
                y += 25f
            }
            y += 15f
        }
        section("By person", items.groupBy { it.memberName }.mapValues { (_, expenses) -> expenses.sumOf { it.amountPaise } }.toList().sortedByDescending { it.second })
        val categories = items.groupBy { it.categoryName ?: "Not categorized" }.mapValues { (_, expenses) -> expenses.sumOf { it.amountPaise } }.toList().sortedByDescending { it.second }
        val shownCategories = if (categories.size <= 15) categories else categories.take(14) + ("Remaining ${categories.size - 14} categories" to categories.drop(14).sumOf { it.second })
        section("By category", shownCategories)
        canvas.drawLine(margin, 804f, 551f, 804f, line)
        canvas.drawText("Generated from expenses stored on this device", margin, 821f, muted)
        document.finishPage(page)
    }

    private fun formatMoney(paise: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        .apply { maximumFractionDigits = if (paise % 100 == 0L) 0 else 2 }
        .format(paise / 100.0)
}
