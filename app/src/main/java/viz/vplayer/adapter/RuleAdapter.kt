package viz.vplayer.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.rule_list_item.view.*
import viz.vplayer.R
import viz.vplayer.room.Rule


class RuleAdapter : RecyclerView.Adapter<RuleAdapter.ViewHolder> {
    private var context: Context? = null
    var list = mutableListOf<Rule>()

    constructor(context: Context, list: MutableList<Rule>) {
        this.context = context
        this.list = list
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.rule_list_item, p0, false)
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
        list[pos].apply {
            holder.itemView.apply {
                textView_rule_url.setTextColor(
                    if (ruleEnable) {
                        Color.BLACK
                    } else {
                        Color.LTGRAY
                    }
                )
                textView_rule_url.text = "[${ruleStatusMsg}]${ruleUrl}"
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}