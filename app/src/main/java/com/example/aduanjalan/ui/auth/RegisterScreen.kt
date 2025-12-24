package com.example.aduanjalan.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
fun RegisterScreen(navController: NavHostController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
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
                scope.launch {
                    snackbarHostState.showSnackbar("Pendaftaran berhasil! Silakan login.")
                }
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
                viewModel.resetAuthState()
            }
            is AuthState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(authState.message)
                }
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }

    fun validateInput(): Boolean {
        var isValid = true
        if (name.isBlank()) { nameError = "Nama tidak boleh kosong"; isValid = false } else { nameError = null }
        if (email.isBlank()) { emailError = "Email tidak boleh kosong"; isValid = false }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailError = "Format email tidak valid"; isValid = false }
        else { emailError = null }
        if (password.length < 8) { passwordError = "Password minimal 8 karakter"; isValid = false }
        else { passwordError = null }
        return isValid
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PrimaryColor // Latar belakang utama adalah primary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PrimaryColor) // Pastikan Column juga memiliki background
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            // Bagian Header (tidak berubah)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Mengambil bagian atas
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Ikon Registrasi",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Buat Akun Baru",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Satu langkah untuk ikut berkontribusi",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        label = { Text("Nama Lengkap") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Nama") },
                        isError = nameError != null,
                        supportingText = { nameError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                        isError = emailError != null,
                        supportingText = { emailError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = null },
                        label = { Text("Password") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Toggle Password")
                            }
                        },
                        isError = passwordError != null,
                        supportingText = { passwordError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (validateInput()) viewModel.register(name, email, password) })
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { if (validateInput()) viewModel.register(name, email, password) },
                        enabled = authState != AuthState.Loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (authState == AuthState.Loading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Daftar", fontSize = 16.sp)
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
                        onClick = { navController.navigate("login") },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Masuk", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}