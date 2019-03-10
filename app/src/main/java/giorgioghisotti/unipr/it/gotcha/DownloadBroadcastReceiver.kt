package giorgioghisotti.unipr.it.gotcha

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action: String? = intent?.action
        val sharedPreferences = context?.getSharedPreferences("sp", Context.MODE_PRIVATE) ?: return
        val count = sharedPreferences.getInt("download_count", 0) - 1

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            if(count == 0){
                val myIntent = Intent(context, MainMenu::class.java)
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(myIntent)
            } else {
                sharedPreferences.edit().putInt("download_count", count).apply()
            }
        }
    }
}