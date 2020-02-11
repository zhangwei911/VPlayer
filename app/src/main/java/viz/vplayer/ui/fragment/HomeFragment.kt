package viz.vplayer.ui.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.test.espresso.idling.net.UriIdlingResource
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.fragment_home.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import viz.vplayer.BuildConfig
import viz.vplayer.R
import viz.vplayer.adapter.SearchAdapter
import viz.vplayer.adapter.SelectEpisodesAdapter
import viz.vplayer.adapter.WebAdapter
import viz.vplayer.bean.*
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.eventbus.RuleEvent
import viz.vplayer.room.Rule
import viz.vplayer.ui.activity.RuleListActivity
import viz.vplayer.ui.activity.VideoPlayerActivity
import viz.vplayer.util.*
import viz.vplayer.vm.MainVM
import java.io.Serializable
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HomeFragment : BaseFragment(), View.OnClickListener {
    override fun getFragmentClassName(): String = "HomeFragment"
    override fun getContentViewId(): Int = R.layout.fragment_home

    override fun useEventBus(): Boolean = true
    private lateinit var mainVM: MainVM
    private val SERIAL_EXECUTOR: Executor = Executors.newSingleThreadExecutor()
    private lateinit var searchAdapter: SearchAdapter
    private val htmlList = mutableListOf<HtmlBean>()
    private val htmlParamsList = mutableListOf<MutableMap<String, String>>()
    private val htmlParamsKWList = mutableListOf<String>()
    private var currentPos = 0
    private var isAll = false
    private val spinnerNameItems = mutableListOf<String>()
    private val gson = Gson()
    private var searchUrl = ""
    private var rulesUrlList = mutableListOf<String>()
    private var isLongClick = false
    private var totalSelectSize = 0
    private var successAddCacheCount = 0
    private var webAdapter: WebAdapter? = null
    private val webList = mutableListOf<WebBean>()
    private var currentPosWeb = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainVM = activity?.run {
            ViewModelProvider(this).get(MainVM::class.java)
        } ?: throw Exception("Invalid Activity")
        searchAdapter = SearchAdapter(context!!)
        mainVM.addCache.observe(viewLifecycleOwner, Observer {
            l.d("已成功添加${it}个任务(总共${totalSelectSize}个)")
        })
        mainVM.play.observe(viewLifecycleOwner, Observer { videoInfoBean ->
            videoInfoBean?.apply {
                mainVM.play.postValue(null)
                if (htmlList.isEmpty()) {
                    return@apply
                }
                if (isDownload) {
                    WorkerUtil.startWorker(
                        url,
                        title,
                        img,
                        searchUrl,
                        duration,
                        app.applicationContext,
                        this@HomeFragment
                    )
                    successAddCacheCount++
                    mainVM.addCache.postValue(successAddCacheCount)
                    Toast.show("添加${successAddCacheCount}个缓冲任务成功")
                    return@apply
                }
                val intent = Intent(context, VideoPlayerActivity::class.java)
                intent.putExtra("url", url)
                intent.putExtra("searchUrl", searchUrl)
                intent.putExtra("title", title)
                intent.putExtra("duration", duration)
                intent.putExtra("img", img)
                intent.putExtra("episodes", mainVM.episodes.value as Serializable)
                intent.putExtra("html", htmlList[searchAdapter.data[currentPos].from])
                startActivity(intent)
                loadingView.visibility = View.GONE
            }
        })
        mainVM.search.observe(viewLifecycleOwner, Observer { searchList ->
            l.i(searchList)
            if (searchList.isNullOrEmpty()) {
                loadingView.visibility = View.GONE
                return@Observer
            }
            if (isAll) {
                searchAdapter.data.addAll(searchList)
            } else {
                searchAdapter.data = searchList
            }
            mainVM.saveSearchResult(searchAdapter.data)
            mainVM.search.postValue(null)
            searchAdapter.notifyDataSetChanged()
            loadingView.visibility = View.GONE
            if (BuildConfig.DEBUG) {
//                searchClick(0)
            }
        })
        mainVM.getSearchResult().observe(viewLifecycleOwner, Observer { searchResult ->
            searchAdapter.data = searchResult
            searchAdapter.notifyDataSetChanged()
        })
        mainVM.episodes.observe(viewLifecycleOwner, Observer { episodeList ->
            l.i(episodeList)
            group_web.visibility = View.GONE
            editText_search.clearFocus()
            if (episodeList == null) {
                loadingView.visibility = View.GONE
                return@Observer
            }
            if (episodeList.isEmpty()) {
                loadingView.visibility = View.GONE
                Toast.show(context, "数据为空")
                return@Observer
            }
            if (isLongClick) {
                val data = mutableListOf<EpisodeListBean>()
                episodeList.forEachIndexed { index, s ->
                    data.add(EpisodeListBean(index, s))
                }
                cacheDirect(data)
                return@Observer
            }
            if (episodeList[0].endsWith(".m3u8")) {
                mainVM.play.postValue(
                    VideoInfoBean(
                        episodeList[0],
                        searchAdapter.data[currentPos].name,
                        0,
                        searchAdapter.data[currentPos].img
                    )
                )
            } else {
                mainVM.getVideoInfo(
                    episodeList[0],
                    htmlList[searchAdapter.data[currentPos].from].videoHtmlResultBean,
                    searchAdapter.data[currentPos].img
                )
            }
        })
        mainVM.rules.observe(viewLifecycleOwner, Observer { rulesJsonPair ->
            if (rulesJsonPair == null) {
                return@Observer
            }
            try {
                if (rulesUrlList.contains(rulesJsonPair.first)) {
                    clearRules()
                } else {
                    rulesUrlList.add(rulesJsonPair.first)
                }
                val type = object : TypeToken<MutableList<JsonBean>>() {}.type
                val jsonBeanList = gson.fromJson<MutableList<JsonBean>>(rulesJsonPair.second, type)
//                l.d(jsonBeanList)
                mainVM.jsonBeanList.postValue(jsonBeanList)
                if (spinnerNameItems.size > 0) {
                    spinnerNameItems.remove("所有")
                }
                if (webList.size > 0) {
                    webList.remove(WebBean("所有"))
                }
                jsonBeanList.forEach { jsonBean: JsonBean ->
                    jsonBean.apply {
                        val map = mutableMapOf<String, String>()
                        params.forEach { paramBean: ParamBean ->
                            map[paramBean.key] = paramBean.value
                        }
                        spinnerNameItems.add(webName)
                        htmlParamsList.add(map)
                        htmlParamsKWList.add(kwKey)
                        htmlList.add(html)
                        val webBean = WebBean()
                        webBean.name = webName
                        webBean.searchUrl = searchUrl
                        webList.add(webBean)
                    }
                }
                searchAdapter.fromNameList = spinnerNameItems
                spinnerNameItems.add("所有")
                webList.add(WebBean("所有"))
                webAdapter = WebAdapter(context!!, webList)
                recyclerView_web.adapter = webAdapter
                updateWebSelect(currentPosWeb)
                mainVM.rules.postValue(null)
                test()
            } catch (e: Exception) {
                e.printStackTrace()
                mainVM.errorInfo.postValue(
                    ErrorInfo(
                        "解析rule规则数据异常",
                        ErrorCode.ERR_JSON_INVALID,
                        rulesJsonPair.first
                    )
                )
            }
        })
        mainVM.webView.observe(viewLifecycleOwner, Observer {
            webView_for_get_url.loadUrl(it)
        })
        mainVM.getSearchInfo().observe(viewLifecycleOwner, Observer {
            it?.let {
                editText_search.setText(it)
            }
        })
        mainVM.getSearchUrl().observe(viewLifecycleOwner, Observer {
            it?.let {
                webAdapter?.list?.apply {
                    if (it == "all") {
                        updateWebSelect(size - 1)
                    } else {
                        forEachIndexed { index, webBean ->
                            if (webBean.searchUrl == it) {
                                updateWebSelect(index)
                            }
                        }
                    }
                }
            }
        })
        mainVM.errorInfo.observe(viewLifecycleOwner, Observer { errorInfo ->
            errorInfo?.let {
                loadingView.visibility = View.GONE
                Toast.showLong(context, errorInfo.errMsg)
                when (errorInfo.errCode) {
                    ErrorCode.ERR_JSON_INVALID -> {
                        if (errorInfo.url != DEFAULT_RULE_URL) {
                            updateRule(errorInfo.url, 0, "规则无效")
                        }
                    }
                    ErrorCode.ERR_JSON_EMPTY -> {
                        updateRule(errorInfo.url, -1, "规则数据为空")
                    }
                }
            }
        })
        initViews()
        initListener()
        getRules()
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
    }

    private fun cacheDirect(data: MutableList<EpisodeListBean>) {
        loadingView.visibility = View.GONE
        MaterialDialog(context!!).show {
            val adapter = SelectEpisodesAdapter(context)
            adapter.data = data
            adapter.isMultiSelect = true
            val gridWidth = windowContext.resources.getInteger(R.integer.md_grid_width)
            val layoutManager = GridLayoutManager(windowContext, gridWidth)
            customListAdapter(
                adapter = adapter,
                layoutManager = layoutManager
            )
            val rv = getRecyclerView()
            val onDragSelectionListener =
                DragSelectTouchListener.OnDragSelectListener { start, end, isSelected ->
                    if (isSelected) {
                        for (i in start..end) {
                            adapter.select(i)
                        }
                    } else {
                        for (i in start..end) {
                            adapter.unselect(i)
                        }
                    }
                }
            val mDragSelectTouchListener = DragSelectTouchListener().apply {
                // check region OnDragSelectListener for more infos
                withSelectListener(onDragSelectionListener)
                // following is all optional
                withMaxScrollDistance(16)    // default: 16; 	defines the speed of the auto scrolling
                withTopOffset(0)       // default: 0; 		set an offset for the touch region on top of the RecyclerView
                withBottomOffset(0)    // default: 0; 		set an offset for the touch region on bottom of the RecyclerView
                withScrollAboveTopRegion(true)  // default: true; 	enable auto scrolling, even if the finger is moved above the top region
                withScrollBelowTopRegion(true)  // default: true; 	enable auto scrolling, even if the finger is moved below the top region
//                withDebug(true)               // default: false;
            }
            rv.addOnItemTouchListener(mDragSelectTouchListener)
            rv.addOnItemTouchListener(
                RecyclerItemClickListener(
                    context,
                    rv,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                            adapter.select(position)
                        }

                        override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                            if (adapter.data[position].isSelect) {
                                adapter.unselect(position)
                            } else {
                                adapter.select(position)
                            }
                            mDragSelectTouchListener.startDragSelection(position)
                        }

                        override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {

                        }
                    })
            )
            positiveButton(R.string.cache) {
                data.filter { it.isSelect }.apply { totalSelectSize = size }
                    .forEachIndexed { index, episodeListBean ->
                        episodeListBean.apply {
                            if (url.contains(".html")) {
                                mainVM.getVideoInfo(
                                    url,
                                    htmlList[searchAdapter.data[currentPos].from].videoHtmlResultBean,
                                    searchAdapter.data[currentPos].img,
                                    true,
                                    index == totalSelectSize - 1
                                )
                            } else {
                                WorkerUtil.startWorker(
                                    url,
                                    searchAdapter.data[currentPos].name,
                                    searchAdapter.data[currentPos].img,
                                    webAdapter!!.list!![currentPosWeb].searchUrl,
                                    0,
                                    app.applicationContext,
                                    this@HomeFragment
                                )
                                successAddCacheCount++
                                mainVM.addCache.postValue(successAddCacheCount)
                                if (index == totalSelectSize - 1) {
                                    Toast.show("添加${successAddCacheCount}个缓冲任务成功(总共${totalSelectSize}个)")
                                }
                            }
                        }
                    }
            }
            negativeButton(R.string.cancel)
        }
    }

    private fun test() {
        if (BuildConfig.DEBUG) {
            editText_search.setText("爱情公寓5")
            updateWebSelect(6)
//            materialButton_search.performClick()
        }
    }

    override fun onStart() {
        super.onStart()
        mainVM.rules.postValue(null)
    }

    private fun updateRule(url: String, ruleStatus: Int, taskName: String) {
        Task.callInBackground {
            val rule = app.db.ruleDao().getByUrl(url)
            rule.ruleStatus = ruleStatus
            app.db.ruleDao().updateALl(rule)
        }.continueWithEnd(taskName)
    }

    private fun getRules() {
        if (!isWifi) {
            Toast.show("非WIFI连接或没有网络")
            return
        }
        if (mainVM.rules.value != null) {
            return
        }
        Task.callInBackground {
            val urlList = mutableListOf<String>()
            val rule = app.db.ruleDao().getByUrl(DEFAULT_RULE_URL)
            if (rule == null) {
                val ruleNew = Rule()
                ruleNew.ruleUrl = DEFAULT_RULE_URL
                app.db.ruleDao().insertAll(ruleNew)
                urlList.add(DEFAULT_RULE_URL)
            } else {
                val rules = app.db.ruleDao().getAll()
                rules.filter { it.ruleEnable && it.ruleStatus == 1 }.forEach {
                    urlList.add(it.ruleUrl)
                }
            }
            clearRules()
            urlList.forEach {
                mainVM.getJson(it)
            }
        }.continueWithEnd("获取详细规则")
    }

    private fun clearRules() {
        webList.clear()
        spinnerNameItems.clear()
        htmlParamsList.clear()
        htmlParamsKWList.clear()
        htmlList.clear()
        mainVM.rules.postValue(null)
    }

    private fun updateWebSelect(position: Int) {
        currentPosWeb = position
        webAdapter?.list?.forEachIndexed { index, webBean ->
            webBean.isSelected = position == index
        }
        webAdapter?.notifyDataSetChanged()
    }

    private fun initListener() {
        materialButton_search.setOnClickListener(this)
        imageButton_add_website.setOnClickListener(this)
        imageButton_menu.setOnClickListener(this)
        textView_label_website.setOnClickListener(this)
        view_search_modal.setOnClickListener(this)
        recyclerView_web.setOnClickListener(this)
        editText_search.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    group_web.visibility = View.VISIBLE
                }
            }
    }

    private fun initViews() {
        val lm = LinearLayoutManager(context)
        recyclerView_search.layoutManager = lm
        recyclerView_search.adapter = searchAdapter
        recyclerView_search.addOnItemTouchListener(
            RecyclerItemClickListener(
                context!!,
                recyclerView_search,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        isLongClick = false
                        searchClick(position)
                    }

                    override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                        isLongClick = true
                        searchClick(position)
                    }

                    override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                    }
                })
        )
        recyclerView_search.imageListener(context)
        recyclerView_web.layoutManager =
            StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        recyclerView_web.addOnItemTouchListener(
            RecyclerItemClickListener(
                context!!,
                recyclerView_web,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        currentPosWeb = position
                        updateWebSelect(currentPosWeb)
                    }

                    override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                    }

                    override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                    }
                })
        )
        editText_search.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                materialButton_search.performClick()
                true
            }
            false
        }
        initWebView()
    }

    private fun initWebView() {
        val webViewUtil = WebViewUtil()
        webViewUtil.initWebView(
            webView_for_get_url,
            { htmlList[searchAdapter.data[currentPos].from].videoHtmlResultBean }
        ) { url ->
            val reg =
                Regex("https://data.nmbaojie.com/zhilian.php\\?auth_key=[a-z0-9].*&url=https://data.nmbaojie.com/[a-z0-9_].*\\.m3u8\\?auth_key=[a-z0-9].*")
            val urlSearchResult = reg.find(url)
            if (urlSearchResult != null) {
                val vhrb = htmlList[searchAdapter.data[currentPos].from].videoHtmlResultBean.copy()
                vhrb.isFrame = false
                mainVM.getVideoInfo(
                    url,
                    vhrb,
                    searchAdapter.data[currentPos].img
                )
            } else {
                mainVM.play.postValue(
                    VideoInfoBean(
                        url,
                        searchAdapter.data[currentPos].name,
                        0,
                        searchAdapter.data[currentPos].img
                    )
                )
            }
        }
    }

    private fun searchClick(position: Int) {
        currentPos = position
        searchAdapter.data.run {
            if (isEmpty()) {
                null
            } else {
                loadingView.visibility = View.VISIBLE
                this[position]
            }
        }?.apply {
            webAdapter?.list?.apply {
                if (htmlList.isEmpty()) {
                    return
                }
                val isWebPlay = htmlList[from].searchHtmlResultBean.isWebPlay
                if (isWebPlay) {
                    val bundle = Bundle()
                    bundle.putString("url", url)
                    findNavController().navigate(R.id.webActivity, bundle)
                } else {
                    searchUrl = this[from].searchUrl
                    mainVM.getVideoEpisodesInfo(
                        url,
                        htmlList[from].episodesBean
                    )
                }
            }
        }
    }

    override fun onClick(v: View?) {
        val vid = v!!.id
        when (vid) {
            R.id.materialButton_search -> {
                if (!isWifi) {
                    Toast.show("非WIFI连接或没有网络")
                    return
                }
                val kw = editText_search.text.toString()
                if (!kw.isNullOrEmpty()) {
                    mainVM.saveSearchInfo(kw)
                    hideSoftKeyboard(context!!, mutableListOf(editText_search))
                    group_web.visibility = View.GONE
                    editText_search.clearFocus()
                    loadingView.visibility = View.VISIBLE
                    val index = currentPosWeb
                    isAll = index == webAdapter!!.list!!.size - 1
                    if (isAll) {
                        searchAdapter.data.clear()
                        webAdapter?.list?.apply {
                            for (from in 0 until size - 1) {
                                htmlParamsList[from][htmlParamsKWList[from]] = kw
                                mainVM.searchVideos(
                                    from,
                                    htmlParamsList[from],
                                    if (htmlList[index].searchHtmlResultBean.isKWInUrl) {
                                        String.format(this[from].searchUrl, kw)
                                    } else {
                                        this[from].searchUrl
                                    },
                                    htmlList[from].searchHtmlResultBean,
                                    uriIdlingResource
                                )
                            }
                        }
                    } else {
                        htmlParamsList[index][htmlParamsKWList[index]] = kw
                        webAdapter?.list?.apply {
                            mainVM.searchVideos(
                                index,
                                htmlParamsList[index],
                                if (htmlList[index].searchHtmlResultBean.isKWInUrl) {
                                    String.format(this[index].searchUrl, kw)
                                } else {
                                    this[index].searchUrl
                                },
                                htmlList[index].searchHtmlResultBean,
                                uriIdlingResource
                            )
                        }
                    }
                } else {
                    Toast.show(context, "请输入电影/电视剧名称")
                }
            }
            R.id.imageButton_add_website -> {
                MaterialDialog(context!!).show {
                    input(hintRes = R.string.add_rule_hint) { dialog, text ->
                        Task.callInBackground {
                            val url = text.toString()
                            if (url.trim().isEmpty()) {
                                activity?.runOnUiThread {
                                    Toast.show(
                                        context,
                                        R.string.add_rule_hint
                                    )
                                }
                                return@callInBackground
                            }
                            val rule = app.db.ruleDao().getByUrl(url)
                            if (rule != null) {
                                activity?.runOnUiThread {
                                    Toast.show(
                                        context,
                                        R.string.rule_url_exsit
                                    )
                                }
                            } else {
                                val ruleNew = Rule()
                                ruleNew.ruleUrl = url
                                app.db.ruleDao().insertAll(ruleNew)
                                activity?.runOnUiThread {
                                    Toast.show(
                                        context,
                                        R.string.rule_url_add_success
                                    )
                                }
                                mainVM.getJson(url)
                            }
                        }.continueWithEnd("新增规则")
                    }
                    positiveButton(R.string.ok)
                    negativeButton(R.string.cancel)
                    neutralButton(R.string.list) {
                        val intent = Intent(context, RuleListActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            R.id.imageButton_menu -> {
                if (!isWifi) {
                    Toast.show("非WIFI连接或没有网络")
                    return
                }
                mainVM.jsonBeanList.value?.apply {
                    val menuFragment = MenuFragment()
                    menuFragment.jsonBeanList = this
                    menuFragment.show(childFragmentManager, "menu")
                }
            }
            R.id.textView_label_website -> {
                val bundle = Bundle()
                bundle.putString(
                    "url",
                    "https://www.iqiyi.com"
                )
                findNavController().navigate(R.id.webActivity, bundle)
            }
            R.id.view_search_modal -> {
                hideSoftKeyboard(context!!, mutableListOf(editText_search))
                group_web.visibility = View.GONE
                editText_search.clearFocus()
            }
            R.id.recyclerView_web -> {
                hideSoftKeyboard(context!!, mutableListOf(editText_search))
                group_web.visibility = View.GONE
                editText_search.clearFocus()
            }
        }
    }

    /**
     * 隐藏软键盘(可用于Activity，Fragment)
     */
    private fun hideSoftKeyboard(context: Context, viewList: MutableList<View>) {
        val inputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        viewList.forEach {
            inputMethodManager.hideSoftInputFromWindow(
                it.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            );
        }
    }

    override fun onPause() {
        super.onPause()
        mainVM.errorInfo.postValue(null)
        mainVM.episodes.postValue(null)
        mainVM.rules.postValue(null)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun ruleEvent(ruleEvent: RuleEvent) {
        if (ruleEvent.isRefresh) {
            getRules()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun netEvent(netEvent: NetEvent) {
        isWifi = netEvent.isWifi
        if (isWifi) {
            getRules()
        }
    }

    private var uriIdlingResource: UriIdlingResource? = null
    /**
     * Only called from test, creates and returns a new {@link UriIdlingResource}.
     */
    @VisibleForTesting
    @NonNull
    fun getIdlingResource(): UriIdlingResource {
        if (uriIdlingResource == null) {
            uriIdlingResource = UriIdlingResource("http", 15 * 1000)
        }
        return uriIdlingResource!!
    }
}