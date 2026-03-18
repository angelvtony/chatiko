package com.example.chatiko.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val KEY_ALIAS = "chatiko_key"

    fun generateKeyPair() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) return

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            "AndroidKeyStore"
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()

        generator.initialize(spec)
        generator.generateKeyPair()
    }

    fun getPublicKey(): PublicKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val cert = keyStore.getCertificate(KEY_ALIAS)
        return cert.publicKey
    }

    fun getPrivateKey(): PrivateKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as PrivateKey
    }

    fun getPublicKeyString(): String {
        return Base64.encodeToString(getPublicKey().encoded, Base64.DEFAULT)
    }

    // -------------------------
    // HYBRID ENCRYPTION
    // -------------------------

    fun encryptMessageHybrid(message: String, receiverPublicKey: PublicKey): EncryptedPayload {
        // 1️⃣ Generate random AES key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val aesKey = keyGen.generateKey()

        // 2️⃣ Encrypt message with AES
        val cipherAES = Cipher.getInstance("AES/GCM/NoPadding")
        cipherAES.init(Cipher.ENCRYPT_MODE, aesKey)
        val iv = cipherAES.iv
        val encryptedMessage = cipherAES.doFinal(message.toByteArray())

        // 3️⃣ Encrypt AES key with receiver's RSA
        val cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipherRSA.init(Cipher.ENCRYPT_MODE, receiverPublicKey)
        val encryptedKey = cipherRSA.doFinal(aesKey.encoded)

        return EncryptedPayload(
            encryptedKey = Base64.encodeToString(encryptedKey, Base64.NO_WRAP),
            encryptedMessage = Base64.encodeToString(encryptedMessage, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decryptMessageHybrid(payload: EncryptedPayload): String {
        // 1️⃣ Decode
        val encryptedKeyBytes = Base64.decode(payload.encryptedKey, Base64.NO_WRAP)
        val encryptedMessageBytes = Base64.decode(payload.encryptedMessage, Base64.NO_WRAP)
        val ivBytes = Base64.decode(payload.iv, Base64.NO_WRAP)

        // 2️⃣ Decrypt AES key with RSA
        val cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipherRSA.init(Cipher.DECRYPT_MODE, getPrivateKey())
        val aesKeyBytes = cipherRSA.doFinal(encryptedKeyBytes)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // 3️⃣ Decrypt message with AES
        val cipherAES = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, ivBytes)
        cipherAES.init(Cipher.DECRYPT_MODE, aesKey, spec)
        val decryptedBytes = cipherAES.doFinal(encryptedMessageBytes)

        return String(decryptedBytes)
    }

    data class EncryptedPayload(
        val encryptedKey: String,
        val encryptedMessage: String,
        val iv: String
    )
}