package viz.vplayer.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_menu.*
import viz.vplayer.R
import viz.vplayer.util.RecyclerItemClickListener
import viz.vplayer.adapter.MenuAdapter
import viz.vplayer.bean.JsonBean
import viz.vplayer.bean.MenuBean
import viz.vplayer.ui.activity.HistoryActivity


class MenuFragment : BottomSheetDialogFragment() {
    private var adapter: MenuAdapter? = null
    var jsonBeanList = mutableListOf<JsonBean>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_menu, container, false)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerView_menu.layoutManager = LinearLayoutManager(context)
        val list = mutableListOf<MenuBean>()
        val mb = MenuBean("历史记录")
        list.add(mb)
        adapter = MenuAdapter(context!!, list)
        recyclerView_menu.adapter = adapter
        recyclerView_menu.addOnItemTouchListener(
            RecyclerItemClickListener(
                context!!,
                recyclerView_menu,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        if (position == 0) {
                            val intent = Intent(context, HistoryActivity::class.java)
                            intent.putExtra("htmlList", jsonBeanList.toTypedArray())
                            startActivity(intent)
                            dismiss()
                        }
                    }

                    override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                    }

                    override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                    }
                })
        )
    }
}