package de.crysxd.octoapp.octoprint

import com.google.common.truth.Truth.assertThat
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class HttpUrlExtTest {

    @Test
    fun `WHEN basic auth is removed with no auth given THEN the plain URL is returned`() {
        // GIVEN
        val url = "http://octopi.local:80/path".toHttpUrl()

        // WHEN
        val result = url.withoutBasicAuth().toString()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local/path")
    }

    @Test
    fun `WHEN basic auth is removed with normal auth THEN the plain URL is returned`() {
        // GIVEN
        val url = "http://user:password@octopi.local:80/path".toHttpUrl()

        // WHEN
        val result = url.withoutBasicAuth().toString()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local/path")
    }

    @Test
    fun `WHEN basic auth is removed with special chars auth THEN the plain URL is returned`() {
        // GIVEN
        val url = "http://u%24%25%23:p%40%24%24wor%2F@octopi.local:80/path".toHttpUrl()

        // WHEN
        val result = url.withoutBasicAuth().toString()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local/path")
    }

    @Test
    fun `WHEN path is resolved THEN the upgraded URL is returned`() {
        // GIVEN
        val url = "http://u%24%25%23:p%40%24%24wor%2F@octopi.local:80/path/".toHttpUrl()

        // WHEN
        val result = url.resolvePath("extra/something?query=true").toString()

        // THEN
        assertThat(result).isEqualTo("http://u%24%25%23:p%40%24%24wor%2F@octopi.local/path/extra/something?query=true")
    }

    @Test
    fun `WHEN path is resolved with no trailing slash THEN the upgraded URL is returned`() {
        // GIVEN
        val url = "http://u%24%25%23:p%40%24%24wor%2F@octopi.local:80/path".toHttpUrl()

        // WHEN
        val result = url.resolvePath("extra/something?query=true").toString()

        // THEN
        assertThat(result).isEqualTo("http://u%24%25%23:p%40%24%24wor%2F@octopi.local/path/extra/something?query=true")
    }

    @Test
    fun `WHEN basic auth is extracted and removed THEN correct values are returned`() {
        // GIVEN
        val url = "http://u%24%25%23:p%40%24%24wor%2F@octopi.local:80/path".toHttpUrl()

        // WHEN
        val (cleanUrl, header) = url.extractAndRemoveBasicAuth()

        // THEN
        assertThat(cleanUrl.toString()).isEqualTo("http://octopi.local/path")
        assertThat(header).isEqualTo("Basic dSQlIzpwQCQkd29yLw==")
    }

    @Test
    fun `WHEN basic auth with no password is extracted and removed THEN correct values are returned`() {
        // GIVEN
        val url = "http://u%24%25%23@octopi.local:80/path".toHttpUrl()

        // WHEN
        val (cleanUrl, header) = url.extractAndRemoveBasicAuth()

        // THEN
        assertThat(cleanUrl.toString()).isEqualTo("http://octopi.local/path")
        assertThat(header).isEqualTo("Basic dSQlIzo=")
    }

    @Test
    fun `WHEN basic auth with no auth is extracted and removed THEN correct values are returned`() {
        // GIVEN
        val url = "http://octopi.local:80/path".toHttpUrl()

        // WHEN
        val (cleanUrl, header) = url.extractAndRemoveBasicAuth()

        // THEN
        assertThat(cleanUrl.toString()).isEqualTo("http://octopi.local/path")
        assertThat(header).isNull()
    }

    @Test
    fun `WHEN checking if based on and is based on THEN true returned`() {
        // GIVEN
        val url1 = "http://octopi.local:80/path".toHttpUrl()
        val url2 = "http://octopi.local:80/path/with/more".toHttpUrl()

        // WHEN
        val result = url2.isBasedOn(url1)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checking if based on and one has basic auth THEN true returned`() {
        // GIVEN
        val url1 = "http://octopi.local:80/path".toHttpUrl()
        val url2 = "http://user:password@octopi.local:80/path/with/more".toHttpUrl()

        // WHEN
        val result = url2.isBasedOn(url1)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checking if based on and scheme is different THEN false returned`() {
        // GIVEN
        val url1 = "http://octopi.local:80/path".toHttpUrl()
        val url2 = "https://octopi.local:80/path".toHttpUrl()

        // WHEN
        val result = url2.isBasedOn(url1)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `WHEN checking if based on and missing trailing slash THEN true returned`() {
        // GIVEN
        val url1 = "http://octopi.local:80/path/".toHttpUrl()
        val url2 = "http://octopi.local:80/path".toHttpUrl()

        // WHEN
        val result = url1.isBasedOn(url2)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checking if based on and missing trailing slash 2 THEN true returned`() {
        // GIVEN
        val url1 = "http://octopi.local:80/path".toHttpUrl()
        val url2 = "http://octopi.local:80/path/".toHttpUrl()

        // WHEN
        val result = url1.isBasedOn(url2)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checking if OctoEverywhere and is OctoEverywhere THEN true returned`() {
        // GIVEN
        val url = "http://shared-1234.octoeverywhere.com/path".toHttpUrl()

        // WHEN
        val result = url.isOctoEverywhereUrl()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checking if OctoEverywhere and is no tOctoEverywhere THEN true returned`() {
        // GIVEN
        val url = "http://octopi.local:80/path/".toHttpUrl()

        // WHEN
        val result = url.isOctoEverywhereUrl()

        // THEN
        assertThat(result).isFalse()
    }


    @Test
    fun `WHEN redacting host of class A local IP THEN nothing is done`() {
        // GIVEN
        val url = "http://10.0.1.1/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://10.0.1.1/path/")
    }

    @Test
    fun `WHEN redacting host of class B local IP THEN nothing is done`() {
        // GIVEN
        val url = "http://172.16.1.1/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://172.16.1.1/path/")
    }

    @Test
    fun `WHEN redacting host of class C local IP THEN nothing is done`() {
        // GIVEN
        val url = "http://192.168.1.1/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://192.168.1.1/path/")
    }

    @Test
    fun `WHEN redacting host of public IP THEN host is redacted`() {
        // GIVEN
        val url = "http://192.160.1.1/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://redacted-host-2e53d0d/path/")
    }

    @Test
    fun `WHEN redacting host of public domain THEN host is redacted`() {
        // GIVEN
        val url = "http://somedomain.com/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://redacted-host-9c19914b/path/")
    }

    @Test
    fun `WHEN redacting host of OctoEverywhere domain THEN host is partly redacted`() {
        // GIVEN
        val url = "http://shared-1233.octoeverywhere.com/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://redacted-d52ad9b3.octoeverywhere.com/path/")
    }

    @Test
    fun `WHEN redacting host of ngrok domain THEN host is partly redacted`() {
        // GIVEN
        val url = "http://some-tunnel.ngrok.com/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://redacted-c780e73b.ngrok.com/path/")
    }

    @Test
    fun `WHEN redacting host of UPnP domain THEN nothing is done`() {
        // GIVEN
        val url = "http://$UPNP_ADDRESS_PREFIX-38298392/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo(url.toString())
    }

    @Test
    fun `WHEN redacting host of mDNS domain THEN nothing is done`() {
        // GIVEN
        val url = "http://octopi.local/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo(url.toString())
    }

    @Test
    fun `WHEN redacting host of home domain THEN nothing is done`() {
        // GIVEN
        val url = "http://octopi.home/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo(url.toString())
    }

    @Test
    fun `WHEN redacting host from log THEN host is redacted is done`() {
        // GIVEN
        val url = "http://some-tunnel.ngrok.com/path/".toHttpUrl()
        val log = "This is a log with some http://some-tunnel.ngrok.com/path/ url"

        // WHEN
        val result = url.redactLoggingString(log)

        // THEN
        assertThat(result).isEqualTo("This is a log with some http://redacted-c780e73b.ngrok.com/path/ url")
    }

    @Test
    fun `WHEN redacting local ip from log THEN nothing is done`() {
        // GIVEN
        val url = "http://192.168.1.1/path/".toHttpUrl()
        val log = "This is a log with some http://192.168.1.1/path/ url"

        // WHEN
        val result = url.redactLoggingString(log)

        // THEN
        assertThat(result).isEqualTo("This is a log with some http://192.168.1.1/path/ url")
    }

    @Test
    fun `WHEN redacting basic auth THEN basic auth is removed`() {
        // GIVEN
        val url = "http://u%24er:pa%%word@192.168.1.1/path/".toHttpUrl()
        val log = "This is a log with some u\$er pa%%word u%24er sensitive stuff"

        // WHEN
        val result = url.redactLoggingString(log)

        // THEN
        assertThat(result).isEqualTo("This is a log with some \$basicAuthUser \$basicAuthsPassword \$basicAuthUser sensitive stuff")
    }

    @Test
    fun `WHEN redacting basic auth THEN nothing is done`() {
        // GIVEN
        val url = "http://u%24er@192.168.1.1/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://\$basicAuthUser@192.168.1.1/path/")
    }

    @Test
    fun `WHEN redacting basic auth with password THEN nothing is done`() {
        // GIVEN
        val url = "http://u%24er:pa%%word@192.168.1.1/path/".toHttpUrl()

        // WHEN
        val result = url.forLogging().toString()

        // THEN
        assertThat(result).isEqualTo("http://\$basicAuthUser:\$basicAuthPassword@192.168.1.1/path/")
    }
}