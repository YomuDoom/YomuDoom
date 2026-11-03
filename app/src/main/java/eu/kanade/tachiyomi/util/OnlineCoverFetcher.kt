package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import logcat.LogPriority
import okhttp3.Request
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLEncoder

data class AvailableCover(
    val url: String,
    val description: String,
    val source: String,
    val data: Any = url,
)

object OnlineCoverFetcher {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchStreamFromUrl(url: String): InputStream? = withContext(Dispatchers.IO) {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        try {
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                )
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body
                if (body != null) {
                    val bytes = body.bytes()
                    if (bytes.isNotEmpty()) {
                        ByteArrayInputStream(bytes)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logcat(LogPriority.DEBUG, e)
            null
        }
    }

    private fun generateTitleQueries(title: String): List<String> {
        val queries = mutableListOf<String>()
        val cleanTitle = title.replace(Regex("\\[.*?\\]|\\(.*?\\)"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleanTitle.isNotBlank()) queries.add(cleanTitle)
        if (title != cleanTitle && title.isNotBlank()) queries.add(title.trim())
        if (cleanTitle.contains(" - ")) queries.add(cleanTitle.substringBefore(" - ").trim())
        if (cleanTitle.contains(":")) queries.add(cleanTitle.substringBefore(":").trim())
        if (cleanTitle.contains("/")) queries.add(cleanTitle.substringBefore("/").trim())
        return queries.distinct()
    }

    suspend fun getAvailableCovers(
        title: String,
        existingUrl: String? = null,
        mangaData: Any? = null,
    ): List<AvailableCover> = withContext(Dispatchers.IO) {
        val results = mutableListOf<AvailableCover>()

        // 1. Capa original registrada, se houver
        if (!existingUrl.isNullOrBlank() && (existingUrl.startsWith("http://") || existingUrl.startsWith("https://"))) {
            results.add(
                AvailableCover(
                    url = existingUrl,
                    description = "Capa original",
                    source = "Fonte",
                    data = mangaData ?: existingUrl,
                ),
            )
        }

        val queries = generateTitleQueries(title)
        if (queries.isEmpty()) return@withContext results

        coroutineScope {
            val mangaDexDeferred = async(Dispatchers.IO) { fetchMangaDexCovers(queries) }
            val kitsuDeferred = async(Dispatchers.IO) { fetchKitsuCovers(queries) }

            results.addAll(mangaDexDeferred.await())
            results.addAll(kitsuDeferred.await())
        }

        results.distinctBy { it.url }
    }

    private fun fetchMangaDexCovers(queries: List<String>): List<AvailableCover> {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        val covers = mutableListOf<AvailableCover>()

        for (query in queries) {
            if (query.isBlank()) continue
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val searchUrl = "https://api.mangadex.org/manga?title=$encoded&limit=5" +
                    "&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic"
                val searchReq = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "YomuDoom/1.0 (https://github.com/YomuDoom/YomuDoom)")
                    .build()
                val searchResp = client.newCall(searchReq).execute()
                if (searchResp.isSuccessful) {
                    val bodyStr = searchResp.body.string()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val mangaList = root["data"]?.jsonArray.orEmpty()

                    for (mangaElem in mangaList) {
                        val mangaObj = mangaElem.jsonObject
                        val mangaId = mangaObj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                        val covUrl = "https://api.mangadex.org/cover?manga[]=$mangaId&limit=100&order[volume]=asc"
                        val covReq = Request.Builder()
                            .url(covUrl)
                            .header("User-Agent", "YomuDoom/1.0 (https://github.com/YomuDoom/YomuDoom)")
                            .build()
                        val covResp = client.newCall(covReq).execute()
                        if (covResp.isSuccessful) {
                            val covBodyStr = covResp.body.string()
                            val covRoot = json.parseToJsonElement(covBodyStr).jsonObject
                            val covList = covRoot["data"]?.jsonArray.orEmpty()

                            data class ParsedMangaDexCover(
                                val url: String,
                                val volume: String?,
                                val volumeNum: Float?,
                                val locale: String?,
                            )

                            val parsedCovers = mutableListOf<ParsedMangaDexCover>()
                            for (c in covList) {
                                val cObj = c.jsonObject
                                val attr = cObj["attributes"]?.jsonObject ?: continue
                                val fileName = attr["fileName"]?.jsonPrimitive?.contentOrNull ?: continue
                                val volume = attr["volume"]?.jsonPrimitive?.contentOrNull
                                val locale = attr["locale"]?.jsonPrimitive?.contentOrNull
                                val volumeNum = volume?.toFloatOrNull()
                                val imgUrl = "https://uploads.mangadex.org/covers/$mangaId/$fileName.512.jpg"
                                parsedCovers.add(ParsedMangaDexCover(imgUrl, volume, volumeNum, locale))
                            }

                            // Ordena capas por volume
                            val sorted = parsedCovers.sortedWith(
                                compareBy(
                                    { it.volumeNum ?: Float.MAX_VALUE },
                                    { it.volume ?: "" },
                                ),
                            )

                            for (item in sorted) {
                                val desc = if (!item.volume.isNullOrBlank()) "Vol. ${item.volume}" else "Capa"
                                val finalDesc = if (!item.locale.isNullOrBlank() && item.locale != "ja") {
                                    "$desc (${item.locale})"
                                } else {
                                    desc
                                }
                                covers.add(
                                    AvailableCover(
                                        url = item.url,
                                        description = finalDesc,
                                        source = "MangaDex",
                                        data = item.url,
                                    ),
                                )
                            }
                        }
                    }

                    if (covers.isNotEmpty()) {
                        break
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e)
            }
        }
        return covers
    }

