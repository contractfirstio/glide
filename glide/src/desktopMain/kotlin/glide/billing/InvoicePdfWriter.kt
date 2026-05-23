package glide.billing

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object InvoicePdfWriter {
    private const val MARGIN = 50f
    private const val LINE_GAP = 5f
    private const val AMOUNT_COLUMN_WIDTH = 90f
    private const val COLUMN_GAP = 12f
    private val fileDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val fontRegular = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val fontBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)

    fun write(content: InvoiceContent, billId: String): File {
        val directory = invoiceDirectory()
        directory.mkdirs()
        val file = File(directory, "invoice-${fileDateFormat.format(LocalDate.now())}-${content.invoiceNumber}.pdf")

        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            val pageWidth = page.mediaBox.width
            val amountRightX = pageWidth - MARGIN
            val descriptionMaxWidth = amountRightX - MARGIN - AMOUNT_COLUMN_WIDTH - COLUMN_GAP

            PDPageContentStream(document, page).use { stream ->
                var y = page.mediaBox.upperRightY - MARGIN

                y = drawLine(stream, fontBold, 22f, MARGIN, y, "INVOICE")
                y -= LINE_GAP * 2

                y = drawLine(stream, fontBold, 12f, MARGIN, y, INVOICE_FROM_NAME)
                y = drawLine(stream, fontRegular, 10f, MARGIN, y, "Invoice no. ${content.invoiceNumber}")
                y = drawLine(stream, fontRegular, 10f, MARGIN, y, "Issue date: ${content.issuedDateLabel}")
                y = drawLine(stream, fontRegular, 10f, MARGIN, y, "Due: ${content.dueDateLabel}")
                y = drawLine(stream, fontRegular, 10f, MARGIN, y, "Status: ${content.status.label}")
                y -= LINE_GAP * 2

                y = drawLine(stream, fontBold, 11f, MARGIN, y, "Bill to")
                y = drawLine(stream, fontRegular, 10f, MARGIN, y, content.billToName)
                if (content.billToEmail.isNotBlank()) {
                    y = drawLine(stream, fontRegular, 10f, MARGIN, y, content.billToEmail)
                }
                if (content.billToPhone.isNotBlank()) {
                    y = drawLine(stream, fontRegular, 10f, MARGIN, y, content.billToPhone)
                }
                y -= LINE_GAP * 2

                val headerY = y
                drawAt(stream, fontBold, 10f, MARGIN, headerY, "Description")
                drawAtRight(stream, fontBold, 10f, amountRightX, headerY, "Amount")
                y = headerY - lineStep(10f)
                drawHorizontalRule(stream, MARGIN, amountRightX, y)
                y -= LINE_GAP * 2

                y = drawTableRow(
                    stream = stream,
                    font = fontRegular,
                    fontSize = 10f,
                    y = y,
                    description = content.packLineDescription,
                    amount = content.formattedGross,
                    descriptionMaxWidth = descriptionMaxWidth,
                    amountRightX = amountRightX,
                )

                content.creditLines.forEach { credit ->
                    y = drawTableRow(
                        stream = stream,
                        font = fontRegular,
                        fontSize = 10f,
                        y = y,
                        description = credit.description,
                        amount = credit.formattedAmount(content.currencyCode),
                        descriptionMaxWidth = descriptionMaxWidth,
                        amountRightX = amountRightX,
                    )
                }

                y -= LINE_GAP
                drawHorizontalRule(stream, MARGIN, amountRightX, y)
                y -= LINE_GAP * 2

                val totalLabel = "Total due"
                val totalAmount = content.formattedTotal
                val totalFontSize = 11f
                val totalWidth = stringWidth(fontBold, totalFontSize, totalAmount)
                val labelWidth = stringWidth(fontBold, totalFontSize, totalLabel)
                drawAt(stream, fontBold, totalFontSize, amountRightX - totalWidth - COLUMN_GAP - labelWidth, y, totalLabel)
                drawAtRight(stream, fontBold, totalFontSize, amountRightX, y, totalAmount)
                y -= lineStep(totalFontSize)

                val footerY = MARGIN + 8f
                if (y < footerY + lineStep(8f)) {
                    y = footerY + lineStep(8f)
                }
                drawAt(
                    stream,
                    fontRegular,
                    8f,
                    MARGIN,
                    footerY,
                    "Generated by Glide · Bill $billId",
                )
            }
            document.save(file)
        }
        return file
    }

    private fun drawTableRow(
        stream: PDPageContentStream,
        font: PDType1Font,
        fontSize: Float,
        y: Float,
        description: String,
        amount: String,
        descriptionMaxWidth: Float,
        amountRightX: Float,
    ): Float {
        val lines = wrapLines(font, fontSize, description, descriptionMaxWidth)
        var rowY = y
        lines.forEachIndexed { index, line ->
            if (line.isNotEmpty()) {
                drawAt(stream, font, fontSize, MARGIN, rowY, line)
            }
            if (index == 0) {
                drawAtRight(stream, font, fontSize, amountRightX, rowY, amount)
            }
            rowY -= lineStep(fontSize)
        }
        return rowY - LINE_GAP
    }

    private fun drawLine(
        stream: PDPageContentStream,
        font: PDType1Font,
        fontSize: Float,
        x: Float,
        y: Float,
        text: String,
    ): Float {
        drawAt(stream, font, fontSize, x, y, text)
        return y - lineStep(fontSize)
    }

    private fun drawAt(
        stream: PDPageContentStream,
        font: PDType1Font,
        fontSize: Float,
        x: Float,
        y: Float,
        text: String,
    ) {
        val safe = sanitizePdfText(text)
        if (safe.isEmpty()) return
        stream.beginText()
        stream.setFont(font, fontSize)
        stream.newLineAtOffset(x, y - fontSize)
        stream.showText(safe)
        stream.endText()
    }

    private fun drawAtRight(
        stream: PDPageContentStream,
        font: PDType1Font,
        fontSize: Float,
        rightX: Float,
        y: Float,
        text: String,
    ) {
        val safe = sanitizePdfText(text)
        if (safe.isEmpty()) return
        val width = stringWidth(font, fontSize, safe)
        drawAt(stream, font, fontSize, rightX - width, y, safe)
    }

    private fun drawHorizontalRule(
        stream: PDPageContentStream,
        leftX: Float,
        rightX: Float,
        y: Float,
    ) {
        stream.moveTo(leftX, y)
        stream.lineTo(rightX, y)
        stream.stroke()
    }

    private fun lineStep(fontSize: Float): Float = fontSize + LINE_GAP

    private fun stringWidth(font: PDType1Font, fontSize: Float, text: String): Float =
        font.getStringWidth(text) / 1000f * fontSize

    private fun wrapLines(
        font: PDType1Font,
        fontSize: Float,
        text: String,
        maxWidth: Float,
    ): List<String> {
        val normalized = text.trim()
        if (normalized.isEmpty()) return listOf("")
        val words = normalized.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (stringWidth(font, fontSize, candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) {
                    lines.add(current)
                }
                current = if (stringWidth(font, fontSize, word) <= maxWidth) {
                    word
                } else {
                    lines.addAll(breakLongWord(font, fontSize, word, maxWidth))
                    ""
                }
            }
        }
        if (current.isNotEmpty()) {
            lines.add(current)
        }
        return lines.ifEmpty { listOf(normalized) }
    }

    private fun breakLongWord(
        font: PDType1Font,
        fontSize: Float,
        word: String,
        maxWidth: Float,
    ): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = start + 1
            while (end <= word.length && stringWidth(font, fontSize, word.substring(start, end)) <= maxWidth) {
                end++
            }
            val chunkEnd = (end - 1).coerceAtLeast(start + 1)
            chunks.add(word.substring(start, chunkEnd))
            start = chunkEnd
        }
        return chunks
    }

    /** Type 1 fonts accept WinAnsi; replace unsupported characters. */
    private fun sanitizePdfText(text: String): String =
        buildString(text.length) {
            for (ch in text) {
                append(
                    when (ch) {
                        '\n', '\r' -> ' '
                        '—', '–' -> '-'
                        '’', '‘' -> '\''
                        '“', '”' -> '"'
                        '…' -> '.'
                        else -> if (ch.code in 32..255) ch else '?'
                    },
                )
            }
        }.trim()

    private fun invoiceDirectory(): File {
        val documents = File(System.getProperty("user.home"), "Documents")
        return File(documents, "Glide/invoices")
    }
}
