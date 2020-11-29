package jp.gr.java_conf.osndev.mybatterywidget

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.View
import android.widget.RemoteViews

/**
 * MyBatteryWidget
 */
class MyBatteryWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val latency: Int = 0
        private var scheduler: JobScheduler? = null
        private val hookIntents = arrayOf(Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED)
    }
    private val tag = MyBatteryWidgetProvider::class.simpleName ?: ""

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(tag, "onUpdate")
        with(BatteryData(context.applicationContext)) {
            this.setBatteryData()
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, this)
            }
        }
        setup(context)
    }

    override fun onEnabled(context: Context) {
        Log.d(tag, "onEnabled")
        super.onEnabled(context)
        setup(context)
    }

    override fun onDisabled(context: Context) {
        Log.d(tag, "onDisabled")
        try {
            context.applicationContext.unregisterReceiver(this)
        } catch (ex: Exception) {
            Log.e(tag, ex.message ?: "")
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        Log.d(tag, "onRestored")
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        setup(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "onReceive")
        super.onReceive(context, intent)
        setup(context)

        if (!hookIntents.contains(intent.action)) {
            return
        }

        val application = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(application)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(application, MyBatteryWidgetProvider::class.java))

        with(BatteryData(context.applicationContext)) {
            this.setBatteryData(intent.action == Intent.ACTION_POWER_CONNECTED)
            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId, this)
            }
        }
    }

    /**
     * ウィジェットのセットアップ(ブロードキャストレシーバーの登録およびJobSchedularの開始)を行う
     */
    private fun setup(context: Context) {
        try {
            // staticなフラグは当てにならない気がするので無条件でレシーバーを一旦登録解除する
            context.applicationContext.unregisterReceiver(this)
        } catch (ex: Exception) {
            Log.e(tag, ex.message ?: "")
        }

        val filter = IntentFilter()
        for (intent in hookIntents) {
            filter.addAction(intent)
        }
        try {
            context.applicationContext.registerReceiver(this, filter)
        } catch (ex: Exception) {
            Log.e(tag, ex.message ?: "")
        }

        if (null == scheduler) {
            scheduler = context.applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            scheduler?.schedule(BatteryJobService.BatteryJobServiceFactory(context.applicationContext, 0, latency.toLong()))
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, data: BatteryData) {
    val views = RemoteViews(context.packageName, R.layout.my_battery_widget_provider)
    views.setViewVisibility(R.id.charge, if (data.isCharging) View.VISIBLE else View.INVISIBLE)
    views.setTextViewText(R.id.battery, data.levelString)
    views.setProgressBar(R.id.battery_bar, 100, data.level, false)
    appWidgetManager.updateAppWidget(appWidgetId, views)
}