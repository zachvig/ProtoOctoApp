package de.crysxd.octoapp.octoprint

import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

/**
 * This is a very simple host name verifier that can be used if the certificate does not offer
 * a SAN which is required from Android 9 onwards.
 */
class SubjectAlternativeNameCompatVerifier : HostnameVerifier {
    override fun verify(hostname: String, session: SSLSession): Boolean {
        // This is suuuuper rudimentary and only ok because we will explicitly ask the user if he wants to trust this server
        // and we will tell him that the hostname did not match with the system's check.
        val cert = (session.peerCertificates.first() as? X509Certificate) ?: return false
        return cert.subjectDN.name.contains("CN=$hostname", ignoreCase = true) ||
                cert.issuerDN.name.contains("CN=$hostname", ignoreCase = true)
    }
}