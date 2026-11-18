package com.mangareader.local.data.remote

import org.json.JSONObject

/** What to tell the user when the server answers with an error status (body: {"detail": "..."}). */
fun serverErrorMessage(code: Int, body: String?): String {
    if (code == 401) return "Wrong or missing pairing code: type the code shown in the server window (📖 → ⚙)"
    val detail = runCatching { JSONObject(body ?: "").get("detail") }.getOrNull()
    return if (detail is String && detail.isNotBlank()) detail else "The PC answered with error $code"
}
