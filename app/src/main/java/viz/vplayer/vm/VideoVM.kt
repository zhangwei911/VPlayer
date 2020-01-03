package viz.vplayer.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import viz.vplayer.bean.VideoInfoBean

class VideoVM : ViewModel() {
    val play = MutableLiveData<VideoInfoBean>()
}
