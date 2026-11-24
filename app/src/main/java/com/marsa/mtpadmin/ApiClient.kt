package com.marsa.mtpadmin

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

private const val BASE_URL = "https://api-mtp.marsa9.com/api/v1/"

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginUser(
    val id: Int,
    val name: String,
    val email: String,
    val account_role: String,
    val role: String?,
    val unit: String?
)

data class LoginResponse(
    val message: String,
    val token_type: String?,
    val access_token: String?,
    val expires_in_days: Int?,
    val user: LoginUser?
)

interface MtpApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse
}

object ApiClient {
    val service: MtpApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MtpApiService::class.java)
    }
}

object SessionManager {
    private const val PREFS = "mtp_admin_session"
    private const val TOKEN = "access_token"
    private const val NAME = "user_name"

    fun save(context: android.content.Context, token: String, name: String) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN, token)
            .putString(NAME, name)
            .apply()
    }

    fun token(context: android.content.Context): String? {
        return context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getString(TOKEN, null)
    }

    fun name(context: android.content.Context): String? {
        return context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getString(NAME, null)
    }

    fun clear(context: android.content.Context) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
