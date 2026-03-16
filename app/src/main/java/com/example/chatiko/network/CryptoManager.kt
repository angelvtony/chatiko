package com.example.chatiko.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

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

        val publicKey = getPublicKey()

        return Base64.encodeToString(
            publicKey.encoded,
            Base64.DEFAULT
        )
    }
}