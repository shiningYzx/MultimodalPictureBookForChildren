package com.demo.multimodalpicturebookforchildren.ui.login

import android.app.Activity
import android.content.Intent
import com.demo.multimodalpicturebookforchildren.ui.login.LoginActivity
import com.demo.multimodalpicturebookforchildren.data.local.UserDataSharedPreferences

class LoginError {
    companion object {
        @JvmStatic
        fun logout(activity: Activity) {
            val pref = UserDataSharedPreferences(activity)
            pref.token = ""
            pref.userStatus = 0

            // 清理栈并退回 LoginActivity，通知其弹窗
            val intent = Intent(activity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("showLoginError", true)
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }
}


