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
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.DeepSpaceBlack
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

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
                color = LunarWhite
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
                            color = LunarWhite.copy(alpha = 0.6f)
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Enter playlist name",
                            color = LunarWhite.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    enabled = !isCreating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LunarWhite,
                        unfocusedTextColor = LunarWhite,
                        focusedBorderColor = CyberNeonBlue,
                        unfocusedBorderColor = LunarWhite.copy(alpha = 0.4f),
                        focusedLabelColor = CyberNeonBlue,
                        unfocusedLabelColor = LunarWhite.copy(alpha = 0.6f),
                        cursorColor = CyberNeonBlue
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
                            color = CyberNeonBlue,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Text(
                        text = "Create",
                        color = if (isCreateEnabled) CyberNeonBlue else LunarWhite.copy(alpha = 0.3f)
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
                    color = if (isCreating) GunmetalGray else LunarWhite
                )
            }
        },
        containerColor = DeepSpaceBlack.copy(alpha = 0.95f)
    )
}
