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
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            val pageWidth = page.mediaBox.width
            val pageHeight = page.mediaBox.height
            val contentRightX = pageWidth - MARGIN
            val amountRightX = contentRightX
            val descriptionMaxWidth = amountRightX - MARGIN - AMOUNT_COLUMN_WIDTH - COLUMN_GAP

            PDPageContentStream(document, page).use { stream ->
                stream.setLineWidth(0.75f)
                var y = pageHeight - MARGIN

                y = drawAccentBar(stream, MARGIN, contentRightX, y)
                y -= 18f

                val headerBottomY = drawHeader(
                    stream = stream,
                    content = content,
                    leftX = MARGIN,
                    rightX = contentRightX,
                    y = y,
                )
                y = headerBottomY - SECTION_GAP

                drawHorizontalRule(stream, MARGIN, contentRightX, y, colorRule, 0.5f)
                y -= SECTION_GAP

                y = drawSectionHeading(stream, MARGIN, y, "Bill to")
                y = drawContactLines(
                    stream = stream,
                    lines = listOfNotNull(
                        content.billToName.takeIf { it.isNotBlank() },
                        content.billToEmail.takeIf { it.isNotBlank() },
                        content.billToPhone.takeIf { it.isNotBlank() },
                    ),
                    x = MARGIN,
                    y = y,
                    fontSize = 10.5f,
                )
                y -= SECTION_GAP

                content.classSchedule?.let { schedule ->
                    y = drawClassScheduleSection(
                        stream = stream,
                        schedule = schedule,
                        leftX = MARGIN,
                        rightX = contentRightX,
                        y = y,
                    )
                    y -= SECTION_GAP
                }

                y = drawLineItemsTable(
                    stream = stream,
                    content = content,
                    leftX = MARGIN,
                    rightX = contentRightX,
                    amountRightX = amountRightX,
                    descriptionMaxWidth = descriptionMaxWidth,
                    y = y,
                )
                y -= SECTION_GAP

                y = drawPaymentSection(
                    stream = stream,
                    fpsNumber = content.fpsNumber,
                    invoiceNumber = content.invoiceNumber,
                    leftX = MARGIN,
                    rightX = contentRightX,
                    y = y,
                )

                drawFooter(stream, MARGIN, contentRightX, billId)
            }
            document.save(file)
        }
        return file
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

    private fun drawClassScheduleSection(
        stream: PDPageContentStream,
        schedule: InvoiceClassSchedule,
        leftX: Float,
        rightX: Float,
        y: Float,
    ): Float {
        val padding = 12f
        val textX = leftX + padding
        val innerWidth = rightX - leftX - padding * 2
        val bodyFontSize = 9.5f
        val lineHeight = lineStep(bodyFontSize)

        val rowCount = 1 + // class name
            wrapLines(fontRegular, bodyFontSize, schedule.classDetails, innerWidth).size +
            1 + // students heading
            wrapLines(fontRegular, bodyFontSize, schedule.studentNamesLabel, innerWidth).size +
            1 + // pack period
            1 + // sessions heading
            maxOf(1, schedule.scheduledSessionLabels.size)
        val boxHeight = padding * 2 + 18f + rowCount * lineHeight + 12f
        val boxBottomY = y - boxHeight

        fillRect(stream, leftX, boxBottomY, rightX - leftX, boxHeight, colorFill)
        strokeRect(stream, leftX, boxBottomY, rightX - leftX, boxHeight, colorRule, 0.5f)

        var innerY = y - padding - 9f
        innerY = drawAt(stream, fontBold, 9f, textX, innerY, "CLASS DETAILS", color = colorMuted)
        innerY -= 8f

        innerY = drawAt(stream, fontBold, 10.5f, textX, innerY, schedule.className, color = colorInk)
        innerY = drawWrappedLines(stream, textX, innerY, innerWidth, schedule.classDetails, bodyFontSize, colorMuted)
        innerY -= 6f

        innerY = drawAt(stream, fontBold, 9.5f, textX, innerY, "Students", color = colorInk)
        innerY = drawWrappedLines(stream, textX, innerY, innerWidth, schedule.studentNamesLabel, bodyFontSize, colorMuted)
        innerY = drawAt(
            stream,
            fontBold,
            bodyFontSize,
            textX,
            innerY - 2f,
            "Pack period starts: ${schedule.billingWindowStartLabel}",
            color = colorInk,
        )
        innerY -= 4f
        innerY = drawAt(stream, fontBold, 9.5f, textX, innerY, "Class days to attend", color = colorInk)
        if (schedule.scheduledSessionLabels.isEmpty()) {
            innerY = drawAt(stream, fontRegular, bodyFontSize, textX, innerY, "Not scheduled yet", color = colorMuted)
        } else {
            for (sessionLabel in schedule.scheduledSessionLabels) {
                innerY = drawAt(stream, fontRegular, bodyFontSize, textX, innerY, sessionLabel, color = colorMuted)
            }
        }
        return boxBottomY - 4f
    }

    private fun drawWrappedLines(
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        maxWidth: Float,
        text: String,
        fontSize: Float,
        color: PDColor,
    ): Float {
        var rowY = y
        for (line in wrapLines(fontRegular, fontSize, text, maxWidth)) {
            if (line.isNotEmpty()) {
                rowY = drawAt(stream, fontRegular, fontSize, x, rowY, line, color = color)
            }
        }
        return rowY
    }

    private fun drawLineItemsTable(
        stream: PDPageContentStream,
        content: InvoiceContent,
        leftX: Float,
        rightX: Float,
        amountRightX: Float,
        descriptionMaxWidth: Float,
        y: Float,
    ): Float {
        val headerFontSize = 9f
        val rowFontSize = 10f
        val headerHeight = 26f
        val headerBottomY = y - headerHeight
        val headerTextY = headerBottomY + headerHeight - 7f

        fillRect(stream, leftX, headerBottomY, rightX - leftX, headerHeight, colorAccent)
        drawAt(stream, fontBold, headerFontSize, leftX + 10f, headerTextY, "DESCRIPTION", color = rgb(1f, 1f, 1f))
        drawAt(
            stream,
            fontBold,
            headerFontSize,
            amountRightX - 10f,
            headerTextY,
            "AMOUNT",
            align = TextAlign.RIGHT,
            color = rgb(1f, 1f, 1f),
        )

        var rowY = headerBottomY - lineStep(rowFontSize) - 8f
        rowY = drawTableRow(
            stream = stream,
            y = rowY,
            description = content.packLineDescription,
            amount = content.formattedGross,
            descriptionMaxWidth = descriptionMaxWidth,
            amountRightX = amountRightX - 10f,
            descriptionX = leftX + 10f,
            fontSize = rowFontSize,
        )
        content.creditLines.forEach { credit ->
            rowY = drawTableRow(
                stream = stream,
                y = rowY,
                description = credit.description,
                amount = credit.formattedAmount(content.currencyCode),
                descriptionMaxWidth = descriptionMaxWidth,
                amountRightX = amountRightX - 10f,
                descriptionX = leftX + 10f,
                fontSize = rowFontSize,
            )
        }

        rowY -= 8f
        drawHorizontalRule(stream, leftX, rightX, rowY, colorRule, 0.75f)
        rowY -= 16f

        val totalFontSize = 12f
        val totalAmount = content.formattedTotal
        val amountWidth = stringWidth(fontBold, totalFontSize, totalAmount)
        drawAt(
            stream,
            fontRegular,
            10f,
            amountRightX - amountWidth - COLUMN_GAP,
            rowY + 1f,
            "Total due",
            align = TextAlign.RIGHT,
            color = colorMuted,
        )
        drawAt(
            stream,
            fontBold,
            totalFontSize,
            amountRightX,
            rowY,
            totalAmount,
            align = TextAlign.RIGHT,
            color = colorInk,
        )

        return rowY - lineStep(totalFontSize) - 8f
    }

    private fun drawPaymentSection(
        stream: PDPageContentStream,
        fpsNumber: String,
        invoiceNumber: String,
        leftX: Float,
        rightX: Float,
        y: Float,
    ): Float {
        val padding = 14f
        val boxHeight = 72f
        val boxBottomY = y - boxHeight

        fillRect(stream, leftX, boxBottomY, rightX - leftX, boxHeight, colorFill)
        strokeRect(stream, leftX, boxBottomY, rightX - leftX, boxHeight, colorRule, 0.5f)

        var innerY = y - padding - 10f
        innerY = drawAt(stream, fontBold, 10f, leftX + padding, innerY, "Payment", color = colorInk)
        innerY -= 4f
        innerY = drawAt(
            stream,
            fontRegular,
            9.5f,
            leftX + padding,
            innerY,
            "Pay by bank transfer using Faster Payment (FPS).",
            color = colorMuted,
        )
        innerY = drawAt(stream, fontBold, 10.5f, leftX + padding, innerY, "FPS number: $fpsNumber", color = colorInk)
        innerY = drawAt(
            stream,
            fontOblique,
            9f,
            leftX + padding,
            innerY - 2f,
            "Please quote invoice $invoiceNumber as your payment reference.",
            color = colorMuted,
        )
        return boxBottomY
    }

    private fun drawFooter(stream: PDPageContentStream, leftX: Float, rightX: Float, billId: String) {
        val footerY = MARGIN + 6f
        drawHorizontalRule(stream, leftX, rightX, footerY + 14f, colorRule, 0.5f)
        drawAt(
            stream,
            fontRegular,
            8f,
            leftX,
            footerY,
            "Thank you for your business.",
            color = colorMuted,
        )
        drawAt(
            stream,
            fontRegular,
            7.5f,
            rightX,
            footerY,
            billId,
            align = TextAlign.RIGHT,
            color = colorMuted,
        )
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
        val font = if (muted) fontRegular else fontRegular
        for (line in lines) {
            rowY = drawAt(stream, font, fontSize, x, rowY, line, color = color)
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
