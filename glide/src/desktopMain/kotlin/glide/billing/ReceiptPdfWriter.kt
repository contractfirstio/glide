package glide.billing

import glide.billing.BillingPdfSupport.CONTENT_PADDING
import glide.billing.BillingPdfSupport.PdfPageContext
import glide.billing.BillingPdfSupport.SECTION_GAP
import glide.billing.BillingPdfSupport.colorAccent
import glide.billing.BillingPdfSupport.colorAccentSoft
import glide.billing.BillingPdfSupport.colorFill
import glide.billing.BillingPdfSupport.colorInk
import glide.billing.BillingPdfSupport.colorMuted
import glide.billing.BillingPdfSupport.colorPaid
import glide.billing.BillingPdfSupport.colorRule
import glide.billing.BillingPdfSupport.drawAccentBar
import glide.billing.BillingPdfSupport.drawAt
import glide.billing.BillingPdfSupport.drawClassScheduleSection
import glide.billing.BillingPdfSupport.drawContactLines
import glide.billing.BillingPdfSupport.drawHorizontalRule
import glide.billing.BillingPdfSupport.drawLineItemsTable
import glide.billing.BillingPdfSupport.drawMetaRow
import glide.billing.BillingPdfSupport.drawSectionHeading
import glide.billing.BillingPdfSupport.fillRect
import glide.billing.BillingPdfSupport.fontBold
import glide.billing.BillingPdfSupport.fontRegular
import glide.billing.BillingPdfSupport.lineStep
import glide.billing.BillingPdfSupport.strokeRect
import glide.billing.BillingPdfSupport.TextAlign
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPageContentStream
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ReceiptPdfWriter {
    private val fileDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun write(content: ReceiptContent, billId: String): File {
        val directory = receiptDirectory()
        directory.mkdirs()
        val file = File(directory, "receipt-${fileDateFormat.format(LocalDate.now())}-${content.receiptNumber}.pdf")

        PDDocument().use { document ->
            val ctx = PdfPageContext(document, billId, "Receipt continued")
            ctx.startFirstPage()

            ctx.y = drawAccentBar(ctx.stream, ctx.contentLeftX, ctx.contentRightX, ctx.y, colorPaid)
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
                totalLabel = "Amount paid",
            )
            ctx.y -= SECTION_GAP

            drawPaymentReceivedSection(ctx, content)
            ctx.y -= 12f
            drawReceiptNotice(ctx)

            ctx.finish()
            document.save(file)
        }
        return file
    }

    private fun drawHeader(
        stream: PDPageContentStream,
        content: ReceiptContent,
        leftX: Float,
        rightX: Float,
        y: Float,
    ): Float {
        val titleY = y
        val verticalPadding = 6f
        val titleFontSize = 22f
        val subtitleFontSize = 10f
        val fromNameFontSize = 12f
        val contactFontSize = 9f
        val textLeftX = leftX + CONTENT_PADDING
        val textRightX = rightX - CONTENT_PADDING
        val contactLines = listOfNotNull(
            content.fromEmail.takeIf { it.isNotBlank() },
            content.fromPhone.takeIf { it.isNotBlank() },
        )

        var metaY = titleY - lineStep(titleFontSize) - 4f
        metaY = layoutMetaRow(metaY, subtitleFontSize)
        metaY -= 2f
        repeat(4) { metaY = layoutMetaRow(metaY) }
        var leftY = titleY - 4f - lineStep(fromNameFontSize)
        leftY = layoutContactLines(leftY, contactLines.size, contactFontSize)

        val contentBottomY = minOf(leftY, metaY) - verticalPadding
        val contentTopY = titleY + 4f
        val boxHeight = contentTopY - contentBottomY
        fillRect(stream, leftX, contentBottomY, rightX - leftX, boxHeight, colorAccentSoft)
        strokeRect(stream, leftX, contentBottomY, rightX - leftX, boxHeight, colorRule, 0.6f)

        drawAt(stream, fontBold, titleFontSize, textRightX, titleY, "RECEIPT", align = TextAlign.RIGHT, color = colorPaid)
        metaY = titleY - lineStep(titleFontSize) - 4f
        metaY = drawAt(
            stream,
            fontBold,
            subtitleFontSize,
            textRightX,
            metaY,
            "PAYMENT RECEIPT",
            align = TextAlign.RIGHT,
            color = colorAccent,
        )
        metaY -= 2f

        leftY = drawAt(stream, fontBold, fromNameFontSize, textLeftX, y - 4f, content.fromName, color = colorInk)
        leftY = drawContactLines(
            stream = stream,
            lines = contactLines,
            x = textLeftX,
            y = leftY,
            fontSize = contactFontSize,
            muted = true,
        )

        metaY = drawMetaRow(stream, textRightX, metaY, "Receipt no.", content.receiptNumber)
        metaY = drawMetaRow(stream, textRightX, metaY, "Payment date", content.paidDateLabel)
        metaY = drawMetaRow(stream, textRightX, metaY, "Invoice ref.", content.invoiceNumber)
        metaY = drawMetaRow(stream, textRightX, metaY, "Document", "Payment receipt")

        return minOf(leftY, metaY) - 6f
    }

    private fun layoutMetaRow(y: Float, fontSize: Float = 9f): Float = y - lineStep(fontSize) - 1f

    private fun layoutContactLines(y: Float, lineCount: Int, fontSize: Float): Float {
        var rowY = y
        repeat(lineCount) { rowY -= lineStep(fontSize) }
        return rowY
    }

    private fun drawPaymentReceivedSection(ctx: PdfPageContext, content: ReceiptContent) {
        val padding = CONTENT_PADDING
        val leftX = ctx.contentLeftX
        val rightX = ctx.contentRightX
        val textX = ctx.textLeftX
        val referenceLine = content.paymentReference.takeIf { it.isNotBlank() }
            ?.let { "Reference: $it" }
        val sectionHeight = layoutPaymentReceivedSectionHeight(padding, referenceLine)

        ctx.ensureSpace(sectionHeight)
        val topY = ctx.y
        val contentBottomY = topY - sectionHeight

        fillRect(ctx.stream, leftX, contentBottomY, rightX - leftX, sectionHeight, colorFill)
        strokeRect(ctx.stream, leftX, contentBottomY, rightX - leftX, sectionHeight, colorPaid, 1f)

        var innerY = topY - padding - 10f
        innerY = drawAt(ctx.stream, fontBold, 12f, textX, innerY, "PAID IN FULL", color = colorPaid)
        innerY -= 6f
        innerY = drawAt(
            ctx.stream,
            fontRegular,
            9.5f,
            textX,
            innerY,
            "This confirms payment has been received. No further payment is required.",
            color = colorMuted,
        )
        innerY -= 8f
        innerY = drawAt(ctx.stream, fontBold, 10f, textX, innerY, "Payment method", color = colorInk)
        innerY = drawAt(ctx.stream, fontRegular, 10.5f, textX, innerY, content.paymentMethodLabel, color = colorInk)
        if (referenceLine != null) {
            innerY -= 4f
            innerY = drawAt(ctx.stream, fontBold, 10f, textX, innerY, "Payment reference", color = colorInk)
            innerY = drawAt(ctx.stream, fontRegular, 10.5f, textX, innerY, content.paymentReference, color = colorInk)
        }
        innerY -= 4f
        innerY = drawAt(ctx.stream, fontBold, 10f, textX, innerY, "Amount received", color = colorInk)
        drawAt(ctx.stream, fontBold, 12f, textX, innerY - 2f, content.formattedTotal, color = colorPaid)
        ctx.y = contentBottomY
    }

    private fun layoutPaymentReceivedSectionHeight(padding: Float, referenceLine: String?): Float {
        var height = padding + 10f
        height += lineStep(12f) + 6f
        height += lineStep(9.5f) + 8f
        height += lineStep(10f) + lineStep(10.5f)
        if (referenceLine != null) {
            height += 4f + lineStep(10f) + lineStep(10.5f)
        }
        height += 4f + lineStep(10f) + lineStep(12f)
        height += padding
        return height
    }

    private fun drawReceiptNotice(ctx: PdfPageContext) {
        ctx.ensureSpace(lineStep(9f) + 4f)
        drawAt(
            ctx.stream,
            fontBold,
            9f,
            ctx.textLeftX,
            ctx.y,
            "This is a payment receipt, not an invoice. Please retain for your records.",
            color = colorMuted,
        )
        ctx.y -= lineStep(9f) + 4f
    }

    private fun receiptDirectory(): File {
        val dir = File(glide.data.persistence.glideDocumentsExportDir())
        return File(dir, "invoices")
    }
}
