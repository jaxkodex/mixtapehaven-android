package pe.net.libre.mixtapehaven.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.model.SampleData
import pe.net.libre.mixtapehaven.ui.theme.Accent
import pe.net.libre.mixtapehaven.ui.theme.AccentInk
import pe.net.libre.mixtapehaven.ui.theme.Stroke
import pe.net.libre.mixtapehaven.ui.theme.Surface2
import pe.net.libre.mixtapehaven.ui.theme.TextMuted
import pe.net.libre.mixtapehaven.ui.theme.TextPrimary
import pe.net.libre.mixtapehaven.ui.theme.TextSecondary

@Composable
fun LoginScreen(onConnect: () -> Unit, modifier: Modifier = Modifier) {
    var server by remember { mutableStateOf(TextFieldValue("https://${SampleData.SERVER_HOST}")) }
    var username by remember { mutableStateOf(TextFieldValue(SampleData.USER_NAME.lowercase())) }
    var password by remember { mutableStateOf(TextFieldValue("password")) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(48.dp))

        // Logo: rounded square with "oo" motif.
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, Accent, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("oo", style = MaterialTheme.typography.titleLarge, color = Accent)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "Mixtape",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary,
        )

        Text(
            "Your library. Offline, always. Connect to your Jellyfin server to begin.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        Spacer(Modifier.height(8.dp))

        LoginField(
            label = "SERVER ADDRESS",
            leadingIcon = Icons.Outlined.Link,
            value = server,
            onValueChange = { server = it },
        )

        LoginField(
            label = "USERNAME",
            leadingIcon = Icons.Outlined.Person,
            value = username,
            onValueChange = { username = it },
        )

        LoginField(
            label = "PASSWORD",
            leadingIcon = Icons.Outlined.Lock,
            value = password,
            onValueChange = { password = it },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation('•')
            },
            trailingIcon = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            onTrailingClick = { passwordVisible = !passwordVisible },
        )

        Spacer(Modifier.height(8.dp))

        // Primary Connect button.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Accent)
                .clickable(onClick = onConnect)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Connect", style = MaterialTheme.typography.titleMedium, color = AccentInk)
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = AccentInk,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            "Once connected, your music plays without a signal.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LoginField(
    label: String,
    leadingIcon: ImageVector,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2)
                .border(1.dp, Stroke, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(leadingIcon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = visualTransformation,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier.weight(1f),
            )
            if (trailingIcon != null) {
                Icon(
                    trailingIcon,
                    contentDescription = "Toggle password visibility",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .then(if (onTrailingClick != null) Modifier.clickable(onClick = onTrailingClick) else Modifier),
                )
            }
        }
    }
}
