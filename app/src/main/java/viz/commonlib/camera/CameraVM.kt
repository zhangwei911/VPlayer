package viz.commonlib.camera

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraVM : ViewModel() {
    val frame = MutableLiveData<Bitmap>()
}