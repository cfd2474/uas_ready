package com.taksolutions.uasready.data.repository

import android.util.Log
import com.taksolutions.uasready.domain.model.TfrNotice
import com.taksolutions.uasready.domain.model.TfrSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Source of every current FAA TFR. Deciding which apply at the launch point is TfrEvaluator's job. */
interface TfrRepository {
    suspend fun getTfrSnapshot(): Result<TfrSnapshot>
}

/** Parsed TFR details kept between refreshes, keyed by NOTAM id + FAA modification stamp. */
interface TfrDetailStore {
    fun load(): List<TfrNotice>
    fun save(notices: List<TfrNotice>)
}

class FileTfrDetailStore(private val file: File) : TfrDetailStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(TfrNotice.serializer())

    override fun load(): List<TfrNotice> = try {
        if (file.exists()) json.decodeFromString(serializer, file.readText()) else emptyList()
    } catch (e: Exception) {
        Log.w(TAG, "Ignoring unreadable TFR cache: ${e.message}")
        emptyList()
    }

    override fun save(notices: List<TfrNotice>) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(serializer, notices))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not write TFR cache: ${e.message}")
        }
    }

    private companion object { const val TAG = "TfrRepo" }
}

/**
 * Live FAA TFR feed (tfr.faa.gov). See docs/PLATFORM_REFERENCE.md "FAA TFR feed".
 *
 * 1. `GET /tfrapi/getTfrList` — every current TFR (id, type, state, modification stamp).
 * 2. `GET /download/detail_<yr>_<seq>.xml` — altitudes, schedule and polygons, fetched only for
 *    NOTAMs that are new or amended since the stored copy.
 *
 * Safety contract: the snapshot is all-or-nothing. If the list or any needed detail cannot be read,
 * the result is a failure and the app reports TFRs as unverified — a partial list could omit the
 * one TFR that covers the launch point.
 */
class LiveTfrRepository(
    private val store: TfrDetailStore,
    private val baseUrl: String = FAA_TFR_BASE_URL,
    private val parser: TfrXmlParser = TfrXmlParser(),
    private val maxConcurrentDetails: Int = 6,
    private val clock: () -> Long = System::currentTimeMillis
) : TfrRepository {

    companion object {
        private const val TAG = "TfrRepo"
        const val FAA_TFR_BASE_URL = "https://tfr.faa.gov"
        private const val TIMEOUT_MS = 20_000

        /** "6/5574" → "detail_6_5574.xml" */
        fun detailFileName(notamId: String): String = "detail_${notamId.trim().replace('/', '_')}.xml"
    }

    private data class ListEntry(val notamId: String, val type: String, val state: String, val description: String, val modified: String)

    override suspend fun getTfrSnapshot(): Result<TfrSnapshot> = withContext(Dispatchers.IO) {
        try {
            val entries = parseList(httpGet("$baseUrl/tfrapi/getTfrList", "application/json"))
            val cached = store.load().associateBy { it.notamId }
            val permits = Semaphore(maxConcurrentDetails)
            var downloaded = 0
            val notices = coroutineScope {
                entries.map { entry ->
                    async {
                        val prior = cached[entry.notamId]
                        if (prior != null && prior.modifiedStamp == entry.modified && entry.modified.isNotEmpty()) {
                            prior.copy(type = entry.type, state = entry.state, description = entry.description)
                        } else {
                            val detail = permits.withPermit {
                                parser.parse(httpGet("$baseUrl/download/${detailFileName(entry.notamId)}", "application/xml"))
                            }
                            synchronized(this@LiveTfrRepository) { downloaded++ }
                            TfrNotice(
                                notamId = entry.notamId,
                                type = entry.type,
                                description = entry.description,
                                state = entry.state,
                                cfrSection = detail.cfrSection,
                                effective = detail.effective,
                                areas = detail.areas,
                                modifiedStamp = entry.modified
                            )
                        }
                    }
                }.awaitAll()
            }
            store.save(notices) // only current NOTAMs are kept, so cancelled TFRs drop out
            Log.i(TAG, "TFR feed: ${notices.size} current TFRs (${notices.count { it.hasGeometry }} mapped), $downloaded details downloaded")
            Result.success(TfrSnapshot(notices = notices, fetchedEpochMs = clock()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "TFR feed unavailable: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseList(body: String): List<ListEntry> {
        val array = JSONArray(body.trimStart('﻿'))
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val id = o.optString("notam_id").trim()
            if (id.isEmpty()) throw IOException("TFR list entry $i has no notam_id")
            ListEntry(
                notamId = id,
                type = o.optString("type").trim(),
                state = o.optString("state").trim(),
                description = o.optString("description").trim(),
                modified = o.optString("mod_abs_time").trim()
            )
        }
    }

    private fun httpGet(url: String, accept: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "UASReady-Android-App/1.0")
        }
        try {
            val code = conn.responseCode
            if (code != 200) throw IOException("TFR request failed: HTTP $code ($url)")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
