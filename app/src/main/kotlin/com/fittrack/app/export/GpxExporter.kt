package com.fittrack.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.fittrack.app.data.db.ActivityEntity
import com.fittrack.app.data.db.RoutePointEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpxExporter @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun exportActivity(activity: ActivityEntity, routePoints: List<RoutePointEntity>): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fitTrackDir = File(downloadsDir, "FitTrack")
            if (!fitTrackDir.exists()) {
                fitTrackDir.mkdirs()
            }
            
            val filename = "FitTrack_${activity.type.name}_${activity.startTime}.gpx"
            val file = File(fitTrackDir, filename)
            
            val formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))
            
            val gpxString = buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                appendLine("<gpx version=\"1.1\" creator=\"FitTrack\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
                appendLine("  <metadata>")
                appendLine("    <time>${formatter.format(Instant.ofEpochMilli(activity.startTime))}</time>")
                appendLine("  </metadata>")
                appendLine("  <trk>")
                appendLine("    <name>${activity.title}</name>")
                appendLine("    <type>${activity.type.name.lowercase()}</type>")
                appendLine("    <trkseg>")
                
                for (point in routePoints) {
                    appendLine("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
                    appendLine("        <ele>${point.altitudeMeters}</ele>")
                    appendLine("        <time>${formatter.format(Instant.ofEpochMilli(point.timestamp))}</time>")
                    appendLine("      </trkpt>")
                }
                
                appendLine("    </trkseg>")
                appendLine("  </trk>")
                appendLine("</gpx>")
            }
            
            file.writeText(gpxString)
            
            // Note: Returning a content URI would require setting up FileProvider in AndroidManifest
            // For now, return File Uri or let the caller use FileProvider.getUriForFile
            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
