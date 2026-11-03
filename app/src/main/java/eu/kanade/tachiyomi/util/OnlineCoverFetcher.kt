package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import logcat.LogPriority
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    private const val USER_AGENT = "YomuDoom/1.0 (https://github.com/YomuDoom/YomuDoom)"

    suspend fun fetchStreamFromUrl(url: String): InputStream? = withContext(Dispatchers.IO) {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
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
            val anilistDeferred = async(Dispatchers.IO) { fetchAniListCovers(queries) }
            val kitsuDeferred = async(Dispatchers.IO) { fetchKitsuCovers(queries) }
            val mangaDexDeferred = async(Dispatchers.IO) { fetchMangaDexCovers(queries) }

            results.addAll(anilistDeferred.await())
            results.addAll(kitsuDeferred.await())
            results.addAll(mangaDexDeferred.await())
        }

        results.distinctBy { it.url }
    }

    private fun fetchAniListCovers(queries: List<String>): List<AvailableCover> {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        val covers = mutableListOf<AvailableCover>()

        val graphQuery = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 3) {
                media(search: ${'$'}search, type: MANGA) {
                  id
                  title {
                    english
                    romaji
                  }
                  coverImage {
                    extraLarge
                    large
                  }
                  format
                }
              }
            }
        """.trimIndent()

        for (query in queries) {
            if (query.isBlank()) continue
            try {
                val payload = buildJsonObject {
                    put("query", graphQuery)
                    put(
                        "variables",
                        buildJsonObject {
                            put("search", query)
                        },
                    )
                }.toString()

                val req = Request.Builder()
                    .url("https://graphql.anilist.co")
                    .header("User-Agent", USER_AGENT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body.string()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val mediaList = root["data"]?.jsonObject?.get("Page")?.jsonObject?.get("media")?.jsonArray.orEmpty()

                    for (m in mediaList) {
                        val mObj = m.jsonObject
                        val titleObj = mObj["title"]?.jsonObject
                        val englishTitle = titleObj?.get("english")?.jsonPrimitive?.contentOrNull
                        val romajiTitle = titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull
                        val coverObj = mObj["coverImage"]?.jsonObject
                        val imgUrl = coverObj?.get("extraLarge")?.jsonPrimitive?.contentOrNull
                            ?: coverObj?.get("large")?.jsonPrimitive?.contentOrNull

                        if (!imgUrl.isNullOrBlank()) {
                            val desc = englishTitle ?: romajiTitle ?: "Poster"
                            covers.add(
                                AvailableCover(
                                    url = imgUrl,
                                    description = desc,
                                    source = "AniList",
                                    data = imgUrl,
                                ),
                            )
                        }
                    }

                    if (covers.isNotEmpty()) break
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
                val url = "https://kitsu.io/api/edge/manga?filter%5Btext%5D=$encoded&page%5Blimit%5D=3"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
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

                    if (covers.isNotEmpty()) break
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e)
            }
        }
        return covers
    }

    private fun fetchMangaDexCovers(queries: List<String>): List<AvailableCover> {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        val covers = mutableListOf<AvailableCover>()

        for (query in queries) {
            if (query.isBlank()) continue
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val searchUrl = "https://api.mangadex.org/manga?title=$encoded&limit=3&includes[]=cover_art" +
                    "&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic"
                val searchReq = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
                val searchResp = client.newCall(searchReq).execute()
                if (searchResp.isSuccessful) {
                    val bodyStr = searchResp.body.string()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val mangaList = root["data"]?.jsonArray.orEmpty()

                    for (mangaElem in mangaList) {
                        val mangaObj = mangaElem.jsonObject
                        val mangaId = mangaObj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                        val attr = mangaObj["attributes"]?.jsonObject
                        val titleMap = attr?.get("title")?.jsonObject
                        val mangaTitle = titleMap?.values?.firstOrNull()?.jsonPrimitive?.contentOrNull ?: "Capa oficial"

                        val rels = mangaObj["relationships"]?.jsonArray.orEmpty()
                        var coverFile: String? = null
                        for (rel in rels) {
                            val relObj = rel.jsonObject
                            if (relObj["type"]?.jsonPrimitive?.contentOrNull == "cover_art") {
                                val relAttr = relObj["attributes"]?.jsonObject
                                coverFile = relAttr?.get("fileName")?.jsonPrimitive?.contentOrNull
                                if (!coverFile.isNullOrBlank()) break
                            }
                        }

                        if (!coverFile.isNullOrBlank()) {
                            val imgUrl = "https://uploads.mangadex.org/covers/$mangaId/$coverFile.512.jpg"
                            covers.add(
                                AvailableCover(
                                    url = imgUrl,
                                    description = mangaTitle,
                                    source = "MangaDex",
                                    data = imgUrl,
                                ),
                            )
                        }
                    }

                    if (covers.isNotEmpty()) break
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e)
            }
        }
        return covers
    }

    suspend fun fetchCoverStream(title: String, existingUrl: String? = null): InputStream? = withContext(
        Dispatchers.IO,
    ) {
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
