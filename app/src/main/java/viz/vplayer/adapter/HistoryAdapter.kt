package viz.vplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.viz.tools.TimeFormat
import kotlinx.android.synthetic.main.history_item.view.*
import viz.vplayer.R
import viz.vplayer.glide.GlideApp
import viz.vplayer.glide.GlideRequests
import viz.vplayer.room.VideoInfo

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private var context: Context? = null
    var list = mutableListOf<VideoInfo>()
    private var glide: GlideRequests

    constructor(context: Context, list: MutableList<VideoInfo>) {
        this.context = context
        this.list = list
        glide = GlideApp.with(context)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.history_item, p0, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list!!.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        pos: Int, @NonNull payloads: MutableList<Any>
    ) {
        val data = list!![pos]
        val itemView = holder.itemView
        itemView.textView_history_name.text = data.videoTitle
        val leftTime = data.duration - data.currentPosition
        itemView.textView_history_time_left.text = "剩余" + CommonUtil.stringForTime(leftTime) + "未看" + "第${data.index + 1}集"
        if (data.videoImgUrl.trim().isNotEmpty()) {
            glide.load(data.videoImgUrl)
                .override(60, 80)
                .into(itemView.imageView_history)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}