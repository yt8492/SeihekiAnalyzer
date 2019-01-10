import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun main() {
    val loginUrl = "https://login.dlsite.com/login"
    val mainUrl = "https://ssl.dlsite.com/maniax/mypage/userbuy"

    val console = System.console() ?: null

    print("UserId: ")
    val userId = console?.readLine() ?: readLine() ?: return
    print("Password: ")
    val password = console?.readPassword()?.joinToString("") ?: readLine() ?: return

    val res1 = getConnection(loginUrl)
        .execute()
    val welcomePage = res1.parse()
    val welcomeCookie = res1.cookies()
    val token = welcomePage.select("[name=_token]").first()
    val tokenKey = token.attr("name")
    val tokenValue = token.attr("value")

    val cookies = requestByPost(loginUrl, cookies = welcomeCookie, data = mapOf(
        tokenKey to tokenValue,
        "login_id" to userId,
        "password" to password
    )).cookies()

    val historyCookies = requestByPost(mainUrl, cookies = cookies).cookies()

    if (cookies.containsKey("PHPSESSID")) {
        println("ログインに成功しました。")
    } else {
        println("ログインに失敗しました。")
        return
    }

    println("購入履歴取得中...")

    val thisMonthHistory = requestByGet(mainUrl, cookies = historyCookies).parse()

    val pastMonthHistory = requestByGet(mainUrl, cookies = historyCookies, data = mapOf(
        "_layout" to "mypage_userbuy_complete",
        "_form_id" to "mypageUserbuyCompleteForm",
        "_site" to "maniax",
        "_view" to "input",
        "start" to "all"
    )).parse()

    Thread.sleep(1000)

    val urls1 = thisMonthHistory
        .getElementsByClass("work_name")
        .map {
            it.select("[href]").toString().split("\"")[1]
        }

    val urls2 = pastMonthHistory
        .getElementsByClass("work_name")
        .map {
            it.select("[href]").toString().split("\"")[1]
        }

    val urls = urls1 + urls2

    println(
        urls.mapIndexed { index, url ->
            "${"%3d".format(index + 1)}: $url"
        }.joinToString("\n")
    )

    val genreCnt = mutableMapOf<String, Int>()
    var totalCnt = 0

    urls.forEach { url ->
        if (analyzeTag(genreCnt, url)) {
            totalCnt++
        }
    }

    println("\n作品数: ${urls.size}, 失敗: ${urls.size - totalCnt}\n")
    println(
        genreCnt.map { it.toPair() }
            .sortedByDescending { it.second }
            .joinToString("\n") {
                "%s: %.2f %%".format(it.first, (it.second.toDouble() / totalCnt) * 100)
            }
    )
}

fun analyzeTag(genreCnt: MutableMap<String, Int>, url: String, failurCnt: Int = 0): Boolean {
    try {
        when {
            failurCnt == 0 -> println("Analyzing: $url")
            failurCnt < 5 -> println("Retrying: $url")
            else -> {
                println("Error: $url")
                return false
            }
        }
        val voicePage = requestByGet(url).parse()
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
        return analyzeTag(genreCnt, url, failurCnt + 1)
    } catch (e: UnknownHostException) {
        return analyzeTag(genreCnt, url, failurCnt + 1)
    } catch (e: Exception) {
        e.printStackTrace()
        return analyzeTag(genreCnt, url, failurCnt + 1)
    }
}

fun requestByGet(url: String, cookies: Map<String, String>? = null, data: Map<String, String>? = null): Connection.Response {
    var connection = getConnection(url)
    if (cookies != null) {
        connection = connection.cookies(cookies)
    }
    if (data != null) {
        connection = connection.data(data)
    }
    return connection.method(Connection.Method.GET).execute()
}

fun requestByPost(url: String, cookies: Map<String, String>? = null, data: Map<String, String>? = null): Connection.Response {
    var connection = getConnection(url)
    if (cookies != null) {
        connection = connection.cookies(cookies)
    }
    if (data != null) {
        connection = connection.data(data)
    }
    return connection.method(Connection.Method.POST).execute()
}

fun getConnection(url: String): Connection {
    Thread.sleep(500)
    return Jsoup.connect(url).timeout(100000)
}