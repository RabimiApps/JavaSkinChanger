package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WelcomeActivity : AppCompatActivity() {

    private val clientId = "0000000048183522"
    private val redirectUri = "ms-xal-0000000048183522://auth"
    private val scope = "XboxLive.signin offline_access"

    private lateinit var btnNext: Button
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btnNext = findViewById(R.id.btnNext)
        println("[Debug] onCreate called")

        btnNext.setOnClickListener {
            println("[Debug] btnNext clicked")
            val loginUrl = "https://login.live.com/oauth20_authorize.srf" +
                    "?client_id=$clientId" +
                    "&response_type=token" +
                    "&redirect_uri=$redirectUri" +
                    "&scope=$scope" +
                    "&prompt=select_account"
            println("[Debug] Launching OAuth URL: $loginUrl")
            CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(loginUrl))
        }

        handleRedirect(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        println("[Debug] onNewIntent called")
        handleRedirect(intent)
    }

    private fun handleRedirect(intent: Intent?) {
        intent?.data?.let { uri ->
            println("[Debug] handleRedirect with URI: $uri")
            if (uri.toString().startsWith(redirectUri)) {
                val fragment = uri.fragment ?: ""
                println("[Debug] URI Fragment: $fragment")
                val token = fragment.split("&").find { it.startsWith("access_token=") }
                    ?.substringAfter("=")
                if (token != null) {
                    println("[Debug] Access Token: $token")
                    fetchMinecraftUsername(token)
                } else {
                    println("[Debug] Access token not found in fragment")
                    showErrorDialog("トークン取得失敗")
                }
            } else {
                println("[Debug] URI does not match redirectUri")
            }
        }
    }

    private fun fetchMinecraftUsername(msToken: String) {
        println("[Debug] fetchMinecraftUsername called with msToken: $msToken")
        mainScope.launch {
            val username = withContext(Dispatchers.IO) { getMinecraftUsername(msToken) }
            if (username != null) {
                println("[Debug] Minecraft Username: $username")
                showConfirmDialog(username)
            } else {
                println("[Debug] Minecraft API取得失敗")
                showErrorDialog("Minecraft API 取得失敗")
            }
        }
    }

    private fun getMinecraftUsername(msToken: String): String? {
        return try {
            println("[Debug] Starting Xbox Live Authentication")
            val xblResp = postJson(
                URL("https://user.auth.xboxlive.com/user/authenticate"),
                """
                {
                    "Properties": {
                        "AuthMethod": "RPS",
                        "SiteName": "user.auth.xboxlive.com",
                        "RpsTicket": "d=$msToken"
                    },
                    "RelyingParty": "http://auth.xboxlive.com",
                    "TokenType": "JWT"
                }
                """.trimIndent()
            )
            if (xblResp == null) {
                println("[Debug] Xbox Live Authentication failed")
                return null
            }

            val xblToken = xblResp.getString("Token")
            val userHash = xblResp.getJSONObject("DisplayClaims")
                .getJSONArray("xui").getJSONObject(0).getString("uhs")
            println("[Debug] XBL Token: $xblToken")
            println("[Debug] User Hash: $userHash")

            println("[Debug] Starting XSTS Authentication")
            val xstsResp = postJson(
                URL("https://xsts.auth.xboxlive.com/xsts/authorize"),
                """
                {
                    "Properties": {
                        "SandboxId": "RETAIL",
                        "UserTokens": ["$xblToken"]
                    },
                    "RelyingParty": "rp://api.minecraftservices.com/",
                    "TokenType": "JWT"
                }
                """.trimIndent()
            )
            if (xstsResp == null) {
                println("[Debug] XSTS Authentication failed")
                return null
            }

            val xstsToken = xstsResp.getString("Token")
            println("[Debug] XSTS Token: $xstsToken")

            println("[Debug] Starting Minecraft Authentication (login_with_xbox)")
            val mcAuthResp = postJson(
                URL("https://api.minecraftservices.com/authentication/login_with_xbox"),
                """{"identityToken":"XBL3.0 x=$userHash;$xstsToken"}"""
            )
            if (mcAuthResp == null) {
                println("[Debug] Minecraft login_with_xbox failed")
                return null
            }
            val mcToken = mcAuthResp.getString("access_token")
            println("[Debug] Minecraft Token: $mcToken")

            println("[Debug] Fetching Minecraft Profile")
            val mcProfileResp = getJson(
                URL("https://api.minecraftservices.com/minecraft/profile"),
                mcToken
            )
            if (mcProfileResp == null) {
                println("[Debug] Minecraft profile fetch failed")
                return null
            }
            val username = mcProfileResp.getString("name")
            println("[Debug] Minecraft Username: $username")
            username
        } catch (e: Exception) {
            e.printStackTrace()
            println("[Debug] Exception in getMinecraftUsername: ${e.message}")
            null
        }
    }

    // --------------------------
    // postJson: POST を行い body / response をフル出力する（ここを丸ごとコピー可）
    // --------------------------
    private fun postJson(url: URL, body: String): JSONObject? {
        return try {
            // 送信前のログ（ここで送る JSON を確認できる）
            println("[Debug] POST JSON to $url")
            println("[Debug] Request body:\n$body")

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            // responseStream はエラー時に inputStream が使えないことがあるので両方試す
            val resp = try {
                conn.inputStream.bufferedReader().readText()
            } catch (ee: Exception) {
                try {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (eee: Exception) {
                    ""
                }
            }

            println("[Debug] Response code: $code")
            println("[Debug] Response body:\n$resp")

            if (code in 200..299 && resp.isNotEmpty()) JSONObject(resp) else null
        } catch (e: Exception) {
            e.printStackTrace()
            println("[Debug] Exception in postJson: ${e.message}")
            null
        }
    }

    // --------------------------
    // getJson: GET を行いレスポンスをフル出力（ここも丸ごとコピー可）
    // --------------------------
    private fun getJson(url: URL, token: String): JSONObject? {
        return try {
            println("[Debug] GET JSON from $url")
            println("[Debug] Authorization: Bearer $token")

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val code = conn.responseCode
            val resp = try {
                conn.inputStream.bufferedReader().readText()
            } catch (ee: Exception) {
                try {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (eee: Exception) {
                    ""
                }
            }

            println("[Debug] Response code: $code")
            println("[Debug] Response body:\n$resp")

            if (code in 200..299 && resp.isNotEmpty()) JSONObject(resp) else null
        } catch (e: Exception) {
            e.printStackTrace()
            println("[Debug] Exception in getJson: ${e.message}")
            null
        }
    }

    private fun showConfirmDialog(username: String) {
        println("[Debug] showConfirmDialog called with username: $username")
        AlertDialog.Builder(this)
            .setTitle("ログイン確認")
            .setMessage("ログイン: $username\nこのアカウントでログインしますか？")
            .setPositiveButton("はい") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("minecraft_username", username)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("いいえ") { d, _ -> d.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(msg: String) {
        println("[Debug] showErrorDialog: $msg")
        AlertDialog.Builder(this)
            .setTitle("エラー")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        println("[Debug] onDestroy called")
        mainScope.cancel()
        super.onDestroy()
    }
}