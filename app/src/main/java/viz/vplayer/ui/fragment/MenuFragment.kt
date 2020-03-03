package viz.vplayer.ui.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.huawei.hmf.tasks.OnFailureListener
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hmf.tasks.Task
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.result.AuthHuaweiId
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService
import com.viz.tools.Toast
import com.viz.tools.l
import kotlinx.android.synthetic.main.fragment_menu.*
import org.greenrobot.eventbus.EventBus
import viz.commonlib.event.SignEvent
import viz.commonlib.event.ScanEvent
import viz.commonlib.huawei.account.ICallBack
import viz.commonlib.huawei.account.IDTokenParser
import viz.commonlib.huawei.account.REQUEST_SIGN_IN_LOGIN
import viz.commonlib.huawei.account.REQUEST_SIGN_IN_LOGIN_CODE
import viz.commonlib.util.MyObserver
import viz.commonlib.util.REQUEST_CODE_SCAN_ONE
import viz.vplayer.R
import viz.vplayer.adapter.MenuAdapter
import viz.vplayer.bean.JsonBean
import viz.vplayer.bean.MenuBean
import viz.vplayer.ui.activity.HistoryActivity
import viz.vplayer.util.RecyclerItemClickListener


class MenuFragment : BottomSheetDialogFragment() {
    lateinit var mo: MyObserver
    private var adapter: MenuAdapter? = null
    var jsonBeanList = mutableListOf<JsonBean>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mo = MyObserver(lifecycle, "MenuFragment")
        val v = inflater.inflate(R.layout.fragment_menu, container, false)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerView_menu.layoutManager = LinearLayoutManager(context)
        val list = mutableListOf<MenuBean>()
        val mb = MenuBean("历史记录")
        list.add(mb)
        list.add(MenuBean("华为账号登录1"))
        list.add(MenuBean("华为账号登录2"))
        list.add(MenuBean("华为账号静默登录1"))
        list.add(MenuBean("华为账号静默登录2"))
        list.add(MenuBean("华为账号登出"))
        list.add(MenuBean("华为扫码"))
        adapter = MenuAdapter(context!!, list)
        recyclerView_menu.adapter = adapter
        recyclerView_menu.addOnItemTouchListener(
            RecyclerItemClickListener(
                context!!,
                recyclerView_menu,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                        when (position) {
                            0 -> {
                                val intent = Intent(context, HistoryActivity::class.java)
                                intent.putExtra("htmlList", jsonBeanList.toTypedArray())
                                startActivity(intent)
                                dismiss()
                            }
                            1 -> {
                                EventBus.getDefault().postSticky(SignEvent(0))
                                dismiss()
                            }
                            2 -> {
                                EventBus.getDefault().postSticky(SignEvent(1))
                                dismiss()
                            }
                            3 -> {
                                EventBus.getDefault().postSticky(SignEvent(2))
                                dismiss()
                            }
                            4 -> {
                                EventBus.getDefault().postSticky(SignEvent(3))
                                dismiss()
                            }
                            5 -> {
                                EventBus.getDefault().postSticky(SignEvent(4))
                                dismiss()
                            }
                            6 -> {
                                EventBus.getDefault().postSticky(ScanEvent(REQUEST_CODE_SCAN_ONE))
                                dismiss()
                            }
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