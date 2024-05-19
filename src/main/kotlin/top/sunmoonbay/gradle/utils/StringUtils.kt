package top.sunmoonbay.gradle.utils

class StringUtils {
    companion object {
        @JvmStatic
        fun getUrl(url: String): String {
            return if (url.endsWith("/")) {
                url
            } else {
                "$url/"
            }
        }
    }
}
