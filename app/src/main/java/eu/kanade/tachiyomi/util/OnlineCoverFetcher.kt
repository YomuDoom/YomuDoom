package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.network.NetworkHelper
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

object OnlineCoverFetcher {

    suspend fun fetchCoverStream(title: String, existingUrl: String? = null): InputStream? {
        val networkHelper: NetworkHelper = Injekt.get()
        val client = networkHelper.client
        val json = Json { ignoreUnknownKeys = true }

        // 1. Se existir uma thumbnailUrl já registrada, tenta baixar diretamente primeiro
        if (!existingUrl.isNullOrBlank() && (existingUrl.startsWith("http://") || existingUrl.startsWith("https://"))) {
            try {
                val req = Request.Builder().url(existingUrl).build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful && resp.body != null) {
                    val bytes = resp.body!!.bytes()
                    if (bytes.isNotEmpty()) {
                        return ByteArrayInputStream(bytes)
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.DEBUG, e)
            }
        }

        // Gera variações de busca a partir do título
        val queries = mutableListOf<String>()
        val cleanTitle = title.replace(Regex("\\[.*?\\]|\\(.*?\\)"), "").trim()
        if (cleanTitle.isNotBlank()) queries.add(cleanTitle)
        if (title != cleanTitle && title.isNotBlank()) queries.add(title)
        if (cleanTitle.contains(" - ")) queries.add(cleanTitle.substringBefore(" - ").trim())
        if (cleanTitle.contains(":")) queries.add(cleanTitle.substringBefore(":").trim())

        // 2. Kitsu API
        for (query in queries.distinct()) {
            if (query.isBlank()) continue
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://kitsu.io/api/edge/manga?filter[text]=$encoded&page[limit]=1"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "YomuDoom/1.0")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val data = root["data"]?.jsonArray
                    val posterObj = data?.firstOrNull()?.jsonObject
                        ?.get("attributes")?.jsonObject
                        ?.get("posterImage")?.jsonObject
                    val imageUrl = posterObj?.get("large")?.jsonPrimitive?.contentOrNull
                        ?: posterObj?.get("original")?.jsonPrimitive?.contentOrNull

                    if (!imageUrl.isNullOrBlank()) {
                        val imgReq = Request.Builder().url(imageUrl).build()
                        val imgResp = client.newCall(imgReq).execute()
                        if (imgResp.isSuccessful && imgResp.body != null) {
                            val bytes = imgResp.body!!.bytes()
                            if (bytes.isNotEmpty()) {
                                return ByteArrayInputStream(bytes)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e)
            }
        }

        // 3. Fallback MangaDex API
        for (query in queries.distinct()) {
            if (query.isBlank()) continue
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://api.mangadex.org/manga?title=$encoded&limit=1"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "YomuDoom/1.0")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val data = root["data"]?.jsonArray
                    val firstManga = data?.firstOrNull()?.jsonObject
                    val mangaId = firstManga?.get("id")?.jsonPrimitive?.contentOrNull
                    val relationships = firstManga?.get("relationships")?.jsonArray
                    val coverId = relationships?.firstOrNull {
                        it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "cover_art"
                    }?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull

                    if (mangaId != null && coverId != null) {
                        val covReq = Request.Builder()
                            .url("https://api.mangadex.org/cover/$coverId")
                            .header("User-Agent", "YomuDoom/1.0")
                            .build()
                        val covResp = client.newCall(covReq).execute()
                        if (covResp.isSuccessful) {
                            val covBody = covResp.body?.string().orEmpty()
                            val covRoot = json.parseToJsonElement(covBody).jsonObject
                            val fileName = covRoot["data"]?.jsonObject
                                ?.get("attributes")?.jsonObject
                                ?.get("fileName")?.jsonPrimitive?.contentOrNull

                            if (!fileName.isNullOrBlank()) {
                                val finalUrl = "https://uploads.mangadex.org/covers/$mangaId/$fileName.512.jpg"
                                val imgResp = client.newCall(Request.Builder().url(finalUrl).build()).execute()
                                if (imgResp.isSuccessful && imgResp.body != null) {
                                    val bytes = imgResp.body!!.bytes()
                                    if (bytes.isNotEmpty()) {
                                        return ByteArrayInputStream(bytes)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e)
            }
        }

        return null
    }
}
