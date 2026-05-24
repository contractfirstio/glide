package glide.billing

/** HTML receipt body for email — mirrors the PDF so recipients can read it without opening the attachment. */
fun ReceiptContent.formatReceiptEmailHtmlBody(): String {
    val greeting = billToName.takeIf { it.isNotBlank() }?.let { "Dear ${it.receiptHtmlEscape()}," } ?: "Hello,"

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Receipt ${receiptNumber.receiptHtmlEscape()}</title>
        </head>
        <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1f2430;">
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8;padding:32px 16px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:640px;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 8px 24px rgba(31,36,48,0.08);">
                  ${receiptEmailHeader()}
                  <tr>
                    <td style="padding:32px 36px 12px 36px;font-size:15px;line-height:1.6;color:#3d4452;">
                      <p style="margin:0 0 16px 0;">$greeting</p>
                      <p style="margin:0 0 12px 0;">
                        Thank you for your payment of <strong>${formattedTotal.receiptHtmlEscape()}</strong>.
                        This email confirms that your payment has been received in full.
                      </p>
                      <p style="margin:0;">
                        <strong>This is a payment receipt, not an invoice.</strong>
                        A summary is included below and the PDF receipt is attached for your records.
                      </p>
                    </td>
                  </tr>
                  ${receiptTwoColumnSection(
        "Bill to",
        receiptBillToDetailRows(),
        "From",
        receiptFromDetailRows(),
    )}
                  ${receiptDetailCard(
        "Receipt summary",
        listOf(
            "Document type" to "Payment receipt",
            "Receipt number" to receiptNumber,
            "Payment date" to paidDateLabel,
            "Invoice reference" to invoiceNumber,
            "Amount paid" to formattedTotal,
        ),
        highlightLast = true,
    )}
                  ${classSchedule?.let { receiptClassScheduleCard(it) } ?: ""}
                  ${receiptChargesTable()}
                  ${receiptPaymentConfirmationCard()}
                  <tr>
                    <td style="padding:8px 36px 36px 36px;font-size:14px;line-height:1.6;color:#3d4452;">
                      <p style="margin:0 0 20px 0;">Thank you for your business.</p>
                      <p style="margin:0;">
                        Kind regards,<br>
                        <strong style="color:#1f2430;">${fromName.receiptHtmlEscape()}</strong>
                      </p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}

/** Short plain-text fallback when the mail client cannot accept HTML (e.g. mailto: links). */
fun ReceiptContent.formatReceiptEmailPlainTextBody(): String = buildString {
    val greeting = billToName.takeIf { it.isNotBlank() }?.let { "Dear $it," } ?: "Hello,"
    appendLine(greeting)
    appendLine()
    appendLine(
        "Thank you for your payment of $formattedTotal. This is a payment receipt, not an invoice.",
    )
    appendLine()
    appendLine("Receipt number: $receiptNumber")
    appendLine("Payment date: $paidDateLabel")
    appendLine("Invoice reference: $invoiceNumber")
    appendLine("Amount paid: $formattedTotal")
    appendLine("Payment method: $paymentMethodLabel")
    if (paymentReference.isNotBlank()) {
        appendLine("Payment reference: $paymentReference")
    }
    appendLine()
    appendLine("Kind regards,")
    append(fromName)
}

private fun ReceiptContent.receiptEmailHeader(): String = """
    <tr>
      <td style="padding:28px 36px;background:linear-gradient(135deg,#0f766e 0%,#14b8a6 100%);color:#ffffff;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
          <tr>
            <td style="vertical-align:top;">
              <p style="margin:0 0 6px 0;font-size:13px;letter-spacing:0.08em;text-transform:uppercase;opacity:0.85;">Payment receipt</p>
              <p style="margin:0;font-size:22px;font-weight:700;line-height:1.3;">${fromName.receiptHtmlEscape()}</p>
              ${receiptContactLinesHtml(listOfNotNull(
        fromEmail.takeIf { it.isNotBlank() },
        fromPhone.takeIf { it.isNotBlank() },
    ), light = true)}
            </td>
            <td align="right" style="vertical-align:top;">
              <p style="margin:0 0 4px 0;font-size:28px;font-weight:700;line-height:1;">RECEIPT</p>
              <p style="margin:0 0 8px 0;font-size:13px;opacity:0.9;">Paid ${paidDateLabel.receiptHtmlEscape()}</p>
              <p style="margin:0;font-size:18px;font-weight:700;">${formattedTotal.receiptHtmlEscape()}</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
""".trimIndent()

