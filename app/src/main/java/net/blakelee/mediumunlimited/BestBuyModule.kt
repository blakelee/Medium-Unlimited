package net.blakelee.mediumunlimited

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class BestBuyModule : IXposedHookLoadPackage {
    private val TAG = "Best Buy"

    //        findAndHookMethod("o/ɨƖ", "u",
//            afterHookedMethod = { _, result: String ->
//                XposedBridge.log("Best Buy x-platform: $result")
//            }
//        )

    private fun logBestBuyXPlatform(classLoader: ClassLoader) {
        val type = XposedHelpers.findClass("h.f.b.d0.e0.h", classLoader)
            ?.declaredMethods
            ?.first { it.name == "u" }
            ?.parameterTypes
            ?.first() // String

        XposedHelpers.findAndHookMethod(
            "h.f.b.d0.e0.h",
            classLoader,
            "u",
            type,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    val result: String = param.result as String
                    XposedBridge.log("Best Buy x-platform: $result");
                }
            }
        )
    }

    private fun logNdotC(classLoader: ClassLoader) {

        XposedHelpers.findAndHookMethod(
            "h.f.b.d0.e0.n",
            classLoader,
            "c",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val result: String = param.result as String
                    XposedBridge.log("$TAG n.c: $result");
                    val aString = XposedHelpers.getObjectField(param.thisObject, "a")
                    val bString = XposedHelpers.getObjectField(param.thisObject, "b")
                    val eString = XposedHelpers.getObjectField(param.thisObject, "e")
                    XposedBridge.log("$TAG a: $aString")
                    XposedBridge.log("$TAG b: $bString")
                    XposedBridge.log("$TAG e: $eString")
                }
            }
        )


    }

    private fun logFdotD(classLoader: ClassLoader) {
        val className = "h.f.b.d0.e0.f"
        val type = XposedHelpers.findClass(className, classLoader)
            ?.declaredMethods
            ?.first { it.name == "d" }
            ?.parameterTypes
            ?.first() // String

        XposedHelpers.findAndHookMethod(
            className,
            classLoader,
            "d",
            type,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val result = param.result as String
                    val input = param.args[0] as String
                    val c = XposedHelpers.getObjectField(param.thisObject, "c").toString()
                    XposedBridge.log("$TAG result: $result")
                    XposedBridge.log("$TAG input: $input")
                    XposedBridge.log("$TAG c: $c")

                }
            }
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName.contains("com.medium.reader")) {
            enabledUnlimitedPosts(lpparam.classLoader)
        }
    }

    private fun enabledUnlimitedPosts(classLoader: ClassLoader) {
        val type = XposedHelpers.findClass("okhttp3.JavaNetCookieJar", classLoader)
            ?.declaredMethods
            ?.first { it.name == "loadForRequest" }
            ?.parameterTypes
            ?.first() // okhttp3.HttpUrl
        XposedHelpers.findAndHookMethod(
            "okhttp3.JavaNetCookieJar",
            classLoader,
            "loadForRequest",
            type,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val url = param.args[0]
                        val urlString = url::class.java.getMethod("uri").invoke(url)?.toString()
                        val pattern = "https://api\\.medium\\.com/_/api/posts/[^/]+$"
                        if (urlString?.matches(Regex(pattern)) == true) {
                            param.result = emptyList<Any>()
                        } else {
                            return
                        }
                    } catch (t: Throwable) {
                        param.throwable = t
                    }
                }
            }
        )
    }
}