package glide.billing

import glide.billing.BillingPdfSupport.PdfPageContext
import glide.billing.BillingPdfSupport.SECTION_GAP
import glide.billing.BillingPdfSupport.colorAccent
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
        drawAt(stream, fontBold, 26f, rightX, titleY, "RECEIPT", align = TextAlign.RIGHT, color = colorPaid)
        var metaY = titleY - lineStep(26f) - 2f
        metaY = drawAt(
            stream,
            fontBold,
            11f,
            rightX,
            metaY,
            "PAYMENT RECEIPT",
            align = TextAlign.RIGHT,
            color = colorAccent,
        )
        metaY -= 4f

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

        metaY = drawMetaRow(stream, rightX, metaY, "Receipt no.", content.receiptNumber)
        metaY = drawMetaRow(stream, rightX, metaY, "Payment date", content.paidDateLabel)
        metaY = drawMetaRow(stream, rightX, metaY, "Invoice ref.", content.invoiceNumber)
        metaY = drawMetaRow(stream, rightX, metaY, "Document", "Payment receipt")

        return minOf(leftY, metaY) - 4f
    }

    private fun drawPaymentReceivedSection(ctx: PdfPageContext, content: ReceiptContent) {
        val padding = 14f
        val leftX = ctx.contentLeftX
        val rightX = ctx.contentRightX
        val textX = leftX + padding
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
            ctx.contentLeftX,
            ctx.y,
            "This is a payment receipt, not an invoice. Please retain for your records.",
            color = colorMuted,
        )
        ctx.y -= lineStep(9f) + 4f
    }

    private fun receiptDirectory(): File {
        val documents = File(System.getProperty("user.home"), "Documents")
        return File(documents, "Glide/receipts")
    }
}
