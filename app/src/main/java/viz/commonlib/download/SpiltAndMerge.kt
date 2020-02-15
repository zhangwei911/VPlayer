package viz.commonlib.download

import bolts.Task
import com.viz.tools.l
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import viz.vplayer.util.App
import viz.vplayer.util.continueWithEnd
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import kotlin.math.sqrt


object SpiltAndMerge {
//    @Throws(IOException::class)
//    @JvmStatic
//    fun main(args: Array<String>) { // TODO Auto-generated method stub
//        spilt("E:/北邮人/人生果实.Life.Is.Fruity.2017.1024X576-日菁字幕组.mp4", 350, "E:/文件分割")
//        merge("E:/文件分割", "E:/文件分割.mp4")
//    }

    @Throws(IOException::class)
    suspend fun spilt(from: String?, size: Int, to: String) {
        val f = File(from)
        val `in` = FileInputStream(f)
        var out: FileOutputStream? = null
        val inChannel = `in`.channel
        var outChannel: FileChannel? = null
        // 将MB单位转为为字节B
        val m = size * 1024 * 1024.toLong()
        // 计算最终会分成几个文件
        val count = (f.length() / m).toInt()
        // System.out.println(f.length() + " " + m + " " + count);
        for (i in 0..count) { // 生成文件的路径
            val t = "$to/$i.block"
            try {
                out = FileOutputStream(File(t))
                outChannel = out.channel
                // 从inChannel的m*i处，读取固定长度的数据，写入outChannel
                if (i != count) inChannel.transferTo(
                    m * i,
                    m,
                    outChannel
                ) else  // 最后一个文件，大小不固定，所以需要重新计算长度
                    inChannel.transferTo(m * i, f.length() - m * count, outChannel)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                out!!.close()
                outChannel!!.close()
            }
        }
        `in`.close()
        inChannel.close()
    }

    @Throws(IOException::class)
    suspend fun merge(from: String?, to: String?, id: Int) {
        Task.callInBackground {
            val t = File(to)
            var `in`: FileInputStream? = null
            var inChannel: FileChannel? = null
            val out = FileOutputStream(t, true)
            val outChannel = out.channel
            val f = File(from)
            // 获取目录下的每一个文件名，再将每个文件一次写入目标文件
            if (f.isDirectory) {
                val tsList = App.instance.db.tsDao().getAllByM3U8Id(id)
                tsList.sortBy { it.index }
                // 记录新文件最后一个数据的位置
                var start: Long = 0
                l.d("文件数:${tsList.size}")
                l.start("mergeAll")
                var mergeCount = 0
                tsList.forEachIndexed { index, ts ->
                    val file = File(ts.path)
                    if(file.exists()) {
                        `in` = FileInputStream(File(ts.path))
                        inChannel = `in`!!.channel
                        // 从inChannel中读取file.length()长度的数据，写入outChannel的start处
                        outChannel.transferFrom(inChannel, start, file.length())
                        start += file.length()
                        `in`?.close()
                        inChannel?.close()
                        mergeCount++
                    }
                }
                l.d("合成数量:$mergeCount")
                l.end("mergeAll")
            }
            out.close()
            outChannel.close()
        }.continueWithEnd("merge")
    }

    private var finishCount = 0
    private var mergeES:ScheduledExecutorService? = null
    @Throws(IOException::class)
    suspend fun mergePM(from: String?, to: String?) {
        val t = File(to)
        val f = File(from)
        // 获取目录下的每一个文件名，再将每个文件一次写入目标文件
        if (f.isDirectory) {
            val files = f.listFiles()
            // 记录新文件最后一个数据的位置
            var start: Long = 0
            l.d("文件数:${files.size}")
            var childCount = 10
            val listMerge = mutableListOf<MutableList<File>>()
            var indexMerge = 0
            files?.filter { it.extension == "ts" }?.apply {
                childCount = sqrt(size.toFloat()).toInt()
            }?.forEachIndexed { index, file ->
                if (listMerge.size == 0) {
                    listMerge.add(mutableListOf())
                }
                if (file.isFile) {
                    listMerge[indexMerge].add(file)
                }
                when {
                    (index + 1) % childCount == 0 -> {
                        indexMerge++
                        listMerge.add(mutableListOf())
                    }
                    else -> {
                    }
                }
            }
            l.d(listMerge)
            val listChild = mutableListOf<File>()
            l.start("fileTotal")
            l.start("file")
            finishCount = 0
            var total = 0
            listMerge.filter { it.size > 0 }.apply {
                mergeES = ScheduledThreadPoolExecutor(
                    size,
                    BasicThreadFactory.Builder().namingPattern("pool-merge-%d").daemon(true).build()
                )
                total = size
            }.forEachIndexed { index, fromList ->
                val child = "$to-child/$index"
                listChild.add(File(child))
                mergeES?.execute {
                    l.start("singleFile")
                    mergePC(fromList, child)
                    l.end("singleFile")
                    if(finishCount == total) {
                        l.end("file")
                        mergePC(
                            listChild,
                            to
                        )
                        l.end("fileTotal")
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    suspend fun mergeP(from: String?, to: String?) {
        val t = File(to)
        val f = File(from)
        // 获取目录下的每一个文件名，再将每个文件一次写入目标文件
        if (f.isDirectory) {
            val files = f.listFiles()
            // 记录新文件最后一个数据的位置
            var start: Long = 0
            l.d("文件数:${files.size}")
            var childCount = 10
            val listMerge = mutableListOf<MutableList<File>>()
            var indexMerge = 0
            files?.filter { it.extension == "ts" }?.apply {
                childCount = size / 10
            }?.forEachIndexed { index, file ->
                if (listMerge.size == 0) {
                    listMerge.add(mutableListOf())
                }
                if (file.isFile) {
                    listMerge[indexMerge].add(file)
                }
                when {
                    (index + 1) % childCount == 0 -> {
                        indexMerge++
                        listMerge.add(mutableListOf())
                    }
                    else -> {
                    }
                }
            }
            l.d(listMerge)
            val listChild = mutableListOf<File>()
            l.start("fileTotal")
            l.start("file")
            listMerge.filter { it.size > 0 }.forEachIndexed { index, fromList ->
                val child = "$to-child/$index"
                listChild.add(File(child))
                l.start("singleFile")
                mergePC(fromList, child)
                l.end("singleFile")
            }
            l.end("file")
            mergePC(listChild, to)
            l.end("fileTotal")
        }
    }

    @Throws(IOException::class)
    fun mergePC(fromList: MutableList<File>, to: String?) {
        val t = File(to)
        if (!t.parentFile.exists()) {
            t.parentFile.mkdirs()
        }
        var `in`: FileInputStream? = null
        var inChannel: FileChannel? = null
        val out = FileOutputStream(t, true)
        val outChannel = out.channel
        // 记录新文件最后一个数据的位置
        var start: Long = 0
        l.d("文件数:${fromList.size}")
        l.d("合并子文件开始$to")
        for (file in fromList) {
            `in` = FileInputStream(file)
            inChannel = `in`.channel
            // 从inChannel中读取file.length()长度的数据，写入outChannel的start处
            outChannel.transferFrom(inChannel, start, file.length())
            start += file.length()
            `in`.close()
            inChannel.close()
        }
        out.close()
        outChannel.close()
        l.d("合并子文件完成$to")
        finishCount++
    }
}