package giorgioghisotti.unipr.it.gotcha

import org.opencv.core.Mat
import org.opencv.dnn.Net
import java.io.Serializable

class Detection : Serializable {
    var rect : Mat = Mat(0,0,0)
    var label : String = ""
    var confidence : Double = 0.0
    var frame : Mat = Mat(0,0,0)
    var subFrame : Mat = Mat(0,0,0)
}

class SerializableNet : Serializable {
    var net : Net? = null
}