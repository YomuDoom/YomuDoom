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

    fun isImage(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return true
        // PNG: 89 50 4E 47
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) return true
        // GIF: 47 49 46
        if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte()) return true
        // WEBP: RIFF....WEBP
        if (bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
        ) return true
        // BMP: 42 4D
        if (bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()) return true
        return false
    }

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
                    if (isImage(bytes)) {
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

        val queries = generateTitleQueries(title)
        if (queries.isNotEmpty()) {
            coroutineScope {
                val anilistDeferred = async(Dispatchers.IO) { fetchAniListCovers(queries) }
                val kitsuDeferred = async(Dispatchers.IO) { fetchKitsuCovers(queries) }
                val mangaDexDeferred = async(Dispatchers.IO) { fetchMangaDexCovers(queries) }

                val fetched = mutableListOf<AvailableCover>()
                fetched.addAll(anilistDeferred.await())
                fetched.addAll(kitsuDeferred.await())
                fetched.addAll(mangaDexDeferred.await())
                results.addAll(fetched.sortedByDescending { scoreTitleMatch(title, it.description) })
            }
        }

        // Capa original registrada como opção complementar
        if (!existingUrl.isNullOrBlank() && (existingUrl.startsWith("http://") || existingUrl.startsWith("https://"))) {
            results.add(
                AvailableCover(
                    url = existingUrl,
                    description = "Capa original da fonte",
                    source = "Fonte",
                    data = mangaData ?: existingUrl,
                ),
            )
        }

        results.distinctBy { it.url }
    }

    private fun fetchAniListCovers(queries: List<String>): List<AvailableCover> {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        val covers = mutableListOf<AvailableCover>()

        val graphQuery = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 6) {
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
                val url = "https://kitsu.io/api/edge/manga?filter%5Btext%5D=$encoded&page%5Blimit%5D=5"
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
                val searchUrl = "https://api.mangadex.org/manga?title=$encoded&limit=5&includes[]=cover_art" +
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
        val queries = generateTitleQueries(title)
        val onlineCovers = mutableListOf<AvailableCover>()
        if (queries.isNotEmpty()) {
            coroutineScope {
                val anilistDeferred = async(Dispatchers.IO) { fetchAniListCovers(queries) }
                val kitsuDeferred = async(Dispatchers.IO) { fetchKitsuCovers(queries) }
                val mangaDexDeferred = async(Dispatchers.IO) { fetchMangaDexCovers(queries) }

                onlineCovers.addAll(anilistDeferred.await())
                onlineCovers.addAll(kitsuDeferred.await())
                onlineCovers.addAll(mangaDexDeferred.await())
            }
        }

        // Tenta primeiro obter capa dos provedores oficiais online (AniList, Kitsu, MangaDex)
        val sortedCovers = onlineCovers.distinctBy { it.url }
            .sortedByDescending { scoreTitleMatch(title, it.description) }
        for (cover in sortedCovers) {
            val stream = fetchStreamFromUrl(cover.url)
            if (stream != null) {
                return@withContext stream
            }
        }

        // Se nenhum provedor online retornou, tenta a URL existente (se for imagem válida)
        if (!existingUrl.isNullOrBlank() && (existingUrl.startsWith("http://") || existingUrl.startsWith("https://"))) {
            val stream = fetchStreamFromUrl(existingUrl)
            if (stream != null) {
                return@withContext stream
            }
        }

        null
    }

    private fun scoreTitleMatch(query: String, candidate: String): Int {
        val q = query.trim().lowercase().replace("[^a-z0-9]".toRegex(), "")
        val c = candidate.trim().lowercase().replace("[^a-z0-9]".toRegex(), "")
        return when {
            q == c -> 100
            c.startsWith(q) && !c.contains("side") && !c.contains("relax") && !c.contains("spinoff") && !c.contains("spin-off") -> 80
            c.startsWith(q) -> 50
            c.contains(q) -> 40
            else -> 10
        }
    }
}
