package glide.billing

import glide.billing.BillingPdfSupport.PdfPageContext
import glide.billing.BillingPdfSupport.SECTION_GAP
import glide.billing.BillingPdfSupport.colorAccent
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
        innerY = innerY - lineStep(10f)
        innerY -= 4f
        innerY = innerY - lineStep(9.5f)
        innerY = innerY - lineStep(10.5f)
        val wrappedLines = referenceText.trim().split(Regex("\\s+"))
        var current = ""
        var lineCount = 0
        for (word in wrappedLines) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (stringWidth(fontOblique, 9f, candidate) <= innerWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lineCount++
                current = word
            }
        }
        if (current.isNotEmpty()) lineCount++
        innerY -= lineCount * lineStep(9f)
        return innerY - padding
    }

    private fun invoiceDirectory(): File {
        val documents = File(System.getProperty("user.home"), "Documents")
        return File(documents, "Glide/invoices")
    }
}
