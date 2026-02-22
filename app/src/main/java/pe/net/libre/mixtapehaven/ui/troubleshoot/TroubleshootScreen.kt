package pe.net.libre.mixtapehaven.ui.troubleshoot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.net.libre.mixtapehaven.ui.components.NeonButton
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.GunmetalGray
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@Composable
fun TroubleshootScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Troubleshoot Connection",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = LunarWhite
        )

        Spacer(modifier = Modifier.height(24.dp))

        TroubleshootCard(
            title = "Check Server URL",
            description = "Ensure your server URL is correctly formatted:\n\n• Include http:// or https://\n• Example: http://192.168.1.100:8096\n• Example: https://jellyfin.example.com"
        )

        Spacer(modifier = Modifier.height(16.dp))

        TroubleshootCard(
            title = "Network Connection",
            description = "Verify that:\n\n• Your device is connected to the network\n• The Jellyfin server is running\n• You can access the server from a web browser\n• Firewall settings allow connections"
        )

        Spacer(modifier = Modifier.height(16.dp))

        TroubleshootCard(
            title = "Credentials",
            description = "Double-check your login information:\n\n• Username is case-sensitive\n• Password is entered correctly\n• Account has proper permissions\n• Account is not locked"
        )

        Spacer(modifier = Modifier.height(16.dp))

        TroubleshootCard(
            title = "Server Configuration",
            description = "Ensure Jellyfin server settings allow:\n\n• Remote connections if not on local network\n• API access is enabled\n• HTTPS certificate is valid (if using HTTPS)\n• Port forwarding is configured correctly"
        )

        Spacer(modifier = Modifier.height(32.dp))

        NeonButton(
            text = "Back to Connection",
            onClick = onNavigateBack
        )
    }
}

@Composable
private fun TroubleshootCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GunmetalGray
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = CyberNeonBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                color = LunarWhite,
                lineHeight = 20.sp
            )
        }
    }
}
