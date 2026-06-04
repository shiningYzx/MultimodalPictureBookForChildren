package com.demo.multimodalpicturebookforchildren.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.ArrayList

@Parcelize
data class StoryBean(
    var picturePath: String? = null,
    var voicePath: String? = null,
    var title: String? = null,
    var overview: String? = null,
    var author: String? = null,
    var lyricList: List<Lyric> = emptyList(),
    var chapter: String? = null,
    var playNum: Int = 0,
    var favourNum: Int = 0,
    var thumbNum: Int = 0,
    var aid: Int = 0
) : Parcelable {

    constructor(jsonObject: JSONObject) : this() {
        picturePath = jsonObject.optString("picture")
        voicePath = jsonObject.optString("url")
        title = jsonObject.optString("title")
        overview = jsonObject.optString("overview")
        author = jsonObject.optString("author")
        chapter = jsonObject.optString("chapter")
        playNum = jsonObject.optInt("playNums", 0)
        favourNum = jsonObject.optInt("favourNums", 0)
        thumbNum = jsonObject.optInt("thumbNums", 0)
        aid = jsonObject.optInt("aid", 0)

        val contentArray = jsonObject.optJSONArray("content")
        if (contentArray != null) {
            val list = ArrayList<Lyric>()
            for (i in 0 until contentArray.length()) {
                val j = contentArray.getJSONObject(i)
                val lyric = Lyric(j.optLong("beginTime", 0L), j.optString("sentence", ""))
                list.add(lyric)
            }
            lyricList = list
        }
    }
}

