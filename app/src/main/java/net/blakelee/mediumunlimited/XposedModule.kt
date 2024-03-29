package net.blakelee.mediumunlimited

import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class XposedModule(private val packageName: String) : IXposedHookLoadPackage,
    IXposedHookInitPackageResources {

    lateinit var classLoader: ClassLoader

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("Available ${lpparam.packageName}")
        if (lpparam.packageName.contains(packageName)) {
            classLoader = lpparam.classLoader
            XposedBridge.log("Found $packageName")
            onPackageLoaded()
        }
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        if (resparam.packageName == packageName) {
            onPackageResourcesLoaded(resparam)
        }
    }

    abstract fun onPackageLoaded()

    open fun onPackageResourcesLoaded(resparam: XC_InitPackageResources.InitPackageResourcesParam) {}

    fun findClass(className: String) = XposedHelpers.findClass(className, classLoader)

    fun <T> findAndHookMethod(
        className: String,
        methodName: String,
        beforeHookedMethod: (XC_MethodHook.MethodHookParam, T) -> Unit = { _, _ -> },
        afterHookedMethod: (XC_MethodHook.MethodHookParam, T) -> Unit = { _, _ -> }
    ) = findAndHookMethod(className, methodName, object : XC_MethodHookReified<T> {
        override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam, result: T) {
            afterHookedMethod(param, result)
        }

        override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam, result: T) {
            beforeHookedMethod(param, result)
        }
    })

    fun <T> findAndHookMethod(className: String, methodName: String, cb: XC_MethodHookReified<T>) {
        val type = XposedHelpers.findClass(className, classLoader)
            ?.declaredMethods
            ?.first { it.name == methodName }
            ?.parameterTypes

        XposedHelpers.findAndHookMethod(
            className,
            classLoader,
            methodName,
            *type,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    cb.afterHookedMethod(param, param.result as T)
                }

                override fun beforeHookedMethod(param: MethodHookParam) {
                    cb.beforeHookedMethod(param, param.result as T)
                }
            }
        )
    }

    fun findAndHookMethod(
        className: String,
        methodName: String,
        beforeHookedMethod: (XC_MethodHook.MethodHookParam) -> Unit = { _ -> },
        afterHookedMethod: (XC_MethodHook.MethodHookParam) -> Unit = { _ -> },
        vararg type: String
    ) {

        val type = XposedHelpers.findClass(className, classLoader)
            ?.declaredMethods
            ?.filter { it.name == methodName }
            ?.first {
                XposedBridge.log("Found method: " + it.parameterTypes.joinToString { it.name })
                it.parameterTypes.map { it.name }.containsAll(type.toList())
            }
            ?.parameterTypes

        XposedHelpers.findAndHookMethod(
            className,
            classLoader,
            methodName,
            *type,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    afterHookedMethod(param)
                }

                override fun beforeHookedMethod(param: MethodHookParam) {
                    beforeHookedMethod(param)
                }
            }
        )
    }

    fun hookAllMethods(
        className: String,
        afterHookedMethod: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        val clazz = XposedHelpers.findClass(className, classLoader)
        val methods = clazz.declaredMethods
        methods.forEach {
            findAndHookMethod(
                className,
                it.name,
                afterHookedMethod = afterHookedMethod,
                type = it.parameterTypes.map { it.name }.toTypedArray()
            )
        }
    }
}

interface XC_MethodHookReified<T> {
    fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam, result: T) {}
    fun afterHookedMethod(param: XC_MethodHook.MethodHookParam, result: T) {}
}