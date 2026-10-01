package com.sureja.accountant.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sureja.accountant.data.local.TransactionListItem
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(@ApplicationContext private val context: Context) {
    fun share(items: List<TransactionListItem>) {
        val directory=File(context.cacheDir,"exports").apply{mkdirs()};val file=File(directory,"accountant-expenses.csv")
        file.bufferedWriter().use{out->out.appendLine("date,member,amount,category,merchant,payment_method,account,note");items.forEach{i->out.appendLine(listOf(i.occurredAt,i.memberName,"%.2f".format(i.amountPaise/100.0),i.categoryName,i.merchant,i.paymentMethod.name,i.accountName,i.note).joinToString(","){csv(it.orEmpty())})}}
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/csv";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)},"Export expenses").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    private fun csv(value:String)="\"${value.replace("\"","\"\"")}\""
}

