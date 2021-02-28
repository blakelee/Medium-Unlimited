package net.blakelee.mediumunlimited

import de.robv.android.xposed.XposedBridge

class MediumModule : XposedModule("com.newegg.app") {

    override fun onPackageLoaded() {

        findAndHookMethod(
            "okhttp3.Interceptor",
            "intercept",
            afterHookedMethod = { param ->
                XposedBridge.log("newegg x: ${param.result?.toString()}")

            }
        )
    }
}