package viz.vplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.menu_list_item.view.*
import viz.vplayer.R
import viz.vplayer.bean.MenuBean


class MenuAdapter : RecyclerView.Adapter<MenuAdapter.ViewHolder> {
    private var context: Context? = null
    var list= mutableListOf<MenuBean>()

    constructor(context: Context, list: MutableList<MenuBean>) {
        this.context = context
        this.list = list
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.menu_list_item, p0, false)
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
        itemView.textView_menu.text = data.name
        itemView.imageView_menu.visibility = if (data.resId != -1) {
            itemView.imageView_menu.setImageResource(data.resId)
            View.VISIBLE
        } else {
            View.GONE
        }
        itemView.textView_menu.setTextColor(data.menuColor)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}