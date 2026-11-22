package io.github.firmanains.tickit.presentation.home

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private enum class Missing(val message: String) {
    NOTIFICATIONS("Notifikasi mati — alarm nggak akan bunyi."),
    EXACT_ALARM("Izin alarm tepat waktu belum ada — alarm bisa telat."),
    FULL_SCREEN("Izin layar penuh belum ada — alarm cuma muncul sebagai notifikasi."),
}

/** The alarm only behaves like an alarm with these three; nag until granted. */
@Composable
fun PermissionBanners(refreshKey: Int) {
    val context = LocalContext.current
    var answered by remember { mutableIntStateOf(0) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { answered++ }
    val missing = remember(refreshKey, answered) { missingPermissions(context) }

    missing.forEach { item ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    when (item) {
                        Missing.NOTIFICATIONS -> askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        Missing.EXACT_ALARM -> context.openSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        Missing.FULL_SCREEN -> context.openSettings(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    }
                }) { Text("Izinkan") }
            }
        }
    }
}

private fun missingPermissions(context: Context) = buildList {
    if (Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) add(Missing.NOTIFICATIONS)
    if (Build.VERSION.SDK_INT >= 31 && !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
        add(Missing.EXACT_ALARM)
    }
    if (Build.VERSION.SDK_INT >= 34 && !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) {
        add(Missing.FULL_SCREEN)
    }
}

private fun Context.openSettings(action: String) = startActivity(Intent(action, Uri.parse("package:$packageName")))
