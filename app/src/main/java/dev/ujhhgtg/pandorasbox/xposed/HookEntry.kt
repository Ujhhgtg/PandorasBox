package dev.ujhhgtg.pandorasbox.xposed

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.yukihookapi.YukiHookAPI.configs
import com.highcapable.yukihookapi.YukiHookAPI.encase
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import dev.ujhhgtg.pandorasbox.BuildConfig

private const val TAG: String = "PB.PredictiveBackGestures"

@InjectYukiHookWithXposed(isUsingXposedModuleStatus = true)
object HookEntry : IYukiHookXposedInit {
    override fun onInit() = configs {
        isDebug = BuildConfig.DEBUG
    }

    override fun onHook() = encase {
        loadApp {
            if (packageName == "dev.ujhhgtg.pandorasbox") {
                Log.e(TAG, "u should not hook into myself")
                return@loadApp
            }

            Log.d(TAG, "hooking into package $packageName")

            ApplicationInfo::class.java.asResolver().apply {
                firstConstructor {
                    parameters(ApplicationInfo::class.java)
                }.hook {
                    after {
                        val appInfo = args().first().any() as ApplicationInfo
                        try {
                            val field = appInfo.asResolver().firstField { name = "privateFlagsExt" }
                            var flags = field.get() as Int
                            Log.d(TAG, "appinfo ctor after: flags of $packageName: $flags")
                            flags = flags or (1 shl 3)
                            field.set(flags)
                        } catch (ex: Exception) {
                            Log.e(
                                TAG,
                                "appinfo ctor after: exception: " + Log.getStackTraceString(ex)
                            )
                        }
                    }
                }
            }


            "android.app.ActivityThread".toClass().asResolver().apply {
                firstMethod {
                    name = "handleLaunchActivity"
                    parameters { list -> list.isNotEmpty() }
                }.hook {
                    before {
                        val activityRecord = args().first()
                        val activityInfoField =
                            activityRecord.asResolver().firstFieldOrNull { name = "activityInfo" }
                        if (activityInfoField == null) {
                            return@before
                        }
                        val activityInfo = activityInfoField.get() as ActivityInfo
                        val field = activityInfo.asResolver().firstField { name = "privateFlags" }
                        var flags = field.get() as Int
                        Log.d(TAG, "actthr lchAct before: flags of $packageName: $flags")
                        flags = flags or (1 shl 2)
                        flags = flags and (1 shl 3).inv()
                        field.set(flags)
                    }
                }
            }

//            Application::class.java.asResolver().firstMethod { name = "onCreate" }.hook {
//                after {
//                    val context = instance as Context
//                    val filter = IntentFilter("dev.ujhhgtg.pandorasbox.UPDATE_CONFIG")
//                    val receiver: BroadcastReceiver = object : BroadcastReceiver() {
//                        override fun onReceive(contextinner: Context?, intent: Intent?) {
//                            Log.d(TAG, "received hot config update"
//                            )
//                            // TODO
//                        }
//                    }
//                    try {
//                        context.registerReceiver(receiver, filter)
//                    } catch (ex: Exception) {
//                        Log.d(TAG, "cannot re-register receiver: "+ Log.getStackTraceString(ex)})
//                    }
//                }
//            }
        }
    }
}