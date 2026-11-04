package eu.kanade.tachiyomi.util

import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.lang.Hash
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
import java.io.File
import java.io.InputStream
import java.net.URLEncoder

data class AvailableCover(
    val url: String,
    val description: String,
    val source: String,
    val data: Any = url,
    val altTitles: List<String> = emptyList(),
)

object OnlineCoverFetcher {

    private val json = Json { ignoreUnknownKeys = true }
    private const val USER_AGENT = "YomuDoom/1.0 (https://github.com/YomuDoom/YomuDoom)"
    private const val MANGADEX_PLACEHOLDER_MD5 = "694855f70d4887848147c6debe9c96dc"

    fun isPlaceholder(file: File): Boolean {
        if (!file.exists()) return false
        val len = file.length()
        if (len == 59480L) {
            return try {
                val bytes = file.readBytes()
                isPlaceholder(bytes)
            } catch (e: Exception) {
                true
            }
        }
        if (len in 20000L..100000L) {
            return try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, opts)
                opts.outWidth == 600 && opts.outHeight == 642
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    fun isPlaceholder(bytes: ByteArray): Boolean {
        if (bytes.size == 59480) {
            val md5 = Hash.md5(bytes)
            if (md5.equals(MANGADEX_PLACEHOLDER_MD5, ignoreCase = true)) {
                return true
            }
        }
        if (bytes.size in 20000..100000) {
            return try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                opts.outWidth == 600 && opts.outHeight == 642
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    fun isImage(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return true
        // PNG: 89 50 4E 47
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte()
        ) {
            return true
        }
        // GIF: 47 49 46
        if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte()) return true
        // WEBP: RIFF....WEBP
        if (bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && bytes[10] == 0x42.toByte() &&
            bytes[11] == 0x50.toByte()
        ) {
            return true
        }
        // BMP: 42 4D
        if (bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()) return true
        return false
    }

    suspend fun fetchStreamFromUrl(url: String): InputStream? = withContext(Dispatchers.IO) {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
            if (url.contains("mangadex.org")) {
                reqBuilder.header("Referer", "https://mangadex.org/")
            }
            val req = reqBuilder.build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val bytes = resp.body.bytes()
                if (isImage(bytes) && !isPlaceholder(bytes)) {
                    ByteArrayInputStream(bytes)
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

        for (prefix in listOf("O ", "A ", "Os ", "As ", "The ", "o ", "a ", "the ")) {
            if (cleanTitle.startsWith(prefix, ignoreCase = true) && cleanTitle.length > prefix.length) {
                queries.add(cleanTitle.substring(prefix.length).trim())
            }
        }

        val words = cleanTitle.split(" ").filter { it.isNotBlank() }
        if (words.size > 3) {
            val stopWords = setOf(
                "o", "a", "os", "as", "de", "do", "da", "dos", "das", "em", "no", "na",
                "e", "que", "não", "nao", "the", "of", "in", "to",
            )
            val significant = words.filter { it.lowercase() !in stopWords }
            if (significant.size >= 2) {
                queries.add(significant.take(2).joinToString(" "))
            }
        }

        return queries.filter { it.length >= 3 }.distinct()
    }

    suspend fun getAvailableCovers(
        title: String,
        existingUrl: String? = null,
        mangaData: Any? = null,
    ): List<AvailableCover> = withContext(Dispatchers.IO) {
        val results = mutableListOf<AvailableCover>()

        // Capa original registrada como opção preferencial/complementar
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

        val queries = generateTitleQueries(title)
        if (queries.isNotEmpty()) {
            coroutineScope {
                val anilistDeferred = async(Dispatchers.IO) { fetchAniListCovers(queries) }
                val mangaDexDeferred = async(Dispatchers.IO) { fetchMangaDexCovers(queries) }
                val kitsuDeferred = async(Dispatchers.IO) { fetchKitsuCovers(queries) }

                val fetched = mutableListOf<AvailableCover>()
                fetched.addAll(anilistDeferred.await())
                fetched.addAll(mangaDexDeferred.await())
                fetched.addAll(kitsuDeferred.await())
                results.addAll(
                    fetched.map { it to scoreCoverMatch(title, it) }
                        .filter { it.second >= 40 }
                        .sortedWith(
                            compareByDescending<Pair<AvailableCover, Int>> { it.second }
                                .thenByDescending { (cover, _) ->
                                    when (cover.source) {
                                        "AniList" -> 3
                                        "MangaDex" -> 2
                                        "Kitsu" -> 1
                                        else -> 0
                                    }
                                },
                        )
                        .map { it.first },
                )
            }
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
                    native
                  }
                  synonyms
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
                        val nativeTitle = titleObj?.get("native")?.jsonPrimitive?.contentOrNull
                        val synonyms = mObj["synonyms"]?.jsonArray.orEmpty().mapNotNull {
                            it.jsonPrimitive.contentOrNull
                        }
                        val allAlts = (synonyms + listOfNotNull(englishTitle, romajiTitle, nativeTitle)).distinct()
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
                                    altTitles = allAlts,
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
                        val altTitlesList = attr?.get("altTitles")?.jsonArray.orEmpty().mapNotNull { altObj ->
                            altObj.jsonObject.values.firstOrNull()?.jsonPrimitive?.contentOrNull
                        }

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
                                    altTitles = altTitlesList,
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
        // 1. SEMPRE TENTA A URL ORIGINAL DA FONTE PRIMEIRO!
        if (!existingUrl.isNullOrBlank() && (existingUrl.startsWith("http://") || existingUrl.startsWith("https://"))) {
            val stream = fetchStreamFromUrl(existingUrl)
            if (stream != null) {
                return@withContext stream
            }
        }

        // 2. Se a URL original falhou (404, link morto, placeholder anti-hotlink descartado):
        val queries = generateTitleQueries(title)
        val onlineCovers = mutableListOf<AvailableCover>()
        if (queries.isNotEmpty()) {
            coroutineScope {
                val anilistDeferred = async(Dispatchers.IO) { fetchAniListCovers(queries) }
                val mangaDexDeferred = async(Dispatchers.IO) { fetchMangaDexCovers(queries) }
                val kitsuDeferred = async(Dispatchers.IO) { fetchKitsuCovers(queries) }

                onlineCovers.addAll(anilistDeferred.await())
                onlineCovers.addAll(mangaDexDeferred.await())
                onlineCovers.addAll(kitsuDeferred.await())
            }
        }

        // EXIGE STRICT MATCH: score >= 60! NUNCA aceita obra aleatória.
        val validCovers = onlineCovers.distinctBy { it.url }
            .map { it to scoreCoverMatch(title, it) }
            .filter { it.second >= 60 }
            .sortedWith(
                compareByDescending<Pair<AvailableCover, Int>> { it.second }
                    .thenByDescending { (cover, _) ->
                        when (cover.source) {
                            "AniList" -> 3
                            "MangaDex" -> 2
                            "Kitsu" -> 1
                            else -> 0
                        }
                    },
            )

        for ((cover, _) in validCovers) {
            val stream = fetchStreamFromUrl(cover.url)
            if (stream != null) {
                return@withContext stream
            }
        }

        null
    }

    fun scoreCoverMatch(query: String, cover: AvailableCover): Int {
        val mainScore = scoreTitleMatch(query, cover.description)
        val altScore = cover.altTitles.maxOfOrNull { scoreTitleMatch(query, it) } ?: 0
        return maxOf(mainScore, altScore)
    }

    fun scoreTitleMatch(query: String, candidate: String): Int {
        val q = query.trim().lowercase().replace("[^a-z0-9]".toRegex(), "")
        val c = candidate.trim().lowercase().replace("[^a-z0-9]".toRegex(), "")
        if (q.isBlank() || c.isBlank()) return 0
        return when {
            q == c -> 100
            c.startsWith(q) && !c.contains("side") && !c.contains("relax") && !c.contains("spinoff") &&
                !c.contains("spin-off") -> 85
            q.startsWith(c) && !q.contains("side") && !q.contains("relax") -> 85
            c.startsWith(q) -> 65
            q.startsWith(c) -> 60
            c.contains(q) && q.length >= 6 -> 55
            q.contains(c) && c.length >= 6 -> 50
            else -> 0
        }
    }
}
