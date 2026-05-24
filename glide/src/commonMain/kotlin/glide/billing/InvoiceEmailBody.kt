package glide.billing

private const val EMAIL_WIDTH = 72
private const val LABEL_WIDTH = 16
private const val AMOUNT_COLUMN_WIDTH = 12
private const val DESCRIPTION_COLUMN_WIDTH = EMAIL_WIDTH - AMOUNT_COLUMN_WIDTH - 3

/** Plain-text invoice body for email — mirrors the PDF so recipients can read it without opening the attachment. */
fun InvoiceContent.formatInvoiceEmailBody(): String = buildString {
    appendOpening(this@formatInvoiceEmailBody)
    appendLine()
    appendBanner("INVOICE")
    appendLine()

    appendSection("Your details") {
        appendLabelValueRows(
            listOfNotNull(
                billToName.takeIf { it.isNotBlank() }?.let { "Name" to it },
                billToEmail.takeIf { it.isNotBlank() }?.let { "Email" to it },
                billToPhone.takeIf { it.isNotBlank() }?.let { "Phone" to it },
            ),
        )
    }

    appendSection("From") {
        appendLabelValueRows(
            listOfNotNull(
                fromName.takeIf { it.isNotBlank() }?.let { "Company" to it },
                fromEmail.takeIf { it.isNotBlank() }?.let { "Email" to it },
                fromPhone.takeIf { it.isNotBlank() }?.let { "Phone" to it },
            ),
        )
    }

    appendSection("Invoice summary") {
        appendLabelValueRows(
            listOf(
                "Invoice number" to invoiceNumber,
                "Issue date" to issuedDateLabel,
                "Payment due" to dueDateLabel,
                "Amount due" to formattedTotal,
            ),
        )
    }

    classSchedule?.let { schedule ->
        appendClassScheduleSection(schedule)
    }

    appendLineItemsSection(this@formatInvoiceEmailBody)
    appendPaymentSection(this@formatInvoiceEmailBody)
    appendClosing(this@formatInvoiceEmailBody)
}

private fun StringBuilder.appendOpening(content: InvoiceContent) {
    val name = content.billToName.takeIf { it.isNotBlank() }
    appendLine(if (name != null) "Dear $name," else "Hello,")
    appendLine()
    appendLine(
        "Please find attached invoice ${content.invoiceNumber} for ${content.formattedTotal}, " +
            "with payment due by ${content.dueDateLabel}.",
    )
    appendLine(
        "For your convenience, the full invoice details are summarised below. " +
            "You may also refer to the attached PDF at any time.",
    )
}

private fun StringBuilder.appendClosing(content: InvoiceContent) {
    appendLine()
    appendThinRule()
    appendLine()
    appendLine("Thank you for your business.")
    appendLine()
    appendLine("Kind regards,")
    appendLine(content.fromName)
}

private fun StringBuilder.appendBanner(title: String) {
    val padded = title.padStart((EMAIL_WIDTH + title.length) / 2)
    appendLine(padded)
    appendLine(thinRuleOf(EMAIL_WIDTH))
}

private fun StringBuilder.appendSection(title: String, block: StringBuilder.() -> Unit) {
    appendLine()
    appendSectionHeading(title)
    block()
}

private fun StringBuilder.appendSectionHeading(title: String) {
    appendLine(title.uppercase())
    appendLine(thinRuleOf(title.length.coerceAtMost(EMAIL_WIDTH)))
}

private fun StringBuilder.appendClassScheduleSection(schedule: InvoiceClassSchedule) {
    appendSection("Class details") {
        appendLabelValueRows(
            buildList {
                add("Class" to schedule.className)
                if (schedule.classDetails.isNotBlank()) {
                    add("Schedule" to schedule.classDetails)
                }
                if (schedule.locationAddressLines.isNotEmpty()) {
                    add("Location" to schedule.locationAddressLines.joinToString("\n"))
                }
                add("Students" to schedule.studentNamesLabel)
                add("Pack period starts" to schedule.billingWindowStartLabel)
            },
        )
        val sessions = schedule.scheduledSessionLabels.ifEmpty { listOf("Not scheduled yet") }
        appendLine()
        appendIndented("Sessions to attend:")
        sessions.forEach { session ->
            appendIndented("• $session")
        }
    }
}

