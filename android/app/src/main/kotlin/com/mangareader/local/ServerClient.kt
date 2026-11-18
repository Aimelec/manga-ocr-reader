package com.mangareader.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Talks to the PC server (start_server.bat) over HTTPS.
 * The server uses a home-made certificate, so the app trusts the first certificate it sees
 * for each address and refuses a different one later ("trust on first use").
 */
class ServerClient(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var address: String
        get() = prefs.getString("server", DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(value) = prefs.edit().putString("server", value).apply()

    /** Forget the remembered certificate for the current address (after the server made a new one). */
    fun forgetCertificate() = prefs.edit().remove(pinKey()).apply()

    private fun pinKey() = "pin:$address"

    private val trustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            val fingerprint = MessageDigest.getInstance("SHA-256").digest(chain[0].encoded)
                .joinToString("") { "%02x".format(it) }
            val pinned = prefs.getString(pinKey(), null)
            when (pinned) {
                null -> prefs.edit().putString(pinKey(), fingerprint).apply()
                fingerprint -> Unit
                else -> throw CertificateException(
                    "The PC's certificate changed. If you restarted the server, open ⚙ and tap 'Trust again'.")
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val sslFactory = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustManager), SecureRandom())
    }.socketFactory

    private fun open(path: String) =
        (URL("https://$address$path").openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = sslFactory
            hostnameVerifier = HostnameVerifier { _, _ -> true }   // the certificate itself is pinned
        }

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            open("/").run {
                connectTimeout = 3000
                readTimeout = 3000
                responseCode == 200
            }
        }.getOrDefault(false)
    }

    suspend fun read(jpeg: ByteArray): ReadResult = withContext(Dispatchers.IO) {
        val boundary = "----mangareader${System.nanoTime()}"
        val conn = open("/read").apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 5000
            readTimeout = 60000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        conn.outputStream.use { out ->
            out.write(("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"crop.jpg\"\r\n" +
                "Content-Type: image/jpeg\r\n\r\n").toByteArray())
            out.write(jpeg)
            out.write("\r\n--$boundary--\r\n".toByteArray())
        }
        val code = conn.responseCode
        if (code != 200) throw IOException("The PC answered with error $code")
        val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        parseResult(JSONObject(text))
    }

    /** AI explanation of one bubble; emits the text piece by piece while the PC's model writes it. */
    fun explain(line: String, context: List<String>): Flow<String> = flow {
        val conn = open("/explain").apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 5000
            readTimeout = 240000   // the first request also loads the model
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = JSONObject().put("line", line).put("context", JSONArray(context)).toString()
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        if (conn.responseCode != 200) throw IOException("The PC answered with error ${conn.responseCode}")
        conn.inputStream.reader(Charsets.UTF_8).use { reader ->
            val buf = CharArray(256)
            while (true) {
                val n = reader.read(buf)
                if (n < 0) break
                if (n > 0) emit(String(buf, 0, n))
            }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        // Example only: set the real address in the app settings (the server prints it at startup).
        const val DEFAULT_SERVER = "192.168.1.100:8765"
    }
}
