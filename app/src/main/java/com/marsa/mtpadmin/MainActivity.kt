package com.marsa.mtpadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import org.json.JSONObject

private val Background = Color(0xFF0B1120)
private val SurfaceColor = Color(0xFF151F32)
private val Accent = Color(0xFF38BDF8)
private val Muted = Color(0xFF94A3B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MtpAdminApp()
        }
    }
}

@Composable
fun MtpAdminApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var loggedIn by remember {
        mutableStateOf(SessionManager.token(context) != null)
    }

    var userName by remember {
        mutableStateOf(SessionManager.name(context) ?: "Administrator")
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Background,
            surface = SurfaceColor
        )
    ) {
        if (loggedIn) {
            DashboardScreen(
                name = userName,
                onLogout = {
                    SessionManager.clear(context)
                    loggedIn = false
                    password = ""
                }
            )
        } else {
            LoginScreen(
                email = email,
                onEmailChange = {
                    email = it
                    errorMessage = ""
                },
                password = password,
                onPasswordChange = {
                    password = it
                    errorMessage = ""
                },
                showPassword = showPassword,
                onTogglePassword = {
                    showPassword = !showPassword
                },
                loading = loading,
                errorMessage = errorMessage,
                onLogin = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email dan password wajib diisi."
                    } else {
                        scope.launch {
                            loading = true
                            errorMessage = ""

                            try {
                                val result = ApiClient.service.login(
                                    LoginRequest(
                                        email = email.trim(),
                                        password = password
                                    )
                                )

                                val user = result.user
                                val token = result.access_token

                                if (user == null || token.isNullOrBlank()) {
                                    errorMessage = "Respons login dari server tidak lengkap."
                                } else if (user.account_role != "admin") {
                                    errorMessage = "Akun ini tidak memiliki akses administrator."
                                } else {
                                    SessionManager.save(
                                        context,
                                        token,
                                        user.name
                                    )

                                    userName = user.name
                                    loggedIn = true
                                }
                            } catch (e: HttpException) {
                                val body = e.response()?.errorBody()?.string()
                                errorMessage = try {
                                    JSONObject(body ?: "{}")
                                        .optString("message", "Login gagal.")
                                } catch (_: Exception) {
                                    "Login gagal. Periksa akun Anda."
                                }
                            } catch (_: IOException) {
                                errorMessage = "Tidak dapat terhubung ke server. Periksa internet."
                            } catch (_: Exception) {
                                errorMessage = "Terjadi kesalahan. Silakan coba lagi."
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    loading: Boolean,
    errorMessage: String,
    onLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        Accent.copy(alpha = 0.15f),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "M",
                    color = Accent,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "MTP ADMIN",
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Panel pengelolaan layanan MARSA",
                color = Muted,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        SurfaceColor,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp)
            ) {
                Text(
                    "Selamat datang",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "Masuk menggunakan akun administrator",
                    color = Muted,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, null)
                    },
                    trailingIcon = {
                        IconButton(onClick = onTogglePassword) {
                            Icon(
                                if (showPassword)
                                    Icons.Default.VisibilityOff
                                else
                                    Icons.Default.Visibility,
                                contentDescription = "Tampilkan password"
                            )
                        }
                    },
                    visualTransformation = if (showPassword)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))

                    Text(
                        errorMessage,
                        color = Color(0xFFFF8585),
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onLogin,
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Background
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "MASUK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            Text(
                "MTP ADMIN • MARSA SYSTEM",
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun DashboardScreen(
    name: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp)
    ) {
        Text(
            "MTP ADMIN",
            color = Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(30.dp))

        Text(
            "Halo, $name 👋",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Login administrator berhasil.",
            color = Muted,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceColor
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Dashboard",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Koneksi akun berhasil. Data laporan akan ditampilkan pada tahap berikutnya.",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("KELUAR")
        }
    }
}
