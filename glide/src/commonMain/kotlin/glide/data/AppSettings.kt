package glide.data

data class AppSettings(
    val legalCompanyName: String = "",
    val fpsNumber: String = "",
    val companyEmail: String = "",
    val companyPhone: String = "",
) {
    val isConfigured: Boolean
        get() = legalCompanyName.isNotBlank() &&
            fpsNumber.isNotBlank() &&
            companyEmail.isNotBlank() &&
            companyPhone.isNotBlank()
}
