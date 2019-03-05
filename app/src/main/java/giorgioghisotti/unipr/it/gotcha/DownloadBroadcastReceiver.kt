package giorgioghisotti.unipr.it.gotcha

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action: String? = intent?.action
        val sharedPreferences = context?.getSharedPreferences("sp", Context.MODE_PRIVATE)
        val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        val lastItem = sharedPreferences?.getString("last_item", "")

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            if(sharedPreferences?.getLong(lastItem, -1) == id){
                val myIntent = Intent(context, MainMenu::class.java)
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context?.startActivity(myIntent)
            }
        }
    }
}