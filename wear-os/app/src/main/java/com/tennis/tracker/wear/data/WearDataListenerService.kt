package com.tennis.tracker.wear.data

import android.content.Intent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.tennis.tracker.wear.MainActivity

/**
 * Receives match ID pushed from the phone app via Wearable Data Layer.
 * The phone calls: Wearable.getDataClient(context).putDataItem(...)
 * with path "/match" and key "matchId".
 */
class WearDataListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            val item = event.dataItem
            if (item.uri.path == "/match") {
                val matchId = com.google.android.gms.wearable.DataMapItem
                    .fromDataItem(item).dataMap.getString("matchId") ?: return@forEach
                // Broadcast to MainActivity so it can update the UI
                sendBroadcast(Intent(MainActivity.ACTION_MATCH_RECEIVED).apply {
                    putExtra(MainActivity.EXTRA_MATCH_ID, matchId)
                })
            }
        }
    }
}
