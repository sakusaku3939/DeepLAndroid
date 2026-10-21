package com.example.deeplviewer

import com.example.deeplviewer.diagnostics.DiagnosticPrivacy
import org.junit.Assert.*
import org.junit.Test

class DiagnosticPrivacyTest {
    @Test fun urlsNeverIncludeTextOrUnknownPaths() {
        assertEquals("https://www.deepl.com/en/translator/l/en/de", DiagnosticPrivacy.url("https://www.deepl.com/en/translator/l/en/de?token=secret#en/de/private"))
        assertEquals("https://www.deepl.com/[redacted]", DiagnosticPrivacy.url("https://www.deepl.com/login/private-token"))
        assertEquals("https://[other-host]/[redacted]", DiagnosticPrivacy.url("https://user:secret@private.example/account"))
        assertEquals("[non-http]", DiagnosticPrivacy.url("data:text/plain,secret"))
        assertEquals("[invalid-url]", DiagnosticPrivacy.url("https://www.deepl.com/a b"))
        assertFalse(DiagnosticPrivacy.url("https://www.deepl.com/en/translator/secret").contains("secret"))
    }

    @Test fun onlyRecognizedErrorTokensAndUaTokensSurvive() {
        assertEquals("ERR_CONNECTION_RESET", DiagnosticPrivacy.chromiumError("net::ERR_CONNECTION_RESET https://secret"))
        assertEquals("[description-omitted]", DiagnosticPrivacy.chromiumError("private text"))
        val ua = DiagnosticPrivacy.userAgent("Mozilla/5.0 (Linux; Android 17; PersonalDevice; wv) AppleWebKit/537.36 Chrome/155.0.0.0 Mobile Safari/537.36 secret")
        assertTrue(ua.contains("Chrome/155.0.0.0"))
        assertTrue(ua.contains("wv"))
        assertFalse(ua.contains("PersonalDevice"))
        assertFalse(ua.contains("secret"))
    }

    @Test fun crashMessagesAndCauseMessagesAreExcluded() {
        val crash = DiagnosticPrivacy.crash(IllegalStateException("private translation", IllegalArgumentException("secret-token")))
        assertTrue(crash.contains("IllegalStateException"))
        assertTrue(crash.contains("IllegalArgumentException"))
        assertFalse(crash.contains("private translation"))
        assertFalse(crash.contains("secret-token"))
    }
}
