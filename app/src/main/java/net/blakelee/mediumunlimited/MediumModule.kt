package net.blakelee.mediumunlimited

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MediumModule : IXposedHookLoadPackage {
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