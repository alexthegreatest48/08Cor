import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dto.Author
import dto.Post
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply{ level = HttpLoggingInterceptor.Level.BODY
    })
    .build()

private const val BASE_URL = "http://127.0.0.1:9999/api"
private val gson = Gson()

suspend fun OkHttpClient.apiCall(url: String): Response{
    return suspendCoroutine {continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(::newCall).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
            })
    }
}

suspend fun <T> makeRequest(url: String, typeToken: TypeToken<T>): T =
    withContext(Dispatchers.IO){
        gson.fromJson(client.apiCall(url).body?.string(), typeToken)
    }


fun main(){
    CoroutineScope(EmptyCoroutineContext).launch {
        val posts = makeRequest(url = "$BASE_URL/posts", object : TypeToken<List<Post>>() {})
        posts.map{post ->
           async{ makeRequest(url = "$BASE_URL/authors/${post.authorId}", object : TypeToken<Author>(){})}
        }.awaitAll()
    }
    Thread.sleep(100000)
}
