package glide.billing

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.color.PDColor
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object InvoicePdfWriter {
    private const val MARGIN = 54f
    private const val FOOTER_HEIGHT = 28f
    private const val LINE_GAP = 4f
    private const val SECTION_GAP = 22f
    private const val AMOUNT_COLUMN_WIDTH = 96f
    private const val COLUMN_GAP = 16f
    private const val META_LABEL_WIDTH = 88f
    private val fileDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val fontRegular = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val fontBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    private val fontOblique = PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE)

    private val colorInk = rgb(0.12f, 0.14f, 0.18f)
    private val colorMuted = rgb(0.42f, 0.45f, 0.50f)
    private val colorRule = rgb(0.82f, 0.84f, 0.88f)
    private val colorFill = rgb(0.96f, 0.97f, 0.98f)
    private val colorAccent = rgb(0.18f, 0.32f, 0.48f)

    fun write(content: InvoiceContent, billId: String): File {
        val directory = invoiceDirectory()
        directory.mkdirs()
        val file = File(directory, "invoice-${fileDateFormat.format(LocalDate.now())}-${content.invoiceNumber}.pdf")

        PDDocument().use { document ->
            val ctx = PdfPageContext(document, billId)
            ctx.startFirstPage()

            ctx.y = drawAccentBar(ctx.stream, ctx.contentLeftX, ctx.contentRightX, ctx.y)
            ctx.y -= 18f

            val headerBottomY = drawHeader(
                stream = ctx.stream,
                content = content,
                leftX = ctx.contentLeftX,
                rightX = ctx.contentRightX,
                y = ctx.y,
            )
            ctx.y = headerBottomY - SECTION_GAP

            drawHorizontalRule(ctx.stream, ctx.contentLeftX, ctx.contentRightX, ctx.y, colorRule, 0.5f)
            ctx.y -= SECTION_GAP

            ctx.y = drawSectionHeading(ctx.stream, ctx.contentLeftX, ctx.y, "Bill to")
            ctx.y = drawContactLines(
                stream = ctx.stream,
                lines = listOfNotNull(
                    content.billToName.takeIf { it.isNotBlank() },
                    content.billToEmail.takeIf { it.isNotBlank() },
                    content.billToPhone.takeIf { it.isNotBlank() },
                ),
                x = ctx.contentLeftX,
                y = ctx.y,
                fontSize = 10.5f,
            )
            ctx.y -= SECTION_GAP

            content.classSchedule?.let { schedule ->
                drawClassScheduleSection(ctx, schedule)
                ctx.y -= SECTION_GAP
            }

            drawLineItemsTable(
                ctx = ctx,
                content = content,
                descriptionMaxWidth = ctx.descriptionMaxWidth,
            )
            ctx.y -= SECTION_GAP

            drawPaymentSection(
                ctx = ctx,
                fpsNumber = content.fpsNumber,
                invoiceNumber = content.invoiceNumber,
            )

            ctx.finish()
            document.save(file)
        }
        return file
    }

    private class PdfPageContext(
        private val document: PDDocument,
        private val billId: String,
    ) {
        val pageWidth = PDRectangle.A4.width
        val pageHeight = PDRectangle.A4.height
        val contentLeftX = MARGIN
        val contentRightX = pageWidth - MARGIN
        val amountRightX = contentRightX
        val descriptionMaxWidth = amountRightX - MARGIN - AMOUNT_COLUMN_WIDTH - COLUMN_GAP
        val minContentBottomY = MARGIN + FOOTER_HEIGHT

        lateinit var stream: PDPageContentStream
        var y = 0f
        private var pageNumber = 0

        fun startFirstPage() {
            pageNumber = 0
            openPage(continuation = false)
        }

        fun openPage(continuation: Boolean) {
            if (pageNumber > 0) {
                stream.close()
            }
            pageNumber++
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            stream = PDPageContentStream(document, page)
            stream.setLineWidth(0.75f)
            y = pageHeight - MARGIN
            drawPageFooter()
            if (continuation) {
                y = drawAt(stream, fontBold, 10f, contentLeftX, y, "Invoice continued", color = colorMuted)
                y -= SECTION_GAP
            }
        }

        fun ensureSpace(requiredHeight: Float, continuation: Boolean = true) {
            if (y - requiredHeight < minContentBottomY) {
                openPage(continuation)
            }
        }

        fun finish() {
            stream.close()
        }

        private fun drawPageFooter() {
            val footerY = MARGIN + 6f
            drawHorizontalRule(stream, contentLeftX, contentRightX, footerY + 14f, colorRule, 0.5f)
            drawAt(
                stream,
                fontRegular,
                8f,
                contentLeftX,
                footerY,
                "Thank you for your business.",
                color = colorMuted,
            )
            val pageLabel = if (pageNumber > 1) "Page $pageNumber · $billId" else billId
            drawAt(
                stream,
                fontRegular,
                7.5f,
                contentRightX,
                footerY,
                pageLabel,
                align = TextAlign.RIGHT,
                color = colorMuted,
            )
        }
    }

    private fun drawClassScheduleSection(ctx: PdfPageContext, schedule: InvoiceClassSchedule) {
        val padding = 12f
        val leftX = ctx.contentLeftX
        val rightX = ctx.contentRightX
        val textX = leftX + padding
        val innerWidth = rightX - leftX - padding * 2
        val bodyFontSize = 9.5f
        val sessionLabels = schedule.scheduledSessionLabels.ifEmpty { listOf("Not scheduled yet") }

        var sessionIndex = 0
        var isFirstChunk = true
        while (sessionIndex < sessionLabels.size || isFirstChunk) {
            ctx.ensureSpace(lineStep(bodyFontSize) + 80f, continuation = !isFirstChunk)
            val chunkTopY = ctx.y
            val introBottomY = if (isFirstChunk) {
                layoutClassScheduleIntroBottom(schedule, chunkTopY, padding, innerWidth, bodyFontSize)
            } else {
                layoutClassScheduleContinuedIntroBottom(chunkTopY, padding)
            }
            val introHeight = chunkTopY - introBottomY
            val sessionLineHeight = lineStep(bodyFontSize)
            val availableForSessions = (chunkTopY - ctx.minContentBottomY - introHeight - padding - 4f)
                .coerceAtLeast(sessionLineHeight)
            val maxSessions = (availableForSessions / sessionLineHeight).toInt().coerceAtLeast(1)
            val chunkEnd = minOf(sessionIndex + maxSessions, sessionLabels.size)
            val chunkSessions = sessionLabels.subList(sessionIndex, chunkEnd)
            val chunkHeight = introHeight + chunkSessions.size * sessionLineHeight + padding + 4f

            ctx.ensureSpace(chunkHeight, continuation = !isFirstChunk)
            val topY = ctx.y
            val contentBottomY = topY - chunkHeight
            fillRect(ctx.stream, leftX, contentBottomY, rightX - leftX, chunkHeight, colorFill)
            strokeRect(ctx.stream, leftX, contentBottomY, rightX - leftX, chunkHeight, colorRule, 0.5f)

            var innerY = topY - padding - 9f
            if (isFirstChunk) {
                innerY = drawAt(ctx.stream, fontBold, 9f, textX, innerY, "CLASS DETAILS", color = colorMuted)
                innerY -= 8f
                innerY = drawAt(ctx.stream, fontBold, 10.5f, textX, innerY, schedule.className, color = colorInk)
                innerY = drawWrappedLines(ctx.stream, textX, innerY, innerWidth, schedule.classDetails, bodyFontSize, colorMuted)
                if (schedule.locationAddressLines.isNotEmpty()) {
                    innerY -= 4f
                    innerY = drawAt(ctx.stream, fontBold, 9.5f, textX, innerY, "Location address", color = colorInk)
                    for (addressLine in schedule.locationAddressLines) {
                        innerY = drawAt(ctx.stream, fontRegular, bodyFontSize, textX, innerY, addressLine, color = colorMuted)
                    }
                }
                innerY -= 6f
                innerY = drawAt(ctx.stream, fontBold, 9.5f, textX, innerY, "Students", color = colorInk)
                innerY = drawWrappedLines(ctx.stream, textX, innerY, innerWidth, schedule.studentNamesLabel, bodyFontSize, colorMuted)
                innerY = drawAt(
                    ctx.stream,
                    fontBold,
                    bodyFontSize,
                    textX,
                    innerY - 2f,
                    "Pack period starts: ${schedule.billingWindowStartLabel}",
                    color = colorInk,
                )
                innerY -= 4f
            } else {
                innerY = drawAt(ctx.stream, fontBold, 9f, textX, innerY, "CLASS DETAILS (CONTINUED)", color = colorMuted)
                innerY -= 8f
            }
            innerY = drawAt(ctx.stream, fontBold, 9.5f, textX, innerY, "Class days to attend", color = colorInk)
            for (sessionLabel in chunkSessions) {
                innerY = drawAt(ctx.stream, fontRegular, bodyFontSize, textX, innerY, sessionLabel, color = colorMuted)
            }

            sessionIndex = chunkEnd
            isFirstChunk = false
            ctx.y = contentBottomY - 4f
        }
    }

    private fun layoutClassScheduleIntroBottom(
        schedule: InvoiceClassSchedule,
        topY: Float,
        padding: Float,
        innerWidth: Float,
        bodyFontSize: Float,
    ): Float {
        var innerY = topY - padding - 9f
        innerY = advanceTextY(innerY, 9f)
        innerY -= 8f
        innerY = advanceTextY(innerY, 10.5f)
        innerY = advanceWrappedTextY(innerY, bodyFontSize, schedule.classDetails, innerWidth)
        if (schedule.locationAddressLines.isNotEmpty()) {
            innerY -= 4f
            innerY = advanceTextY(innerY, 9.5f)
            for (addressLine in schedule.locationAddressLines) {
                innerY = advanceTextY(innerY, bodyFontSize, addressLine)
            }
        }
        innerY -= 6f
        innerY = advanceTextY(innerY, 9.5f)
        innerY = advanceWrappedTextY(innerY, bodyFontSize, schedule.studentNamesLabel, innerWidth)
        innerY = advanceTextY(innerY - 2f, bodyFontSize)
        innerY -= 4f
        innerY = advanceTextY(innerY, 9.5f)
        return innerY
    }

    private fun layoutClassScheduleContinuedIntroBottom(topY: Float, padding: Float): Float {
        var innerY = topY - padding - 9f
        innerY = advanceTextY(innerY, 9f)
        innerY -= 8f
        innerY = advanceTextY(innerY, 9.5f)
        return innerY
    }

    private fun drawLineItemsTable(
        ctx: PdfPageContext,
        content: InvoiceContent,
        descriptionMaxWidth: Float,
    ) {
        val leftX = ctx.contentLeftX
        val rightX = ctx.contentRightX
        val amountRightX = ctx.amountRightX
        val headerFontSize = 9f
        val rowFontSize = 10f
        val headerHeight = 26f
        val rows = buildList {
            content.debitLines.forEach { debit ->
                add(TableRow(debit.description, debit.formattedAmount(content.currencyCode)))
            }
            content.creditLines.forEach { credit ->
                add(TableRow(credit.description, credit.formattedAmount(content.currencyCode)))
            }
        }
        val totalBlockHeight = 8f + 16f + lineStep(12f) + 8f

        fun drawTableHeader(atY: Float): Float {
            val headerBottomY = atY - headerHeight
            val headerTextY = headerBottomY + headerHeight - 7f
            fillRect(ctx.stream, leftX, headerBottomY, rightX - leftX, headerHeight, colorAccent)
            drawAt(ctx.stream, fontBold, headerFontSize, leftX + 10f, headerTextY, "DESCRIPTION", color = rgb(1f, 1f, 1f))
            drawAt(
                ctx.stream,
                fontBold,
                headerFontSize,
                amountRightX - 10f,
                headerTextY,
                "AMOUNT",
                align = TextAlign.RIGHT,
                color = rgb(1f, 1f, 1f),
            )
            return headerBottomY - lineStep(rowFontSize) - 8f
        }

        var rowIndex = 0
        var firstTable = true
        while (rowIndex < rows.size || firstTable) {
            ctx.ensureSpace(headerHeight + lineStep(rowFontSize) + 8f, continuation = !firstTable)
            var rowY = drawTableHeader(ctx.y)
            firstTable = false

            while (rowIndex < rows.size) {
                val row = rows[rowIndex]
                val rowHeight = tableRowHeight(row.description, descriptionMaxWidth, rowFontSize)
                if (rowY - rowHeight < ctx.minContentBottomY) break
                rowY = drawTableRow(
                    stream = ctx.stream,
                    y = rowY,
                    description = row.description,
                    amount = row.amount,
                    descriptionMaxWidth = descriptionMaxWidth,
                    amountRightX = amountRightX - 10f,
                    descriptionX = leftX + 10f,
                    fontSize = rowFontSize,
                )
                rowIndex++
            }
            ctx.y = rowY
            if (rowIndex < rows.size) continue
            break
        }

        ctx.ensureSpace(totalBlockHeight)
        var rowY = ctx.y
        rowY -= 8f
        drawHorizontalRule(ctx.stream, leftX, rightX, rowY, colorRule, 0.75f)
        rowY -= 16f

        val totalFontSize = 12f
        val totalAmount = content.formattedTotal
        val amountWidth = stringWidth(fontBold, totalFontSize, totalAmount)
        drawAt(
            ctx.stream,
            fontRegular,
            10f,
            amountRightX - amountWidth - COLUMN_GAP,
            rowY + 1f,
            "Total due",
            align = TextAlign.RIGHT,
            color = colorMuted,
        )
        drawAt(
            ctx.stream,
            fontBold,
            totalFontSize,
            amountRightX,
            rowY,
            totalAmount,
            align = TextAlign.RIGHT,
            color = colorInk,
        )
        ctx.y = rowY - lineStep(totalFontSize) - 8f
    }

    private data class TableRow(val description: String, val amount: String)

    private fun tableRowHeight(description: String, descriptionMaxWidth: Float, fontSize: Float): Float {
        val lineCount = wrapLines(fontRegular, fontSize, description, descriptionMaxWidth)
            .count { it.isNotEmpty() }
            .coerceAtLeast(1)
        return lineCount * lineStep(fontSize) + 6f
    }

    private fun drawPaymentSection(
        ctx: PdfPageContext,
        fpsNumber: String,
        invoiceNumber: String,
    ) {
        val padding = 14f
        val leftX = ctx.contentLeftX
        val rightX = ctx.contentRightX
        val textX = leftX + padding
        val innerWidth = rightX - leftX - padding * 2
        val referenceText = "Please quote invoice $invoiceNumber as your payment reference."
        val sectionHeight = ctx.y - layoutPaymentSectionBottom(ctx.y, padding, innerWidth, referenceText)

        ctx.ensureSpace(sectionHeight)
        val topY = ctx.y
        val contentBottomY = layoutPaymentSectionBottom(topY, padding, innerWidth, referenceText)
        val boxHeight = topY - contentBottomY

        fillRect(ctx.stream, leftX, contentBottomY, rightX - leftX, boxHeight, colorFill)
        strokeRect(ctx.stream, leftX, contentBottomY, rightX - leftX, boxHeight, colorRule, 0.5f)

        var innerY = topY - padding - 10f
        innerY = drawAt(ctx.stream, fontBold, 10f, textX, innerY, "Payment", color = colorInk)
        innerY -= 4f
        innerY = drawAt(
            ctx.stream,
            fontRegular,
            9.5f,
            textX,
            innerY,
            "Pay by bank transfer using Faster Payment (FPS).",
            color = colorMuted,
        )
        innerY = drawAt(ctx.stream, fontBold, 10.5f, textX, innerY, "FPS number: $fpsNumber", color = colorInk)
        drawWrappedLines(
            stream = ctx.stream,
            x = textX,
            y = innerY - 2f,
            maxWidth = innerWidth,
            text = referenceText,
            fontSize = 9f,
            color = colorMuted,
            font = fontOblique,
        )
        ctx.y = contentBottomY
    }

    private fun layoutPaymentSectionBottom(
        topY: Float,
        padding: Float,
        innerWidth: Float,
        referenceText: String,
    ): Float {
        var innerY = topY - padding - 10f
        innerY = advanceTextY(innerY, 10f)
        innerY -= 4f
        innerY = advanceTextY(innerY, 9.5f)
        innerY = advanceTextY(innerY, 10.5f)
        innerY = advanceWrappedTextY(innerY - 2f, 9f, referenceText, innerWidth, fontOblique)
        return innerY - padding
    }

    private fun drawHeader(
        stream: PDPageContentStream,
        content: InvoiceContent,
        leftX: Float,
        rightX: Float,
        y: Float,
    ): Float {
        val titleY = y
        drawAt(stream, fontBold, 26f, rightX, titleY, "INVOICE", align = TextAlign.RIGHT, color = colorAccent)
        var leftY = drawAt(stream, fontBold, 14f, leftX, y, content.fromName, color = colorInk)
        leftY = drawContactLines(
            stream = stream,
            lines = listOfNotNull(
                content.fromEmail.takeIf { it.isNotBlank() },
                content.fromPhone.takeIf { it.isNotBlank() },
            ),
            x = leftX,
            y = leftY - 2f,
            fontSize = 9.5f,
            muted = true,
        )

        var metaY = titleY - lineStep(26f) - 6f
        metaY = drawMetaRow(stream, rightX, metaY, "Invoice no.", content.invoiceNumber)
        metaY = drawMetaRow(stream, rightX, metaY, "Issue date", content.issuedDateLabel)
        metaY = drawMetaRow(stream, rightX, metaY, "Payment due", content.dueDateLabel)

        return minOf(leftY, metaY) - 4f
    }

    private fun drawMetaRow(
        stream: PDPageContentStream,
        rightX: Float,
        y: Float,
        label: String,
        value: String,
    ): Float {
        val fontSize = 9.5f
        val valueX = rightX
        val labelX = rightX - META_LABEL_WIDTH
        drawAt(stream, fontRegular, fontSize, labelX, y, label, align = TextAlign.RIGHT, color = colorMuted)
        drawAt(stream, fontBold, fontSize, valueX, y, value, align = TextAlign.RIGHT, color = colorInk)
        return y - lineStep(fontSize) - 2f
    }

    private fun drawWrappedLines(
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        maxWidth: Float,
        text: String,
        fontSize: Float,
        color: PDColor,
        font: PDType1Font = fontRegular,
    ): Float {
        var rowY = y
        for (line in wrapLines(font, fontSize, text, maxWidth)) {
            if (line.isNotEmpty()) {
                rowY = drawAt(stream, font, fontSize, x, rowY, line, color = color)
            }
        }
        return rowY
    }

    private fun advanceTextY(y: Float, fontSize: Float, text: String = "X"): Float {
        if (sanitizePdfText(text).isEmpty()) return y - lineStep(fontSize)
        return y - lineStep(fontSize)
    }

    private fun advanceWrappedTextY(
        y: Float,
        fontSize: Float,
        text: String,
        maxWidth: Float,
        font: PDType1Font = fontRegular,
    ): Float {
        var rowY = y
        for (line in wrapLines(font, fontSize, text, maxWidth)) {
            if (line.isNotEmpty()) {
                rowY = advanceTextY(rowY, fontSize, line)
            }
        }
        return rowY
    }

    private fun drawSectionHeading(stream: PDPageContentStream, x: Float, y: Float, title: String): Float {
        val fontSize = 8f
        val bottomY = drawAt(stream, fontBold, fontSize, x, y, title.uppercase(), color = colorMuted)
        return bottomY - 8f
    }

    private fun drawContactLines(
        stream: PDPageContentStream,
        lines: List<String>,
        x: Float,
        y: Float,
        fontSize: Float,
        muted: Boolean = false,
    ): Float {
        var rowY = y
        val color = if (muted) colorMuted else colorInk
        for (line in lines) {
            rowY = drawAt(stream, fontRegular, fontSize, x, rowY, line, color = color)
        }
        return rowY
    }

    private fun drawAccentBar(stream: PDPageContentStream, leftX: Float, rightX: Float, y: Float): Float {
        val height = 4f
        fillRect(stream, leftX, y - height, rightX - leftX, height, colorAccent)
        return y - height
    }

    private fun drawTableRow(
        stream: PDPageContentStream,
        y: Float,
        description: String,
        amount: String,
        descriptionMaxWidth: Float,
        amountRightX: Float,
        descriptionX: Float,
        fontSize: Float,
    ): Float {
        val lines = wrapLines(fontRegular, fontSize, description, descriptionMaxWidth)
        var rowY = y
        lines.forEachIndexed { index, line ->
            if (line.isNotEmpty()) {
                drawAt(stream, fontRegular, fontSize, descriptionX, rowY, line, color = colorInk)
            }
            if (index == 0) {
                drawAt(stream, fontRegular, fontSize, amountRightX, rowY, amount, align = TextAlign.RIGHT, color = colorInk)
            }
            rowY -= lineStep(fontSize)
        }
        return rowY - 6f
    }

    private enum class TextAlign { LEFT, RIGHT }

    private fun drawAt(
        stream: PDPageContentStream,
        font: PDType1Font,
        fontSize: Float,
        x: Float,
        y: Float,
        text: String,
        align: TextAlign = TextAlign.LEFT,
        color: PDColor = colorInk,
    ): Float {
        val safe = sanitizePdfText(text)
        if (safe.isEmpty()) return y - lineStep(fontSize)
        val width = stringWidth(font, fontSize, safe)
        val drawX = when (align) {
            TextAlign.LEFT -> x
            TextAlign.RIGHT -> x - width
        }
        stream.setNonStrokingColor(color)
        stream.beginText()
        stream.setFont(font, fontSize)
        stream.newLineAtOffset(drawX, y - fontSize)
        stream.showText(safe)
        stream.endText()
        return y - lineStep(fontSize)
    }

    private fun fillRect(
        stream: PDPageContentStream,
        x: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        color: PDColor,
    ) {
        stream.setNonStrokingColor(color)
        stream.addRect(x, bottomY, width, height)
        stream.fill()
    }

    private fun strokeRect(
        stream: PDPageContentStream,
        x: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        color: PDColor,
        lineWidth: Float,
    ) {
        stream.setStrokingColor(color)
        stream.setLineWidth(lineWidth)
        stream.addRect(x, bottomY, width, height)
        stream.stroke()
    }

    private fun drawHorizontalRule(
        stream: PDPageContentStream,
        leftX: Float,
        rightX: Float,
        y: Float,
        color: PDColor,
        lineWidth: Float,
    ) {
        stream.setStrokingColor(color)
        stream.setLineWidth(lineWidth)
        stream.moveTo(leftX, y)
        stream.lineTo(rightX, y)
        stream.stroke()
    }

    private fun lineStep(fontSize: Float): Float = fontSize + LINE_GAP

    private fun stringWidth(font: PDType1Font, fontSize: Float, text: String): Float =
        font.getStringWidth(text) / 1000f * fontSize

    private fun rgb(r: Float, g: Float, b: Float): PDColor = PDColor(floatArrayOf(r, g, b), PDDeviceRGB.INSTANCE)

    private fun wrapLines(
        font: PDType1Font,
        fontSize: Float,
        text: String,
        maxWidth: Float,
    ): List<String> {
        val normalized = text.trim()
        if (normalized.isEmpty()) return emptyList()
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
