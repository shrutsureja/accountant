package com.sureja.accountant.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.sureja.accountant.data.network.AccountantApi
import com.sureja.accountant.data.network.CreateIssueRequest
import com.sureja.accountant.data.network.IssueDetailResponse
import com.sureja.accountant.data.network.IssueDto
import com.sureja.accountant.data.network.IssueScreenshotUpload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class IssueRepository @Inject constructor(private val api: AccountantApi, @ApplicationContext private val context: Context) {
    suspend fun list(): List<IssueDto> = api.issues().issues
    suspend fun create(category: String, title: String, description: String, uris: List<Uri>) {
        val screenshots = withContext(Dispatchers.IO) { uris.take(3).map(::compressScreenshot) }
        api.createIssue(CreateIssueRequest(category, title.trim(), description.trim(), screenshots))
    }
    suspend fun detail(id: String): IssueDetailResponse = api.issue(id)
    suspend fun screenshot(issueId: String, imageId: String): Bitmap? = withContext(Dispatchers.IO) {
        api.issueScreenshot(issueId, imageId).use { body -> BitmapFactory.decodeStream(body.byteStream()) }
    }

    private fun compressScreenshot(uri: Uri): IssueScreenshotUpload {
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val header = resolver.openInputStream(uri) ?: throw IllegalArgumentException("Could not open screenshot")
        header.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) throw IllegalArgumentException("Choose an image file")
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > 1600) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalArgumentException("Could not read screenshot")
        val maximum = maxOf(bitmap.width, bitmap.height)
        val scaled = if (maximum > 1280) Bitmap.createScaledBitmap(bitmap, bitmap.width * 1280 / maximum, bitmap.height * 1280 / maximum, true) else bitmap
        if (scaled !== bitmap) bitmap.recycle()
        try {
            var quality = 82
            var bytes: ByteArray
            do {
                val output = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
                bytes = output.toByteArray()
                quality -= 14
            } while (bytes.size > 850_000 && quality >= 40)
            if (bytes.size > 850_000) throw IllegalArgumentException("Screenshot is too large to attach")
            return IssueScreenshotUpload("image/jpeg", Base64.encodeToString(bytes, Base64.NO_WRAP))
        } finally {
            scaled.recycle()
        }
    }
}
