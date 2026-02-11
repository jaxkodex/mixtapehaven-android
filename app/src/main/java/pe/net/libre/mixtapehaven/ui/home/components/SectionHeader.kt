package pe.net.libre.mixtapehaven.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite

@Composable
fun SectionHeader(
    title: String,
    onSeeMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actionText: String = "View All"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = LunarWhite
        )

        if (onSeeMoreClick != null) {
            TextButton(onClick = onSeeMoreClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberNeonBlue
                )
            }
        }
    }
}
