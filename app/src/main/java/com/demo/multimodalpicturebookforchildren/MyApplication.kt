package com.demo.multimodalpicturebookforchildren

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.demo.multimodalpicturebookforchildren.ui.components.voice.GlobalTouchWindowCallback

/**
 * @author shiningYang
 * @date 2026-06-02-0:16
 * @description:
 */
class MyApplication : Application() {
    companion object {
        @JvmStatic
        lateinit var context: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val name = activity.javaClass.simpleName
                // 登录页不安装全局长按拦截
                if (name == "LoginActivity") return
                
                val window = activity.window
                window.callback = GlobalTouchWindowCallback(activity, window.callback)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
