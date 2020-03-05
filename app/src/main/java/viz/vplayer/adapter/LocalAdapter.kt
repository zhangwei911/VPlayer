package viz.vplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.local_item.view.*
import viz.vplayer.R
import viz.vplayer.glide.GlideApp
import viz.vplayer.glide.GlideRequests
import viz.vplayer.room.Download

class LocalAdapter : RecyclerView.Adapter<LocalAdapter.ViewHolder> {
    private var context: Context? = null
    var list = mutableListOf<Download>()
    private var glide: GlideRequests

    constructor(context: Context, list: MutableList<Download>) {
        this.context = context
        this.list = list
        glide = GlideApp.with(context)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.local_item, p0, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        list[position].apply {
            holder.itemView.apply {
                numberProgressBar.progress = progress
                textView_local_name.text = videoTitle
                numberProgressBar.max = 100
                if (videoImgUrl.trim().isNotEmpty()) {
//                    glide.load(videoImgUrl)
//                        .override(60, 80)
//                        .into(imageView_local)
                    imageView_local.setImageURI(videoImgUrl)
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int, @NonNull payloads: MutableList<Any>
    ) {
        if(payloads.isEmpty()){
            onBindViewHolder(holder, position)
        }else{
            list[position].apply {
                holder.itemView.apply {
                    numberProgressBar.progress = progress
                }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}