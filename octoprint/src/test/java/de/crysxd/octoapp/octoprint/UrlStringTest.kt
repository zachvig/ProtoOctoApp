package de.crysxd.octoapp.octoprint

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UrlStringTest {

    @Test
    fun `WHEN user info is removed without any info THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://octopi.local:1500/path"

        // WHEN
        val result = url.removeUserInfo()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local:1500/path")
    }

    @Test
    fun `WHEN user name is removed THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://user:password@octopi.local:1500/path"

        // WHEN
        val result = url.removeUserInfo()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local:1500/path")
    }

    @Test
    fun `WHEN user name is removed with special chars THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://u%24%25%23%3Ap%40%24%24wor%2F@octopi.local:1500/path"

        // WHEN
        val result = url.removeUserInfo()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local:1500/path")
    }

    @Test
    fun `WHEN user info is extracted with no info THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://octopi.local:1500/path"

        // WHEN
        val result = url.extractAndRemoveUserInfo()

        // THEN
        assertThat(result.first).isEqualTo("http://octopi.local:1500/path")
        assertThat(result.second).isNull()
    }

    @Test
    fun `WHEN user info is extracted THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://user:password@octopi.local:1500/path"

        // WHEN
        val result = url.extractAndRemoveUserInfo()

        // THEN
        assertThat(result.first).isEqualTo("http://octopi.local:1500/path")
        assertThat(result.second).isEqualTo("Basic dXNlcjpwYXNzd29yZA==")
    }

    @Test
    fun `WHEN user info is extracted without password THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://user@octopi.local:1500/path"

        // WHEN
        val result = url.extractAndRemoveUserInfo()

        // THEN
        assertThat(result.first).isEqualTo("http://octopi.local:1500/path")
        assertThat(result.second).isEqualTo("Basic dXNlcjo=")
    }

    @Test
    fun `WHEN user info is extracted with special chars THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://u%24%25%23%3Ap%40%24%24wor%2F@octopi.local:1500/path"

        // WHEN
        val result = url.extractAndRemoveUserInfo()

        // THEN
        assertThat(result.first).isEqualTo("http://octopi.local:1500/path")
        assertThat(result.second).isEqualTo("Basic dSQlIzpwQCQkd29yLzo=")
    }

    @Test
    fun `WHEN checked for full URL and is http URL THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://octopi.local:1500/path"

        // WHEN
        val result = url.isFullUrl()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checked for full URL and is https URL THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "https://octopi.local:1500/path"

        // WHEN
        val result = url.isFullUrl()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checked for full URL and is path THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "/path"

        // WHEN
        val result = url.isFullUrl()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `WHEN checked for OctoEverywhere and is shared URL THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://shared-somthing.octoeverywhere.com/"

        // WHEN
        val result = url.isOctoEverywhereUrl()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checked for OctoEverywhere and is normal URL THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://somthing.octoeverywhere.com/"

        // WHEN
        val result = url.isOctoEverywhereUrl()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `WHEN checked for OctoEverywhere and is local URL THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://octopi.local/"

        // WHEN
        val result = url.isOctoEverywhereUrl()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `WHEN checked for OctoEverywhere and is path THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "/path"

        // WHEN
        val result = url.isOctoEverywhereUrl()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `WHEN sanitizing URL without trailing slash THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://octopi.local"

        // WHEN
        val result = url.sanitizeUrl()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local/")
    }

    @Test
    fun `WHEN sanitizing URL with trailing slash THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://octopi.local/"

        // WHEN
        val result = url.sanitizeUrl()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local/")
    }

    @Test
    fun `WHEN sanitizing URL with http default port THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "http://octopi.local:80/"

        // WHEN
        val result = url.sanitizeUrl()

        // THEN
        assertThat(result).isEqualTo("http://octopi.local/")
    }

    @Test
    fun `WHEN sanitizing URL with https default port THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "https://octopi.local:443/"

        // WHEN
        val result = url.sanitizeUrl()

        // THEN
        assertThat(result).isEqualTo("https://octopi.local/")
    }

    @Test
    fun `WHEN sanitizing URL with wrong default port THEN expected values are returned`() {
        // GIVEN
        val url: UrlString = "https://octopi.local:80/"

        // WHEN
        val result = url.sanitizeUrl()

        // THEN
        assertThat(result).isEqualTo("https://octopi.local:80/")
    }
}