private fun StringBuilder.appendLineItemsSection(content: InvoiceContent) {
    appendSection("Charges") {
        appendTableHeader("Description", "Amount")
        content.debitLines.forEach { line ->
            appendTableRow(line.description, line.formattedAmount(content.currencyCode))
        }
        content.creditLines.forEach { line ->
            appendTableRow(line.description, line.formattedAmount(content.currencyCode))
        }
        appendLine(tableRule())
        appendTableRow("Total due", content.formattedTotal)
    }
}

private fun StringBuilder.appendPaymentSection(content: InvoiceContent) {
    appendSection("Payment instructions") {
        appendWrappedParagraph(
            "Payment may be made by bank transfer using Faster Payments (FPS). " +
                "Please use the details below and quote the invoice number as your payment reference.",
        )
        appendLine()
        appendLabelValueRows(
            buildList {
                if (content.fpsNumber.isNotBlank()) {
                    add("FPS number" to content.fpsNumber)
                }
                add("Payment reference" to content.invoiceNumber)
                add("Amount to pay" to content.formattedTotal)
                add("Payment due" to content.dueDateLabel)
            },
        )
    }
}

private fun StringBuilder.appendLabelValueRows(rows: List<Pair<String, String>>) {
    rows.forEach { (label, value) ->
        appendLabelValue(label, value)
    }
}

private fun StringBuilder.appendLabelValue(label: String, value: String) {
    value.lineSequence().forEachIndexed { index, line ->
        if (index == 0) {
            appendIndented("${label.padEnd(LABEL_WIDTH)} $line")
        } else {
            appendIndented("".padEnd(LABEL_WIDTH + 1) + line)
        }
        appendLine()
    }
}

private fun StringBuilder.appendTableHeader(descriptionTitle: String, amountTitle: String) {
    val header = descriptionTitle.padEnd(DESCRIPTION_COLUMN_WIDTH) +
        amountTitle.padStart(AMOUNT_COLUMN_WIDTH)
    appendIndented(header)
    appendLine()
    appendIndented(tableRule())
    appendLine()
}

private fun StringBuilder.appendTableRow(description: String, amount: String) {
    val rows = wrapText(description, DESCRIPTION_COLUMN_WIDTH)
    rows.forEachIndexed { index, descLine ->
        val amountCell = if (index == rows.lastIndex) amount.padStart(AMOUNT_COLUMN_WIDTH) else ""
        val line = descLine.padEnd(DESCRIPTION_COLUMN_WIDTH) + amountCell
        appendIndented(line)
        appendLine()
    }
    if (rows.isEmpty()) {
        appendIndented(amount.padStart(EMAIL_WIDTH - 2))
        appendLine()
    }
}

private fun StringBuilder.appendWrappedParagraph(text: String) {
    wrapText(text, EMAIL_WIDTH - 2).forEach { line ->
        appendIndented(line)
        appendLine()
    }
}

private fun StringBuilder.appendIndented(text: String) {
    append("  $text")
}

private fun StringBuilder.appendThinRule() {
    appendLine(thinRuleOf(EMAIL_WIDTH))
}

private fun thinRuleOf(length: Int): String =
    "─".repeat(length.coerceIn(0, EMAIL_WIDTH))

private fun tableRule(): String =
    "─".repeat(DESCRIPTION_COLUMN_WIDTH) + "  " + "─".repeat(AMOUNT_COLUMN_WIDTH)

private fun wrapText(text: String, maxWidth: Int): List<String> {
    if (text.isBlank()) return listOf("")
    if (maxWidth <= 0) return listOf(text)

    val words = text.trim().split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            lines.add(current.toString())
            current = StringBuilder()
        }
    }

    for (word in words) {
        if (word.length > maxWidth) {
            flush()
            word.chunked(maxWidth).forEach { chunk -> lines.add(chunk) }
            continue
        }
        if (current.isEmpty()) {
            current.append(word)
        } else if (current.length + 1 + word.length <= maxWidth) {
            current.append(' ').append(word)
        } else {
            flush()
            current.append(word)
        }
    }
    flush()
    return lines.ifEmpty { listOf("") }
}
