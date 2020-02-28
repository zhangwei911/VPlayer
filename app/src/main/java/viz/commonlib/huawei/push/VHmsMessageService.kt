package viz.commonlib.huawei.push

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import com.viz.tools.l

class VHmsMessageService: HmsMessageService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        remoteMessage?.apply {
            l.d(data)
        }
    }

    override fun onDeletedMessages() {
        l.d()
    }

    override fun onMessageSent(msg: String?) {
        msg?.let {
            l.d(it)
        }
    }

    override fun onSendError(errMsg: String?, e: Exception?) {
        l.e(errMsg)
        e?.printStackTrace()
    }

    override fun onNewToken(token: String?) {
        l.d(token)
    }
}