package dev.ujhhgtg.pandorasbox.xposed

import android.app.Application
import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import dev.ujhhgtg.pandorasbox.BuildConfig

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    override fun onHook() = YukiHookAPI.encase {
        loadSystem {
            "com.android.server.wm.TaskFragment".toClass()
                .asResolver()
                .firstMethod {
                    name = "setResumedActivity"
                    parameters("com.android.server.wm.ActivityRecord".toClass(), String::class)
                }.hook {
                    after {
                        val packageName = args[0]?.asResolver()?.firstField {
                                name = "packageName"
                            }?.get<String>()

                        val context = "android.app.ActivityThread".toClass().asResolver()
                            .firstMethod {
                                name = "currentApplication"
                                returnType = Application::class
                            }.of(null).invoke() as Application

                        context.sendBroadcast(Intent("dev.ujhhgtg.pandorasbox.ACTION_FOREGROUND_APP_CHANGE").apply {
                            putExtra("packageName", packageName)
                            setPackage("dev.ujhhgtg.pandorasbox")
                        })
                    }
                }
        }
    }

    override fun onInit() = configs {
        isDebug = BuildConfig.DEBUG
    }
}