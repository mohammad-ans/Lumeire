package com.lustre.app

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit


object ApiClient {
    private const val BASE_URL = "https://yappyyap.xyz:443/lustre/"

    private lateinit var tokenManager: TokenManager

    fun init(context: Context) {
        tokenManager = TokenManager(context.applicationContext)
    }

    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking { tokenManager.getToken() }
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        }
        else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder().addInterceptor(authInterceptor).build()

    private val json = Json { ignoreUnknownKeys = true }
    private val retrofit = Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()

    val authService: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val apiService: ApiService by lazy {retrofit.create(ApiService::class.java)}
    val bookingApiService: BookingApiService by lazy { retrofit.create(BookingApiService::class.java) }
    val voucherService: VoucherApiService by lazy { retrofit.create(VoucherApiService::class.java) }
    suspend fun saveToken(token: String) = tokenManager.saveToken(token)
    suspend fun clearToken() = tokenManager.clearToken()
    suspend fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun resolve(path: String?): String?{
        if(path == null)
            return path
        if(path.startsWith("https://") || path.startsWith("https://"))
            return path
        return BASE_URL.trimEnd('/') + '/' + path.trimStart('/')
    }
}