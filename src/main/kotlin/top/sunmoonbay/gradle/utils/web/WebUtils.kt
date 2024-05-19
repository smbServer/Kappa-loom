package top.sunmoonbay.gradle.utils.web

import org.apache.commons.io.IOUtils
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class WebUtils {
    companion object {
        @JvmStatic
        fun readWebFile(link : String) : ByteArray? {
            var bytes: ByteArray? = null
            try {
                val url = URL(link)
                val urlConnection = url.openConnection()
                var connection: HttpURLConnection? = null
                if (urlConnection is HttpURLConnection) {
                    connection = urlConnection
                }

                if (connection == null) {
                    throw NullPointerException(String.format("Link: '%s' fail", link))
                }

                bytes = IOUtils.toByteArray(connection.inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bytes
        }

        @JvmStatic
        fun readWebString(link : String) : String? {
            return readWebFile(link)?.let { String(it, StandardCharsets.UTF_8) }
        }
    }
}