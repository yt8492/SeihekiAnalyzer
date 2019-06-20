import java.net.UnknownHostException

fun main() {
    try {
        JsoupUtils.getConnection("https://www.dlsite.com/index.html", 0).get()
    } catch (e: UnknownHostException) {
        println("DLsiteへの接続が失敗しました。ネットワークの状態を確認してください。")
        return
    }

    val console = System.console() ?: null

    print("UserId: ")
    val userId = console?.readLine() ?: readLine() ?: ""
    print("Password: ")
    val password = console?.readPassword()?.joinToString("") ?: readLine() ?: ""
    SeihekiAnalyzer.login(userId, password, { println("ログインに成功しました。") }, { println("ログインに失敗しました。") })
        ?.exec(object : SeihekiAnalyzer.Callback {
            override fun onStartUrlFetch() {
                println("購入履歴取得中...")
            }

            override fun onUrlsFetchSuccess(urls: List<String>) {
                println(
                    urls.mapIndexed { index, url ->
                        "${"%3d".format(index + 1)}: $url"
                    }.joinToString("\n")
                )
            }

            override fun onAnalyzingTag(url: String, retryFlg: Boolean) {
                if (!retryFlg) {
                    println("Analyzing: $url")
                } else {
                    println("Retrying: $url")
                }
            }

            override fun onTagAnalyzeFailure(url: String) {
                println("Error: $url")
            }

            override fun onFinishTagsAnalyze(tags: List<SeihekiAnalyzer.Tag>, urls: List<String>, totalCnt: Int) {
                println("\n作品数: ${urls.size}, 失敗: ${urls.size - totalCnt}\n")
                println(
                    tags.sortedByDescending { it.count }.joinToString(separator = "\n") {
                        "%s: %.2f %%".format(it.name, (it.count.toDouble() / totalCnt) * 100)
                    }
                )
            }
        })
}