package com.somphie.staff.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.somphie.staff.core.network.TokenPair
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Guarda el par rotativo de tokens cifrado dentro de DataStore; la clave AES nunca sale de Keystore.
 * Un dato ilegible se informa como error para que la persona vuelva a autenticarse explícitamente.
 */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val json = Json
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile(STORE_FILE) },
    )

    val tokens: Flow<TokenPair?> = dataStore.data.map { preferences ->
        preferences[ENCRYPTED_TOKENS]?.let(::decrypt)
    }

    suspend fun current(): TokenPair? = tokens.first()

    suspend fun save(tokens: TokenPair) {
        val encrypted = withContext(Dispatchers.IO) {
            val plainText = json.encodeToString(TokenPair.serializer(), tokens)
            encrypt(plainText.toByteArray(Charsets.UTF_8))
        }
        dataStore.edit { it[ENCRYPTED_TOKENS] = encrypted }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(ENCRYPTED_TOKENS) }
    }

    private fun encrypt(plainText: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val encrypted = cipher.doFinal(plainText)
        val packet = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
        return Base64.encodeToString(packet, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): TokenPair {
        try {
            val packet = Base64.decode(encoded, Base64.NO_WRAP)
            require(packet.size > IV_LENGTH) { "Encrypted session is incomplete" }
            val buffer = ByteBuffer.wrap(packet)
            val iv = ByteArray(IV_LENGTH).also(buffer::get)
            val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(TAG_BITS, iv))
            val plainText = cipher.doFinal(encrypted).toString(Charsets.UTF_8)
            return json.decodeFromString(TokenPair.serializer(), plainText)
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("No se pudieron descifrar las credenciales guardadas", exception)
        } catch (exception: IllegalArgumentException) {
            throw IllegalStateException("Las credenciales guardadas están dañadas", exception)
        } catch (exception: SerializationException) {
            throw IllegalStateException("Las credenciales guardadas no tienen un formato válido", exception)
        }
    }

    private fun encryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val STORE_FILE = "staff_credentials.preferences_pb"
        const val KEY_ALIAS = "somphie.staff.credentials"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
        val ENCRYPTED_TOKENS = stringPreferencesKey("encrypted_tokens")
    }
}
