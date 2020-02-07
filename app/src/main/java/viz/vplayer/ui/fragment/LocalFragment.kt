package viz.vplayer.ui.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.addCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.list.listItems
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.fragment_local.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import viz.vplayer.R
import viz.vplayer.adapter.LocalAdapter
import viz.vplayer.bean.JsonBean
import viz.vplayer.eventbus.DownloadProgressEvent
import viz.vplayer.eventbus.DownloadStatusEvent
import viz.vplayer.eventbus.HtmlListEvent
import viz.vplayer.eventbus.NetEvent
import viz.vplayer.room.Download
import viz.vplayer.room.NotificationId
import viz.vplayer.ui.activity.VideoPalyerActivity
import viz.vplayer.util.App
import viz.vplayer.util.RecyclerItemClickListener
import viz.vplayer.util.WorkerUtil
import viz.vplayer.util.continueWithEnd
import viz.vplayer.vm.MainVM
import java.io.Serializable

class LocalFragment : BaseFragment() {
    override fun getContentViewId(): Int = R.layout.fragment_local
    override fun useEventBus(): Boolean = true
    private var localAdapter: LocalAdapter? = null
    private val list = mutableListOf<Download>()
    private var htmlList = mutableListOf<JsonBean>()
    private lateinit var mainVM: MainVM

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainVM = activity?.run {
            ViewModelProvider(this).get(MainVM::class.java)
        } ?: throw Exception("Invalid Activity")
        mainVM.jsonBeanList.observe(viewLifecycleOwner, Observer {
            htmlList = it
        })
        initViews()
        Task.callInBackground {
            app.db.downloadDao().getAll().apply {
                l.d(this)
                localAdapter = LocalAdapter(context!!, this)
                activity?.runOnUiThread {
                    recyclerView_download.adapter = localAdapter
                    localAdapter?.notifyDataSetChanged()
                }
            }
        }.continueWithEnd("获取下载记录")
        materialButton_download.setOnClickListener {
            Task.callInBackground {
                if (!isWifi) {
                    Toast.show("非WIFI连接或没有网络")
                    return@callInBackground
                }
                if (materialButton_download.text == getString(R.string.start_all)) {
                    var downloads = App.instance.db.downloadDao().getAllByStatus(0)
                    val downloadSize = downloads.size
                    l.d("共${downloadSize}个下载任务")
                    downloads.forEachIndexed { index, download ->
                        download.apply {
                            l.df("启动视频", videoUrl, videoTitle, "下载任务")
                            activity?.runOnUiThread {
                                WorkerUtil.startWorker(
                                    videoUrl,
                                    videoTitle,
                                    videoImgUrl,
                                    searchUrl,
                                    duration,
                                    activity!!.applicationContext,
                                    viewLifecycleOwner
                                )
                            }
                        }
                    }
                    if (downloadSize > 0) {
                        materialButton_download.text = getString(R.string.stop_all)
                    }
                } else {
                    activity?.run {
                        WorkManager.getInstance(applicationContext).pruneWork()
                        materialButton_download.text = getString(R.string.start_all)
                    }
                }
            }.continueWithEnd(
                if (materialButton_download.text == getString(R.string.start_all)) {
                    "启动"
                } else {
                    "暂停"
                } + "所有视频下载"
            )
        }
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun initViews() {
        recyclerView_download.apply {
            layoutManager = LinearLayoutManager(context)
            addOnItemTouchListener(
                RecyclerItemClickListener(
                    context,
                    this,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                            localAdapter?.list?.get(position)?.apply {
                                if (status == 1) {
                                    val intent = Intent(context, VideoPalyerActivity::class.java)
                                    intent.putExtra("url", videoUrl)
                                    intent.putExtra("title", videoTitle)
                                    intent.putExtra("img", videoImgUrl)
                                    Task.callInBackground {
                                        app.db.downloadDao().getByUrl(videoUrl).apply {
                                            intent.putExtra("duration", duration)
                                            val episodes = mutableListOf<String>()
                                            app.db.episodeDao().getByVid(id).forEach {
                                                episodes.add(it.url)
                                            }
                                            intent.putExtra("episodes", episodes as Serializable)
                                            val jsonBeanList = htmlList.filter {
                                                val jsonBean = it as JsonBean
                                                jsonBean.searchUrl == searchUrl
                                            }
                                            if (jsonBeanList.isNotEmpty()) {
                                                val jsonBean = jsonBeanList[0] as JsonBean
                                                intent.putExtra("html", jsonBean.html)
                                            }
                                            startActivity(intent)
                                        }
                                    }.continueWithEnd("获取视频信息")
                                }
                            }
                        }

                        override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                            localAdapter?.list?.get(position)?.apply {
                                MaterialDialog(context).show {
                                    val items = arrayListOf<String>()
                                    if (status == 0 && !WorkerUtil.isWorking(
                                            videoUrl,
                                            app.applicationContext
                                        )
                                    ) {
                                        items.add("下载")
                                    }
                                    items.add("删除")
                                    listItems(
                                        items = items
                                    ) { dialog, index, text ->
                                        when (text) {
                                            "删除" -> {
                                                MaterialDialog(context).show {
                                                    title(R.string.delete_tips)
                                                    message(
                                                        text = String.format(
                                                            getString(R.string.delete_msg_local),
                                                            videoTitle,
                                                            videoUrl
                                                        )
                                                    )
                                                    positiveButton(
                                                        R.string.delete,
                                                        click = {
                                                            Task.callInBackground {
                                                                app.db.downloadDao()
                                                                    .deleteByNotificationId(
                                                                        NotificationId(
                                                                            notificationId
                                                                        )
                                                                    )
                                                            }.continueWithEnd("删除下载记录")
                                                            localAdapter?.list?.removeAt(position)
                                                            localAdapter?.notifyItemRemoved(position)
                                                        })
                                                    negativeButton(R.string.cancel)
                                                    val btnNeg = getActionButton(WhichButton.NEGATIVE)
                                                    btnNeg.setTextColor(Color.LTGRAY)
                                                    val btnPos = getActionButton(WhichButton.POSITIVE)
                                                    btnPos.setTextColor(context.getColor(R.color.colorPrimary))
                                                }
                                            }
                                            "下载" -> {
                                                WorkerUtil.startWorker(
                                                    videoUrl,
                                                    videoTitle,
                                                    videoImgUrl,
                                                    searchUrl,
                                                    duration,
                                                    app.applicationContext,
                                                    viewLifecycleOwner
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                        }
                    })
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun htmlListEvent(htmlListEvent: HtmlListEvent) {
        htmlList = htmlListEvent.htmlList
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun downloadProgressEvent(downloadProgressEvent: DownloadProgressEvent) {
        localAdapter?.list?.forEachIndexed { index, download ->
            if (download.videoUrl == downloadProgressEvent.videoUrl) {
                download.progress = downloadProgressEvent.progress
                if (download.progress == 100) {
                    download.status = 1
                }
                localAdapter?.notifyItemChanged(index)
                Task.callInBackground {
                    app.db.downloadDao().updateALl(download)
                }.continueWithEnd("保存下载进度")
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun netEvent(netEvent: NetEvent) {
        isWifi = netEvent.isWifi
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun downloadStatusEvent(downloadStatusEvent: DownloadStatusEvent) {
        materialButton_download?.setText(
            if (downloadStatusEvent.isDownloading) {
                R.string.stop_all
            } else {
                R.string.start_all
            }
        )
    }
}