private fun ReceiptContent.receiptBillToDetailRows(): List<Pair<String, String>> =
    listOfNotNull(
        billToName.takeIf { it.isNotBlank() }?.let { "Name" to it },
        billToEmail.takeIf { it.isNotBlank() }?.let { "Email" to it },
        billToPhone.takeIf { it.isNotBlank() }?.let { "Phone" to it },
    )

private fun ReceiptContent.receiptFromDetailRows(): List<Pair<String, String>> =
    listOfNotNull(
        fromName.takeIf { it.isNotBlank() }?.let { "Company" to it },
        fromEmail.takeIf { it.isNotBlank() }?.let { "Email" to it },
        fromPhone.takeIf { it.isNotBlank() }?.let { "Phone" to it },
    )

private fun receiptTwoColumnSection(
    leftTitle: String,
    leftRows: List<Pair<String, String>>,
    rightTitle: String,
    rightRows: List<Pair<String, String>>,
): String {
    if (leftRows.isEmpty() && rightRows.isEmpty()) return ""
    return """
        <tr>
          <td style="padding:8px 36px 0 36px;">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
              <tr>
                <td width="48%" style="vertical-align:top;padding-right:12px;">
                  ${receiptMiniCard(leftTitle, leftRows)}
                </td>
                <td width="48%" style="vertical-align:top;padding-left:12px;">
                  ${receiptMiniCard(rightTitle, rightRows)}
                </td>
              </tr>
            </table>
          </td>
        </tr>
    """.trimIndent()
}

private fun receiptMiniCard(title: String, rows: List<Pair<String, String>>): String {
    if (rows.isEmpty()) {
        return """
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f7f9fc;border:1px solid #e4e8ef;border-radius:10px;">
              <tr><td style="padding:16px 18px;font-size:12px;color:#6b7280;">—</td></tr>
            </table>
        """.trimIndent()
    }
    return """
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f7f9fc;border:1px solid #e4e8ef;border-radius:10px;">
          <tr>
            <td style="padding:16px 18px;">
              <p style="margin:0 0 12px 0;font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#6b7280;">${title.receiptHtmlEscape()}</p>
              ${rows.joinToString("") { (label, value) -> receiptDetailRowHtml(label, value) }}
            </td>
          </tr>
        </table>
    """.trimIndent()
}

private fun receiptDetailCard(
    title: String,
    rows: List<Pair<String, String>>,
    highlightLast: Boolean = false,
): String = """
    <tr>
      <td style="padding:20px 36px 0 36px;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #e4e8ef;border-radius:10px;overflow:hidden;">
          <tr>
            <td style="padding:14px 18px;background-color:#f7f9fc;border-bottom:1px solid #e4e8ef;">
              <p style="margin:0;font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#6b7280;">${title.receiptHtmlEscape()}</p>
            </td>
          </tr>
          <tr>
            <td style="padding:8px 18px 14px 18px;">
              ${rows.mapIndexed { index, (label, value) ->
    val isLast = highlightLast && index == rows.lastIndex
    receiptDetailRowHtml(label, value, emphasiseValue = isLast)
}.joinToString("")}
            </td>
          </tr>
        </table>
      </td>
    </tr>
""".trimIndent()

private fun ReceiptContent.receiptClassScheduleCard(schedule: InvoiceClassSchedule): String {
    val sessions = schedule.scheduledSessionLabels.ifEmpty { listOf("Not scheduled yet") }
    val rows = buildList {
        add("Class" to schedule.className)
        if (schedule.classDetails.isNotBlank()) add("Schedule" to schedule.classDetails)
        if (schedule.locationAddressLines.isNotEmpty()) {
            add("Location" to schedule.locationAddressLines.joinToString(", "))
        }
        add("Students" to schedule.studentNamesLabel)
        add("Plan period starts" to schedule.billingWindowStartLabel)
    }
    val sessionList = sessions.joinToString("") { session ->
        """<li style="margin:0 0 6px 0;color:#3d4452;">${session.receiptHtmlEscape()}</li>"""
    }
    return """
        <tr>
          <td style="padding:20px 36px 0 36px;">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #e4e8ef;border-radius:10px;overflow:hidden;">
              <tr>
                <td style="padding:14px 18px;background-color:#f7f9fc;border-bottom:1px solid #e4e8ef;">
                  <p style="margin:0;font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#6b7280;">Class details</p>
                </td>
              </tr>
              <tr>
                <td style="padding:8px 18px 14px 18px;">
                  ${rows.joinToString("") { (label, value) -> receiptDetailRowHtml(label, value) }}
                  <p style="margin:16px 0 8px 0;font-size:12px;font-weight:700;color:#6b7280;text-transform:uppercase;letter-spacing:0.06em;">Sessions to attend</p>
                  <ul style="margin:0;padding-left:20px;">$sessionList</ul>
                </td>
              </tr>
            </table>
          </td>
        </tr>
    """.trimIndent()
}

