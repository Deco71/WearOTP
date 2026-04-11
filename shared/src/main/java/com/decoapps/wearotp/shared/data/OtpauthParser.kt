package com.decoapps.wearotp.shared.data

import android.net.Uri

data class OtpauthFields(
    val issuer: String?,
    val accountName: String?,
    val secret: String,
    val algorithm: String,
    val digits: Int,
    val interval: Int,
)

enum class ParseError {
    INVALID_OTPAUTH_URI,
    INVALID_OTPAUTH_SCHEME,
    UNSUPPORTED_OTP_TYPE,
    MISSING_ACCOUNT_LABEL,
    MISSING_SECRET,
    INVALID_DIGITS,
    INVALID_PERIOD
}

sealed class OtpauthParseResult {
    data class Success(val fields: OtpauthFields) : OtpauthParseResult()
    data class Error(val error: ParseError) : OtpauthParseResult()
}

fun parseOtpauth(otpauth: String): OtpauthParseResult {
    val uri = try {
        Uri.parse(otpauth)
    } catch (_: Throwable) {
        return OtpauthParseResult.Error(ParseError.INVALID_OTPAUTH_URI)
    }

    if (uri.scheme?.lowercase() != "otpauth") {
        return OtpauthParseResult.Error(ParseError.INVALID_OTPAUTH_SCHEME)
    }

    val type = (uri.host ?: uri.authority).orEmpty().lowercase()
    if (type != "totp") {
        return OtpauthParseResult.Error(ParseError.UNSUPPORTED_OTP_TYPE)
    }

    val label = uri.pathSegments.firstOrNull()?.let { Uri.decode(it) }.orEmpty()
    if (label.isBlank()) {
        return OtpauthParseResult.Error(ParseError.MISSING_ACCOUNT_LABEL)
    }

    var issuerFromLabel: String? = null
    var accountFromLabel: String? = null
    if (label.contains(":")) {
        val parts = label.split(":", limit = 2)
        issuerFromLabel = parts[0].trim().ifBlank { null }
        accountFromLabel = parts[1].trim().ifBlank { null }
    } else {
        accountFromLabel = label.trim().ifBlank { null }
    }

    val secret = uri.getQueryParameter("secret")?.trim()?.ifBlank { null }
        ?: return OtpauthParseResult.Error(ParseError.MISSING_SECRET)

    val issuerQuery = uri.getQueryParameter("issuer")?.trim()?.ifBlank { null }
    val algorithm = uri.getQueryParameter("algorithm")?.trim()?.uppercase()?.ifBlank { "SHA1" } ?: "SHA1"
    val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
    val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

    if (digits <= 0) {
        return OtpauthParseResult.Error(ParseError.INVALID_DIGITS)
    }
    if (period <= 0) {
        return OtpauthParseResult.Error(ParseError.INVALID_PERIOD)
    }

    val issuer = issuerQuery ?: issuerFromLabel
    val accountName = accountFromLabel

    return OtpauthParseResult.Success(
        OtpauthFields(
            issuer = issuer,
            accountName = accountName,
            secret = secret.uppercase(),
            algorithm = algorithm,
            digits = digits,
            interval = period
        )
    )
}

