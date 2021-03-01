package net.blakelee.mediumunlimited

import android.annotation.SuppressLint
import android.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.hookAllConstructors
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LayoutInflated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BhPhotoModule : XposedModule("com.bhphoto"), IXposedHookInitPackageResources {

    private val browse = "com.bhphoto.browse.Browse"
    private val productList = "com.bhphoto.model.products.ProductList"
    private val product = "com.bhphoto.model.products.Product"
    private val productsAdapter = "com.bhphoto.browse.a.i"

    private lateinit var fragment: Fragment
    private lateinit var drawerButton: View
    private lateinit var spinner: Spinner
    private lateinit var recycler: ViewGroup
    private lateinit var scrollView: ViewGroup
    @Volatile private var resultCount = 0
    @Volatile private var products: List<BHPhotoItem> = listOf()

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

                val maybeExists = XposedHelpers.findClassIfExists(
                    "com.bhphoto.model.products.ProductList.ProductListDeserializer",
                    classLoader
                )
            },
            type = *arrayOf(productList)
        )

        getProductAdapterExtraItems()
        getProductAdapterInitialItems()
        setRecyclerView()
        setFragment()

        val homeActivity =
            XposedHelpers.findClass("com.bhphoto.home.activities.HomeActivity", classLoader)
        homeActivity.methods.forEach {
            findAndHookMethod(
                homeActivity.name,
                it.name,
                type = *it.parameterTypes.map { it.name }.toTypedArray(),
                afterHookedMethod = {
                    val name = it.method.name
                    when {
                        name.contains("a") -> {
                            it.args[0] = (it.args[0] as ArrayList<*>).filter {
                                it.toString().contains("Computers")
                            }
                        }
                    }
                }
            )
        }
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

    fun setFragment() {
        findAndHookMethod(
            "com.bhphoto.browse.Browse",
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
                products = (XposedHelpers.getObjectField(it.thisObject, "a") as List<Any>)
                    .map(::BHPhotoItem)
            },
            type = *arrayOf(productList)
        )
    }

    fun getProductAdapterInitialItems() {
        hookAllConstructors(
            XposedHelpers.findClass(productsAdapter, classLoader),
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {

                    products = (param.args[0] as List<Any>).map(::BHPhotoItem)

                    val adapterClass = XposedHelpers.findClass("com.bhphoto.browse.a.i", classLoader)
                    val adapter = adapterClass.cast(param.thisObject)
                    val categories = XposedHelpers.getObjectField(adapter, "b")

                    scrollView = XposedHelpers.getObjectField(categories, "parentLayout") as ViewGroup
                    recycler = XposedHelpers.getObjectField(categories, "recyclerView") as ViewGroup

                    val productList = XposedHelpers.getObjectField(categories, "k")
                    resultCount = XposedHelpers.getObjectField(productList, "resultCount") as Int

                    scrollToBottom()
                }
            })
    }

    private fun scrollToBottom() {
        if (recycler.childCount < resultCount) {
            scrollView.postDelayed({
                XposedHelpers.callMethod(
                    scrollView,
                    "fullScroll",
                    View.FOCUS_DOWN
                )
                scrollToBottom()
            }, 200)
        } else {
            XposedBridge.log("Got all results")
//            clickItem(0)

            /**
             * Check for in stock items here
             *
             * When they are in stock, add them to cart and do that flow
             */
        }
    }

    @SuppressLint("ResourceType")
    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        resparam.res?.hookLayout(
            resparam.packageName,
            "layout",
            "activity_home",
            object : XC_LayoutInflated() {

                override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                    drawerButton = liparam.view.findViewById(2131362335)
                    drawerButton.post {
                        drawerButton.performClick()
                    }
                }
            })

        resparam.res.hookLayout(
            resparam.packageName,
            "layout",
            "activity_browse",
            object : XC_LayoutInflated() {
                override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                    spinner = liparam.view.findViewById(2131363445)
                    spinner
                }
            })

        resparam.res.hookLayout(
            resparam.packageName,
            "layout",
            "fragment_filters",
            object : XC_LayoutInflated() {
                override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                    val s = liparam.view?.findViewById<Spinner?>(2131363445) ?: return
                    this@BhPhotoModule.spinner = s
                }
            })
    }

    private fun clickItem(index: Int) {
        val vh = XposedHelpers.callMethod(recycler, "findViewHolderForLayoutPosition", index)
        (XposedHelpers.getObjectField(vh, "h") as View).performClick()
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

inline fun <reified T> Any.getValue(field: String): T {
    return this::class.java.getField(field).get(this) as T
}

