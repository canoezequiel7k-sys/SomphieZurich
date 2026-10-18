package com.somphie.staff.core.network

import com.somphie.staff.BuildConfig
import com.somphie.staff.core.storage.EncryptedTokenStore
import com.somphie.staff.feature.stations.data.PreparationDataSource
import com.somphie.staff.feature.stations.data.PreparationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object StaffNetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @Named("refreshApi")
    fun provideRefreshApi(json: Json): StaffApi =
        retrofit(json, OkHttpClient()).create(StaffApi::class.java)

    @Provides
    @Singleton
    fun provideAccessTokenInterceptor(tokens: EncryptedTokenStore): Interceptor =
        Interceptor { chain ->
            val token = runBlocking { tokens.tokens.first() }?.accessToken
            val request = chain.request().newBuilder().apply {
                if (token != null) header("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokens: EncryptedTokenStore,
        @Named("refreshApi") refreshApi: StaffApi,
    ): Authenticator = RefreshTokenAuthenticator(tokens, refreshApi)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        accessTokenInterceptor: Interceptor,
        authenticator: Authenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(accessTokenInterceptor)
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    fun provideStaffApi(json: Json, client: OkHttpClient): StaffApi =
        retrofit(json, client).create(StaffApi::class.java)

    @Provides
    @Singleton
    fun providePreparationDataSource(repository: PreparationRepository): PreparationDataSource = repository

    private fun retrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}

private class RefreshTokenAuthenticator(
    private val tokens: EncryptedTokenStore,
    private val refreshApi: StaffApi,
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) return null

        // El lock evita gastar dos refresh tokens rotativos ante 401 simultáneos.
        synchronized(refreshLock) {
            val storedTokens = runBlocking { tokens.current() } ?: return null
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (storedTokens.accessToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${storedTokens.accessToken}")
                    .build()
            }

            val refreshResponse = try {
                refreshApi.refresh(RefreshRequest(storedTokens.refreshToken)).execute()
            } catch (_: IOException) {
                // La red puede volver; conservar el refresh permite reintentar sin perder la sesión.
                return null
            }

            if (refreshResponse.isSuccessful) {
                val freshTokens = refreshResponse.body() ?: return null
                runBlocking { tokens.save(freshTokens) }
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${freshTokens.accessToken}")
                    .build()
            }

            refreshResponse.errorBody()?.close()
            if (refreshResponse.code() == HTTP_UNAUTHORIZED) {
                runBlocking { tokens.clear() }
            }
            return null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_AUTH_ATTEMPTS = 2
        const val HTTP_UNAUTHORIZED = 401
    }
}
