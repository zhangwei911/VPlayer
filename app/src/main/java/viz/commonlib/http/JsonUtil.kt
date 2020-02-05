package viz.commonlib.http

import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import okhttp3.RequestBody.Companion.toRequestBody

class JsonUtil {
    private val mapJson = mutableMapOf<String, JsonObject>()
    private var isAddJson = false

    fun addJson(urlTag: String, key: String, value: Any): JsonUtil {
        if (mapJson[urlTag] == null) {
            mapJson[urlTag] = JsonObject()
        }
        val jsonObject = mapJson[urlTag]!!
        when (value) {
            is String -> {
                jsonObject.addProperty(key, value)
            }
            is Number -> {
                jsonObject.addProperty(key, value)
            }
            is Char -> {
                jsonObject.addProperty(key, value)
            }
            is Boolean -> {
                jsonObject.addProperty(key, value)
            }
        }
        isAddJson = true
        return this
    }

    fun parseJson(jsonStr: String): ResponseBody {
        return jsonStr.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }

    fun parseAndAddJson(urlTag: String, jsonStr: String): JsonUtil {
        val jsonObjectNew = JSONObject(jsonStr)
        if (mapJson[urlTag] == null) {
            mapJson[urlTag] = JsonObject()
        }
        val jsonObject = mapJson[urlTag]!!
        jsonObjectNew.keys().forEach { key ->
            when (val value = jsonObjectNew.get(key)) {
                is String -> {
                    jsonObject.addProperty(key, value)
                }
                is Number -> {
                    jsonObject.addProperty(key, value)
                }
                is Char -> {
                    jsonObject.addProperty(key, value)
                }
                is Boolean -> {
                    jsonObject.addProperty(key, value)
                }
            }
        }
        isAddJson = true
        return this
    }

    fun getBody(urlTag: String): RequestBody {
        val jsonObject = mapJson[urlTag] ?: JsonObject()
        mapJson.remove(urlTag)
        return jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }
}
