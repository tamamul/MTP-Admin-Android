package com.marsa.mtpadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import org.json.JSONObject
import com.marsa.mtpadmin.ui.LoginScreen
import com.marsa.mtpadmin.ui.DashboardScreen
import com.marsa.mtpadmin.ui.theme.MtpAdminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
ApiClient.initialize(applicationContext)
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
        mutableStateOf(SessionManager.token(context) != null)
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
                    themePrefs.edit()
                        .putBoolean(KEY_DARK_THEME, enabled)
                        .apply()
                },
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
