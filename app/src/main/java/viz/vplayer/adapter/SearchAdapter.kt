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
    var fromNameList = mutableListOf<String>()
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
        data[position].apply {
            holder.itemView.apply {
                textView_name.text = name + "(${fromNameList[from]})"
                textView_desc.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(desc, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml(desc)
                }
                if (!img.isNullOrEmpty()) {
                    glide!!.load(img)
                        .error(R.drawable.ic_fail)
                        .skipMemoryCache(true)
                        .into(imageView_thumb)
                }
            }
        }
    }
}