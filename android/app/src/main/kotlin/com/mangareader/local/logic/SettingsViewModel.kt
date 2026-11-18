package com.mangareader.local.logic

/** The connection dialog: where the server is, its pairing code, and re-trusting its certificate. */
class SettingsViewModel(private val server: ReadingServer, private val onChanged: () -> Unit) {

    val address get() = server.address
    val pairingCode get() = server.pairingCode

    /** Address as "ip:port" (https:// and a trailing slash are fine; blank keeps the old one). */
    fun save(address: String, pairingCode: String) {
        val clean = cleanAddress(address)
        if (clean.isNotEmpty()) server.address = clean
        server.pairingCode = cleanCode(pairingCode)
        onChanged()
    }

    /** After the server made a new certificate (new PC address, deleted cert folder...). */
    fun trustCertificateAgain() {
        server.forgetCertificate()
        onChanged()
    }

    companion object {
        fun cleanAddress(text: String) = text.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        fun cleanCode(text: String) = text.filterNot { it.isWhitespace() || it == '-' }.uppercase()
    }
}