    private fun fetchKitsuCovers(queries: List<String>): List<AvailableCover> {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        val covers = mutableListOf<AvailableCover>()

        for (query in queries) {
            if (query.isBlank()) continue
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://kitsu.io/api/edge/manga?filter%5Btext%5D=$encoded&page%5Blimit%5D=10"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "YomuDoom/1.0 (https://github.com/YomuDoom/YomuDoom)")
                    .header("Accept", "application/vnd.api+json")
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body.string()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val dataList = root["data"]?.jsonArray.orEmpty()

                    for (item in dataList) {
                        val itemObj = item.jsonObject
                        val attr = itemObj["attributes"]?.jsonObject ?: continue
                        val posterObj = attr["posterImage"]?.jsonObject
                        val imgUrl = posterObj?.get("large")?.jsonPrimitive?.contentOrNull
                            ?: posterObj?.get("original")?.jsonPrimitive?.contentOrNull
                            ?: posterObj?.get("medium")?.jsonPrimitive?.contentOrNull

                        if (!imgUrl.isNullOrBlank()) {
                            val canonTitle = attr["canonicalTitle"]?.jsonPrimitive?.contentOrNull
                            val subtype = attr["subtype"]?.jsonPrimitive?.contentOrNull
                            val desc = if (!subtype.isNullOrBlank()) {
                                "${canonTitle ?: "Poster"} ($subtype)"
                            } else {
                                canonTitle ?: "Poster"
                            }
                            covers.add(
                                AvailableCover(
                                    url = imgUrl,
                                    description = desc,
                                    source = "Kitsu",
                                    data = imgUrl,
                                ),
                            )
                        }
                    }

                    if (covers.isNotEmpty()) {
                        break
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e)
            }
        }
        return covers
    }

    suspend fun fetchCoverStream(title: String, existingUrl: String? = null): InputStream? = withContext(Dispatchers.IO) {
        val available = getAvailableCovers(title, existingUrl)
        for (cover in available) {
            val stream = fetchStreamFromUrl(cover.url)
            if (stream != null) {
                return@withContext stream
            }
        }
        null
    }
}
