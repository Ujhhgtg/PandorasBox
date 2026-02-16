package dev.ujhhgtg.pandorasbox.xposed

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.res.Configuration
import android.os.Bundle
import android.transition.Transition
import android.util.DisplayMetrics
import android.util.Log
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.yukihookapi.YukiHookAPI.configs
import com.highcapable.yukihookapi.YukiHookAPI.encase
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import dev.ujhhgtg.pandorasbox.BuildConfig
import java.util.Collections.emptyMap


private const val TAG: String = "PB.Xposed"
private const val TAG_PBG: String = "PB.X.PreBack"
private const val TAG_S: String = "PB.X.Scaler"
private const val TAG_SS: String = "PB.X.SigSpoof"
private const val TAG_DT: String = "PB.X.DefTrans"

private var pbgBlacklistMap: MutableMap<String, Set<String>> = emptyMap()

@InjectYukiHookWithXposed(isUsingXposedModuleStatus = true)
object HookEntry : IYukiHookXposedInit {
    init {
        System.loadLibrary("dexkit")
    }

    override fun onInit() = configs {
        isDebug = BuildConfig.DEBUG
    }

    private fun clearPbgFlag(activityInfo: ActivityInfo) {
        val field = activityInfo.asResolver().firstField { name = "privateFlags" }
        var flags = field.get() as Int
        flags = flags and (1 shl 2).inv()
        field.set(flags)
    }

