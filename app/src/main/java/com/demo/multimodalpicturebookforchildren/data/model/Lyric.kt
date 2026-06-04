package com.demo.multimodalpicturebookforchildren.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Lyric(
    val time: Long,
    val text: String
) : Parcelable

