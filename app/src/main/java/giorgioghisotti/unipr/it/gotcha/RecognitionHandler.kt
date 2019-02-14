package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.support.v4.content.LocalBroadcastManager

private const val REC_JOB_ID = 1000

const val BROADCAST_ACTION = "com.example.android.threadsample.BROADCAST"
const val DETECTIONS = "OpenCV Detections"

class RecognitionHandler : JobIntentService() {

    private var workingLock : Boolean = false

    override fun onHandleWork (intent: Intent) {
        this.workingLock = true //Ignore new intents while one intent is being processed

        val dataString = intent.dataString
        Thread.sleep(10_000)
        System.out.println(dataString)
        /*
         * Creates a new Intent containing a Uri object
         * BROADCAST_ACTION is a custom Intent action
         */
        val localIntent = Intent(BROADCAST_ACTION).apply {
            // Puts the status into the Intent
            putExtra(DETECTIONS, dataString)
        }
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)

        this.workingLock = false
    }

    fun enqueueWork(context: Context, work: Intent) {
        if (this.workingLock) enqueueWork(context, RecognitionHandler::class.java, REC_JOB_ID, work)
    }

}