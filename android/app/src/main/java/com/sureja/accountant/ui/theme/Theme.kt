package com.sureja.accountant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AccountantColors {
    val Blue = Color(0xFF2563EB)
    val BlueDark = Color(0xFF1D4ED8)
    val BlueLight = Color(0xFFEAF2FF)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color.White
    val Text = Color(0xFF0F172A)
    val SecondaryText = Color(0xFF64748B)
    val Border = Color(0xFFE2E8F0)
    val MutedSurface = Color(0xFFF1F5F9)
    val Danger = Color(0xFFDC2626)
    val Success = Color(0xFF16A34A)
    val Warning = Color(0xFFF59E0B)
}

object AccountantSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val page = 20.dp
    val lg = 24.dp
    val xl = 32.dp
}

private val LightColors = lightColorScheme(
    primary = AccountantColors.Blue,
    onPrimary = AccountantColors.Surface,
    primaryContainer = AccountantColors.BlueLight,
    onPrimaryContainer = AccountantColors.BlueDark,
    secondary = AccountantColors.SecondaryText,
    onSecondary = AccountantColors.Surface,
    secondaryContainer = AccountantColors.MutedSurface,
    onSecondaryContainer = AccountantColors.Text,
    tertiary = AccountantColors.BlueDark,
    onTertiary = AccountantColors.Surface,
    background = AccountantColors.Background,
    onBackground = AccountantColors.Text,
    surface = AccountantColors.Surface,
    onSurface = AccountantColors.Text,
    surfaceVariant = AccountantColors.MutedSurface,
    onSurfaceVariant = AccountantColors.SecondaryText,
    surfaceContainerLowest = AccountantColors.Surface,
    surfaceContainerLow = AccountantColors.Background,
    surfaceContainer = AccountantColors.MutedSurface,
    surfaceContainerHigh = AccountantColors.MutedSurface,
    surfaceContainerHighest = AccountantColors.Border,
    surfaceBright = AccountantColors.Surface,
    surfaceDim = AccountantColors.Background,
    outline = AccountantColors.Border,
    outlineVariant = AccountantColors.Border,
    inverseSurface = AccountantColors.Text,
    inverseOnSurface = AccountantColors.Surface,
    inversePrimary = AccountantColors.BlueLight,
    surfaceTint = AccountantColors.Blue,
    error = AccountantColors.Danger,
    onError = AccountantColors.Surface,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

private val AccountantTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

private val AccountantShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

@Composable
fun AccountantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AccountantTypography,
        shapes = AccountantShapes,
        content = content,
    )
}
