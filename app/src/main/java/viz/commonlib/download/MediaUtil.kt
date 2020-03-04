package viz.commonlib.download

import android.media.MediaMetadataRetriever

object MediaUtil {
    fun getDuration(videoPath: String): Float {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(videoPath)
        val duration =
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: return 0f
        return duration.toFloat()
    }
}