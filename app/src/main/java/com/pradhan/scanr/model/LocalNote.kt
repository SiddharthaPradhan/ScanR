package com.pradhan.scanr.model

import org.json.JSONObject

class LocalNote(var noteName: String, val timeOfCreation: String,  var category:String, var content:String) {
    fun toJson(): JSONObject{
        var data: JSONObject = JSONObject()
        data.put(TIME_OF_CREATION, timeOfCreation)
        data.put(CATEGORY, category)
        data.put(CONTENT, content)
        return data
    }

    companion object {
        const val TIME_OF_CREATION = "timeOfCreation"
        const val CATEGORY = "category"
        const val CONTENT = "content"

        fun fromJsonStringToLocalNote(jsonData: String, noteName: String): LocalNote {
            val data = JSONObject(jsonData)
            return LocalNote(
                noteName, data.get(TIME_OF_CREATION) as String, data.get(CATEGORY) as String,
                data.get(CONTENT) as String
            )
        }

    }

}