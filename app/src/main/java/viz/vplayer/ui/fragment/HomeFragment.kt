package viz.vplayer.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.espresso.idling.net.UriIdlingResource
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.fragment_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import viz.vplayer.BuildConfig
import viz.vplayer.R
import viz.vplayer.adapter.SearchAdapter
import viz.vplayer.bean.*
import viz.vplayer.eventbus.HtmlListEvent
import viz.vplayer.eventbus.RuleEvent
import viz.vplayer.room.Rule
import viz.vplayer.ui.activity.RuleListActivity
import viz.vplayer.ui.activity.VideoPalyerActivity
import viz.vplayer.util.*
import viz.vplayer.vm.MainVM
import java.io.Serializable
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HomeFragment : BaseFragment(), View.OnClickListener {
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
    private val spinnerItems = mutableListOf<String>()
    private val spinnerNameItems = mutableListOf<String>()
    private val gson = Gson()
    private var searchUrl = ""

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainVM = activity?.run {
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
                .create(MainVM::class.java)
        } ?: throw Exception("Invalid Activity")
        searchAdapter = SearchAdapter(context!!)
        mainVM.play.observe(viewLifecycleOwner, Observer { videoInfoBean ->
            val intent = Intent(context, VideoPalyerActivity::class.java)
            intent.putExtra("url", videoInfoBean.url)
            intent.putExtra("searchUrl", searchUrl)
            intent.putExtra("title", videoInfoBean.title)
            intent.putExtra("duration", videoInfoBean.duration)
            intent.putExtra("img", videoInfoBean.img)
            intent.putExtra("episodes", mainVM.episodes.value as Serializable)
            intent.putExtra("html", htmlList[searchAdapter.data[currentPos].from])
            startActivity(intent)
            loadingView.visibility = View.GONE
        })
        mainVM.search.observe(viewLifecycleOwner, Observer { searchList ->
            l.i(searchList)
            if (isAll) {
                searchAdapter.data.addAll(searchList)
            } else {
                searchAdapter.data = searchList
            }
            searchAdapter.notifyDataSetChanged()
            loadingView.visibility = View.GONE
            if (BuildConfig.DEBUG) {
                searchClick(0)
            }
        })
        mainVM.episodes.observe(viewLifecycleOwner, Observer { episodeList ->
            l.i(episodeList)
            if (episodeList.isEmpty()) {
                loadingView.visibility = View.GONE
                Toast.show(context, "数据为空")
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
            try {
                val type = object : TypeToken<MutableList<JsonBean>>() {}.type
                val jsonBeanList = gson.fromJson<MutableList<JsonBean>>(rulesJsonPair.second, type)
                l.d(jsonBeanList)
                mainVM.jsonBeanList.postValue(jsonBeanList)
                if (spinnerNameItems.size > 0) {
                    spinnerNameItems.remove("所有")
                }
                jsonBeanList.forEach { jsonBean: JsonBean ->
                    val map = mutableMapOf<String, String>()
                    jsonBean.params.forEach { paramBean: ParamBean ->
                        map[paramBean.key] = paramBean.value
                    }
                    spinnerItems.add(jsonBean.searchUrl)
                    spinnerNameItems.add(jsonBean.webName)
                    htmlParamsList.add(map)
                    htmlParamsKWList.add(jsonBean.kwKey)
                    htmlList.add(jsonBean.html)
                }
                searchAdapter.fromNameList = spinnerNameItems
                spinnerNameItems.add("所有")
                val spinnerAdapter =
                    ArrayAdapter(
                        context!!,
                        R.layout.simple_spinner_item, spinnerNameItems
                    )
                spinner_website.adapter = spinnerAdapter
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
        mainVM.jsonBeanList.observe(viewLifecycleOwner, Observer {
            EventBus.getDefault().postSticky(HtmlListEvent(it))
        })
        mainVM.errorInfo.observe(viewLifecycleOwner, Observer { errorInfo ->
            loadingView.visibility = View.GONE
            Toast.showLong(context, errorInfo.errMsg)
            when (errorInfo.errCode) {
                ErrorCode.ERR_JSON_INVALID -> {
                    updateRule(errorInfo.url, 0, "规则无效")
                }
                ErrorCode.ERR_JSON_EMPTY -> {
                    updateRule(errorInfo.url, -1, "规则数据为空")
                }
            }
        })
        initViews()
        initListener()
        getRules()

        Task.callInBackground {
            var downloads = App.instance.db.downloadDao().getAllByStatus(0)
            downloads.forEachIndexed { index, download ->
                download.apply {
                    WorkerUtil.startWorker(
                        videoUrl,
                        videoTitle,
                        videoImgUrl,
                        activity!!.applicationContext,
                        viewLifecycleOwner
                    )
                }
            }
        }.continueWithEnd("启动所有视频下载")
    }

    private fun test() {
        if (BuildConfig.DEBUG) {
            textInputEditText_search.setText("锦衣之下")
            spinner_website.setSelection(2)
//            materialButton_search.performClick()
        }
    }


    private fun updateRule(url: String, ruleStatus: Int, taskName: String) {
        Task.callInBackground {
            val rule = app.db.ruleDao().getByUrl(url)
            rule.ruleStatus = ruleStatus
            app.db.ruleDao().updateALl(rule)
        }.continueWithEnd(taskName)
    }

    private fun getRules() {
        Task.callInBackground {
            val url = "http://viphp-vi.stor.sinaapp.com/rules.txt"
            val urlList = mutableListOf<String>()
            val rule = app.db.ruleDao().getByUrl(url)
            if (rule == null) {
                val ruleNew = Rule()
                ruleNew.ruleUrl = url
                app.db.ruleDao().insertAll(ruleNew)
                urlList.add(url)
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
        spinnerItems.clear()
        spinnerNameItems.clear()
        htmlParamsList.clear()
        htmlParamsKWList.clear()
        htmlList.clear()
    }

    private fun initListener() {
        materialButton_search.setOnClickListener(this)
        imageButton_add_website.setOnClickListener(this)
        imageButton_menu.setOnClickListener(this)
    }

    private fun initViews() {
        textView_version.text =
            "版本号:" + activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName
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
                        searchClick(position)
                    }

                    override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                    }

                    override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                    }
                })
        )
        htmlList.forEach { htmlBean: HtmlBean ->
            l.d(gson.toJson(htmlBean))
        }
        textInputEditText_search.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                materialButton_search.performClick()
                true
            }
            false
        }
    }

    private fun searchClick(position: Int) {
        loadingView.visibility = View.VISIBLE
        currentPos = position
        val searchBean = searchAdapter.data[position]
        searchUrl = spinnerItems[searchBean.from]
        mainVM.getVideoEpisodesInfo(
            searchBean.url,
            htmlList[searchBean.from].episodesBean
        )
    }

    override fun onClick(v: View?) {
        val vid = v!!.id
        when (vid) {
            R.id.materialButton_search -> {
                val kw = textInputEditText_search.text.toString()
                if (!kw.isNullOrEmpty()) {
                    loadingView.visibility = View.VISIBLE
                    textInputLayout_search.clearFocus()
                    val index = spinner_website.selectedItemPosition
                    isAll = index == spinner_website.count - 1
                    if (isAll) {
                        searchAdapter.data.clear()
                        for (from in 0 until spinner_website.count - 1) {
                            htmlParamsList[from][htmlParamsKWList[from]] = kw
                            mainVM.searchVideos(
                                from,
                                htmlParamsList[from],
                                spinnerItems[from],
                                htmlList[from].searchHtmlResultBean,
                                uriIdlingResource
                            )
                        }
                    } else {
                        htmlParamsList[index][htmlParamsKWList[index]] = kw
                        mainVM.searchVideos(
                            index,
                            htmlParamsList[index],
                            spinnerItems[index],
                            htmlList[index].searchHtmlResultBean,
                            uriIdlingResource
                        )
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
                val menuFragment = MenuFragment()
                menuFragment.jsonBeanList = mainVM.jsonBeanList.value!!
                menuFragment.show(childFragmentManager, "menu")
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun ruleEvent(ruleEvent: RuleEvent) {
        if (ruleEvent.isRefresh) {
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