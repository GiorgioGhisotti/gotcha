package giorgioghisotti.unipr.it.gotcha

import org.opencv.core.Mat

class Detection() {
    var rect : Mat = Mat(0,0,0)
    var label : String = ""
    var confidence : Double = 0.0
    var frame : Mat = Mat(0,0,0)
}