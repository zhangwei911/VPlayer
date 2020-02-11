package viz.vplayer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_history.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import viz.commonlib.util.MyObserver
import viz.vplayer.R
import viz.vplayer.util.RecyclerItemClickListener
import viz.vplayer.adapter.HistoryAdapter
import viz.vplayer.bean.JsonBean
import viz.vplayer.dagger2.MyObserverModule
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.util.continueWithEnd
import viz.vplayer.util.imageListener
import viz.vplayer.vm.MainVM
import java.io.Serializable
import javax.inject.Inject

class HistoryActivity : BaseActivity() {
    @Inject
    lateinit var mo: MyObserver
    private val mainVM: MainVM by lazy {
        ViewModelProvider(this).get(MainVM::class.java)
    }

    override fun getContentViewId(): Int = R.layout.activity_history
    override fun getCommonTtile(): String = getString(R.string.history)
    private lateinit var adapter: HistoryAdapter
    private var isWifi = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent!!.historyActivitySubcomponentBuilder()
            .myObserverModule(MyObserverModule(lifecycle, javaClass.name))
            .create(this)
            .inject(this)
        val htmlParcelableArray = intent.getParcelableArrayExtra("htmlList") ?: return
        val htmlList = htmlParcelableArray.toMutableList()
        l.d(htmlList)
        recyclerView_history.imageListener(this)
        recyclerView_history.layoutManager = LinearLayoutManager(this)
        recyclerView_history.addOnItemTouchListener(
            RecyclerItemClickListener(
                this,
                recyclerView_history,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        if(!isWifi){
                            Toast.show("非WIFI连接或没有网络")
                            return
                        }
                        val data = adapter.list[position]
//                        if (data.videoUrl.endsWith("m3u8")) {
                        val intent = Intent(this@HistoryActivity, VideoPlayerActivity::class.java)
                        intent.putExtra("url", data.videoUrl)
                        intent.putExtra("title", data.videoTitle)
                        intent.putExtra("duration", data.duration)
                        intent.putExtra("img", data.videoImgUrl)
                        val episodes = mutableListOf<String>()
                        data.episodeList.forEach {
                            episodes.add(it.url)
                        }
                        intent.putExtra("episodes", episodes as Serializable)
                        val jsonBeanList = htmlList.filter {
                            val jsonBean = it as JsonBean
                            jsonBean.searchUrl == data.searchUrl
                        }
                        if (jsonBeanList.isNotEmpty()) {
                            val jsonBean = jsonBeanList[0] as JsonBean
                            intent.putExtra("html", jsonBean.html)
                        }
                        startActivity(intent)
//                        } else {
//                            mainVM.getVideoInfo(
//                                data.episodeList[0].url,
//                                htmlList[searchAdapter.data[currentPos].from].videoHtmlResultBean,
//                                searchAdapter.data[currentPos].img
//                            )
//                        }
                    }

                    override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                        val data = adapter.list[position]
                        MaterialDialog(this@HistoryActivity).show {
                            title(R.string.history_detail)
                            message(
                                text = String.format(
                                    "名称:%s",
                                    data.videoTitle
                                )
                            )
                            negativeButton(R.string.delete, click = {
                                Task.callInBackground {
                                    app.db.videoInfoDao()
                                        .delete(data)
                                }.continueWithEnd("删除历史记录")
                                adapter.list.removeAt(position)
                                adapter.notifyItemRemoved(position)
                            })
                            positiveButton(R.string.close)
                            lifecycleOwner(this@HistoryActivity)
                        }
                    }

                    override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                    }
                })
        )
    }

    private fun refresh() {
        Task.callInBackground {
            val viList = app.db.videoInfoDao().getAll()
            viList.forEach { vi ->
                val episodeList = app.db.episodeDao().getByVid(vi.id)
                if (episodeList.size > 0) {
                    val filterEpisodeList =
                        episodeList.filter { episode -> episode.url == vi.videoUrl }
                    if (filterEpisodeList.isNotEmpty()) {
                        vi.index = filterEpisodeList[0].urlIndex
                    } else {
                        vi.index = 0
                    }
                    vi.episodeList = episodeList
                }
            }
            l.d(viList)
            adapter = HistoryAdapter(this, viList)
            runOnUiThread {
                recyclerView_history.adapter = adapter
            }
        }.continueWithEnd("获取历史记录")
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun netEvent(netEvent: NetEvent) {
        isWifi = netEvent.isWifi
    }
}