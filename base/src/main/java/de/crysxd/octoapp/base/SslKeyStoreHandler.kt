package de.crysxd.octoapp.base

import android.content.Context
import java.io.File
import java.security.KeyStore
import java.security.cert.Certificate
import java.util.*

class SslKeyStoreHandler(private val context: Context) {

    private val password = context.packageName.toCharArray()
    private val keyStoreFile = File(context.filesDir, "ssl_certs.ks")

    fun storeCertificates(certs: List<Certificate>) {
        val ks = loadKeyStore() ?: createKeyStore().also { it.load(null) }
        certs.forEach {
            ks.setCertificateEntry(UUID.randomUUID().toString(), it)
        }
        keyStoreFile.outputStream().use {
            ks.store(it, password)
        }
    }

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
}