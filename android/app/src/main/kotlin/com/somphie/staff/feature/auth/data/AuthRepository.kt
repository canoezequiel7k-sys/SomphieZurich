package com.somphie.staff.feature.auth.data

import com.somphie.staff.core.network.ApiFailure
import com.somphie.staff.core.network.ChangePasswordRequest
import com.somphie.staff.core.network.LoginRequest
import com.somphie.staff.core.network.StaffApi
import com.somphie.staff.core.network.StaffProfile
import com.somphie.staff.core.network.TokenPair
import com.somphie.staff.core.network.apiCall
import com.somphie.staff.core.storage.EncryptedTokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Adapta los contratos de autenticación y mantiene los tokens fuera del estado de las pantallas. */
@Singleton
class AuthRepository @Inject constructor(
    private val api: StaffApi,
    private val tokens: EncryptedTokenStore,
    private val json: Json,
) {
    suspend fun signIn(email: String, password: String) {
        val result = apiCall(json) { api.login(LoginRequest(email.trim(), password)) }
        tokens.save(TokenPair(result.accessToken, result.refreshToken, result.expiresInSec))
    }

    suspend fun currentProfile(): StaffProfile = apiCall(json) { api.me() }

    suspend fun changePassword(current: String, new: String) {
        apiCall(json) {
            val response = api.changePassword(ChangePasswordRequest(current, new))
            if (!response.isSuccessful) throw HttpException(response)
        }
    }

    suspend fun signOut() {
        val current = tokens.tokens.first()
        try {
            if (current != null) {
                val response = api.logout(com.somphie.staff.core.network.RefreshRequest(current.refreshToken))
                if (!response.isSuccessful) throw HttpException(response)
            }
        } finally {
            // El cierre local no depende de que la red consiga revocar el refresh en el servidor.
            tokens.clear()
        }
    }
}
