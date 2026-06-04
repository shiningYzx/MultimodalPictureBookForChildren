package com.demo.multimodalpicturebookforchildren.data.model

import java.util.Random

object CallWord {
    private val questionWords = arrayOf(
        "为什么天空是蓝色的？",
        "小鸟为什么会飞？",
        "为什么海水是咸的？",
        "为什么树会长得那么高？",
        "动物会说话吗？",
        "为什么我们要睡觉？",
        "为什么有些人会长得很高，有些人会矮？",
        "为什么要上学？",
        "宠物能活多久？",
        "手机是怎么工作的？",
        "飞机为什么能飞？",
        "火车为什么会有轨道？",
        "机器人会变得像人类一样聪明吗？",
        "雨是从哪里来的？",
        "星星为什么会闪烁？",
        "为什么有时会感到快乐，有时会感到伤心？",
        "时间是什么？",
        "宇宙有多大？",
        "未来是什么样子的？",
        "我们能不能回到过去？"
    )

    private val jumpWords = arrayOf(
        "播放第一章",
        "播放第五章",
        "播放第九章",
        "播放第十三章",
        "播放第十六章",
        "播放小蚂蚁的求助",
        "播放彩虹花的奉献",
        "播放冬天的考验",
        "播放春天的重生"
    )

    fun getQuestionWords(len: Int): List<String> {
        val list = questionWords.toList().shuffled()
        return list.take(len.coerceAtMost(list.size))
    }

    fun getJumpWords(len: Int): Array<String> {
        val words = Array(len) { "" }
        val random = Random()
        for (i in 0 until len) {
            words[i] = jumpWords[random.nextInt(jumpWords.size)]
        }
        return words
    }
}

