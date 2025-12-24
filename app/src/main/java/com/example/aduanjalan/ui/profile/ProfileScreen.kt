package com.example.aduanjalan.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.aduanjalan.ui.component.BottomBar
import com.example.aduanjalan.ui.theme.PrimaryColor
import kotlinx.coroutines.launch

// Sealed class tidak berubah
private sealed class EditSheetState {
    object Hidden : EditSheetState()
    data class Name(val title: String) : EditSheetState()
    data class Telephone(val title: String) : EditSheetState()
    data class Address(val title: String) : EditSheetState()
    data class Gender(val title: String) : EditSheetState()
    data class Password(val title: String) : EditSheetState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- LANGKAH 1: DEFINISIKAN PALET WARNA DINAMIS ---
    val isDarkTheme = isSystemInDarkTheme()
    val scaffoldBackgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFFAFAFA)
    val bottomSheetColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray
    val imagePlaceholderBgColor = if (isDarkTheme) Color.DarkGray else Color.LightGray

    // State tidak berubah
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    var currentSheetState by remember { mutableStateOf<EditSheetState>(EditSheetState.Hidden) }
    var editingValue by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }

    // LaunchedEffect tidak berubah
    LaunchedEffect(uiState.user) {
        uiState.user?.let {
            name = it.name ?: ""
            email = it.email ?: ""
            telephone = it.telephoneNumber ?: ""
            address = it.address ?: ""
            gender = it.gender ?: ""
            imageUrl = it.photo
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { imageUri = it } }
    )

    Scaffold(
        // --- LANGKAH 2: TERAPKAN WARNA LATAR BELAKANG ---
        containerColor = scaffoldBackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
            )
        },
        bottomBar = { BottomBar(navController = navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Foto Profil
                Box(contentAlignment = Alignment.BottomEnd) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = imageUri ?: (if (imageUrl != null) "https://aduanjalanapi.bemftuho.id/storage/$imageUrl" else null),
                            error = rememberVectorPainter(image = Icons.Default.AccountCircle),
                            placeholder = rememberVectorPainter(image = Icons.Default.AccountCircle)
                        ),
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(2.dp, PrimaryColor, CircleShape)
                            // Terapkan warna placeholder dinamis
                            .background(imagePlaceholderBgColor),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Foto",
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryColor)
                            .clickable { galleryLauncher.launch("image/*") }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Terapkan warna teks dinamis
                Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColorPrimary)
                Text(email, fontSize = 16.sp, color = textColorSecondary)

                Spacer(modifier = Modifier.height(24.dp))

                // Daftar Item Profil
                ProfileItem(icon = Icons.Default.Person, label = "Nama Lengkap", value = name) {
                    editingValue = name
                    currentSheetState = EditSheetState.Name("Ubah Nama Lengkap")
                }
                ProfileItem(icon = Icons.Default.Email, label = "Email", value = email, isEditable = false)
                ProfileItem(icon = Icons.Default.Phone, label = "Nomor Telepon", value = telephone) {
                    editingValue = telephone
                    currentSheetState = EditSheetState.Telephone("Ubah Nomor Telepon")
                }
                ProfileItem(icon = Icons.Default.Home, label = "Alamat", value = address) {
                    editingValue = address
                    currentSheetState = EditSheetState.Address("Ubah Alamat")
                }
                ProfileItem(icon = Icons.Default.Wc, label = "Jenis Kelamin", value = gender) {
                    currentSheetState = EditSheetState.Gender("Pilih Jenis Kelamin")
                }
                ProfileItem(icon = Icons.Default.Lock, label = "Ubah Password", value = "********") {
                    editingValue = ""
                    passwordConfirmation = ""
                    currentSheetState = EditSheetState.Password("Ubah Password")
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // Modal Bottom Sheet Logic
    if (currentSheetState != EditSheetState.Hidden) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { currentSheetState = EditSheetState.Hidden },
            sheetState = sheetState,
            // --- LANGKAH 3: TERAPKAN WARNA BOTTOM SHEET ---
            containerColor = bottomSheetColor
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val title = when (val state = currentSheetState) {
                    is EditSheetState.Name -> state.title
                    is EditSheetState.Telephone -> state.title
                    is EditSheetState.Address -> state.title
                    is EditSheetState.Gender -> state.title
                    is EditSheetState.Password -> state.title
                    else -> ""
                }
                // Terapkan warna teks dinamis pada judul sheet
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColorPrimary)
                Spacer(modifier = Modifier.height(24.dp))

                // Konten dinamis tidak perlu diubah, komponen Material 3 sudah beradaptasi
                when (currentSheetState) {
                    is EditSheetState.Name -> {
                        OutlinedTextField(value = editingValue, onValueChange = { editingValue = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
                    }
                    is EditSheetState.Telephone -> {
                        OutlinedTextField(value = editingValue, onValueChange = { editingValue = it }, label = { Text("Nomor Telepon") }, modifier = Modifier.fillMaxWidth())
                    }
                    is EditSheetState.Address -> {
                        OutlinedTextField(value = editingValue, onValueChange = { editingValue = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
                    }
                    is EditSheetState.Gender -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { viewModel.updateProfile(name, email, telephone, address, "Laki-laki", imageUri, ""); currentSheetState = EditSheetState.Hidden }, modifier = Modifier.weight(1f)) { Text("Laki-laki") }
                            Button(onClick = { viewModel.updateProfile(name, email, telephone, address, "Perempuan", imageUri, ""); currentSheetState = EditSheetState.Hidden }, modifier = Modifier.weight(1f)) { Text("Perempuan") }
                        }
                    }
                    is EditSheetState.Password -> {
                        OutlinedTextField(value = editingValue, onValueChange = { editingValue = it }, label = { Text("Password Baru") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = passwordConfirmation, onValueChange = { passwordConfirmation = it }, label = { Text("Konfirmasi Password Baru") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (currentSheetState !is EditSheetState.Gender) {
                    Button(
                        onClick = {
                            var passToUpdate = ""
                            if (currentSheetState is EditSheetState.Password) {
                                if (editingValue.length < 8) {
                                    scope.launch { snackbarHostState.showSnackbar("Password minimal 8 karakter") }; return@Button
                                }
                                if (editingValue != passwordConfirmation) {
                                    scope.launch { snackbarHostState.showSnackbar("Konfirmasi password tidak cocok") }; return@Button
                                }
                                passToUpdate = editingValue
                            }
                            viewModel.updateProfile(
                                name = if (currentSheetState is EditSheetState.Name) editingValue else name,
                                email = email,
                                telephone = if (currentSheetState is EditSheetState.Telephone) editingValue else telephone,
                                address = if (currentSheetState is EditSheetState.Address) editingValue else address,
                                gender = gender,
                                imageUri = imageUri,
                                password = passToUpdate
                            )
                            currentSheetState = EditSheetState.Hidden
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

// --- LANGKAH 4: PERBARUI PROFILEITEM AGAR MENGGUNAKAN WARNA DINAMIS ---
@Composable
fun ProfileItem(
    icon: ImageVector,
    label: String,
    value: String,
    isEditable: Boolean = true,
    onClick: () -> Unit = {}
) {
    // Deteksi tema di dalam komponen ini juga
    val isDarkTheme = isSystemInDarkTheme()
    val textColorPrimary = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black
    val textColorSecondary = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else Color.Gray
    val dividerColor = if (isDarkTheme) Color.Gray.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.3f)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isEditable) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = PrimaryColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Gunakan warna dinamis
                Text(label, fontSize = 12.sp, color = textColorSecondary)
                Text(
                    if (value.isBlank()) "-" else value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColorPrimary
                )
            }
            if (isEditable) {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Edit", tint = textColorSecondary)
            }
        }
        Divider(color = dividerColor, modifier = Modifier.padding(horizontal = 16.dp))
    }
}