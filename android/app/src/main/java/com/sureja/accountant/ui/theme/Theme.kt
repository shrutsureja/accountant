package com.sureja.accountant.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors=lightColorScheme(primary=Color(0xFF176B5B),onPrimary=Color.White,secondary=Color(0xFF52665F),surface=Color(0xFFFBFCFA),background=Color(0xFFFBFCFA),surfaceVariant=Color(0xFFE8EFEB),outline=Color(0xFF737B77),error=Color(0xFFBA1A1A))
@Composable fun AccountantTheme(content: @Composable () -> Unit)=MaterialTheme(colorScheme=LightColors,typography=Typography(),content=content)

