package jp.gr.java_conf.osndev.mybatterywidget

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class BatteryJobService : JobService() {
    companion object {
        fun BatteryJobServiceFactory(context: Context, jobId: Int, latency: Long = 0): JobInfo {
            val componentName = ComponentName(context, BatteryJobService::class.java)
            return JobInfo.Builder(jobId, componentName).apply {
                    setPersisted(true)
                    setMinimumLatency(latency * (1000 * 60))
                    setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                    setPeriodic(15 * 60 * 1000)
                    //                    setBackoffCriteria(10000, JobInfo.BACKOFF_POLICY_LINEAR)
                    //                    setPeriodic(0)
                    //                    setRequiresCharging(false)
                }.build()
        }
    }
    private val tag = MyBatteryWidgetProvider::class.simpleName ?: ""

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(tag, "onStopJob")
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(tag, "onStartJob")
        Thread(Runnable {
            val intent = Intent(this, MyBatteryWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(application).getAppWidgetIds(ComponentName(application, MyBatteryWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(intent)
        }).start()
        return false
    }
}