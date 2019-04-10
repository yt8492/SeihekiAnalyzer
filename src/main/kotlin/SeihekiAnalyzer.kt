import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SeihekiAnalyzer private constructor(private val loginCookies: Map<String, String>) {

    private val genreCnt = mutableMapOf<String, Int>()
    private var totalCnt = 0

    fun exec(callback: Callback) {
        callback.onStartUrlFetch()
        val urls = getUrls()
        callback.onUrlsFetchSuccess(urls)
        callback.onStartTagsAnalyze()
        totalCnt = urls.filter { analyzeTag(it, callback) }.count()
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
                    try {
                        e.select("[href]").toString().split("\"")[1]
                    } catch (error: IndexOutOfBoundsException) {
                        ""
                    }
                }
            }
        return userBuyHistoryUrls
    }

    private fun analyzeTag(url: String, callback: Callback, failureCnt: Int = 0): Boolean {
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
            return analyzeTag(url, callback, failureCnt + 1)
        } catch (e: UnknownHostException) {
            return analyzeTag(url, callback, failureCnt + 1)
        } catch (e: Exception) {
            return analyzeTag(url, callback, 5)
        }
    }

    interface Callback {
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
        private const val URL_LOGIN = "https://login.dlsite.com/login"
        private const val URL_USER_BUY_HISTORY = "https://ssl.dlsite.com/maniax/mypage/userbuy"

        @JvmStatic
        fun login(userId: String, password: String, onLoginSuccess: (() -> Unit) = {},
                  onLoginFailure: (() -> Unit) = { error("Login Failed") }): SeihekiAnalyzer? {
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
            return if (loginCookies.containsKey("PHPSESSID")) {
                onLoginSuccess()
                SeihekiAnalyzer(loginCookies)
            } else {
                onLoginFailure()
                null
            }
        }
    }
}