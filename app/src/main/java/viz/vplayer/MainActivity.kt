package viz.vplayer

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_main.*
import viz.vplayer.adapter.SearchAdapter
import viz.vplayer.bean.*
import viz.vplayer.vm.MainVM
import java.io.Serializable
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnClickListener {
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
    private val spinnerItems = arrayOf(
        "http://www.lexianglive.com/index.php?s=vod-search-name",
        "http://5nj.com/index.php?m=vod-search",
        "http://1090ys.com/?c=search",
        "所有"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        mainVM.play.observe(this, Observer { videoInfoBean ->
            val intent = Intent(this, VideoPalyerActivity::class.java)
            intent.putExtra("url", videoInfoBean.url)
            intent.putExtra("title", videoInfoBean.title)
            intent.putExtra("duration", videoInfoBean.duration)
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
            if (episodeList[0].endsWith("m3u8")) {
                mainVM.play.postValue(
                    VideoInfoBean(
                        episodeList[0],
                        searchAdapter.data[currentPos].name,
                        0
                    )
                )
            } else {
                mainVM.getVideoInfo(
                    episodeList[0],
                    htmlList[searchAdapter.data[currentPos].from].videoHtmlResultBean
                )
            }
        })
        mainVM.errorInfo.observe(this, Observer { errorMsg ->
            loadingView.visibility = View.GONE
            Toast.showLong(this, errorMsg)
        })
        textInputEditText_search.setText("庆余年")
        initViews()
        initListener()
//        mainVM.freeVip("https://v.qq.com/x/cover/rjae621myqca41h/e003358h201.html")
    }

    private fun initListener() {
        materialButton_search.setOnClickListener(this)
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
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        loadingView.visibility = View.VISIBLE
                        currentPos = position
                        val searchBean = searchAdapter.data[position]
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

        htmlParamsList.add(mutableMapOf())
        htmlParamsKWList.add("wd")
        htmlList.add(
            HtmlBean(
                SearchHtmlResultBean(
                    "ul.stui-vodlist__media",
                    0,
                    "li",
                    "a",
                    0,
                    true,
                    "title",
                    "div.detail",
                    0,
                    false,
                    "",
                    "a",
                    0,
                    true,
                    "data-original",
                    "a",
                    0,
                    true,
                    "href",
                    false
                ),
                EpisodesBean(
                    "div.num-tab-main",
                    0,
                    "",
                    "li",
                    "a",
                    0,
                    true,
                    "href",
                    false,
                    false,
                    0,
                    "",
                    "",
                    0,
                    false

                ),
                VideoHtmlResultBean(
                    "",
                    "div#cms_player",
                    false,
                    false,
                    0,
                    "",
                    0,
                    "script",
                    0,
                    false,
                    "",
                    true,
                    "(?<=var cms_player \\= \\{\"url\":\")(.*m3u8)(?=\",)",
                    0,
                    true,
                    "div.num-tab-main",
                    0,
                    "li",
                    "a",
                    0,
                    true,
                    "href",
                    "a",
                    0,
                    false,
                    "",
                    false
                )
            )
        )
        htmlParamsList.add(mutableMapOf())
        htmlParamsKWList.add("wd")
        htmlList.add(
            HtmlBean(
                SearchHtmlResultBean(
                    "div.index-area",
                    0,
                    "ul > li",
                    "a",
                    0,
                    true,
                    "title",
                    "a > span.lzbz",
                    0,
                    false,
                    "",
                    "img",
                    0,
                    true,
                    "data-original",
                    "a",
                    0,
                    true,
                    "href",
                    false
                ),
                EpisodesBean(
                    "div.main > script",
                    0,
                    "(?<=\\%24\\%24\\%24)(((?!\\%24\\%24\\%24).)*?)(?='\\);)",
                    "",
                    "",
                    0,
                    false,
                    "",
                    false,
                    true,
                    1,
                    "%23",
                    "(?:https)(((?!https).)*?)(?:m3u8)",
                    0,
                    true
                ),
                VideoHtmlResultBean(
                    "",
                    "div#cms_player",
                    false,
                    false,
                    0,
                    "",
                    0,
                    "script",
                    0,
                    false,
                    "",
                    true,
                    "(?<=var cms_player \\= \\{\"url\":\")(.*m3u8)(?=\",)",
                    0,
                    true,
                    "div.num-tab-main",
                    0,
                    "li",
                    "a",
                    0,
                    true,
                    "href",
                    "a",
                    0,
                    false,
                    "",
                    false
                )
            )
        )

        htmlParamsList.add(mutableMapOf())
        htmlParamsKWList.add("wd")
        htmlList.add(
            HtmlBean(
                SearchHtmlResultBean(
                    "div.stui-pannel_bd",
                    0,
                    "ul > li",
                    "a",
                    0,
                    true,
                    "title",
                    "div.detail",
                    0,
                    false,
                    "",
                    "a",
                    0,
                    true,
                    "data-original",
                    "a",
                    0,
                    true,
                    "href",
                    false
                ),
                EpisodesBean(
                    "div#play_1",
                    0,
                    "",
                    "ul > li",
                    "a",
                    0,
                    true,
                    "href",
                    false,
                    false,
                    0,
                    "",
                    "",
                    0,
                    false
                ),
                VideoHtmlResultBean(
                    "",
                    "body",
                    false,
                    true,
                    0,
                    "src",
                    0,
                    "script",
                    2,
                    false,
                    "",
                    true,
                    "(?<=<video src=\")(.*)(?=\" controls=)",
                    0,
                    false,
                    "",
                    0,
                    "",
                    "",
                    0,
                    false,
                    "",
                    "",
                    0,
                    false,
                    "",
                    false
                )
            )
        )
        val spinnerAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, spinnerItems)
        spinner_website.adapter = spinnerAdapter
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
        }
    }
}
