package giorgioghisotti.unipr.it.gotcha

import org.opencv.core.Mat
import java.io.Serializable

class Detection : Serializable {
    var detected : Mat = Mat(0,0,0)
    var frame : Mat = Mat(0,0,0)
    var subFrame : Mat = Mat(0,0,0)

    fun release() {
        this.detected.release()
        this.frame.release()
        this.subFrame.release()
    }
}
