/*
 * KuruBeats (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.kurubeats.app.lyrics

import android.content.Context
import com.kurubeats.app.constants.EnablePaxsenixLyricsKey
import com.kurubeats.app.paxsenix.PaxsenixLyrics
import com.kurubeats.app.utils.dataStore
import com.kurubeats.app.utils.get

object PaxsenixLyricsProvider : LyricsProvider {
    override val name = "Paxsenix (Auto)"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnablePaxsenixLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = PaxsenixLyrics.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        PaxsenixLyrics.getAllLyrics(title, artist, duration, callback)
    }
}
