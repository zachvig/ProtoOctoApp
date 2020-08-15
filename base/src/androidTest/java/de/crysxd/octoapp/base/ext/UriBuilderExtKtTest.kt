package de.crysxd.octoapp.base.ext

import android.net.Uri
import org.junit.Test

class UriBuilderExtKtTest {

    @Test
    fun test1() {
        val base = "http://my.octoprint/"
        val extension = "/webcam?stream"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assert(uri.toString() == "http://my.octoprint/webcam?stream")
    }

    @Test
    fun test2() {
        val base = "http://my.octoprint/test/"
        val extension = "/webcam?stream=123"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assert(uri.toString() == "http://my.octoprint/test/webcam?stream=123")
    }

    @Test
    fun test3() {
        val base = "http://my.octoprint/test"
        val extension = "/webcam?stream"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assert(uri.toString() == "http://my.octoprint/test/webcam?stream")
    }

    @Test
    fun test4() {
        val base = "http://my.octoprint"
        val extension = "/webcam?stream"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assert(uri.toString() == "http://my.octoprint/webcam?stream")
    }

    @Test
    fun test5() {
        val base = "http://my.octoprint"
        val extension = "/webcam"

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assert(uri.toString() == "http://my.octoprint/webcam")
    }

    @Test
    fun test6() {
        val base = "http://my.octoprint"
        val extension = null

        val uri = Uri.parse(base).buildUpon()
            .resolve(extension)
            .build()

        assert(uri.toString() == "http://my.octoprint/")
    }
}