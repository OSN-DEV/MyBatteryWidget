package jp.gr.java_conf.osndev.mybatterywidget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryData(private val context: Context) {
    /**
     * 充電フラグ true: 充電中、false: それ以外
     */
    var isCharging: Boolean = false
        private set

    /**
     * 満充電フラグ true: バッテリー100%、false: それ以外
     */
    private var isFull: Boolean = false

    /**
     * バッテリーのレベル
     */
    var level: Int = 0
        private set

    /**
     * バッテリーのレベル
     */
    var levelString: String = ""
        private set


    /**
     * バッテリー情報を設定する
     */
    fun setBatteryData(isCharging: Boolean? = null) {
        // https://developer.android.com/training/monitoring-device-state/battery-monitoring?hl=ja
        val data: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        if (null == data) {
            this.isCharging = false
            this.isFull = false
            this.level = 0
            this.levelString = context.getString(R.string.level_unknown)
        } else {
            val status: Int = data.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            this.isCharging = isCharging ?: (BatteryManager.BATTERY_STATUS_CHARGING == status)
            this.isFull = (BatteryManager.BATTERY_STATUS_FULL == status)

            val level: Int = data.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = data.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = level * 100 / scale.toFloat()
            this.level = percent.toInt()
            this.levelString = this.level.toString() + "%"

//            Log.d("#####", "status: $status")
//            Log.d("#####", "BATTERY_STATUS_CHARGING: " + BatteryManager.BATTERY_STATUS_CHARGING)
        }
//        Log.d("#####", if(this.isCharging)  "充電中" else "バッテリー駆動")
//        Log.d("#####", if(this.isFull)  "満タン" else "満タンじゃない")
//        Log.d("#####", this.level.toString() + "%")
    }
}