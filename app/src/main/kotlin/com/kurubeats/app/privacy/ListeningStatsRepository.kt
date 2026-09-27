/*
 * KuruBeats (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.kurubeats.app.privacy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kurubeats.app.db.MusicDatabase
import javax.inject.Inject

class ListeningStatsRepository @Inject constructor(
    private val database: MusicDatabase,
) {
    suspend fun reset() = withContext(Dispatchers.IO) {
        database.withTransaction {
            clearListenHistory()
            resetTotalPlayTime()
            clearPlayCounts()
        }
    }
}
