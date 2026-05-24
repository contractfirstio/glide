package glide.billing

/** HTML invoice body for email — mirrors the PDF so recipients can read it without opening the attachment. */
fun InvoiceContent.formatInvoiceEmailHtmlBody(): String {
    val greeting = billToName.takeIf { it.isNotBlank() }?.let { "Dear ${it.htmlEscape()}," } ?: "Hello,"

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Invoice ${invoiceNumber.htmlEscape()}</title>
        </head>
        <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1f2430;">
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f8;padding:32px 16px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:640px;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 8px 24px rgba(31,36,48,0.08);">
                  ${emailHeader()}
                  <tr>
                    <td style="padding:32px 36px 12px 36px;font-size:15px;line-height:1.6;color:#3d4452;">
                      <p style="margin:0 0 16px 0;">$greeting</p>
                      <p style="margin:0 0 12px 0;">
                        Please find attached invoice <strong>${invoiceNumber.htmlEscape()}</strong>
                        for <strong>${formattedTotal.htmlEscape()}</strong>, with payment due by
                        <strong>${dueDateLabel.htmlEscape()}</strong>.
                      </p>
                      <p style="margin:0;">
                        A summary of your invoice is included below. You may also refer to the attached PDF at any time.
                      </p>
                    </td>
                  </tr>
                  ${twoColumnSection(
        "Bill to",
        billToDetailRows(),
        "From",
        fromDetailRows(),
    )}
                  ${detailCard(
        "Invoice summary",
        listOf(
            "Invoice number" to invoiceNumber,
            "Issue date" to issuedDateLabel,
            "Payment due" to dueDateLabel,
            "Amount due" to formattedTotal,
        ),
        highlightLast = true,
    )}
                  ${classSchedule?.let { classScheduleCard(it) } ?: ""}
                  ${chargesTable()}
                  ${paymentCard()}
                  <tr>
                    <td style="padding:8px 36px 36px 36px;font-size:14px;line-height:1.6;color:#3d4452;">
                      <p style="margin:0 0 20px 0;">Thank you for your business.</p>
                      <p style="margin:0;">
                        Kind regards,<br>
                        <strong style="color:#1f2430;">${fromName.htmlEscape()}</strong>
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
fun InvoiceContent.formatInvoiceEmailPlainTextBody(): String = buildString {
    val greeting = billToName.takeIf { it.isNotBlank() }?.let { "Dear $it," } ?: "Hello,"
    appendLine(greeting)
    appendLine()
    appendLine(
        "Please find attached invoice $invoiceNumber for $formattedTotal, due $dueDateLabel.",
    )
    appendLine()
    appendLine("Invoice number: $invoiceNumber")
    appendLine("Issue date: $issuedDateLabel")
    appendLine("Payment due: $dueDateLabel")
    appendLine("Amount due: $formattedTotal")
    if (fpsNumber.isNotBlank()) {
        appendLine("FPS number: $fpsNumber")
    }
    appendLine("Payment reference: $invoiceNumber")
    appendLine()
    appendLine("Kind regards,")
    append(fromName)
}

private fun InvoiceContent.emailHeader(): String = """
    <tr>
      <td style="padding:28px 36px;background:linear-gradient(135deg,#1e3a5f 0%,#2d5f8f 100%);color:#ffffff;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
          <tr>
            <td style="vertical-align:top;">
              <p style="margin:0 0 6px 0;font-size:13px;letter-spacing:0.08em;text-transform:uppercase;opacity:0.85;">Invoice</p>
              <p style="margin:0;font-size:22px;font-weight:700;line-height:1.3;">${fromName.htmlEscape()}</p>
              ${contactLinesHtml(listOfNotNull(
        fromEmail.takeIf { it.isNotBlank() },
        fromPhone.takeIf { it.isNotBlank() },
    ), light = true)}
            </td>
            <td align="right" style="vertical-align:top;">
              <p style="margin:0 0 8px 0;font-size:28px;font-weight:700;line-height:1;">${formattedTotal.htmlEscape()}</p>
              <p style="margin:0;font-size:13px;opacity:0.9;">Due ${dueDateLabel.htmlEscape()}</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
""".trimIndent()

private fun InvoiceContent.billToDetailRows(): List<Pair<String, String>> =
    listOfNotNull(
        billToName.takeIf { it.isNotBlank() }?.let { "Name" to it },
        billToEmail.takeIf { it.isNotBlank() }?.let { "Email" to it },
        billToPhone.takeIf { it.isNotBlank() }?.let { "Phone" to it },
    )

private fun InvoiceContent.fromDetailRows(): List<Pair<String, String>> =
    listOfNotNull(
        fromName.takeIf { it.isNotBlank() }?.let { "Company" to it },
        fromEmail.takeIf { it.isNotBlank() }?.let { "Email" to it },
        fromPhone.takeIf { it.isNotBlank() }?.let { "Phone" to it },
    )

private fun twoColumnSection(
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
                  ${miniCard(leftTitle, leftRows)}
                </td>
                <td width="48%" style="vertical-align:top;padding-left:12px;">
                  ${miniCard(rightTitle, rightRows)}
                </td>
              </tr>
            </table>
          </td>
        </tr>
    """.trimIndent()
}

private fun miniCard(title: String, rows: List<Pair<String, String>>): String {
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
              <p style="margin:0 0 12px 0;font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#6b7280;">${title.htmlEscape()}</p>
              ${rows.joinToString("") { (label, value) -> detailRowHtml(label, value) }}
            </td>
          </tr>
        </table>
    """.trimIndent()
}

private fun detailCard(
    title: String,
    rows: List<Pair<String, String>>,
    highlightLast: Boolean = false,
): String = """
    <tr>
      <td style="padding:20px 36px 0 36px;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #e4e8ef;border-radius:10px;overflow:hidden;">
          <tr>
            <td style="padding:14px 18px;background-color:#f7f9fc;border-bottom:1px solid #e4e8ef;">
              <p style="margin:0;font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#6b7280;">${title.htmlEscape()}</p>
            </td>
          </tr>
          <tr>
            <td style="padding:8px 18px 14px 18px;">
              ${rows.mapIndexed { index, (label, value) ->
    val isLast = highlightLast && index == rows.lastIndex
    detailRowHtml(label, value, emphasiseValue = isLast)
}.joinToString("")}
            </td>
          </tr>
        </table>
      </td>
    </tr>
""".trimIndent()

private fun InvoiceContent.classScheduleCard(schedule: InvoiceClassSchedule): String {
    val sessions = schedule.scheduledSessionLabels.ifEmpty { listOf("Not scheduled yet") }
    val rows = buildList {
        add("Class" to schedule.className)
        if (schedule.classDetails.isNotBlank()) add("Schedule" to schedule.classDetails)
        if (schedule.locationAddressLines.isNotEmpty()) {
            add("Location" to schedule.locationAddressLines.joinToString(", "))
        }
        add("Students" to schedule.studentNamesLabel)
        add("Pack period starts" to schedule.billingWindowStartLabel)
    }
    val sessionList = sessions.joinToString("") { session ->
        """<li style="margin:0 0 6px 0;color:#3d4452;">${session.htmlEscape()}</li>"""
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
                  ${rows.joinToString("") { (label, value) -> detailRowHtml(label, value) }}
                  <p style="margin:16px 0 8px 0;font-size:12px;font-weight:700;color:#6b7280;text-transform:uppercase;letter-spacing:0.06em;">Sessions to attend</p>
                  <ul style="margin:0;padding-left:20px;">$sessionList</ul>
                </td>
              </tr>
            </table>
          </td>
        </tr>
    """.trimIndent()
}

private fun InvoiceContent.chargesTable(): String {
    val lineRows = buildString {
        debitLines.forEach { line ->
            append(chargeRowHtml(line.description, line.formattedAmount(currencyCode), credit = false))
        }
        creditLines.forEach { line ->
            append(chargeRowHtml(line.description, line.formattedAmount(currencyCode), credit = true))
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
              <tr style="background-color:#2d5f8f;color:#ffffff;">
                <th align="left" style="padding:12px 18px;font-size:12px;font-weight:600;">Description</th>
                <th align="right" style="padding:12px 18px;font-size:12px;font-weight:600;width:120px;">Amount</th>
              </tr>
              $lineRows
              <tr style="background-color:#f7f9fc;">
                <td style="padding:16px 18px;font-size:15px;font-weight:700;color:#1f2430;">Total due</td>
                <td align="right" style="padding:16px 18px;font-size:18px;font-weight:700;color:#1e3a5f;">${formattedTotal.htmlEscape()}</td>
              </tr>
            </table>
          </td>
        </tr>
    """.trimIndent()
}

private fun chargeRowHtml(description: String, amount: String, credit: Boolean): String {
    val amountColor = if (credit) "#0f766e" else "#1f2430"
    return """
        <tr>
          <td style="padding:14px 18px;border-top:1px solid #eef1f5;font-size:14px;color:#3d4452;">${description.htmlEscape()}</td>
          <td align="right" style="padding:14px 18px;border-top:1px solid #eef1f5;font-size:14px;font-weight:600;color:$amountColor;white-space:nowrap;">${amount.htmlEscape()}</td>
        </tr>
    """.trimIndent()
}

private fun InvoiceContent.paymentCard(): String {
    val rows = buildList {
        if (fpsNumber.isNotBlank()) add("FPS number" to fpsNumber)
        add("Payment reference" to invoiceNumber)
        add("Amount to pay" to formattedTotal)
        add("Payment due" to dueDateLabel)
    }
    return """
        <tr>
          <td style="padding:20px 36px 0 36px;">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #c9daf0;border-radius:10px;overflow:hidden;background-color:#f3f8ff;">
              <tr>
                <td style="padding:14px 18px;background-color:#e8f1fb;border-bottom:1px solid #c9daf0;">
                  <p style="margin:0;font-size:11px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#1e3a5f;">Payment instructions</p>
                </td>
              </tr>
              <tr>
                <td style="padding:16px 18px;font-size:14px;line-height:1.6;color:#3d4452;">
                  <p style="margin:0 0 14px 0;">
                    Payment may be made by bank transfer using Faster Payments (FPS).
                    Please quote the invoice number as your payment reference.
                  </p>
                  ${rows.joinToString("") { (label, value) -> detailRowHtml(label, value) }}
                </td>
              </tr>
            </table>
          </td>
        </tr>
    """.trimIndent()
}

private fun detailRowHtml(label: String, value: String, emphasiseValue: Boolean = false): String {
    val valueStyle = if (emphasiseValue) {
        "font-size:16px;font-weight:700;color:#1e3a5f;"
    } else {
        "font-size:14px;color:#1f2430;"
    }
    return """
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:6px 0;">
          <tr>
            <td width="42%" style="padding:2px 12px 2px 0;font-size:13px;color:#6b7280;vertical-align:top;">${label.htmlEscape()}</td>
            <td style="padding:2px 0;$valueStyle">${value.htmlEscape().replace("\n", "<br>")}</td>
          </tr>
        </table>
    """.trimIndent()
}

private fun contactLinesHtml(lines: List<String>, light: Boolean = false): String {
    if (lines.isEmpty()) return ""
    val color = if (light) "rgba(255,255,255,0.9)" else "#6b7280"
    return lines.joinToString("") { line ->
        """<p style="margin:6px 0 0 0;font-size:13px;color:$color;">${line.htmlEscape()}</p>"""
    }
}

private fun String.htmlEscape(): String = buildString(length) {
    for (ch in this@htmlEscape) {
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
