package org.welbodipartnership.cradle5.data.cryptography

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.welbodipartnership.cradle5.util.coroutines.AppCoroutineDispatchers
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import kotlin.reflect.KClass

class KeyStoreWrapper @Inject constructor(private val dispatchers: AppCoroutineDispatchers) {
  private val keystoreChangeMutex = Mutex()
  private var keyStore: KeyStore? = null

  @Throws(GeneralSecurityException::class)
  suspend fun hasKeyStoreEntry(): Boolean = withKeyStoreDelayedRetry({ getKeyRetrySpec }) { ks ->
    ks.containsAlias(KEY_ALIAS)
  }

  @Throws(GeneralSecurityException::class)
  suspend fun getKeyStoreEntry(): SecretKey = withKeyStoreDelayedRetry({ getKeyRetrySpec }) { ks ->
    val entry = ks.getEntry(KEY_ALIAS, null)
    (entry as? KeyStore.SecretKeyEntry)
      ?.secretKey
      ?: throw GeneralSecurityException(
        "failed to cast secret key to KeyStore.SecretKeyEntry; " +
          "actual class: ${entry::class.simpleName}"
      )
  }

  @RequiresApi(Build.VERSION_CODES.M)
  suspend fun encrypt(plaintext: Plaintext): AesGcmCiphertext {
    val secretKey = getOrCreateKeyStoreEntry()
    return withKeyStoreDelayedRetry({ encryptDecryptRetrySpec }) {
      AesGcmCiphertext.encrypt(secretKey, plaintext)
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  suspend fun decrypt(ciphertext: AesGcmCiphertext): Plaintext {
    val secretKey = getOrCreateKeyStoreEntry()
    return withKeyStoreDelayedRetry({ encryptDecryptRetrySpec }) {
      ciphertext.decrypt(secretKey)
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  private suspend fun getOrCreateKeyStoreEntry(): SecretKey =
    if (hasKeyStoreEntry()) {
      getKeyStoreEntry()
    } else {
      createKeyStoreEntry()
    }

  @Throws(GeneralSecurityException::class)
  private suspend fun getKeystoreOrThrow(forceReload: Boolean = false): KeyStore {
    suspend fun reloadKeystoreOrThrow(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
      ?.apply {
        try {
          runInterruptible(dispatchers.io) { load(null) }
        } catch (e: IOException) {
          throw GeneralSecurityException(e)
        }
      }
      ?: throw GeneralSecurityException("failed to get $ANDROID_KEY_STORE")

    if (forceReload) {
      return keystoreChangeMutex.withLock { reloadKeystoreOrThrow() }
    }

    return keyStore ?: keystoreChangeMutex.withLock {
      keyStore ?: reloadKeystoreOrThrow()
    }
  }

  private suspend inline fun <T> withKeyStoreDelayedRetry(
    retrySpecProvider: () -> RetrySpec,
    blockToRetry: (keystore: KeyStore) -> T
  ): T {
    val ks = getKeystoreOrThrow()
    return try {
      blockToRetry(ks)
    } catch (e: Exception) {
      val retrySpec = retrySpecProvider()
      if (e::class !in retrySpec.exceptionClassesForRetry) {
        throw e
      }

      val delayTime = if (retrySpec.useAsRandomTime) {
        (retrySpec.retryTimeMillis * Math.random()).toLong()
      } else {
        retrySpec.retryTimeMillis
      }
      Log.w(TAG, "KeyStore error encountered; retrying after $delayTime ms", e)
      delay(delayTime)
      val newKs = if (retrySpec.shouldReloadKeystore) {
        getKeystoreOrThrow(forceReload = true)
      } else {
        ks
      }
      blockToRetry(newKs)
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  @Throws(GeneralSecurityException::class)
  private fun createKeyStoreEntry(): SecretKey {
    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
    val keyGenParameterSpec = KeyGenParameterSpec.Builder(
      KEY_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    ).apply {
      setKeySize(256)
      setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        setIsStrongBoxBacked(true)
      }
    }.build()

    keyGenerator.init(keyGenParameterSpec)
    return keyGenerator.generateKey() ?: throw GeneralSecurityException("failed to generate a key")
  }

  private class RetrySpec(
    val exceptionClassesForRetry: List<KClass<*>>,
    val retryTimeMillis: Long,
    val useAsRandomTime: Boolean,
    val shouldReloadKeystore: Boolean,
  )

  companion object {
    private const val TAG = "KeyStoreWrapper"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "Cradle5Key"

    /**
     * https://github.com/google/tink/blob/e231fe638eb1f1ed8f1d45cc665ee98736c781d6/java_src/src/main/java/com/google/crypto/tink/integration/android/AndroidKeystoreKmsClient.java#L180
     * https://github.com/signalapp/Signal-Android/blob/d74e9f74103ad76eb7b5378e06fb789e7b365767/app/src/main/java/org/thoughtcrime/securesms/crypto/KeyStoreHelper.java#L108
     */
    private val getKeyRetrySpec by lazy {
      RetrySpec(
        listOf(
          NullPointerException::class,
          UnrecoverableKeyException::class
        ),
        retryTimeMillis = 30,
        useAsRandomTime = false,
        shouldReloadKeystore = true,
      )
    }

    /**
     * https://github.com/google/tink/commit/a056e3d1a9b3d76d05e7e222b0a6ce7b4b5c0087#diff-a269fe9d8a63e9e8667dbab5df7b1c7e08da7c4e144d0bfcd5a5c303031a95d9
     *
     * > Android KeyStore has a hard limitation of 15 key operations that can be in flight at any
     * > given time (4 for Strongbox). If this limit is exceeded the least recently key usage
     * > operation gets pruned. This does not affect key generation or attestation though.
     *
     * > When encrypting or decrypting with a KeyStore key, if Tink encountered an error, it'd
     * > retry one more time. We can't retry more than once because we can't tell why KeyStore
     * > failed -- if the error is not transient, retrying multiple times might lengthen the
     * > user's wait time.
     */
    private val encryptDecryptRetrySpec by lazy {
      RetrySpec(
        listOf(
          ProviderException::class,
          GeneralSecurityException::class
        ),
        retryTimeMillis = 100,
        useAsRandomTime = true,
        shouldReloadKeystore = false,
      )
    }
  }
}
