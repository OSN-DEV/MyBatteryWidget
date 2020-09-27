package jp.gr.java_conf.osndev.mybatterywidget

import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat.getSystemService
import java.lang.Exception

/**
 * Implementation of App Widget functionality.
 */
class MyBatteryWidgetProvider : AppWidgetProvider() {
    companion object {
        var isReceiverRegistered: Boolean = false
//        var latencies: IntArray = intArrayOf(0,5,10)
        var latencies: IntArray = intArrayOf(0)
        var scheduler: JobScheduler? = null
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("#####", "##### MyBatteryWidgetProvider.onUpdate")
        registerReceiver(context)
        // There may be multiple widgets active, so update all of them
        with (BatteryData(context.applicationContext)) {
            this.setBatteryData()
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, this)
            }
        }
    }

    override fun onEnabled(context: Context) {
        Log.d("#####", "##### MyBatteryWidgetProvider.onEnabled")
        registerReceiver(context)

        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, MyBatteryWidgetProvider::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP)
    }

    override fun onDisabled(context: Context) {
        Log.d("#####", "##### MyBatteryWidgetProvider.onDisabled")

        if (isReceiverRegistered) {
            try {
                context.applicationContext.unregisterReceiver(this)
            } catch (ex: Exception) {
            }
            isReceiverRegistered = false
        }
        // Enter relevant functionality for when the last widget is disabled
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, MyBatteryWidgetProvider::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        Log.d("#####", "##### MyBatteryWidgetProvider.onRestored")
        registerReceiver(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("#####", "##### MyBatteryWidgetProvider.onReceive")
        Log.d("#####", "" + intent.action)
        super.onReceive(context, intent)
        registerReceiver(context)

        when(intent.action) {
            Intent.ACTION_POWER_CONNECTED -> { }
            Intent.ACTION_POWER_DISCONNECTED -> { }
            else -> {
                return
            }
        }

        val application = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(application)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(application, MyBatteryWidgetProvider::class.java)
        )

        with (BatteryData(context.applicationContext)) {
            this.setBatteryData(intent.action == Intent.ACTION_POWER_CONNECTED)
            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId, this)
            }
        }
    }


    private fun registerReceiver(context: Context) {
        if (!isReceiverRegistered) {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_POWER_CONNECTED)
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            context.applicationContext.registerReceiver(this, filter)
            isReceiverRegistered = true
        }

        if (null == scheduler) {
            Log.d("#####", "start schedular")
            scheduler = context.applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            for (i in 0..latencies.size - 1) {
                scheduler?.schedule(BatteryJobService.BatteryJobServiceFactory(context.applicationContext, i, latencies[i].toLong()))
                latencies[i] = 0
            }
        }
    }

}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, data: BatteryData) {
    Log.d("#####", "##### MyBatteryWidgetProvider.updateAppWidget")

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.my_battery_widget_provider)
    views.setViewVisibility(R.id.charge, if (data.isCharging) View.VISIBLE else View.INVISIBLE)
    views.setTextViewText(R.id.battery, data.levelString)
    views.setProgressBar(R.id.battery_bar, 100, data.level, false)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}