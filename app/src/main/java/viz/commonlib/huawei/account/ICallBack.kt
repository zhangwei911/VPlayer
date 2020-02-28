package viz.commonlib.huawei.account

interface ICallBack{
    fun onSuccess()

    fun onSuccess(result: String)

    fun onFailed()
}