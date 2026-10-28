package com.example.deeplviewer.diagnostics

import java.net.URI

/** Only allowlisted route shapes and browser tokens leave the device. */
object DiagnosticPrivacy {
    fun url(value: String?): String = runCatching {
        val uri = URI(value ?: "")
        if (uri.scheme !in listOf("https", "http")) return "[non-http]"
        if (uri.host != "www.deepl.com") return "${uri.scheme}://[other-host]/[redacted]"
        val route = Regex("^/(?:[a-z]{2}/)?(translator|write)(?:/l/[a-z]{2,3}(?:-[a-zA-Z0-9]{2,8})*/[a-z]{2,3}(?:-[a-zA-Z0-9]{2,8})*/?)?/?$")
        "${uri.scheme}://www.deepl.com" + if (route.matches(uri.path)) uri.path else "/[redacted]"
    }.getOrDefault("[invalid-url]")

    fun chromiumError(value: String?): String =
        Regex("\\bERR_[A-Z_]+\\b").find(value.orEmpty())?.value ?: "[description-omitted]"

    fun userAgent(value: String): String = Regex("(?:Mozilla|AppleWebKit|Chrome|Version|Safari)/[0-9.]+|\\bMobile\\b|\\bwv\\b")
        .findAll(value).joinToString(" ") { it.value }.take(512)

    fun crash(error: Throwable): String = buildString {
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = error
        while (current != null && seen.add(current) && seen.size <= 8) {
            appendLine(current.javaClass.name)
            current.stackTrace.take(80).forEach {
                appendLine("  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
            }
            current = current.cause
            if (current != null) append("Caused by: ")
        }
    }.take(32768)
}
