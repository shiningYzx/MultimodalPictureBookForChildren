package com.demo.multimodalpicturebookforchildren.ui.components

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

class RecordingPermissionError {
    companion object {
        @JvmStatic
        fun recordingPermissionHint(activity: Activity) {
            AlertDialog.Builder(activity)
                .setTitle("未授予录音权限")
                .setMessage("本软件需要录音权限以支持点读与问答，请前往设置开启录音权限哦~")
                .setPositiveButton("确定") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("前往设置") { dialog, _ ->
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    activity.startActivity(intent)
                    dialog.dismiss()
                }
                .show()
        }
    }
}

