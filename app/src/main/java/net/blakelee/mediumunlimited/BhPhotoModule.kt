package net.blakelee.mediumunlimited

import android.annotation.SuppressLint
import android.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.hookAllConstructors
import de.robv.android.xposed.XposedHelpers

class BhPhotoModule : XposedModule("com.bhphoto") {

    private val browse = "com.bhphoto.browse.Browse"
    private val productList = "com.bhphoto.model.products.ProductList"
    private val product = "com.bhphoto.model.products.Product"
    private val productsAdapter = "com.bhphoto.browse.a.i"

    private lateinit var fragment: Fragment

    override fun onPackageLoaded() {
        XposedBridge.log("bhphoto loaded")

        findAndHookMethod(
            browse,
            "b",
            afterHookedMethod = {

                val clazz = XposedHelpers.findClass(productList, classLoader)
                val productList = clazz.cast(it.args[0])

                val list = (clazz.getField("products").get(productList) as List<Any>)
                    .map(::BHPhotoItem)

                XposedBridge.log(list.toString())

                val maybeExists = XposedHelpers.findClassIfExists("com.bhphoto.model.products.ProductList.ProductListDeserializer", classLoader)
            },
            type = *arrayOf(productList)
        )

        getProductAdapterExtraItems()
        getProductAdapterInitialItems()
        setRecyclerView()
    }

    @SuppressLint("ResourceType")
    fun setRecyclerView() {
        findAndHookMethod(
            "com.bhphoto.browse.fragments.Categories",
            "l",
            afterHookedMethod = {
                fragment = it.args[0] as Fragment
            }
        )
    }

    fun getProductAdapterExtraItems() {
        findAndHookMethod(
            productsAdapter,
            "a",
            afterHookedMethod = {
                val clazz = XposedHelpers.findClass(productList, classLoader)
                val productList = clazz.cast(it.args[0])

                val list = (clazz.getField("products").get(productList) as List<Any>)
                    .map(::BHPhotoItem)

                XposedBridge.log(list.toString())
            },
            type = *arrayOf(productList)
        )
    }

    fun getProductAdapterInitialItems() {
        hookAllConstructors(XposedHelpers.findClass(productsAdapter, classLoader), object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val list = (param!!.args[0] as List<Any>)
                    .map(::BHPhotoItem)

                val recyclerClass = XposedHelpers.findClass("androidx.recyclerview.widget.RecyclerView", classLoader)
                val adapterClass = XposedHelpers.findClass("androidx.recyclerview.widget.RecyclerView.Adapter", classLoader)

                val recycler = recyclerClass.cast(XposedHelpers.getObjectField(fragment, "recyclerView"))
                val adapter = adapterClass.cast(XposedHelpers.getObjectField(recycler, "mAdapter"))

                recyclerClass.methods.first { it.name == "scrollToPosition" }
                    .invoke(recycler, 20)

                RecyclerView

//                    .invoke(recycler, adapterClass.fields.first { it.name == "size" }.get(adapter) as Int - 1)
            }
        })
    }
}

data class BHPhotoItem(
    val available: Boolean,
    val shortDescription: String,
    val price: String?
) {
    constructor(any: Any) : this(
        any.getValue("available"),
        any.getValue("shortDescription"),
        any.getValue("price")
    )
}

inline fun <reified T>Any.getValue(field: String): T {
    return this::class.java.getField(field).get(this) as T
}

