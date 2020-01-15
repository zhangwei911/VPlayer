package viz.vplayer.adapter

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_search.view.*
import viz.vplayer.R
import viz.vplayer.bean.SearchBean
import viz.vplayer.glide.GlideApp
import viz.vplayer.glide.GlideRequests

class SearchAdapter(private val context: Context) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {
    var data = mutableListOf<SearchBean>()
    private var glide: GlideRequests? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        glide = GlideApp.with(context)
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.list_item_search, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemView = holder.itemView
        val searchBean = data[position]
        itemView.textView_name.text = searchBean.name + "(${searchBean.from})"
        itemView.textView_desc.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(searchBean.desc, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(searchBean.desc)
        }
        if (!searchBean.img.isNullOrEmpty()) {
            glide!!.load(searchBean.img)
                .error(R.drawable.ic_fail)
                .skipMemoryCache(true)
                .into(itemView.imageView_thumb)
        }
    }
}