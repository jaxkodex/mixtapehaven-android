package pe.net.libre.mixtapehaven.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.ui.theme.AccentPrimary
import pe.net.libre.mixtapehaven.ui.theme.BackgroundDeep
import pe.net.libre.mixtapehaven.ui.theme.SurfaceElevated
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary

@Composable
fun NewPlaylistDialog(
    isCreating: Boolean,
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    val isCreateEnabled = playlistName.isNotBlank() && !isCreating

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New playlist",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = {
                        Text(
                            text = "Playlist name",
                            color = TextPrimary.copy(alpha = 0.6f)
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Enter playlist name",
                            color = TextPrimary.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    enabled = !isCreating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = TextPrimary.copy(alpha = 0.4f),
                        focusedLabelColor = AccentPrimary,
                        unfocusedLabelColor = TextPrimary.copy(alpha = 0.6f),
                        cursorColor = AccentPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(playlistName) },
                enabled = isCreateEnabled
            ) {
                if (isCreating) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Text(
                        text = "Create",
                        color = if (isCreateEnabled) AccentPrimary else TextPrimary.copy(alpha = 0.3f)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text(
                    text = "Cancel",
                    color = if (isCreating) SurfaceElevated else TextPrimary
                )
            }
        },
        containerColor = BackgroundDeep.copy(alpha = 0.95f)
    )
}
