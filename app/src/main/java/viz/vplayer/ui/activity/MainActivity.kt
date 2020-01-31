package viz.vplayer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_main.*
import viz.vplayer.BuildConfig
import viz.vplayer.R
import viz.vplayer.adapter.SearchAdapter
import viz.vplayer.bean.HtmlBean
import viz.vplayer.bean.JsonBean
import viz.vplayer.bean.ParamBean
import viz.vplayer.bean.VideoInfoBean
import viz.vplayer.room.Rule
import viz.vplayer.ui.fragment.MenuFragment
import viz.vplayer.util.RecyclerItemClickListener
import viz.vplayer.util.continueWithEnd
import viz.vplayer.vm.MainVM
import java.io.Serializable
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class MainActivity : BaseActivity(), View.OnClickListener {
    private val mainVM: MainVM by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(MainVM::class.java)
    }
    private val SERIAL_EXECUTOR: Executor = Executors.newSingleThreadExecutor()
    private val searchAdapter = SearchAdapter(this)
    private val htmlList = mutableListOf<HtmlBean>()
    private val htmlParamsList = mutableListOf<MutableMap<String, String>>()
    private val htmlParamsKWList = mutableListOf<String>()
    private var currentPos = 0
    private var isAll = false
    private val spinnerItems = mutableListOf<String>()
    private val spinnerNameItems = mutableListOf<String>()
    private val gson = Gson()
    private var searchUrl = ""

    override fun getContentViewId(): Int = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainVM.play.observe(this, Observer { videoInfoBean ->
            val intent = Intent(this, VideoPalyerActivity::class.java)
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
        mainVM.search.observe(this, Observer { searchList ->
            l.i(searchList)
            if (isAll) {
                searchAdapter.data.addAll(searchList)
            } else {
                searchAdapter.data = searchList
            }
            searchAdapter.notifyDataSetChanged()
            loadingView.visibility = View.GONE
        })
        mainVM.episodes.observe(this, Observer { episodeList ->
            l.i(episodeList)
            if (episodeList.isEmpty()) {
                loadingView.visibility = View.GONE
                Toast.show(this, "数据为空")
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
        mainVM.rules.observe(this, Observer { rulesJson ->
            try {
                val type = object : TypeToken<MutableList<JsonBean>>() {}.type
                val jsonBeanList = gson.fromJson<MutableList<JsonBean>>(rulesJson, type)
                l.d(jsonBeanList)
                mainVM.jsonBeanList.postValue(jsonBeanList)
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
                        this,
                        R.layout.simple_spinner_item, spinnerNameItems
                    )
                spinner_website.adapter = spinnerAdapter
                if (BuildConfig.DEBUG) {
                    spinner_website.setSelection(1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainVM.errorInfo.postValue("解析rule规则数据异常")
            }

        })
        mainVM.errorInfo.observe(this, Observer { errorMsg ->
            loadingView.visibility = View.GONE
            Toast.showLong(this, errorMsg)
        })
        if (BuildConfig.DEBUG) {
            textInputEditText_search.setText("疾速杀机")
        }
        initViews()
        initListener()
//        mainVM.freeVip("https://v.qq.com/x/cover/rjae621myqca41h/e003358h201.html")
        Task.callInBackground {
            val url = "http://viphp-vi.stor.sinaapp.com/rules.txt"
            val rule = app.db.ruleDao().getByUrl(url)
            if (rule == null) {
                val ruleNew = Rule()
                ruleNew.ruleUrl = url
                app.db.ruleDao().insertAll(ruleNew)
            }
            mainVM.getJson(url)
        }.continueWithEnd("获取详细规则")
    }

    private fun initListener() {
        materialButton_search.setOnClickListener(this)
        imageButton_add_website.setOnClickListener(this)
        imageButton_menu.setOnClickListener(this)
    }

    private fun initViews() {
        textView_version.text = "版本号:" + packageManager.getPackageInfo(packageName, 0).versionName
        val lm = LinearLayoutManager(this)
        recyclerView_search.layoutManager = lm
        recyclerView_search.adapter = searchAdapter
        recyclerView_search.addOnItemTouchListener(
            RecyclerItemClickListener(
                this,
                recyclerView_search,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        loadingView.visibility = View.VISIBLE
                        currentPos = position
                        val searchBean = searchAdapter.data[position]
                        searchUrl = spinnerItems[searchBean.from]
                        mainVM.getVideoEpisodesInfo(
                            searchBean.url,
                            htmlList[searchBean.from].episodesBean
                        )
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
                                htmlList[from].searchHtmlResultBean
                            )
                        }
                    } else {
                        htmlParamsList[index][htmlParamsKWList[index]] = kw
                        mainVM.searchVideos(
                            index,
                            htmlParamsList[index],
                            spinnerItems[index],
                            htmlList[index].searchHtmlResultBean
                        )
                    }
                } else {
                    Toast.show(this, "请输入电影/电视剧名称")
                }
            }
            R.id.imageButton_add_website -> {
                MaterialDialog(this).show {
                    input(hintRes = R.string.add_rule_hint) { dialog, text ->
                        Task.callInBackground {
                            val url = text.toString()
                            if (url.trim().isEmpty()) {
                                runOnUiThread {
                                    Toast.show(
                                        this@MainActivity,
                                        R.string.add_rule_hint
                                    )
                                }
                                return@callInBackground
                            }
                            val rule = app.db.ruleDao().getByUrl(url)
                            if (rule != null) {
                                runOnUiThread {
                                    Toast.show(
                                        this@MainActivity,
                                        R.string.rule_url_exsit
                                    )
                                }
                            } else {
                                val ruleNew = Rule()
                                ruleNew.ruleUrl = url
                                app.db.ruleDao().insertAll(ruleNew)
                                runOnUiThread {
                                    Toast.show(
                                        this@MainActivity,
                                        R.string.rule_url_add_success
                                    )
                                }
                            }
                        }.continueWithEnd("新增规则")
                    }
                    positiveButton(R.string.ok)
                    negativeButton(R.string.cancel)
                    neutralButton(R.string.list) {
                        val intent = Intent(this@MainActivity, RuleListActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            R.id.imageButton_menu -> {
                val menuFragment = MenuFragment()
                menuFragment.jsonBeanList = mainVM.jsonBeanList.value!!
                menuFragment.show(supportFragmentManager, "menu")
            }
        }
    }
}
