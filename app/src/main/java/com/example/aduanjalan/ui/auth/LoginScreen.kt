package com.example.aduanjalan.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.aduanjalan.ui.theme.PrimaryColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val viewModel: AuthViewModel = hiltViewModel()
    val authState = viewModel.authState
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isDarkTheme = isSystemInDarkTheme()
    val formBackgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray
    val dividerColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.LightGray

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate("home") { popUpTo("login") { inclusive = true } }
                viewModel.resetAuthState()
            }
            is AuthState.Error -> {
                scope.launch { snackbarHostState.showSnackbar(authState.message) }
                viewModel.resetAuthState()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    fun validateInput(): Boolean {
        var isValid = true
        if (email.isBlank()) { emailError = "Email tidak boleh kosong"; isValid = false }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailError = "Format email tidak valid"; isValid = false }
        else { emailError = null }

        if (password.isBlank()) { passwordError = "Password tidak boleh kosong"; isValid = false }
        else { passwordError = null }
        return isValid
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Latar belakang utama diatur oleh Column di bawah
        containerColor = PrimaryColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PrimaryColor) // Latar belakang utama adalah primary
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            // Bagian Header (tidak berubah)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Mengambil bagian atas layar
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddRoad,
                    contentDescription = "Ikon Aplikasi",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aduan Jalan Rusak",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Kota Kendari",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light
                )
            }

            // --- PERBAIKAN UTAMA: AREA FORM DIBUNGKUS SURFACE ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f), // Mengambil sisa bagian bawah layar
                color = formBackgroundColor,
                // Inilah yang membuat sudut atasnya melengkung
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Masuk ke Akun Anda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = { Text("Email") },
                        isError = emailError != null,
                        supportingText = { emailError?.let { Text(it) } },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = null },
                        label = { Text("Password") },
                        isError = passwordError != null,
                        supportingText = { passwordError?.let { Text(it) } },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Icon") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Toggle Password Visibility")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (validateInput()) viewModel.login(email, password) })
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { if (validateInput()) viewModel.login(email, password) },
                        enabled = authState != AuthState.Loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (authState == AuthState.Loading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Masuk", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Divider(modifier = Modifier.weight(1f), color = dividerColor)
                        Text("  Atau  ", color = textColorSecondary)
                        Divider(modifier = Modifier.weight(1f), color = dividerColor)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { navController.navigate("register") },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Buat Akun", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}