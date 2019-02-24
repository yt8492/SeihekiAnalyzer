import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SeihekiAnalyzer {

    private val loginCookies: Map<String, String>
    private val callback: Callback
    private val genreCnt = mutableMapOf<String, Int>()
    private var totalCnt = 0

    private constructor(loginCookies: Map<String, String>, callback: Callback) {
        this.loginCookies = loginCookies
        this.callback = callback
        start()
    }

    private constructor(userId: String, password: String, callback: Callback) {
        val loginAccessConnection = JsoupUtils.getConnection(URL_LOGIN).execute()
        val loginPage = loginAccessConnection.parse()
        val token = loginPage.select("[name=_token]").first()
        val tokenKey = token.attr("name")
        val tokenValue = token.attr("value")

        val loginCookies = JsoupUtils.requestByPost(URL_LOGIN, cookies = loginAccessConnection.cookies(), data = mapOf(
            tokenKey to tokenValue,
            "login_id" to userId,
            "password" to password
        )).cookies()

        this.loginCookies = loginCookies
        this.callback = callback

        if (loginCookies.containsKey("PHPSESSID")) {
            callback.onLoginSuccess()
            start()
        } else {
            callback.onLoginFailure()
        }

    }

    private fun start() {
        callback.onStartUrlFetch()
        val urls = getUrls()
        callback.onUrlsFetchSuccess(urls)
        callback.onStartTagsAnalyze()
        urls.forEach { url ->
            if (analyzeTag(url)) {
                totalCnt++
            }
        }
        callback.onFinishTagsAnalyze(genreCnt.map {
            Tag(it.key, it.value)
        }, urls, totalCnt)
    }

    private fun getUrls(): List<String> {
        val historyCookies = JsoupUtils.requestByPost(URL_USER_BUY_HISTORY, cookies = loginCookies).cookies()

        val thisMonthUserBuyHistoryResult = JsoupUtils.requestByGet(URL_USER_BUY_HISTORY, cookies = historyCookies).parse()
        val pastMonthUserBuyHistoryResult = JsoupUtils.requestByGet(URL_USER_BUY_HISTORY, cookies = historyCookies, data = mapOf(
            "_layout" to "mypage_userbuy_complete",
            "_form_id" to "mypageUserbuyCompleteForm",
            "_site" to "maniax",
            "_view" to "input",
            "start" to "all"
        )).parse()

        val userBuyHistoryUrls = listOf(thisMonthUserBuyHistoryResult, pastMonthUserBuyHistoryResult)
            .flatMap { d ->
                d.getElementsByClass("work_name")
                .map { e ->
                    e.select("[href]").toString().split("\"")[1]
                }
            }
        return userBuyHistoryUrls
    }

    private fun analyzeTag(url: String, failureCnt: Int = 0): Boolean {
        try {
            when {
                failureCnt == 0 -> callback.onAnalyzingTag(url, false)
                failureCnt < 5 -> callback.onAnalyzingTag(url, true)
                else -> {
                    callback.onTagAnalyzeFailure(url)
                    return false
                }
            }
            val voicePage = JsoupUtils.requestByGet(url).parse()
            val rows = voicePage.getElementById("work_outline").select("tr")
            val tags = rows.find { row ->
                row.child(0).text() == "ジャンル"
            }?.getElementsByClass("main_genre")?.select("[href]")?.text()?.split(" ".toRegex()) ?: listOf()
            tags.forEach { tag ->
                var cnt = genreCnt[tag]
                cnt = if (cnt == null) {
                    1
                } else {
                    cnt + 1
                }
                genreCnt[tag] = cnt
            }
            return true
        } catch (e: SocketTimeoutException) {
            return analyzeTag(url, failureCnt + 1)
        } catch (e: UnknownHostException) {
            return analyzeTag(url, failureCnt + 1)
        } catch (e: Exception) {
            e.printStackTrace()
            return analyzeTag(url, failureCnt + 1)
        }
    }

    interface Callback {
        fun onLoginSuccess()

        fun onLoginFailure() {
            error("Login Failed")
        }

        fun onStartUrlFetch() = Unit

        fun onUrlsFetchSuccess(urls: List<String>)

        fun onStartTagsAnalyze() = Unit

        fun onAnalyzingTag(url: String, retryFlg: Boolean) = Unit

        fun onTagAnalyzeFailure(url: String) = Unit

        fun onFinishTagsAnalyze(tags: List<Tag>, urls: List<String>, totalCnt: Int)
    }

    data class Tag(val name: String, val count: Int) {
        override fun toString() = "tag: $name, count: $count"
    }

    companion object {
        const val URL_LOGIN = "https://login.dlsite.com/login"
        const val URL_USER_BUY_HISTORY = "https://ssl.dlsite.com/maniax/mypage/userbuy"

        @JvmStatic
        fun loginAndAnalyze(userId: String, password: String, callback: Callback) {
            SeihekiAnalyzer(userId, password, callback)
        }

        @JvmStatic
        fun loginAndAnalyze(loginCookies: Map<String, String>, callback: Callback) {
            SeihekiAnalyzer(loginCookies, callback)
        }
    }
}