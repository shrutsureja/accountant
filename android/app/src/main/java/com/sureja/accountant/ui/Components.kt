package com.sureja.accountant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sureja.accountant.data.local.TransactionListItem
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun money(paise:Long):String=NumberFormat.getCurrencyInstance(Locale("en","IN")).apply{maximumFractionDigits=if(paise%100==0L)0 else 2}.format(paise/100.0)

@Composable fun AmountText(amount:Long,modifier:Modifier=Modifier,fontSize:Int=28)=Text(money(amount),modifier,fontSize=fontSize.sp,fontWeight=FontWeight.SemiBold)
@Composable fun SectionTitle(text:String,action:(()->Unit)?=null,actionLabel:String="View all") { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(text,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);if(action!=null)TextButton(onClick=action){Text(actionLabel)}} }
@Composable fun EmptyState(title:String,body:String,actionLabel:String?=null,onAction:()->Unit={}) { Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(body,color=MaterialTheme.colorScheme.onSurfaceVariant);if(actionLabel!=null)Button(onClick=onAction){Text(actionLabel)}} }
@Composable fun TransactionRow(item:TransactionListItem,onClick:()->Unit={}) { Surface(onClick=onClick,color=Color.Transparent){Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.merchant?.takeIf{it.isNotBlank()}?:item.categoryName?:"Expense",fontWeight=FontWeight.Medium);Text(listOfNotNull(item.categoryName,item.memberName,item.paymentMethod.name.lowercase().replaceFirstChar(Char::uppercase)).joinToString(" • "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(runCatching{OffsetDateTime.parse(item.occurredAt).format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))}.getOrDefault(item.occurredAt),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};AmountText(item.amountPaise,fontSize=17)}} }
@Composable fun ErrorBanner(message:String) { Surface(color=MaterialTheme.colorScheme.errorContainer,shape=MaterialTheme.shapes.small){Text(message,Modifier.fillMaxWidth().padding(12.dp),color=MaterialTheme.colorScheme.onErrorContainer)} }

