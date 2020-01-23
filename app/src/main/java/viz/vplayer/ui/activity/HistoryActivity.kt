package viz.vplayer.ui.activity

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.viz.tools.l
import kotlinx.android.synthetic.main.activity_history.*
import viz.vplayer.R
import viz.vplayer.util.RecyclerItemClickListener
import viz.vplayer.adapter.HistoryAdapter
import viz.vplayer.util.continueWithEnd
import viz.vplayer.vm.MainVM

class HistoryActivity : BaseActivity() {
    private val mainVM: MainVM by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(MainVM::class.java)
    }

    override fun getContentViewId(): Int = R.layout.activity_history
    override fun getCommonTtile(): String = getString(R.string.history)
    private lateinit var adapter: HistoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val htmlList = intent.getParcelableArrayExtra("htmlList").toMutableList()
        l.d(htmlList)
        recyclerView_history.layoutManager = LinearLayoutManager(this)
        Task.callInBackground {
            val viList = app.db.videoInfoDao().getAll()
            viList.forEach { vi ->
                val episodeList = app.db.episodeDao().getByVid(vi.id)
                if (episodeList.size > 0) {
                    val filterEpisodeList = episodeList.filter { episode -> episode.url == vi.videoUrl }
                    if(filterEpisodeList.isNotEmpty()) {
                        vi.index = filterEpisodeList[0].urlIndex
                    }else{
                        vi.index = 0
                    }
                    vi.episodeList = episodeList
                }
            }
            adapter = HistoryAdapter(this, viList)
            runOnUiThread {
                recyclerView_history.adapter = adapter
            }
        }.continueWithEnd("获取历史记录")
        recyclerView_history.addOnItemTouchListener(
            RecyclerItemClickListener(
                this,
                recyclerView_history,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        val data = adapter.list[position]
//                        if (data.videoUrl.endsWith("m3u8")) {
//                            val intent = Intent(this@HistoryActivity, VideoPalyerActivity::class.java)
//                            intent.putExtra("url", data.videoUrl)
//                            intent.putExtra("title", data.videoTitle)
//                            intent.putExtra("duration", data.duration)
//                            intent.putExtra("img", data.videoImgUrl)
//                            intent.putExtra("episodes", data.episodeList as Serializable)
//                            intent.putExtra("html", htmlList[searchAdapter.data[currentPos].from])
//                            startActivity(intent)
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
                                        .delete(adapter.list[position])
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
}