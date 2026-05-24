package glide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.data.AppSettings
import glide.ui.theme.GlideButton
import glide.ui.theme.GlideOutlinedField

@Composable
fun CompanySetupDialog(
    initial: AppSettings,
    onConfirm: (AppSettings) -> Unit,
) {
    var legalCompanyName by remember(initial.legalCompanyName) { mutableStateOf(initial.legalCompanyName) }
    var fpsNumber by remember(initial.fpsNumber) { mutableStateOf(initial.fpsNumber) }
    var companyEmail by remember(initial.companyEmail) { mutableStateOf(initial.companyEmail) }
    var companyPhone by remember(initial.companyPhone) { mutableStateOf(initial.companyPhone) }

    val canConfirm = listOf(legalCompanyName, companyEmail, companyPhone, fpsNumber)
        .all { it.trim().isNotBlank() }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Welcome to Glide") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Enter your business details. They will appear on invoices so customers know how to pay you.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlideOutlinedField(
                    value = legalCompanyName,
                    onValueChange = { legalCompanyName = it },
                    label = "Legal company name",
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlideOutlinedField(
                    value = companyEmail,
                    onValueChange = { companyEmail = it },
                    label = "Company email",
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlideOutlinedField(
                    value = companyPhone,
                    onValueChange = { companyPhone = it },
                    label = "Company phone number",
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlideOutlinedField(
                    value = fpsNumber,
                    onValueChange = { fpsNumber = it },
                    label = "FPS number",
                )
            }
        },
        confirmButton = {
            GlideButton(
                onClick = {
                    onConfirm(
                        AppSettings(
                            legalCompanyName = legalCompanyName.trim(),
                            fpsNumber = fpsNumber.trim(),
                            companyEmail = companyEmail.trim(),
                            companyPhone = companyPhone.trim(),
                        ),
                    )
                },
                enabled = canConfirm,
            ) {
                Text("Continue")
            }
        },
    )
}
