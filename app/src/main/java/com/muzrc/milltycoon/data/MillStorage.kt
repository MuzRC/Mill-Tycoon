package com.muzrc.milltycoon.data

import android.content.Context
import com.muzrc.milltycoon.engine.MillController

class MillStorage(context: Context) {
    private val prefs = context.getSharedPreferences("mill_tycoon_3d_v1", Context.MODE_PRIVATE)

    fun loadInto(controller: MillController) {
        controller.cash = prefs.getFloat("cash", 400f).toDouble()
        controller.rawStock = prefs.getFloat("raw", 20f)
        controller.finishedStock = prefs.getFloat("finished", 0f)
        controller.machineCondition = prefs.getFloat("condition", 100f)
        controller.totalProfit = prefs.getFloat("total_profit", 0f).toDouble()

        controller.supplyLevel = prefs.getInt("lvl_supply", 1)
        controller.machineLevel = prefs.getInt("lvl_machine", 1)
        controller.storageLevel = prefs.getInt("lvl_storage", 1)
        controller.ecoLevel = prefs.getInt("lvl_eco", 1)
    }

    fun saveFrom(controller: MillController) {
        prefs.edit()
            .putFloat("cash", controller.cash.toFloat())
            .putFloat("raw", controller.rawStock)
            .putFloat("finished", controller.finishedStock)
            .putFloat("condition", controller.machineCondition)
            .putFloat("total_profit", controller.totalProfit.toFloat())
            .putInt("lvl_supply", controller.supplyLevel)
            .putInt("lvl_machine", controller.machineLevel)
            .putInt("lvl_storage", controller.storageLevel)
            .putInt("lvl_eco", controller.ecoLevel)
            .apply()
    }
}
