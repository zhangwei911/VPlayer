package viz.vplayer.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.viz.tools.apk.ScreenUtils
import kotlinx.android.synthetic.main.web_list_item.view.*
import viz.vplayer.R
import viz.vplayer.bean.WebBean


class WebAdapter : RecyclerView.Adapter<WebAdapter.ViewHolder> {
    private var context: Context? = null
    var list = mutableListOf<WebBean>()
    var colors = mutableListOf(
        Color.argb(255, 240, 240, 240),
        Color.argb(255, 255, 122, 67),
        Color.argb(255, 0, 176, 255),
        Color.argb(255, 255, 255, 255),
        Color.argb(255, 156, 92, 252),
        Color.argb(255, 95, 83, 254),
        Color.argb(255, 0, 144, 255),
        Color.argb(255, 255, 156, 0),
        Color.argb(255, 191, 251, 143)
    )

    constructor(context: Context, list: MutableList<WebBean>) {
        this.context = context
        this.list = list
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.web_list_item, p0, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        pos: Int, @NonNull payloads: MutableList<Any>
    ) {
        val data = list[pos]
        holder.itemView.apply {
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, ScreenUtils.dpToPx(context, 60f).toInt()
            )
            lp.setMargins(ScreenUtils.dpToPx(context, 8f).toInt())
            cardView_web.layoutParams = lp
//            val color = colors[if (data.isSelected) {
//                1
//            } else {
//                0
//            }]
//            cardView_web.background.colorFilter = LightingColorFilter(
//                color,
//                color
//            )
            constraintLayout_web.background = ContextCompat.getDrawable(
                context, if (data.isSelected) {
                    R.drawable.web_bg
                } else {
                    R.drawable.web_bg_unselected
                }
            )
            textView_web.text = data.name
            imageView_web.visibility = if (data.resId != -1) {
                imageView_web.setImageResource(data.resId)
                View.VISIBLE
            } else {
                View.GONE
            }
            textView_web.setTextColor(
                if (data.isSelected) {
                    Color.WHITE
                } else {
                    data.menuColor
                }
            )
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}