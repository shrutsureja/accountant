package com.sureja.accountant.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sureja.accountant.ui.theme.AccountantColors

fun categoryIcon(name: String): ImageVector {
    val key = name.trim().lowercase()
    return when {
        "grocer" in key || "shopping" in key -> Icons.Default.ShoppingCart
        "food" in key || "dining" in key || "restaurant" in key -> Icons.Default.Restaurant
        "fuel" in key || "petrol" in key -> Icons.Default.LocalGasStation
        "medical" in key || "health" in key -> Icons.Default.MedicalServices
        "education" in key || "school" in key -> Icons.Default.School
        "entertainment" in key || "movie" in key -> Icons.Default.Movie
        "household" in key || "home" in key -> Icons.Default.Home
        "investment" in key -> Icons.Default.TrendingUp
        "gift" in key -> Icons.Default.CardGiftcard
        "care" in key -> Icons.Default.Spa
        "transport" in key || "travel" in key -> Icons.Default.DirectionsCar
        "utilit" in key || "bill" in key -> Icons.Default.ReceiptLong
        "other" in key -> Icons.Default.MoreHoriz
        else -> Icons.Default.Category
    }
}

@Composable
fun CategoryIcon(name: String, modifier: Modifier = Modifier, tint: Color = AccountantColors.Blue) {
    Icon(categoryIcon(name), contentDescription = null, modifier = modifier, tint = tint)
}
