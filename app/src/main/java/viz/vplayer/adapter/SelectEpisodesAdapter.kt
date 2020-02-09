package viz.vplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_select_episode.view.*
import viz.vplayer.R
import viz.vplayer.bean.EpisodeListBean

class SelectEpisodesAdapter(private val context: Context) :
    RecyclerView.Adapter<SelectEpisodesAdapter.ViewHolder>() {
    var data = mutableListOf<EpisodeListBean>()
    var isMultiSelect = false
    private var lastSelectedIndex = 0

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun autoClick() {
            itemView.performClick()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.list_item_select_episode, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        holder.itemView.apply {
            textView_select_episodes_label.text = ((position + 1).toString())
            val elb = data[position]
            textView_select_episodes_label.setTextColor(
                context.getColor(
                    if (elb.isSelect) {
                        R.color.colorPrimary
                    } else {
                        android.R.color.holo_blue_light
                    }
                )
            )
            textView_select_episodes_label.setBackgroundColor(
                context.getColor(
                    if (elb.isSelect) {
                        R.color.selectBG
                    } else {
                        android.R.color.transparent
                    }
                )
            )
        }
    }

    fun select(position: Int): Boolean {
        if (isMultiSelect) {
            if (data.none { it.isSelect }) {
                data[position].isSelect = true
                notifyItemChanged(position, position)
                return true
            } else if (data.filterIndexed { index, episodeListBean -> episodeListBean.isSelect && index != position }.isNotEmpty()) {
                data[position].isSelect = !data[position].isSelect
                notifyItemChanged(position, position)
                return true
            }
        } else {
            if (!data[position].isSelect) {
//                data.forEachIndexed { index, episodeListBean ->
//                    episodeListBean.apply {
//                        isSelect = index == position
//                    }
//                }
//                notifyDataSetChanged()
                data[lastSelectedIndex].isSelect = false
                data[position].isSelect = true
                notifyItemChanged(lastSelectedIndex, lastSelectedIndex)
                lastSelectedIndex = position
                notifyItemChanged(position, position)
                return true
            }
        }
        return false
    }

    fun unselect(position: Int): Boolean {
        if (isMultiSelect) {
            data[position].isSelect = false
            notifyItemChanged(position, position)
            return true
        }
        return false
    }
}