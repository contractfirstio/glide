package glide.billing

import glide.billing.BillingPdfSupport.CONTENT_PADDING
import glide.billing.BillingPdfSupport.PdfPageContext
import glide.billing.BillingPdfSupport.SECTION_GAP
import glide.billing.BillingPdfSupport.colorAccent
import glide.billing.BillingPdfSupport.colorAccentSoft
import glide.billing.BillingPdfSupport.colorFill
import glide.billing.BillingPdfSupport.colorInk
import glide.billing.BillingPdfSupport.colorMuted
import glide.billing.BillingPdfSupport.colorRule
import glide.billing.BillingPdfSupport.drawAccentBar
import glide.billing.BillingPdfSupport.drawAt
import glide.billing.BillingPdfSupport.drawClassScheduleSection
import glide.billing.BillingPdfSupport.drawContactLines
import glide.billing.BillingPdfSupport.drawHorizontalRule
import glide.billing.BillingPdfSupport.drawLineItemsTable
import glide.billing.BillingPdfSupport.drawMetaRow
import glide.billing.BillingPdfSupport.drawSectionHeading
import glide.billing.BillingPdfSupport.drawWrappedLines
import glide.billing.BillingPdfSupport.fillRect
import glide.billing.BillingPdfSupport.fontBold
import glide.billing.BillingPdfSupport.fontOblique
import glide.billing.BillingPdfSupport.fontRegular
import glide.billing.BillingPdfSupport.lineStep
import glide.billing.BillingPdfSupport.strokeRect
import glide.billing.BillingPdfSupport.stringWidth
import glide.billing.BillingPdfSupport.TextAlign
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPageContentStream
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object InvoicePdfWriter {
    private val fileDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun write(content: InvoiceContent, billId: String): File {
        val directory = invoiceDirectory()
        directory.mkdirs()
        val file = File(directory, "invoice-${fileDateFormat.format(LocalDate.now())}-${content.invoiceNumber}.pdf")

        PDDocument().use { document ->
            val ctx = PdfPageContext(document, billId, "Invoice continued")
            ctx.startFirstPage()

            ctx.y = drawAccentBar(ctx.stream, ctx.contentLeftX, ctx.contentRightX, ctx.y)
            ctx.y -= 8f

            val headerBottomY = drawHeader(
                stream = ctx.stream,
                content = content,
                leftX = ctx.contentLeftX,
                rightX = ctx.contentRightX,
                y = ctx.y,
            )
            ctx.y = headerBottomY - 10f

            drawHorizontalRule(ctx.stream, ctx.contentLeftX, ctx.contentRightX, ctx.y, colorRule, 0.5f)
            ctx.y -= 10f

            ctx.y = drawSectionHeading(ctx.stream, ctx.textLeftX, ctx.y, "Bill to")
            ctx.y = drawContactLines(
                stream = ctx.stream,
                lines = listOfNotNull(
                    content.billToName.takeIf { it.isNotBlank() },
                    content.billToEmail.takeIf { it.isNotBlank() },
                    content.billToPhone.takeIf { it.isNotBlank() },
                ),
                x = ctx.textLeftX,
                y = ctx.y,
                fontSize = 9.5f,
            )
            ctx.y -= SECTION_GAP

            content.classSchedule?.let { schedule ->
                drawClassScheduleSection(ctx, schedule)
                ctx.y -= SECTION_GAP
            }

            drawLineItemsTable(
                ctx = ctx,
                debitLines = content.debitLines,
                creditLines = content.creditLines,
                currencyCode = content.currencyCode,
                formattedTotal = content.formattedTotal,
                totalLabel = "Total due",
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

    private fun drawHeader(
        stream: PDPageContentStream,
        content: InvoiceContent,
        leftX: Float,
        rightX: Float,
        y: Float,
    ): Float {
        val titleY = y
        val verticalPadding = 6f
        val titleFontSize = 22f
        val fromNameFontSize = 12f
        val contactFontSize = 9f
        val textLeftX = leftX + CONTENT_PADDING
        val textRightX = rightX - CONTENT_PADDING
        val contactLines = listOfNotNull(
            content.fromEmail.takeIf { it.isNotBlank() },
            content.fromPhone.takeIf { it.isNotBlank() },
        )

        var metaY = titleY - lineStep(titleFontSize) - 4f
        repeat(3) { metaY = layoutMetaRow(metaY) }
        var leftY = titleY - 4f - lineStep(fromNameFontSize)
        leftY = layoutContactLines(leftY, contactLines.size, contactFontSize)

        val contentBottomY = minOf(leftY, metaY) - verticalPadding
        val contentTopY = titleY + 4f
        val boxHeight = contentTopY - contentBottomY
        fillRect(stream, leftX, contentBottomY, rightX - leftX, boxHeight, colorAccentSoft)
        strokeRect(stream, leftX, contentBottomY, rightX - leftX, boxHeight, colorRule, 0.6f)

        drawAt(stream, fontBold, titleFontSize, textRightX, titleY, "INVOICE", align = TextAlign.RIGHT, color = colorAccent)
        leftY = drawAt(stream, fontBold, fromNameFontSize, textLeftX, y - 4f, content.fromName, color = colorInk)
        leftY = drawContactLines(
            stream = stream,
            lines = contactLines,
            x = textLeftX,
            y = leftY,
            fontSize = contactFontSize,
            muted = true,
        )

        metaY = titleY - lineStep(titleFontSize) - 4f
        metaY = drawMetaRow(stream, textRightX, metaY, "Invoice no.", content.invoiceNumber)
        metaY = drawMetaRow(stream, textRightX, metaY, "Issue date", content.issuedDateLabel)
        metaY = drawMetaRow(stream, textRightX, metaY, "Payment due", content.dueDateLabel)

        return minOf(leftY, metaY) - 6f
    }

    private fun layoutMetaRow(y: Float): Float = y - lineStep(9f) - 1f

    private fun layoutContactLines(y: Float, lineCount: Int, fontSize: Float): Float {
        var rowY = y
        repeat(lineCount) { rowY -= lineStep(fontSize) }
        return rowY
    }

    private fun drawPaymentSection(
        ctx: PdfPageContext,
        fpsNumber: String,
        invoiceNumber: String,
    ) {
        val padding = CONTENT_PADDING
        val leftX = ctx.contentLeftX
        val rightX = ctx.contentRightX
        val textX = ctx.textLeftX
        val innerWidth = rightX - leftX - padding * 2
        val referenceText = "Please quote invoice $invoiceNumber as your payment reference."
        val sectionHeight = ctx.y - layoutPaymentSectionBottom(ctx.y, padding, innerWidth, referenceText)

        ctx.ensureSpace(sectionHeight)
        val topY = ctx.y
        val contentBottomY = layoutPaymentSectionBottom(topY, padding, innerWidth, referenceText)
        val boxHeight = topY - contentBottomY

        fillRect(ctx.stream, leftX, contentBottomY, rightX - leftX, boxHeight, colorFill)
        strokeRect(ctx.stream, leftX, contentBottomY, rightX - leftX, boxHeight, colorRule, 0.5f)

        var innerY = topY - padding - 6f
        innerY = drawAt(ctx.stream, fontBold, 9.5f, textX, innerY, "PAYMENT DETAILS", color = colorInk)
        innerY -= 2f
        innerY = drawAt(
            ctx.stream,
            fontRegular,
            9f,
            textX,
            innerY,
            "Pay by bank transfer using Faster Payment (FPS).",
            color = colorMuted,
        )
        innerY = drawAt(ctx.stream, fontBold, 10f, textX, innerY, "FPS number: $fpsNumber", color = colorInk)
        drawWrappedLines(
            stream = ctx.stream,
            x = textX,
            y = innerY - 1f,
            maxWidth = innerWidth,
            text = referenceText,
            fontSize = 8.5f,
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
        var innerY = topY - padding - 6f
        innerY = innerY - lineStep(9.5f)
        innerY -= 2f
        innerY = innerY - lineStep(9f)
        innerY = innerY - lineStep(10f)
        val wrappedLines = referenceText.trim().split(Regex("\\s+"))
        var current = ""
        var lineCount = 0
        for (word in wrappedLines) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (stringWidth(fontOblique, 8.5f, candidate) <= innerWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lineCount++
                current = word
            }
        }
        if (current.isNotEmpty()) lineCount++
        innerY -= lineCount * lineStep(8.5f)
        return innerY - padding
    }

    private fun invoiceDirectory(): File {
        val dir = java.io.File(glide.data.persistence.glideDocumentsExportDir())
        return java.io.File(dir, "invoices")
    }
}
