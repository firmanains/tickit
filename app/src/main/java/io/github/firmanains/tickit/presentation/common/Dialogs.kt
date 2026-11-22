package io.github.firmanains.tickit.presentation.common

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.firmanains.tickit.domain.model.Proof
import java.io.File
import java.time.LocalTime

/** "No progress" is only accepted with a written reason. */
@Composable
fun ReasonDialog(habitName: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kenapa $habitName nggak jalan?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alasanmu dipajang di Wall of Shame.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, placeholder = { Text("Jujur aja") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(reason) }, enabled = reason.isNotBlank()) { Text("Pajang") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

/**
 * A tick has to come with proof: what you did (required) and a photo (optional).
 * Photos taken here but not saved are handed back to [onDiscardPhoto].
 */
@Composable
fun ProofDialog(
    habitName: String,
    newPhotoPath: () -> String,
    onDiscardPhoto: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (Proof) -> Unit,
) {
    val context = LocalContext.current
    var note by rememberSaveable { mutableStateOf("") }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var shootingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var noCamera by rememberSaveable { mutableStateOf(false) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val shot = shootingPath ?: return@rememberLauncherForActivityResult
        shootingPath = null
        if (saved) {
            photoPath?.let(onDiscardPhoto)
            photoPath = shot
        } else {
            onDiscardPhoto(shot)
        }
    }
    fun takePhoto() {
        val path = newPhotoPath()
        shootingPath = path
        try {
            camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.photos", File(path)))
        } catch (e: ActivityNotFoundException) {
            shootingPath = null
            onDiscardPhoto(path)
            noCamera = true
        }
    }
    fun dismiss() {
        photoPath?.let(onDiscardPhoto)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::dismiss,
        title = { Text("Bukti $habitName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ngapain aja?") },
                    placeholder = { Text("Lari 3 km keliling komplek") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                photoPath?.let { path ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(path)).crossfade(true).build(),
                        contentDescription = "Foto bukti $habitName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::takePhoto) { Text(if (photoPath == null) "Ambil foto" else "Foto ulang") }
                    photoPath?.let { path ->
                        TextButton(onClick = { onDiscardPhoto(path); photoPath = null }) { Text("Hapus foto") }
                    }
                }
                if (noCamera) Text("Nggak ada app kamera di HP ini. Catatan aja cukup.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(Proof(note, photoPath)) }, enabled = note.isNotBlank()) { Text("Simpan bukti") }
        },
        dismissButton = { TextButton(onClick = ::dismiss) { Text("Batal") } },
    )
}

fun showTimePicker(context: Context, initial: LocalTime, onPick: (LocalTime) -> Unit) =
    TimePickerDialog(context, { _, hour, minute -> onPick(LocalTime.of(hour, minute)) }, initial.hour, initial.minute, true).show()
