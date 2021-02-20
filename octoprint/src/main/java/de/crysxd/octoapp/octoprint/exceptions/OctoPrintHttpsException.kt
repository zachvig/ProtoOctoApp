package de.crysxd.octoapp.octoprint.exceptions

import de.crysxd.octoapp.octoprint.SubjectAlternativeNameCompatVerifier
import okhttp3.HttpUrl
import java.net.URL
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.*

class OctoPrintHttpsException(
    url: HttpUrl,
    cause: Throwable
) : OctoPrintException(
    cause = ProxyException.create(cause, url.toString()),
    userFacingMessage = "HTTPS connection could not be established. Make sure you installed all required certificates on your phone."
) {

    val serverCertificates: List<Certificate>
    var weakHostnameVerificationRequired = false
        private set

    init {
        serverCertificates = extractCertificates(url.toUrl())
    }

    private fun extractCertificates(url: URL, hostnameVerifier: HostnameVerifier? = null): List<Certificate> = try {
        // Create SSLContext which accepts all certificates without any checks
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())

        // Establish a connection to the remote server and extract the certificates provided, then close
        // THIS CONNECTION IS NOT TRUSTWORTHY! SO NOT SEND DATA :)
        val connection = (url.openConnection() as HttpsURLConnection)
        connection.sslSocketFactory = sc.socketFactory
        hostnameVerifier?.let { connection.hostnameVerifier = it }
        connection.connect()
        val certs = connection.serverCertificates.toList()
        connection.disconnect()
        certs
    } catch (e: SSLPeerUnverifiedException) {
        // Certificate is most likely missing SAN field
        weakHostnameVerificationRequired = true
        extractCertificates(url, SubjectAlternativeNameCompatVerifier())
    } catch (e: Exception) {
        Logger.getGlobal().log(Level.WARNING, "Unable to extract certificates from server", e)
        emptyList()
    }
}