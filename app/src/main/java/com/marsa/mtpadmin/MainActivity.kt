package com.marsa.mtpadmin

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.marsa.mtpadmin.ApiClient
import com.marsa.mtpadmin.LoginRequest
import com.marsa.mtpadmin.SessionManager
import com.marsa.mtpadmin.ui.dashboard.DashboardScreen
import com.marsa.mtpadmin.ui.login.LoginScreen
import com.marsa.mtpadmin.ui.theme.MtpAdminTheme
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ApiClient.initialize(applicationContext)

        // Izin notifikasi Android 13+
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        // Ambil dan simpan FCM Token
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                getSharedPreferences("mtp_admin_session", MODE_PRIVATE)
                    .edit()
                    .putString("fcm_token", token)
                    .apply()
            }
        setContent {
            MtpAdminApp()
        }
    }
}

private const val PREFS_THEME = "mtp_admin_settings"
private const val KEY_DARK_THEME = "dark_theme"

@Composable
fun MtpAdminApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themePrefs = remember {
        context.getSharedPreferences(PREFS_THEME, 0)
    }

    var darkTheme by remember {
        mutableStateOf(themePrefs.getBoolean(KEY_DARK_THEME, true))
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var loggedIn by remember {
        mutableStateOf(SessionManager.isLoggedIn(context))
    }
    var userName by remember {
        mutableStateOf(SessionManager.name(context) ?: "Administrator")
    }

    MtpAdminTheme(darkTheme = darkTheme) {
        if (loggedIn) {
            DashboardScreen(
                name = userName,
                darkTheme = darkTheme,
                onThemeChange = { enabled ->
                    darkTheme = enabled
                    themePrefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
                },
                onLogout = {
                    SessionManager.clear(context)
                    loggedIn = false
                    password = ""
                    errorMessage = ""
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
                onTogglePassword = { showPassword = !showPassword },
                loading = loading,
                errorMessage = errorMessage,
                onLogin = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email dan password wajib diisi."
                        return@LoginScreen
                    }
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

                            when {
                                user == null || token.isNullOrBlank() -> {
                                    errorMessage = "Respons login dari server tidak lengkap."
                                }
                                user.account_role != "admin" -> {
                                    errorMessage = "Akun ini tidak memiliki akses administrator."
                                }
                                else -> {
                                    SessionManager.save(context, token, user.name)
                                    userName = user.name
                                    loggedIn = true
                                }
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
            )
        }
    }
}
