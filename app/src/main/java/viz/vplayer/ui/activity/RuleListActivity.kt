package viz.vplayer.ui.activity

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import bolts.Task
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.viz.tools.Toast
import kotlinx.android.synthetic.main.activity_rule_list.*
import org.greenrobot.eventbus.EventBus
import viz.commonlib.util.MyObserver
import viz.vplayer.util.DEFAULT_RULE_URL
import viz.vplayer.R
import viz.vplayer.util.RecyclerItemClickListener
import viz.vplayer.adapter.RuleAdapter
import viz.vplayer.dagger2.MyObserverModule
import viz.vplayer.eventbus.RuleEvent
import viz.vplayer.room.Rule
import viz.vplayer.room.RuleUrl
import viz.vplayer.util.continueWithEnd
import javax.inject.Inject

class RuleListActivity : BaseActivity() {
    @Inject
    lateinit var mo: MyObserver
    override fun getContentViewId(): Int =
        R.layout.activity_rule_list
    override fun getCommonTtile(): String = getString(R.string.list)
    private lateinit var adapter: RuleAdapter
    private var isRefresh = false
    private var ruleListRec = mutableListOf<Rule>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent!!.ruleListActivitySubcomponentBuilder()
            .myObserverModule(MyObserverModule(lifecycle, javaClass.name))
            .create(this)
            .inject(this)
        recyclerView_rule.layoutManager = LinearLayoutManager(this)
        Task.callInBackground {
            val ruleList = app.db.ruleDao().getAll()
            ruleListRec = ruleList
            adapter = RuleAdapter(this, ruleList)
            runOnUiThread {
                recyclerView_rule.adapter = adapter
            }
        }.continueWithEnd("获取规则")
        recyclerView_rule.addOnItemTouchListener(
            RecyclerItemClickListener(
                this,
                recyclerView_rule,
                object :
                    RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, e: MotionEvent) {
                    }

                    override fun onItemLongClick(view: View, position: Int, e: MotionEvent) {
                        val data = adapter.list!![position]
                        val isDefaultRule =
                            data.ruleUrl == DEFAULT_RULE_URL
                        if (isDefaultRule) {
                            Toast.show(this@RuleListActivity, "默认规则禁止操作")
                            return
                        }
                        MaterialDialog(this@RuleListActivity).show {
                            listItems(
                                items = arrayListOf(
                                    if (data.ruleEnable) {
                                        "禁用"
                                    } else {
                                        "启用"
                                    },
                                    "编辑",
                                    "删除"
                                )
                            ) { dialog, index, text ->
                                when (index) {
                                    0 -> {
                                        enableRule(data, position, !data.ruleEnable)
                                    }
                                    1 -> {
                                        runOnUiThread {
                                            MaterialDialog(this@RuleListActivity).show {
                                                input(prefill = data.ruleUrl) { dialog, text ->
                                                    Task.callInBackground {
                                                        if (text.trim().isEmpty()) {
                                                            runOnUiThread {
                                                                Toast.show(
                                                                    this@RuleListActivity,
                                                                    "请输入规则地址"
                                                                )
                                                            }
                                                            return@callInBackground
                                                        }
                                                        val rule = app.db.ruleDao()
                                                            .getByUrl(data.ruleUrl)
                                                        rule.ruleUrl = text.toString()
                                                        app.db.ruleDao().updateALl(rule)
                                                        data.ruleUrl = rule.ruleUrl
                                                        runOnUiThread {
                                                            adapter.notifyItemChanged(
                                                                position,
                                                                "payload"
                                                            )
                                                        }
                                                    }.continueWithEnd("编辑规则")
                                                }
                                                positiveButton(R.string.ok)
                                                negativeButton(R.string.cancel)
                                            }
                                        }
                                    }
                                    2 -> {
                                        runOnUiThread {
                                            MaterialDialog(this@RuleListActivity).show {
                                                title(R.string.delete_tips)
                                                message(
                                                    text = String.format(
                                                        getString(R.string.delete_msg_rule),
                                                        data.ruleUrl
                                                    )
                                                )
                                                positiveButton(
                                                    R.string.delete,
                                                    click = {
                                                        Task.callInBackground {
                                                            app.db.ruleDao()
                                                                .deleteByUrl(RuleUrl(data.ruleUrl))
                                                        }.continueWithEnd("删除规则")
                                                        adapter.list!!.removeAt(position)
                                                        adapter.notifyItemRemoved(position)
                                                    })
                                                negativeButton(R.string.cancel)
                                            }
                                        }
                                    }
                                    else -> {
                                    }
                                }
                                isRefresh = if (ruleListRec.size == adapter.list.size) {
                                    ruleListRec == adapter.list
                                } else {
                                    true
                                }
                            }
                            lifecycleOwner(this@RuleListActivity)
                        }
                    }

                    override fun onItemDoubleClick(view: View, position: Int, e: MotionEvent) {
                    }
                })
        )
    }

    private fun enableRule(data: Rule, position: Int, enable: Boolean) {
        Task.callInBackground {
            val rule = app.db.ruleDao().getByUrl(data.ruleUrl)
            rule.ruleEnable = enable
            app.db.ruleDao().updateALl(rule)
            data.ruleEnable = rule.ruleEnable
            runOnUiThread {
                adapter.notifyItemChanged(position, "payload")
            }
        }.continueWithEnd(
            if (enable) {
                "启用规则"
            } else {
                "禁用规则"
            }
        )
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().postSticky(RuleEvent(isRefresh))
    }
}