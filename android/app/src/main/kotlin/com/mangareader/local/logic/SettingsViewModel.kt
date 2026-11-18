package com.mangareader.local.logic

/** The "PC address" dialog: where the server is, and re-trusting its certificate. */
class SettingsViewModel(private val server: ReadingServer, private val onChanged: () -> Unit) {

    val address get() = server.address

    /** Accepts "ip:port", with or without https:// and a trailing slash. Blank keeps the old address. */
    fun save(address: String) {
        val clean = cleanAddress(address)
        if (clean.isNotEmpty()) server.address = clean
        onChanged()
    }

    /** After the server made a new certificate (new PC address, deleted cert folder...). */
    fun trustCertificateAgain() {
        server.forgetCertificate()
        onChanged()
    }

    companion object {
        fun cleanAddress(text: String) = text.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
    }
}
