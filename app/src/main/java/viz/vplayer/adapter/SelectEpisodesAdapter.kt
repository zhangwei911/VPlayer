package viz.vplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_select_episode.view.*
import viz.vplayer.R

class SelectEpisodesAdapter(private val context: Context) :
    RecyclerView.Adapter<SelectEpisodesAdapter.ViewHolder>() {
    var data = mutableListOf<String>()

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
        holder.itemView.apply {
            textView_select_episodes_label.text = ((position + 1).toString())
        }
    }
}