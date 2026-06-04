package com.demo.multimodalpicturebookforchildren.data.model

data class Chat(
    var msg: String?,
    var type: Int,
    var audioPath: String?,
    var id: Long?
) {
    companion object {
        const val SEND = 0
        const val RECEIVE = 1
    }
}

