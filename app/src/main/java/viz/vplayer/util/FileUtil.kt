package viz.vplayer.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.viz.tools.Toast
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths

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

    /**
     * 获取网络文件大小
     *
     * @param path 文件链接
     * @return 文件大小
     */
    fun downloadFileSize(context: Context, path: String): Int {
        var length: Int = 0
        object : Thread() {
            override fun run() {
                try {
                    val url = URL(path)     //创建url对象
                    val conn = url
                        .openConnection() as HttpURLConnection     //建立连接
                    conn.requestMethod = "GET"    //设置请求方法
                    conn.readTimeout = 5000       //设置响应超时时间
                    conn.connectTimeout = 5000   //设置连接超时时间
                    conn.connect()   //发送请求
                    val responseCode = conn.responseCode    //获取响应码
                    if (responseCode == 200) {   //响应码是200(固定值)就是连接成功，否者就连接失败
                        length = conn.contentLength    //获取文件的大小
                    } else {
                        Toast.show(context, "连接失败")
                    }

                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }.start()
        return length
    }

    fun write(content: String, filePath: String) {
        filePath.createNewFile()
        val fos = FileOutputStream(filePath)
        val channel = fos.channel
        val buf = ByteBuffer.wrap(content.toByteArray())
        buf.put(content.toByteArray())
        buf.flip()
        channel.write(buf)
        channel.close()
        fos.close()
    }

    fun read(filePath: String): MutableList<String> {
        val result = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.lines(Paths.get(URI.create(filePath.fileScheme()))).forEach {
                result.add(it)
            }
        } else {
            val reader = InputStreamReader(FileInputStream(filePath))
            val br = BufferedReader(reader)
            var line = br.readLine()
            while (line != null) {
                result.add(line)
                line = br.readLine()
            }
            reader.close()
            br.close()
        }
        return result
    }

    fun readStr(filePath: String): String {
        return read(filePath).joinToString("\n")
    }
}