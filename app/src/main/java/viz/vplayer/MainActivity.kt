package viz.vplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import bolts.Task
import com.lidroid.xutils.HttpUtils
import com.lidroid.xutils.exception.HttpException
import com.lidroid.xutils.http.ResponseInfo
import com.lidroid.xutils.http.callback.RequestCallBack
import com.lidroid.xutils.http.client.HttpRequest
import com.viz.tools.l
import org.jsoup.Jsoup
import viz.vplayer.bean.VideoInfoBean
import viz.vplayer.vm.MainVM
import viz.vplayer.vm.VideoVM
import java.io.Serializable
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val mainVM: MainVM by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(MainVM::class.java)
    }
    private val SERIAL_EXECUTOR: Executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        mainVM.play.observe(this, Observer { videoInfoBean ->
            val intent = Intent(this, VideoPalyerActivity::class.java)
            intent.putExtra("url", videoInfoBean.url)
            intent.putExtra("title", videoInfoBean.title)
            intent.putExtra("duration", videoInfoBean.duration)
            intent.putExtra("episodes", mainVM.episodes.value as Serializable)
            startActivity(intent)
        })
        mainVM.search.observe(this, Observer { searchList ->
            l.i(searchList)
//            mainVM.getVideoEpisodesInfo(searchList[0].url)
        })
        mainVM.episodes.observe(this, Observer { episodeList ->
            l.i(episodeList)
            mainVM.getVideoInfo(episodeList[0])
        })
        mainVM.searchVideos("庆余年", "http://www.lexianglive.com/index.php?s=vod-search-name")
    }
}
