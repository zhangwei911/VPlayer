package viz.vplayer.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView


class RecyclerItemClickListener(
    context: Context,
    recyclerView: RecyclerView,
    private val mListener: OnItemClickListener?
) : RecyclerView.OnItemTouchListener {

    private val mGestureDetector: GestureDetector
    private var isFling = false

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int, e: MotionEvent)

        fun onItemLongClick(view: View, position: Int, e: MotionEvent)

        fun onItemDoubleClick(view: View, position: Int, e: MotionEvent)
    }

    init {

        mGestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (isFling) {
                        isFling = false
                        return false
                    }
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent?,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    isFling = true
                    return super.onFling(e1, e2, velocityX, velocityY)
                }

                override fun onLongPress(e: MotionEvent) {
                    val childView = recyclerView.findChildViewUnder(e.x, e.y)

                    if (childView != null && mListener != null) {
                        mListener.onItemLongClick(
                            childView,
                            recyclerView.getChildAdapterPosition(childView),
                            e
                        )
                    }
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val childView = recyclerView.findChildViewUnder(e.x, e.y)
                    if (childView != null && mListener != null) {
                        mListener.onItemDoubleClick(
                            childView,
                            recyclerView.getChildAdapterPosition(childView),
                            e
                        )
                    }
                    return super.onDoubleTap(e)
                }
            })
    }

    override fun onInterceptTouchEvent(view: RecyclerView, e: MotionEvent): Boolean {
        val childView = view.findChildViewUnder(e.x, e.y)

        if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
            mListener.onItemClick(childView, view.getChildAdapterPosition(childView), e)
        }

        return false
    }

    override fun onTouchEvent(
        view: androidx.recyclerview.widget.RecyclerView,
        motionEvent: MotionEvent
    ) {
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}