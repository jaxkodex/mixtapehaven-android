package pe.net.libre.mixtapehaven.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pe.net.libre.mixtapehaven.ui.components.FuturisticTextField
import pe.net.libre.mixtapehaven.ui.components.NeonButton
import pe.net.libre.mixtapehaven.ui.theme.CyberNeonBlue
import pe.net.libre.mixtapehaven.ui.theme.LunarWhite
import pe.net.libre.mixtapehaven.ui.theme.MixtapeHavenTheme
import pe.net.libre.mixtapehaven.ui.theme.VaporwaveMagenta

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToTroubleshoot: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isConnectionSuccessful) {
        if (uiState.isConnectionSuccessful) {
            onNavigateToHome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo and Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Album,
                contentDescription = "Mixtape Haven Logo",
                tint = CyberNeonBlue,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = "Mixtape Haven",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = LunarWhite
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "Connect to Your Server",
            fontSize = 18.sp,
            fontFamily = FontFamily.SansSerif,
            color = LunarWhite.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Server URL Field
        Text(
            text = "Server URL",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = LunarWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        FuturisticTextField(
            value = uiState.serverUrl,
            onValueChange = { viewModel.updateServerUrl(it) },
            label = "Server URL",
            placeholder = "https://jellyfin.example.com",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Username Field
        Text(
            text = "Username",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = LunarWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        FuturisticTextField(
            value = uiState.username,
            onValueChange = { viewModel.updateUsername(it) },
            label = "Username",
            placeholder = "Enter your username",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Password Field
        Text(
            text = "Password",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = LunarWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        FuturisticTextField(
            value = uiState.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = "Password",
            placeholder = "Enter your password",
            visualTransformation = if (uiState.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.connect()
                }
            ),
            trailingIcon = {
                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                    Icon(
                        imageVector = if (uiState.isPasswordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (uiState.isPasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                        tint = LunarWhite
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Error Message
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            uiState.errorMessage?.let { error ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error,
                        color = VaporwaveMagenta,
                        fontSize = 14.sp,
                        fontFamily = if (error.contains("Technical Details:")) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.SansSerif
                        },
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Connect Button
        NeonButton(
            text = "Connect",
            onClick = { viewModel.connect() },
            isLoading = uiState.isLoading,
            enabled = uiState.serverUrl.isNotBlank() &&
                     uiState.username.isNotBlank() &&
                     uiState.password.isNotBlank()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Troubleshoot Link
        TextButton(onClick = onNavigateToTroubleshoot) {
            Text(
                text = "Troubleshoot Connection",
                color = CyberNeonBlue,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Privacy Message
        Text(
            text = "Your connection data is stored locally on this device\nand is never shared.",
            color = LunarWhite.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// Preview with mock ViewModel
@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Onboarding Screen - Empty"
)
@Composable
fun OnboardingScreenPreview() {
    MixtapeHavenTheme {
        OnboardingScreen(
            viewModel = PreviewOnboardingViewModel(
                previewState = OnboardingUiState()
            ),
            onNavigateToHome = {},
            onNavigateToTroubleshoot = {}
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Onboarding Screen - Filled"
)
@Composable
fun OnboardingScreenFilledPreview() {
    MixtapeHavenTheme {
        OnboardingScreen(
            viewModel = PreviewOnboardingViewModel(
                previewState = OnboardingUiState(
                    serverUrl = "https://jellyfin.example.com",
                    username = "john_doe",
                    password = "password123"
                )
            ),
            onNavigateToHome = {},
            onNavigateToTroubleshoot = {}
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Onboarding Screen - Loading"
)
@Composable
fun OnboardingScreenLoadingPreview() {
    MixtapeHavenTheme {
        OnboardingScreen(
            viewModel = PreviewOnboardingViewModel(
                previewState = OnboardingUiState(
                    serverUrl = "https://jellyfin.example.com",
                    username = "john_doe",
                    password = "password123",
                    isLoading = true
                )
            ),
            onNavigateToHome = {},
            onNavigateToTroubleshoot = {}
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Onboarding Screen - Error"
)
@Composable
fun OnboardingScreenErrorPreview() {
    MixtapeHavenTheme {
        OnboardingScreen(
            viewModel = PreviewOnboardingViewModel(
                previewState = OnboardingUiState(
                    serverUrl = "https://jellyfin.example.com",
                    username = "john_doe",
                    password = "password123",
                    errorMessage = "Invalid server URL format"
                )
            ),
            onNavigateToHome = {},
            onNavigateToTroubleshoot = {}
        )
    }
}

// Mock ViewModel interface for Previews
private class PreviewOnboardingViewModel(
    private val previewState: OnboardingUiState
) {
    val uiState: StateFlow<OnboardingUiState> = MutableStateFlow(previewState)

    fun updateServerUrl(url: String) {}
    fun updateUsername(username: String) {}
    fun updatePassword(password: String) {}
    fun togglePasswordVisibility() {}
    fun connect() {}
    fun clearError() {}
}

// Extension function to make preview work with actual composable
@Composable
private fun OnboardingScreen(
    viewModel: PreviewOnboardingViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToTroubleshoot: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isConnectionSuccessful) {
        if (uiState.isConnectionSuccessful) {
            onNavigateToHome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo and Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Album,
                contentDescription = "Mixtape Haven Logo",
                tint = CyberNeonBlue,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = "Mixtape Haven",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = LunarWhite
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "Connect to Your Server",
            fontSize = 18.sp,
            fontFamily = FontFamily.SansSerif,
            color = LunarWhite.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Server URL Field
        Text(
            text = "Server URL",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = LunarWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        FuturisticTextField(
            value = uiState.serverUrl,
            onValueChange = { viewModel.updateServerUrl(it) },
            label = "Server URL",
            placeholder = "https://jellyfin.example.com",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Username Field
        Text(
            text = "Username",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = LunarWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        FuturisticTextField(
            value = uiState.username,
            onValueChange = { viewModel.updateUsername(it) },
            label = "Username",
            placeholder = "Enter your username",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Password Field
        Text(
            text = "Password",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = LunarWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        FuturisticTextField(
            value = uiState.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = "Password",
            placeholder = "Enter your password",
            visualTransformation = if (uiState.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.connect()
                }
            ),
            trailingIcon = {
                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                    Icon(
                        imageVector = if (uiState.isPasswordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (uiState.isPasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                        tint = LunarWhite
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Error Message
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            uiState.errorMessage?.let { error ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error,
                        color = VaporwaveMagenta,
                        fontSize = 14.sp,
                        fontFamily = if (error.contains("Technical Details:")) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.SansSerif
                        },
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Connect Button
        NeonButton(
            text = "Connect",
            onClick = { viewModel.connect() },
            isLoading = uiState.isLoading,
            enabled = uiState.serverUrl.isNotBlank() &&
                     uiState.username.isNotBlank() &&
                     uiState.password.isNotBlank()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Troubleshoot Link
        TextButton(onClick = onNavigateToTroubleshoot) {
            Text(
                text = "Troubleshoot Connection",
                color = CyberNeonBlue,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Privacy Message
        Text(
            text = "Your connection data is stored locally on this device\nand is never shared.",
            color = LunarWhite.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
