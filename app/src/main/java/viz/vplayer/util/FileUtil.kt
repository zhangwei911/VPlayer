package viz.vplayer.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.*

object FileUtil {
    fun getPath(context: Context): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Environment.getExternalStorageDirectory().path
        } else {
            context.getExternalFilesDir(null)!!.path
        }
    }

    @Throws(IOException::class)
    fun copyBigDataToSD(
        context: Context,
        path: String,
        strOutFileName: String,
        sourceTag: String = "",
        targetTag: String = "",
        strDir: String = ""
    ) {
        Log.i("face", "start copy file $strOutFileName")
        val tmpFile = path + strDir + strOutFileName + targetTag
        val f = File(tmpFile)
        if (f.exists()) {
            Log.i("face", "file exists $strOutFileName")
            return
        }
        f.parentFile?.mkdirs()
        val myInput: InputStream
        val myOutput: OutputStream =
            FileOutputStream(tmpFile)
        myInput = context.assets.open(strDir + strOutFileName + sourceTag)
        val buffer = ByteArray(1024)
        var length = myInput.read(buffer)
        while (length > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }
        myOutput.flush()
        myInput.close()
        myOutput.close()
        Log.i("face", "end copy file $strOutFileName")
    }
}