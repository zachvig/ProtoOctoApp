package de.crysxd.octoapp.base.ext

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UriBuilderExtKtTest {

    @Test
    fun test1() {
        val base = "http://my.octoprint/"
        val extension = "/webcam?stream"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint/webcam?stream")
    }

    @Test
    fun test2() {
        val base = "http://my.octoprint/test/"
        val extension = "/webcam?stream=123"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint/test/webcam?stream=123")
    }

    @Test
    fun test3() {
        val base = "http://my.octoprint/test"
        val extension = "/webcam?stream"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint/test/webcam?stream")
    }

    @Test
    fun test4() {
        val base = "http://my.octoprint"
        val extension = "/webcam?stream"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint/webcam?stream")
    }

    @Test
    fun test5() {
        val base = "http://my.octoprint"
        val extension = "/webcam"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint/webcam")
    }

    @Test
    fun test6() {
        val base = "http://my.octoprint"
        val extension = null

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint")
    }

    @Test
    fun test7() {
        val base = "http://my.octoprint/test"
        val extension = "/webcam/?stream=test"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint/test/webcam/?stream=test")
    }

    @Test
    fun test8() {
        val base = "http://my.octoprint"
        val extension = "/webcam?stream=true"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint/webcam?stream=true")
    }

    @Test
    fun test9() {
        val base = "http://my.octoprint/"
        val extension = null

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assertThat(uri.toString()).isEqualTo("http://my.octoprint/")
    }
}