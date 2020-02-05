package viz.commonlib.http

import com.google.gson.Gson
import com.viz.tools.TimeFormat
import com.viz.tools.l
import java.util.*

class ErrorEntity {
    var url: String = ""
    var message: String = ""
    var status: Int = -1
    var timestamp: String = ""
    var error: String = ""
    var path: String = ""

    constructor()

    constructor(errorBodyJson: String) {
        try {
            val gson = Gson()
            val entity = gson.fromJson(errorBodyJson, ErrorEntity::class.java)
            message = entity.message
            status = entity.status
            timestamp = TimeFormat.changeFormat(
                entity.timestamp.substring(0, entity.timestamp.indexOf(".")).replace("T", " "),
                "yyyy-MM-dd hh:mm:ss",
                "yyyy-MM-dd HH:ss:mm",
                TimeZone.getDefault().displayName,
                TimeFormat.TIMEZONE_SHANGHAI
            )
            error = entity.error
            path = entity.path
        } catch (e: Exception) {
            l.d("ErrorEntity解析错误:" + e.message)
        }
    }

    override fun toString(): String {
        return "ErrorEntity(message=$message, status=$status, timestamp=$timestamp, error=$error, path=$path, url=$url)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ErrorEntity

        if (url != other.url) return false
        if (message != other.message) return false
        if (status != other.status) return false
        if (timestamp != other.timestamp) return false
        if (error != other.error) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + (message.hashCode())
        result = 31 * result + status
        result = 31 * result + (timestamp.hashCode())
        result = 31 * result + (error.hashCode())
        result = 31 * result + (path.hashCode())
        return result
    }


}