package viz.commonlib.huawei.account

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import com.google.gson.Gson
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
import org.json.JSONObject
import viz.vplayer.R

class LoginUtil(private val activity: Activity) {
    private var mAuthManager: HuaweiIdAuthService? = null
    private var mAuthParam: HuaweiIdAuthParams? = null
    fun validateIdToken(idToken: String) {
        if (TextUtils.isEmpty(idToken)) {
            l.d("ID Token is empty")
        } else {
            val idTokenParser = IDTokenParser()
            try {
                idTokenParser.verify(idToken, object : ICallBack {
                    override fun onSuccess() {}
                    override fun onSuccess(idTokenJsonStr: String) {
                        if (!TextUtils.isEmpty(idTokenJsonStr)) {
                            l.d(
                                "id Token Validate Success, verify signature: $idTokenJsonStr"
                            )
                            val jsonObj = JSONObject(idTokenJsonStr)
                            activity.runOnUiThread {
                                Toast.show("${jsonObj.getString("display_name")}, 欢迎登录!")
                            }
                        } else {
                            l.d("Id token validate failed.")
                            activity.runOnUiThread {
                                Toast.show(activity.getString(R.string.re_login))
                            }
                        }
                    }

                    override fun onFailed() {
                        l.d("Id token validate failed.")
                        activity.runOnUiThread {
                            Toast.show(activity.getString(R.string.re_login))
                        }
                    }
                })
            } catch (e: Exception) {
                l.d("id Token validate failed." + e.javaClass.simpleName)
                activity.runOnUiThread {
                    Toast.show(activity.getString(R.string.data_err))
                }
            } catch (e: Error) {
                l.d("id Token validate failed." + e.javaClass.simpleName)
                activity.runOnUiThread {
                    Toast.show(activity.getString(R.string.data_err))
                }
                if (Build.VERSION.SDK_INT < 23) {
                    l.d(
                        "android SDK Version is not support. Current version is: " + Build.VERSION.SDK_INT
                    )
                }
            }
        }
    }

    fun handleOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SIGN_IN_LOGIN) { //login success
//get user message by parseAuthResultFromIntent
            val authHuaweiIdTask =
                HuaweiIdAuthManager.parseAuthResultFromIntent(data)
            if (authHuaweiIdTask.isSuccessful) {
                val huaweiAccount = authHuaweiIdTask.result
                l.d(huaweiAccount.displayName + " signIn success ")
                l.d("AccessToken: " + huaweiAccount.accessToken)
                validateIdToken(huaweiAccount.idToken)
            } else {
                l.d(
                    "signIn failed: " + (authHuaweiIdTask.exception as ApiException).statusCode
                )
            }
        }
        if (requestCode == REQUEST_SIGN_IN_LOGIN_CODE) { //login success
            val authHuaweiIdTask =
                HuaweiIdAuthManager.parseAuthResultFromIntent(data)
            if (authHuaweiIdTask.isSuccessful) {
                val huaweiAccount = authHuaweiIdTask.result
                l.d("signIn get code success.")
                l.d("ServerAuthCode: " + huaweiAccount.authorizationCode)
                /**** english doc:For security reasons, the operation of changing the code to an AT must be performed on your server. The code is only an example and cannot be run.  */
                /** */
            } else {
                l.d(
                    "signIn get code failed: " + (authHuaweiIdTask.exception as ApiException).statusCode
                )
            }
        }
    }

    /**
     * Codelab Code
     * Pull up the authorization interface by getSignInIntent
     */
    fun signIn() {
        mAuthParam = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .setIdToken()
            .setAccessToken()
            .createParams()
        mAuthManager = HuaweiIdAuthManager.getService(activity, mAuthParam)
        activity.startActivityForResult(mAuthManager!!.signInIntent, REQUEST_SIGN_IN_LOGIN)
    }

    fun signInCode() {
        mAuthParam = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .setProfile()
            .setAuthorizationCode()
            .createParams()
        mAuthManager = HuaweiIdAuthManager.getService(activity, mAuthParam)
        activity.startActivityForResult(mAuthManager!!.signInIntent, REQUEST_SIGN_IN_LOGIN_CODE)
    }

    /**
     * Codelab Code
     * sign Out by signOut
     */
    fun signOut() {
        mAuthManager?.apply {
            val signOutTask: Task<Void> = signOut()
            signOutTask.addOnSuccessListener(OnSuccessListener<Void?> { l.d("signOut Success")
                activity.runOnUiThread {
                    Toast.show(activity.getString(R.string.logout_success))
                }})
                .addOnFailureListener(
                    OnFailureListener { l.d("signOut fail") })
        }
    }

    /**
     * Codelab Code
     * Silent SignIn by silentSignIn
     */
    fun silentSignIn() {
        mAuthParam = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .setIdToken()
            .setAccessToken()
            .createParams()
        mAuthManager = HuaweiIdAuthManager.getService(activity, mAuthParam)
        mAuthManager?.apply {
            val task: Task<AuthHuaweiId> = silentSignIn()
            task.addOnSuccessListener {
                l.d(
                    "silentSignIn success"
                )
            }
            task.addOnFailureListener(OnFailureListener { e ->
                //if Failed use getSignInIntent
                if (e is ApiException) {
                    val apiException = e
                    signIn()
                }
            })
        }
    }

    /**
     * Codelab Code
     * Silent SignIn by silentSignIn
     */
    fun silentSignInCode() {
        mAuthParam = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .setProfile()
            .setAuthorizationCode()
            .createParams()
        mAuthManager = HuaweiIdAuthManager.getService(activity, mAuthParam)
        mAuthManager?.apply {
            val task: Task<AuthHuaweiId> = silentSignIn()
            task.addOnSuccessListener {
                l.d(
                    "silentSignIn success"
                )
            }
            task.addOnFailureListener(OnFailureListener { e ->
                //if Failed use getSignInIntent
                if (e is ApiException) {
                    val apiException = e
                    signIn()
                }
            })
        }
    }
}