private fun ReceiptContent.receiptChargesTable(): String {
    val lineRows = buildString {
        debitLines.forEach { line ->
            append(receiptChargeRowHtml(line.description, line.formattedAmount(currencyCode), credit = false))
        }
        creditLines.forEach { line ->
            append(receiptChargeRowHtml(line.description, line.formattedAmount(currencyCode), credit = true))
        }
    }
    return """
        <tr>
          <td style="padding:20px 36px 0 36px;">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #e4e8ef;border-radius:10px;overflow:hidden;">
              <tr>
                <td colspan="2" style="padding:14px 18px;background-color:#f7f9fc;border-bottom:1px solid #e4e8ef;">
                  <p style="margin:0;font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#6b7280;">Charges</p>
                </td>
              </tr>
              <tr style="background-color:#0f766e;color:#ffffff;">
                <th align="left" style="padding:12px 18px;font-size:12px;font-weight:600;">Description</th>
                <th align="right" style="padding:12px 18px;font-size:12px;font-weight:600;width:120px;">Amount</th>
              </tr>
              $lineRows
              <tr style="background-color:#f7f9fc;">
                <td style="padding:16px 18px;font-size:15px;font-weight:700;color:#1f2430;">Amount paid</td>
                <td align="right" style="padding:16px 18px;font-size:18px;font-weight:700;color:#0f766e;">${formattedTotal.receiptHtmlEscape()}</td>
              </tr>
            </table>
          </td>
        </tr>
    """.trimIndent()
}

private fun ReceiptContent.receiptPaymentConfirmationCard(): String {
    val rows = buildList {
        add("Status" to "Paid in full")
        add("Payment method" to paymentMethodLabel)
        if (paymentReference.isNotBlank()) add("Payment reference" to paymentReference)
        add("Payment date" to paidDateLabel)
        add("Amount received" to formattedTotal)
    }
    return """
        <tr>
          <td style="padding:20px 36px 0 36px;">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #99f6e4;border-radius:10px;overflow:hidden;background-color:#f0fdfa;">
              <tr>
                <td style="padding:14px 18px;background-color:#ccfbf1;border-bottom:1px solid #99f6e4;">
                  <p style="margin:0;font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#0f766e;">Payment confirmation</p>
                </td>
              </tr>
              <tr>
                <td style="padding:16px 18px;font-size:14px;line-height:1.6;color:#3d4452;">
                  <p style="margin:0 0 14px 0;font-weight:600;color:#0f766e;">
                    Payment received — no further payment is required.
                  </p>
                  ${rows.joinToString("") { (label, value) -> receiptDetailRowHtml(label, value) }}
                </td>
              </tr>
            </table>
          </td>
        </tr>
    """.trimIndent()
}

private fun receiptChargeRowHtml(description: String, amount: String, credit: Boolean): String {
    val amountColor = if (credit) "#0f766e" else "#1f2430"
    return """
        <tr>
          <td style="padding:14px 18px;border-top:1px solid #eef1f5;font-size:14px;color:#3d4452;">${description.receiptHtmlEscape()}</td>
          <td align="right" style="padding:14px 18px;border-top:1px solid #eef1f5;font-size:14px;font-weight:600;color:$amountColor;white-space:nowrap;">${amount.receiptHtmlEscape()}</td>
        </tr>
    """.trimIndent()
}

private fun receiptDetailRowHtml(label: String, value: String, emphasiseValue: Boolean = false): String {
    val valueStyle = if (emphasiseValue) {
        "font-size:16px;font-weight:700;color:#0f766e;"
    } else {
        "font-size:14px;color:#1f2430;"
    }
    return """
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:6px 0;">
          <tr>
            <td width="42%" style="padding:2px 12px 2px 0;font-size:13px;color:#6b7280;vertical-align:top;">${label.receiptHtmlEscape()}</td>
            <td style="padding:2px 0;$valueStyle">${value.receiptHtmlEscape().replace("\n", "<br>")}</td>
          </tr>
        </table>
    """.trimIndent()
}

private fun receiptContactLinesHtml(lines: List<String>, light: Boolean = false): String {
    if (lines.isEmpty()) return ""
    val color = if (light) "rgba(255,255,255,0.9)" else "#6b7280"
    return lines.joinToString("") { line ->
        """<p style="margin:6px 0 0 0;font-size:13px;color:$color;">${line.receiptHtmlEscape()}</p>"""
    }
}

private fun String.receiptHtmlEscape(): String = buildString(length) {
    for (ch in this@receiptHtmlEscape) {
        when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(ch)
        }
    }
}
