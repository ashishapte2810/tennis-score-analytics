package com.tennis.tracker.wear.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.tennis.tracker.wear.MainActivity

const val PREF_FILE     = "tennis_complication"
const val PREF_SCORE    = "score"       // e.g. "3–2" (games) or "–" when idle
const val PREF_DETAIL   = "detail"      // e.g. "40–30" (current point) or "No match"
const val PREF_MATCH_ID = "match_id"    // last active match ID — restored on cold start

class TennisComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return buildData(context = this, score = "3–2", detail = "40–30")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        val score  = prefs.getString(PREF_SCORE, null)
        val detail = prefs.getString(PREF_DETAIL, null)
        return buildData(this, score ?: "🎾", detail ?: "Open app")
    }

    private fun buildData(context: Context, score: String, detail: String): ShortTextComplicationData {
        val tapIntent = Intent(context, MainActivity::class.java)
        val tapAction = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return ShortTextComplicationData.Builder(
            text  = PlainComplicationText.Builder(score).build(),
            contentDescription = PlainComplicationText.Builder("Tennis score: $score").build()
        )
            .setTitle(PlainComplicationText.Builder(detail).build())
            .setTapAction(tapAction)
            .build()
    }
}