    private inline fun <reified T> YukiHookPrefsBridge.getOrElse(
        packageName: String,
        key: String,
        default: T
    ): T {
        return this.let {
            if (it.contains("${packageName}_${key}")) {
                return@let it.get<T>(PrefsData("${packageName}_${key}", default))
            } else {
                return@let it.get<T>(PrefsData("default_${key}", default))
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyDensity(
        dm: DisplayMetrics,
        scale: Float
    ) {
        val baseDpi = dm.densityDpi
        val newDpi = (baseDpi * scale).toInt()

        dm.densityDpi = newDpi
        dm.density = newDpi / DisplayMetrics.DENSITY_DEFAULT.toFloat()
        dm.scaledDensity = dm.density * (dm.scaledDensity / dm.density)
    }

    override fun onHook() = encase {
        loadApp(isExcludeSelf = true) {
            Log.i(TAG, "hooking into app $packageName")

            // --- Scaler ---
            Log.i(TAG_S, "hooking scaler")
            val scaleType = prefs("xposed").getInt("${packageName}_sc_t", 0)
            // 0: Disabled
            // 1: Configuration.fontScale & Resources.displayMetrics
            // 2: Resources.getDimension()
            // 3: TextView.setTextSize()
            when (scaleType) {
                0 -> {
                    Log.i(TAG_S, "$packageName scaler disabled")
                }

                1 -> {
                    Activity::class.asResolver()
                        .firstMethod { name = "attachBaseContext" }
                        .hook {
                            before {
                                val fontScale =
                                    prefs("xposed").getFloat("${packageName}_sc_fs", 1.0f)
                                val densityScale =
                                    prefs("xposed").getFloat("${packageName}_sc_ds", 1.0f)

                                val arg = args().first()
                                val ctx = arg.any() as Context
                                val res = ctx.resources
                                val config = Configuration(res.configuration)
                                if (fontScale != 1.0f) {
                                    config.fontScale = fontScale
                                }
                                val newCtx = ctx.createConfigurationContext(config)
                                if (densityScale != 1.0f) {
                                    val metrics = ctx.resources.displayMetrics
                                    applyDensity(metrics, densityScale)
                                }
                                arg.set(newCtx)
                            }
                        }
                }

                2 -> {
                    Log.w(TAG_S, "not implemented")
                }

                3 -> {
                    TextView::class.asResolver()
                        .firstMethod {
                            name = "setTextSize"
                            parameters(Int::class, Float::class)
                        }
                        .hook {
                            before {
                                val fontScale =
                                    prefs("xposed").getFloat("${packageName}_sc_fs", 1.0f)
                                val size = args(1)
                                size.set(size.cast<Float>()!! * fontScale)
                            }
                        }
                }
            }

            // --- Predictive Back Gestures ---

            Log.d(TAG_PBG, "hooking pbg")
            val pbgEnabled = prefs("xposed").getOrElse<Boolean>(packageName, "pbg_e", false)
            Log.d(TAG_PBG, "$packageName pbg enabled: $pbgEnabled")
            if (pbgEnabled) {
                ApplicationInfo::class.asResolver()
                    .firstConstructor {
                        parameters(ApplicationInfo::class.java)
                    }.hook {
                        after {
                            val appInfo = args(0).cast<ApplicationInfo>()!!
                            try {
                                val field =
                                    appInfo.asResolver().firstField { name = "privateFlagsExt" }
                                var flags = field.get() as Int
//                                Log.d(
//                                    TAG_PBG,
//                                    "appinfo ctor 2 after: flags of $packageName: $flags"
//                                )
                                flags = flags or (1 shl 3)
                                field.set(flags)
                            } catch (ex: Exception) {
                                Log.e(
                                    TAG_PBG,
                                    "appinfo ctor 2 after: exception: " + Log.getStackTraceString(ex)
                                )
                            }
                        }
                    }

                pbgBlacklistMap[packageName] =
                    prefs("xposed").getOrElse<Set<String>>(packageName, "pbg_bl", mutableSetOf())

                ActivityInfo::class.asResolver()
                    .firstConstructor()
                    .hook {
                        after {
                            val info = instance as ActivityInfo
                            Log.d(TAG_PBG, "current activity: ${info.name}")

                            if (pbgBlacklistMap[packageName]?.contains(info.name)
                                    ?: false
                            ) {
                                Log.i(TAG_PBG, "activity ${info.name} is in blacklist, skipping")
                                return@after
                            }

                            if (info.packageName != packageName) {
                                Log.i(
                                    TAG_PBG,
                                    "activity ${info.name} does not belong to current app $packageName, skipping"
                                )
                                return@after
                            }

                            val onBackPressedMethod = runCatching {
                                info.name.toClass().getMethod("onBackPressed")
                            }.getOrNull()
                            if (onBackPressedMethod != Activity::onBackPressed && onBackPressedMethod != AppCompatActivity::onBackPressed) {
                                Log.i(TAG_PBG, "app has onBackPressed() overridden, skipping")
                                clearPbgFlag(info)
                                return@after
                            }
                            val field = info.asResolver().firstField { name = "privateFlags" }
                            var flags = field.get() as Int
//                            Log.d(TAG_PBG, "actinfo ctor 2 after: flags of $packageName: $flags")
                            flags = flags or (1 shl 2)
                            flags = flags and (1 shl 3).inv()
                            field.set(flags)
                        }
                    }

                "android.app.ActivityThread".toClass().asResolver()
                    .firstMethod { name = "handleLaunchActivity" }.hook {
                        before {
                            val record = args().first()
                            val infoField =
                                record.asResolver().firstFieldOrNull { name = "activityInfo" }
                            if (infoField == null) {
                                Log.w(TAG_PBG, "activityInfo is null, skipping")
                                return@before
                            }
                            val info = infoField.get() as ActivityInfo
//                            Log.d(TAG_PBG, "current hooked activity: ${info.name}")
                            if (pbgBlacklistMap[packageName]?.contains(info.name) ?: false) {
                                Log.i(TAG_PBG, "activity ${info.name} is in blacklist, skipping")
                                clearPbgFlag(info)
                                return@before
                            }

                            val onBackPressedMethod = runCatching {
                                info.name.toClass().getMethod("onBackPressed")
                            }.getOrNull()
                            if (onBackPressedMethod != Activity::onBackPressed && onBackPressedMethod != AppCompatActivity::onBackPressed) {
                                Log.i(TAG_PBG, "app has onBackPressed() overridden, skipping")
                                clearPbgFlag(info)
                                return@before
                            }
                            val field = info.asResolver().firstField { name = "privateFlags" }
                            var flags = field.get() as Int
//                            Log.d(TAG_PBG, "actthr lchAct before: flags of $packageName: $flags")
                            flags = flags or (1 shl 2)
                            flags = flags and (1 shl 3).inv()
                            field.set(flags)
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

            // --- Default Transitions ---
            Log.d(TAG_PBG, "hooking default transitions")
            val dtEnabled = prefs("xposed").getOrElse<Boolean>(packageName, "dt_e", false)
            if (dtEnabled) {
                Activity::class.asResolver()
                    .apply {
                        firstMethod {
                            name = "onCreate"
                            parameters(Bundle::class)
                        }.hook {
                            after {
                                val activity = instance<Activity>()
                                clearWindowTransitions(activity)
                            }
                        }

                        firstMethod {
                            name = "overridePendingTransition"
                        }.hook {
                            replaceUnit { }
                        }

                        firstMethod {
                            name = "overrideActivityTransition"
                        }.hook {
                            replaceUnit { }
                        }
                    }

                Window::class.asResolver()
                    .apply {
                        for (setter in activityTransitionSetters) {
                            firstMethod {
                                name = setter
                                parameters(Transition::class)
                            }.hook {
                                before {
                                    args(0).set(null)
                                }
                            }
                        }
                    }

                ActivityOptions::class.asResolver()
                    .apply {
                        for (maker in activityOptionsAnimationMakers) {
                            firstMethod { name = maker }.hook {
                                replaceAny { return@replaceAny ActivityOptions.makeBasic() }
                            }
                        }

                        firstMethod {
                            name = "makeSceneTransitionAnimation"
                            parameterCount { it >= 2 }
                        }.hook {
                            replaceAny {
                                val activity = args[0] as Activity?
                                if (activity != null) {
                                    return@replaceAny ActivityOptions.makeSceneTransitionAnimation(
                                        activity
                                    )
                                }
                                return@replaceAny ActivityOptions.makeBasic()
                            }
                        }
                    }
            }

            // --- WeChat ---
            if ((packageName.contains("tencent") && packageName.contains("mm")) || packageName.contains(
                    "wechat"
                ) || packageName.contains("weixin")
            ) {

            }
        }

        loadSystem {
            // --- Signature Spoof ---

            Log.i(TAG_SS, "hooking signature spoof")

            val ssEnabled = prefs("xposed").getBoolean("default_ss_e", false)
            Log.d(TAG_PBG, "ss enabled: $ssEnabled")
            if (!ssEnabled)
                return@loadSystem

            "com.android.server.pm.ComputerEngine".toClass().asResolver()
                .firstMethod { name = "generatePackageInfo" }
                .hook {
//                    before {
//                        val flagsArg = args(1)
//                        val flags = when (flagsArg.any()) {
//                            is Int -> flagsArg.cast<Int>()!!.toLong()
//                            is Long -> flagsArg.cast<Long>()!!
//                            else -> return@before
//                        }
//
//                        val newFlags =
//                            flags or
//                                    PackageManager.GET_SIGNING_CERTIFICATES.toLong() or
//                                    PackageManager.GET_META_DATA.toLong()
//
//                        flagsArg.set(
//                            if (flagsArg.any() is Int) newFlags.toInt() else newFlags
//                        )
//                    }

                    after {
                        val pi = result as PackageInfo? ?: return@after

                        val flagsArg = args(1)
                        val flags =
                            if (args[1] is Int) {
                                (flagsArg.cast<Int>()!!).toLong()
                            } else {
                                flagsArg.cast<Long>()!!
                            }
                        if (!isFetchingSignatures(flags)) return@after

                        val fakeSig = prefs("xposed").getString("${pi.packageName}_ss_s", "")
                        Log.d(TAG_PBG, "${pi.packageName} ss enabled: ${fakeSig != ""}")
                        if (fakeSig == "") {
                            return@after
                        }

                        val makeSoleSigner =
                            prefs("xposed").getBoolean("${pi.packageName}_ss_so", false)

                        Log.d(TAG_SS, "spoofing signature for ${pi.packageName}")

                        if (pi.signatures != null) {
                            val sig = runCatching { Signature(fakeSig) }.getOrNull() ?: return@after
                            if (makeSoleSigner) {
                                pi.signatures = arrayOf(sig)
                            } else {
                                pi.signatures = arrayOf(sig) + pi.signatures!!
                            }
                        }

                        if (pi.signingInfo != null) {
                            val sig = runCatching { Signature(fakeSig) }.getOrNull() ?: return@after
                            val sd = pi.signingInfo!!.asResolver()
                                .firstField {
                                    name {
                                        it in signingDetailsFieldNames
                                    }
                                }
                                .get()!!

                            val signField = sd.asResolver()
                                .firstField { name { it in signaturesFieldNames } }
                            val pastSignField = sd.asResolver()
                                .firstField { name { it in pastSigningCertificatesFieldNames } }

                            @Suppress("UNCHECKED_CAST")
                            val origSign = signField.get() as Array<Signature>

                            @Suppress("UNCHECKED_CAST")
                            val origPastSign = pastSignField.get() as Array<Signature>

                            if (!makeSoleSigner) {
                                signField.set(arrayOf(sig) + origSign)
                                pastSignField.set(arrayOf(sig) + origPastSign)
                            } else {
                                signField.set(arrayOf(sig))
                                pastSignField.set(arrayOf(sig))
                            }
                        }
                    }
                }
        }
    }

    // Default Transitions
    private val activityTransitionSetters = arrayOf(
        "setEnterTransition",
        "setExitTransition",
        "setReturnTransition",
        "setReenterTransition",
        "setSharedElementEnterTransition",
        "setSharedElementExitTransition",
        "setSharedElementReturnTransition",
        "setSharedElementReenterTransition"
    )
    private val activityOptionsAnimationMakers = arrayOf(
        "makeCustomAnimation",
        "makeScaleUpAnimation",
        "makeClipRevealAnimation",
        "makeThumbnailScaleUpAnimation"
    )

    // SS
    private val signingDetailsFieldNames = arrayOf("mSigningDetails", "signingDetails")
    private val signaturesFieldNames = arrayOf("mSignatures", "signatures")
    private val pastSigningCertificatesFieldNames =
        arrayOf("mPastSigningCertificates", "pastSigningCertificates")

    private fun isFetchingSignatures(flags: Long): Boolean {
        val mask = PackageManager.GET_SIGNING_CERTIFICATES
        return (flags and mask.toLong()) != 0L
    }

    private fun clearWindowTransitions(activity: Activity) {
        val window = activity.window ?: return

        try {
            // Set all transitions to null - system will use default behavior
            window.enterTransition = null
            window.exitTransition = null
            window.returnTransition = null
            window.reenterTransition = null
            window.sharedElementEnterTransition = null
            window.sharedElementExitTransition = null
            window.sharedElementReturnTransition = null
            window.sharedElementReenterTransition = null
        } catch (t: Throwable) {
            Log.i(TAG_DT, "error clearing transitions: " + t.message)
        }
    }
}