package viz.vplayer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.method.KeyListener
import android.view.KeyEvent
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
            startActivity(intent)
        })
        mainVM.search.observe(this, Observer { searchList ->
            l.i(searchList)
            searchAdapter.data = searchList
            searchAdapter.notifyDataSetChanged()
        })
        mainVM.episodes.observe(this, Observer { episodeList ->
            l.i(episodeList)
            mainVM.getVideoInfo(episodeList[0])
        })
        mainVM.errorInfo.observe(this, Observer { errorMsg ->
            Toast.showLong(this, errorMsg)
        })
        textInputEditText_search.setText("庆余年")
        initViews()
        initListener()
    }

    private fun initListener() {
        materialButton_search.setOnClickListener(this)
    }

    private fun initViews() {
        val lm = LinearLayoutManager(this)
        recyclerView_search.layoutManager = lm
        recyclerView_search.adapter = searchAdapter
        recyclerView_search.addOnItemTouchListener(
            RecyclerItemClickListener(
                this,
                recyclerView_search,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        mainVM.getVideoEpisodesInfo(mainVM.search.value!![position].url)
                    }

                    override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                    }

                    override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                    }
                })
        )
        val spinnerItems = arrayOf("http://www.lexianglive.com/index.php?s=vod-search-name")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerItems)
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
                    textInputLayout_search.clearFocus()
                    mainVM.searchVideos(kw, spinner_website.selectedItem.toString())
                } else {
                    Toast.show(this, "请输入电影/电视剧名称")
                }
            }
        }
    }
}
