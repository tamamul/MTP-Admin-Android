package com.marsa.mtpadmin

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

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

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

data class ReportSummary(
    val id: Int,
    val report_number: String?,
    val reporter_name: String?,
    val reporter_role: String?,
    val reporter_unit: String?,
    val title: String?,
    val category: String?,
    val priority: String?,
    val status: String?,
    val stage: Int,
    val is_private: Boolean,
    val assigned_to: Int?,
    val created_at: String?,
    val updated_at: String?
)

data class Pagination<T>(
    val current_page: Int,
    val data: List<T>,
    val last_page: Int,
    val per_page: Int,
    val total: Int
)

data class ReportEvent(
    val id: Int,
    val type: String?,
    val title: String?,
    val visibility: String?,
    val from_status: String?,
    val to_status: String?,
    val from_stage: Int?,
    val to_stage: Int?,
    val note: String?,
    val actor: String?,
    val created_at: String?
)

data class ReportAttachment(
    val id: Int,
    val name: String?,
    val mime_type: String?,
    val size_bytes: Long?,
    val created_at: String?
)

data class ReportDetail(
    val id: Int,
    val report_number: String?,
    val reporter_name: String?,
    val reporter_role: String?,
    val reporter_unit: String?,
    val title: String?,
    val category: String?,
    val priority: String?,
    val status: String?,
    val stage: Int,
    val is_private: Boolean,
    val assigned_to: Int?,
    val created_at: String?,
    val updated_at: String?,
    val description: String?,
    val resolved_at: String?,
    val events: List<ReportEvent> = emptyList(),
    val attachments: List<ReportAttachment> = emptyList()
)

data class Technician(
    val id: Int,
    val name: String,
    val role: String?,
    val unit: String?
)

data class AssignmentRequest(
    val technician_id: Int?
)

data class StatusRequest(
    val status: String,
    val stage: Int,
    val note: String? = null,
    val internal_note: String? = null
)

data class AssignmentResult(
    val report_id: Int,
    val report_number: String?,
    val assigned_to: Int?,
    val technician: String?
)

interface MtpApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("auth/me")
    suspend fun me(): Map<String, Any?>

    @POST("auth/logout")
    suspend fun logout(): Map<String, Any?>

    @GET("admin/reports")
    suspend fun reports(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): ApiResponse<Pagination<ReportSummary>>

    @GET("admin/reports/{id}")
    suspend fun reportDetail(
        @Path("id") id: Int
    ): ApiResponse<ReportDetail>

    @PATCH("admin/reports/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: Int,
        @Body request: StatusRequest
    ): ApiResponse<ReportSummary>

    @PATCH("admin/reports/{id}/assignment")
    suspend fun assignTechnician(
        @Path("id") id: Int,
        @Body request: AssignmentRequest
    ): ApiResponse<AssignmentResult>

    @GET("admin/technicians")
    suspend fun technicians(): ApiResponse<List<Technician>>
}

object ApiClient {
    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    val service: MtpApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val token = appContext?.let {
                    SessionManager.token(it)
                }

                val request = chain.request().newBuilder().apply {
                    if (!token.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                    addHeader("Accept", "application/json")
                }.build()

                chain.proceed(request)
            })
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MtpApiService::class.java)
    }
}

object SessionManager {
    private const val PREFS = "mtp_admin_session"
    private const val TOKEN = "access_token"
    private const val NAME = "user_name"

    fun save(context: Context, token: String, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN, token)
            .putString(NAME, name)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return !token(context).isNullOrBlank()
    }

    fun token(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN, null)
    }

    fun name(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(NAME, null)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
