import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.SocketTimeoutException

fun main() {
    val loginUrl = "https://login.dlsite.com/login"
    val mainUrl = "https://ssl.dlsite.com/maniax/mypage/userbuy"

    val console = System.console() ?: null

    print("UserId: ")
    val userId = console?.readLine() ?: readLine()
    print("Password: ")
    val password = console?.readPassword()?.joinToString("") ?: readLine()

    val res1 = Jsoup.connect(loginUrl)
        .method(Connection.Method.GET)
        .execute()
    val welcomePage = res1.parse()
    val welcomeCookie = res1.cookies()
    val token = welcomePage.select("[name=_token]").first()
    val tokenKey = token.attr("name")
    val tokenValue = token.attr("value")

    Thread.sleep(1000)

    val cookies = Jsoup.connect(loginUrl)
        .data(tokenKey, tokenValue)
        .data("login_id", userId)
        .data("password", password)
        .cookies(welcomeCookie)
        .followRedirects(false)
        .userAgent("Mozilla")
        .method(Connection.Method.POST)
        .execute()
        .cookies()

    println(cookies.map { "${it.key}: ${it.value}" }.joinToString("\n"))

    Thread.sleep(1000)

    val historyCookies = Jsoup.connect(mainUrl)
        .followRedirects(true)
        .userAgent("Mozilla")
        .cookies(cookies)
        .method(Connection.Method.POST)
        .execute()
        .cookies()

    Thread.sleep(1000)

    val thisMonthHistory = Jsoup.connect(mainUrl)
        .cookies(historyCookies)
        .userAgent("Mozilla")
        .get()
        .body()

    Thread.sleep(1000)

    val pastMonthHistory = Jsoup.connect("$mainUrl/complete")
        .cookies(historyCookies)
        .data("_layout", "mypage_userbuy_complete")
        .data("_form_id", "mypageUserbuyCompleteForm")
        .data("_site", "maniax")
        .data("_view", "input")
        .data("start", "all")
        .userAgent("Mozilla")
        .get()
        .body()

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

    println(urls.joinToString("\n"))

    val genreCnt = mutableMapOf<String, Int>()
    var totalCnt = 0

    urls.forEach { url ->
        try {
            println("Analyzing: $url ...")
            val voicePage = Jsoup.connect(url)
                .userAgent("Mozilla")
                .get()
                .body()
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
            totalCnt++
        } catch (e: SocketTimeoutException) {
            println("error: $url")
        } finally {
            Thread.sleep(500)
        }
    }
    println(genreCnt.map { it.toPair() }.sortedByDescending { it.second }.joinToString("\n"){"%s: %.2f %%".format(it.first, (it.second.toDouble() / totalCnt) * 100)})
}