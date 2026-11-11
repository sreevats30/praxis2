package com.companion.yogawellness.replay

import com.companion.yogawellness.pose.Landmark
import com.companion.yogawellness.pose.PhoneTiltStatus
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.SensorSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class ClipFrame(val frame: PoseFrame, val sensors: SensorSnapshot?)

data class PoseClip(val name: String, val createdAtMs: Long?, val frames: List<ClipFrame>) {
    val durationSec: Double
        get() = if (frames.size < 2) 0.0 else (frames.last().frame.timestampMs - frames.first().frame.timestampMs) / 1000.0
}

/**
 * `.jsonl` pose-clip format (one JSON object per line):
 *
 *   {"type":"header","version":1,"createdAtMs":…,"landmarks":33,"note":"…"}
 *   {"t":1234,"w":480,"h":640,"lum":118.2,"lm":[[x,y,z,vis],…],"tilt":[pitch,roll,ok],"motion":0.02}
 *
 * Only landmarks are stored — never camera images.
 */
object PoseClipCodec {
    const val FORMAT_VERSION = 1

    fun header(createdAtMs: Long, note: String? = null): String = JSONObject()
        .put("type", "header")
        .put("version", FORMAT_VERSION)
        .put("createdAtMs", createdAtMs)
        .put("landmarks", 33)
        .apply { if (note != null) put("note", note) }
        .toString()

    fun encode(frame: PoseFrame, sensors: SensorSnapshot?): String {
        val sb = StringBuilder(1400)
        sb.append("{\"t\":").append(frame.timestampMs)
            .append(",\"w\":").append(frame.imageWidth)
            .append(",\"h\":").append(frame.imageHeight)
        frame.meanLuminance?.let { sb.append(",\"lum\":").append(fmt(it, 1)) }
        sb.append(",\"lm\":[")
        frame.landmarks.forEachIndexed { i, lm ->
            if (i > 0) sb.append(',')
            sb.append('[').append(fmt(lm.x.toDouble(), 5)).append(',').append(fmt(lm.y.toDouble(), 5))
                .append(',').append(fmt(lm.z.toDouble(), 4)).append(',').append(fmt(lm.visibility.toDouble(), 3)).append(']')
        }
        sb.append(']')
        sensors?.tilt?.let {
            sb.append(",\"tilt\":[").append(fmt(it.pitchDeg, 2)).append(',').append(fmt(it.rollDeg, 2))
                .append(',').append(if (it.isUprightOk) 1 else 0).append(']')
        }
        sensors?.motionSd?.let { sb.append(",\"motion\":").append(fmt(it, 4)) }
        sb.append('}')
        return sb.toString()
    }

    /** Returns null for blank lines and the header. */
    fun decode(line: String): ClipFrame? {
        if (line.isBlank()) return null
        val o = JSONObject(line)
        if (o.optString("type") == "header") return null
        val arr = o.getJSONArray("lm")
        val lms = (0 until arr.length()).map { i ->
            val p = arr.getJSONArray(i)
            Landmark(p.getDouble(0).toFloat(), p.getDouble(1).toFloat(), p.getDouble(2).toFloat(), p.getDouble(3).toFloat())
        }
        val frame = PoseFrame(
            timestampMs = o.getLong("t"),
            imageWidth = o.getInt("w"),
            imageHeight = o.getInt("h"),
            landmarks = lms,
            meanLuminance = if (o.has("lum")) o.getDouble("lum") else null
        )
        val tilt = o.optJSONArray("tilt")?.let { t: JSONArray ->
            val ok = t.getInt(2) == 1
            PhoneTiltStatus(ok, t.getDouble(0), t.getDouble(1), if (ok) null else "Tilt your phone more upright")
        }
        val motion = if (o.has("motion")) o.getDouble("motion") else null
        val sensors = if (tilt == null && motion == null) null else SensorSnapshot(tilt, motion)
        return ClipFrame(frame, sensors)
    }

    fun headerCreatedAt(line: String): Long? = runCatching {
        val o = JSONObject(line)
        if (o.optString("type") == "header") o.getLong("createdAtMs") else null
    }.getOrNull()

    private fun fmt(v: Double, decimals: Int): String = String.format(Locale.US, "%.${decimals}f", v)
}
