package glide.billing

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.color.PDColor
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB

internal object BillingPdfSupport {
    const val MARGIN = 48f
    const val FOOTER_HEIGHT = 24f
    const val LINE_GAP = 2f
    const val SECTION_GAP = 14f
    const val CONTENT_PADDING = 10f
    const val AMOUNT_COLUMN_WIDTH = 96f
    const val COLUMN_GAP = 12f
    const val META_LABEL_WIDTH = 88f

    val fontRegular = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    val fontBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    val fontOblique = PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE)

    val colorInk = rgb(0.12f, 0.14f, 0.18f)
    val colorMuted = rgb(0.42f, 0.45f, 0.50f)
    val colorRule = rgb(0.82f, 0.84f, 0.88f)
    val colorFill = rgb(0.96f, 0.97f, 0.98f)
    val colorSurface = rgb(0.99f, 0.99f, 1f)
    val colorAccent = rgb(0.18f, 0.32f, 0.48f)
    val colorAccentSoft = rgb(0.92f, 0.95f, 0.98f)
    val colorPaid = rgb(0.10f, 0.45f, 0.32f)

    class PdfPageContext(
        private val document: PDDocument,
        private val billId: String,
        private val continuationLabel: String,
    ) {
        val pageWidth = PDRectangle.A4.width
        val pageHeight = PDRectangle.A4.height
        val contentLeftX = MARGIN
        val contentRightX = pageWidth - MARGIN
        val textLeftX = contentLeftX + CONTENT_PADDING
        val textRightX = contentRightX - CONTENT_PADDING
        val amountColumnRightX = textRightX
        val descriptionMaxWidth = amountColumnRightX - COLUMN_GAP - AMOUNT_COLUMN_WIDTH - textLeftX
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
                y = drawAt(stream, fontBold, 10f, contentLeftX, y, continuationLabel, color = colorMuted)
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
            val footerY = MARGIN + 4f
            drawHorizontalRule(stream, contentLeftX, contentRightX, footerY + 10f, colorRule, 0.5f)
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

    data class TableRow(val description: String, val amount: String)

    fun drawClassScheduleSection(ctx: PdfPageContext, schedule: InvoiceClassSchedule) {
        val padding = CONTENT_PADDING
        val leftX = ctx.contentLeftX
        val rightX = ctx.contentRightX
        val textX = ctx.textLeftX
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
            val availableForSessions = (chunkTopY - ctx.minContentBottomY - introHeight - padding - 2f)
                .coerceAtLeast(sessionLineHeight)
            val maxSessions = (availableForSessions / sessionLineHeight).toInt().coerceAtLeast(1)
            val chunkEnd = minOf(sessionIndex + maxSessions, sessionLabels.size)
            val chunkSessions = sessionLabels.subList(sessionIndex, chunkEnd)
            val chunkHeight = introHeight + chunkSessions.size * sessionLineHeight + padding + 2f

            ctx.ensureSpace(chunkHeight, continuation = !isFirstChunk)
            val topY = ctx.y
            val contentBottomY = topY - chunkHeight
            fillRect(ctx.stream, leftX, contentBottomY, rightX - leftX, chunkHeight, colorFill)
            strokeRect(ctx.stream, leftX, contentBottomY, rightX - leftX, chunkHeight, colorRule, 0.5f)

            var innerY = topY - padding - 6f
            if (isFirstChunk) {
                innerY = drawAt(ctx.stream, fontBold, 9f, textX, innerY, "CLASS DETAILS", color = colorMuted)
                innerY -= 4f
                innerY = drawAt(ctx.stream, fontBold, 10f, textX, innerY, schedule.className, color = colorInk)
                innerY = drawWrappedLines(ctx.stream, textX, innerY, innerWidth, schedule.classDetails, bodyFontSize, colorMuted)
                if (schedule.locationAddressLines.isNotEmpty()) {
                    innerY -= 2f
                    innerY = drawAt(ctx.stream, fontBold, 9f, textX, innerY, "Location address", color = colorInk)
                    for (addressLine in schedule.locationAddressLines) {
                        innerY = drawAt(ctx.stream, fontRegular, bodyFontSize, textX, innerY, addressLine, color = colorMuted)
                    }
                }
                innerY -= 4f
                innerY = drawAt(ctx.stream, fontBold, 9f, textX, innerY, "Students", color = colorInk)
                innerY = drawWrappedLines(ctx.stream, textX, innerY, innerWidth, schedule.studentNamesLabel, bodyFontSize, colorMuted)
                innerY = drawAt(
                    ctx.stream,
                    fontBold,
                    bodyFontSize,
                    textX,
                    innerY - 1f,
                    "Plan period starts: ${schedule.billingWindowStartLabel}",
                    color = colorInk,
                )
                innerY -= 2f
            } else {
                innerY = drawAt(ctx.stream, fontBold, 9f, textX, innerY, "CLASS DETAILS (CONTINUED)", color = colorMuted)
                innerY -= 4f
            }
            innerY = drawAt(ctx.stream, fontBold, 9.5f, textX, innerY, "Class days to attend", color = colorInk)
            for (sessionLabel in chunkSessions) {
                innerY = drawAt(ctx.stream, fontRegular, bodyFontSize, textX, innerY, sessionLabel, color = colorMuted)
            }

            sessionIndex = chunkEnd
            isFirstChunk = false
            ctx.y = contentBottomY - 2f
        }
    }

    fun drawLineItemsTable(
        ctx: PdfPageContext,
        debitLines: List<InvoiceDebitLine>,
        creditLines: List<InvoiceCreditLine>,
        currencyCode: String,
        formattedTotal: String,
        totalLabel: String,
    ) {
        val leftX = ctx.contentLeftX
        val rightX = ctx.contentRightX
        val amountColumnRightX = ctx.amountColumnRightX
        val descriptionMaxWidth = ctx.descriptionMaxWidth
        val textLeftX = ctx.textLeftX
        val headerFontSize = 8.5f
        val rowFontSize = 9.5f
        val headerHeight = 20f
        val rows = buildList {
            debitLines.forEach { debit ->
                add(TableRow(debit.description, debit.formattedAmount(currencyCode)))
            }
            creditLines.forEach { credit ->
                add(TableRow(credit.description, credit.formattedAmount(currencyCode)))
            }
        }
        val totalBlockHeight = 4f + 10f + lineStep(11f) + 6f + 10f

        fun drawTableHeader(atY: Float): Float {
            val headerBottomY = atY - headerHeight
            val headerTextY = headerBottomY + headerHeight - 6f
            fillRect(ctx.stream, leftX, headerBottomY, rightX - leftX, headerHeight, colorAccent)
            drawAt(ctx.stream, fontBold, headerFontSize, textLeftX, headerTextY, "DESCRIPTION", color = rgb(1f, 1f, 1f))
            drawAt(
                ctx.stream,
                fontBold,
                headerFontSize,
                amountColumnRightX,
                headerTextY,
                "AMOUNT",
                align = TextAlign.RIGHT,
                color = rgb(1f, 1f, 1f),
            )
            return headerBottomY - lineStep(rowFontSize) - 4f
        }

        var rowIndex = 0
        var firstTable = true
        var isStriped = false
        while (rowIndex < rows.size || firstTable) {
            ctx.ensureSpace(headerHeight + lineStep(rowFontSize) + 4f, continuation = !firstTable)
            var rowY = drawTableHeader(ctx.y)
            firstTable = false

            while (rowIndex < rows.size) {
                val row = rows[rowIndex]
                val rowHeight = tableRowHeight(row.description, descriptionMaxWidth, rowFontSize)
                if (rowY - rowHeight < ctx.minContentBottomY) break
                if (isStriped) {
                    fillRect(
                        ctx.stream,
                        leftX,
                        rowY - rowHeight + 2f,
                        rightX - leftX,
                        rowHeight - 2f,
                        colorSurface,
                    )
                }
                rowY = drawTableRow(
                    stream = ctx.stream,
                    y = rowY,
                    description = row.description,
                    amount = row.amount,
                    descriptionMaxWidth = descriptionMaxWidth,
                    amountRightX = amountColumnRightX,
                    descriptionX = textLeftX,
                    fontSize = rowFontSize,
                )
                rowIndex++
                isStriped = !isStriped
            }
            ctx.y = rowY
            if (rowIndex < rows.size) continue
            break
        }

        ctx.ensureSpace(totalBlockHeight)
        var rowY = ctx.y
        rowY -= 4f
        drawHorizontalRule(ctx.stream, leftX, rightX, rowY, colorRule, 0.75f)
        rowY -= 10f

        val totalFontSize = 11f
        val labelFontSize = 9f
        val horizontalPadding = 8f
        val verticalPadding = 5f
        val amountWidth = stringWidth(fontBold, totalFontSize, formattedTotal)
        val labelWidth = stringWidth(fontRegular, labelFontSize, totalLabel)
        val labelRightX = amountColumnRightX - amountWidth - COLUMN_GAP
        val textTopY = rowY + 2f
        val textBottomY = rowY - totalFontSize - 2f
        val boxBottomY = textBottomY - verticalPadding
        val boxTopY = textTopY + verticalPadding
        val boxHeight = boxTopY - boxBottomY
        val boxLeftX = labelRightX - labelWidth - horizontalPadding
        val boxWidth = amountColumnRightX - boxLeftX + horizontalPadding

        fillRect(
            ctx.stream,
            boxLeftX,
            boxBottomY,
            boxWidth,
            boxHeight,
            colorAccentSoft,
        )
        strokeRect(
            ctx.stream,
            boxLeftX,
            boxBottomY,
            boxWidth,
            boxHeight,
            colorRule,
            0.5f,
        )
        drawAt(
            ctx.stream,
            fontRegular,
            labelFontSize,
            labelRightX,
            rowY,
            totalLabel,
            align = TextAlign.RIGHT,
            color = colorMuted,
        )
        drawAt(
            ctx.stream,
            fontBold,
            totalFontSize,
            amountColumnRightX,
            rowY,
            formattedTotal,
            align = TextAlign.RIGHT,
            color = colorInk,
        )
        ctx.y = boxBottomY - 4f
    }

    fun drawMetaRow(
        stream: PDPageContentStream,
        rightX: Float,
        y: Float,
        label: String,
        value: String,
    ): Float {
        val fontSize = 9f
        val valueX = rightX
        val labelX = rightX - META_LABEL_WIDTH
        drawAt(stream, fontRegular, fontSize, labelX, y, label, align = TextAlign.RIGHT, color = colorMuted)
        drawAt(stream, fontBold, fontSize, valueX, y, value, align = TextAlign.RIGHT, color = colorInk)
        return y - lineStep(fontSize) - 1f
    }

    fun drawSectionHeading(stream: PDPageContentStream, x: Float, y: Float, title: String): Float {
        val fontSize = 8f
        val bottomY = drawAt(stream, fontBold, fontSize, x, y, title.uppercase(), color = colorMuted)
        return bottomY - 4f
    }

    fun drawContactLines(
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

    fun drawAccentBar(stream: PDPageContentStream, leftX: Float, rightX: Float, y: Float, color: PDColor = colorAccent): Float {
        val height = 4f
        fillRect(stream, leftX, y - height, rightX - leftX, height, color)
        return y - height
    }

    fun drawWrappedLines(
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

    enum class TextAlign { LEFT, RIGHT }

    fun drawAt(
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

    fun fillRect(
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

    fun strokeRect(
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

    fun drawHorizontalRule(
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

    fun lineStep(fontSize: Float): Float = fontSize + LINE_GAP

    fun stringWidth(font: PDType1Font, fontSize: Float, text: String): Float =
        font.getStringWidth(text) / 1000f * fontSize

    fun rgb(r: Float, g: Float, b: Float): PDColor = PDColor(floatArrayOf(r, g, b), PDDeviceRGB.INSTANCE)

    fun sanitizePdfText(text: String): String =
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

    private fun layoutClassScheduleIntroBottom(
        schedule: InvoiceClassSchedule,
        topY: Float,
        padding: Float,
        innerWidth: Float,
        bodyFontSize: Float,
    ): Float {
        var innerY = topY - padding - 6f
        innerY = advanceTextY(innerY, 9f)
        innerY -= 4f
        innerY = advanceTextY(innerY, 10f)
        innerY = advanceWrappedTextY(innerY, bodyFontSize, schedule.classDetails, innerWidth)
        if (schedule.locationAddressLines.isNotEmpty()) {
            innerY -= 2f
            innerY = advanceTextY(innerY, 9f)
            for (addressLine in schedule.locationAddressLines) {
                innerY = advanceTextY(innerY, bodyFontSize, addressLine)
            }
        }
        innerY -= 4f
        innerY = advanceTextY(innerY, 9f)
        innerY = advanceWrappedTextY(innerY, bodyFontSize, schedule.studentNamesLabel, innerWidth)
        innerY = advanceTextY(innerY - 1f, bodyFontSize)
        innerY -= 2f
        innerY = advanceTextY(innerY, 9.5f)
        return innerY
    }

    private fun layoutClassScheduleContinuedIntroBottom(topY: Float, padding: Float): Float {
        var innerY = topY - padding - 6f
        innerY = advanceTextY(innerY, 9f)
        innerY -= 4f
        innerY = advanceTextY(innerY, 9.5f)
        return innerY
    }

    private fun tableRowHeight(description: String, descriptionMaxWidth: Float, fontSize: Float): Float {
        val lineCount = wrapLines(fontRegular, fontSize, description, descriptionMaxWidth)
            .count { it.isNotEmpty() }
            .coerceAtLeast(1)
        return lineCount * lineStep(fontSize) + 3f
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
        return rowY - 3f
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
}
