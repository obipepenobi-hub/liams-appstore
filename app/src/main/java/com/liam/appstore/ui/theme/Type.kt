package com.liam.appstore.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.liam.appstore.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Caprasimo: rundlicher Display-Font für Überschriften ("Werkstatt", App-Titel).
val CaprasimoFamily = FontFamily(
    Font(GoogleFont("Caprasimo"), fontProvider, FontWeight.Normal)
)

// Figtree: Fließtext / UI.
val FigtreeFamily = FontFamily(
    Font(GoogleFont("Figtree"), fontProvider, FontWeight.Normal),
    Font(GoogleFont("Figtree"), fontProvider, FontWeight.Medium),
    Font(GoogleFont("Figtree"), fontProvider, FontWeight.SemiBold),
    Font(GoogleFont("Figtree"), fontProvider, FontWeight.Bold)
)

val WerkstattTypography = Typography(
    displayLarge = TextStyle(fontFamily = CaprasimoFamily, fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = CaprasimoFamily, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = CaprasimoFamily, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = CaprasimoFamily, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = CaprasimoFamily, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    bodyLarge = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontFamily = FigtreeFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 13.sp)
)
