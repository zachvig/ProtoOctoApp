package de.crysxd.octoapp.base

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import java.io.File
import java.security.KeyStore
import java.security.cert.Certificate
import java.util.*

class SslKeyStoreHandler(private val context: Context) {

    private val password = context.packageName.toCharArray()
    private val keyStoreFile = File(context.filesDir, "ssl_certs.ks")
    private val sharedPreferences = context.getSharedPreferences("ssl_handler", Context.MODE_PRIVATE)

    fun storeCertificates(certs: List<Certificate>) {
        val ks = loadKeyStore() ?: createKeyStore().also { it.load(null) }
        certs.forEach {
            ks.setCertificateEntry(UUID.randomUUID().toString(), it)
        }
        keyStoreFile.outputStream().use {
            ks.store(it, password)
        }
    }

    fun enforceWeakVerificationForHost(url: String) {
        sharedPreferences.edit { putBoolean(url.host, true) }
    }

    fun isWeakVerificationForHost(url: String) = sharedPreferences.getBoolean(url.host, false)

    fun loadKeyStore() = if (keyStoreFile.exists()) {
        createKeyStore().also { ks ->
            keyStoreFile.inputStream().use {
                ks.load(it, password)
            }
        }
    } else {
        null
    }

    private fun createKeyStore() = KeyStore.getInstance(KeyStore.getDefaultType())

    private val String.host get() = Uri.parse(this).host
}