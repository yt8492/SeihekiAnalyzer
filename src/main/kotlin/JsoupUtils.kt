import org.jsoup.Connection
import org.jsoup.Jsoup

object JsoupUtils {
    @JvmStatic
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

    @JvmStatic
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

    @JvmStatic
    fun getConnection(url: String): Connection {
        Thread.sleep(1000)
        return Jsoup.connect(url).timeout(100000)
    }
}