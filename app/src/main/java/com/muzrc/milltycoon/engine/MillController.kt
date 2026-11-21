package com.muzrc.milltycoon.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.pow
import kotlin.random.Random

class MillController {
    // State Kewangan & Inventori
    var cash by mutableDoubleStateOf(400.0)
    var rawStock by mutableFloatStateOf(20f)
    var finishedStock by mutableFloatStateOf(0f)
    var machineCondition by mutableFloatStateOf(100f)
    var totalProfit by mutableDoubleStateOf(0.0)

    // Tahap Infrastruktur
    var supplyLevel by mutableIntStateOf(1)
    var machineLevel by mutableIntStateOf(1)
    var storageLevel by mutableIntStateOf(1)
    var ecoLevel by mutableIntStateOf(1)

    // Pasaran, Boost & Status
    var marketPrice by mutableDoubleStateOf(14.0)
    private var marketTimer = 0f
    var turboTimeLeft by mutableFloatStateOf(0f)
    var statusBanner by mutableStateOf("Ketuk bangunan kilang 3D untuk Turbo Boost!")

    // Pembolehubah Animasi
    var inboundProgress by mutableFloatStateOf(0f)
    var outboundProgress by mutableFloatStateOf(0f)
    var animClock by mutableFloatStateOf(0f)

    // Formula Pengiraan Dinamik
    val rawCostPerUnit = 6.0
    val maxStorage: Float get() = storageLevel * 45f
    val supplyCost: Double get() = 220.0 * 1.65.pow(supplyLevel - 1)
    val machineCost: Double get() = 300.0 * 1.70.pow(machineLevel - 1)
    val storageCost: Double get() = 250.0 * 1.60.pow(storageLevel - 1)
    val ecoCost: Double get() = 280.0 * 1.65.pow(ecoLevel - 1)
    val serviceCost: Double get() = (100f - machineCondition) * 1.7 * machineLevel
    val powerDiscount: Double get() = (1.0 - (ecoLevel - 1) * 0.12).coerceAtLeast(0.35)
    val currentProcessRate: Float get() = (2.4f + machineLevel * 1.15f) * (machineCondition / 100f).coerceIn(0.25f, 1f)
    val currentUtilityCost: Double get() = 2.6 * machineLevel * powerDiscount
    val isProcessing: Boolean get() = rawStock > 0f && finishedStock < maxStorage

    fun updateFrame(dt: Float) {
        animClock += dt

        if (turboTimeLeft > 0f) {
            turboTimeLeft = (turboTimeLeft - dt).coerceAtLeast(0f)
        }

        // Turun naik harga pasaran setiap 15 saat
        marketTimer += dt
        if (marketTimer >= 15f) {
            marketTimer = 0f
            marketPrice = Random.nextInt(11, 18).toDouble()
        }

        // 1. Lori Bahan Mentah Masuk
        val spaceLeft = maxStorage - rawStock
        if (spaceLeft >= 2f && cash >= rawCostPerUnit) {
            val speed = 0.20f + (supplyLevel * 0.04f)
            inboundProgress += speed * dt
            if (inboundProgress >= 1f) {
                inboundProgress = 0f
                val order = 8f * supplyLevel
                val affordable = (cash / rawCostPerUnit).toFloat()
                val bought = minOf(order, spaceLeft, affordable)
                if (bought >= 1f) {
                    val cost = bought * rawCostPerUnit
                    cash -= cost
                    rawStock += bought
                    statusBanner = "Stok mentah tiba: +${bought.toInt()}t (-RM ${cost.toInt()})"
                }
            }
        }

        // 2. Pemprosesan Kilang & Kos Utiliti
        val turboMult = if (turboTimeLeft > 0f) 2.0f else 1.0f
        if (isProcessing) {
            val processed = minOf(rawStock, (currentProcessRate * turboMult) * dt, maxStorage - finishedStock)
            rawStock -= processed
            finishedStock += processed

            cash = (cash - currentUtilityCost * dt).coerceAtLeast(0.0)
            machineCondition = (machineCondition - (1.25f * dt)).coerceAtLeast(20f)
        }

        // 3. Lori Eksport Keluar
        if (finishedStock >= 6f || outboundProgress > 0f) {
            val outSpeed = 0.24f + (supplyLevel * 0.03f)
            outboundProgress += outSpeed * dt
            if (outboundProgress >= 1f) {
                outboundProgress = 0f
                val sold = minOf(finishedStock, 10f * supplyLevel)
                if (sold > 0f) {
                    finishedStock -= sold
                    val rev = sold * marketPrice
                    cash += rev
                    totalProfit += rev
                    statusBanner = "Eksport selesai: ${sold.toInt()}t (+RM ${rev.toInt()})"
                }
            }
        }
    }

    // Tindakan Pemain
    fun activateTurbo() {
        if (turboTimeLeft <= 0f) {
            turboTimeLeft = 5f
            statusBanner = "TURBO BOOST AKTIF (2x Kelajuan selama 5s)!"
        }
    }

    fun serviceMachine() {
        if (machineCondition < 98f && cash >= serviceCost) {
            cash -= serviceCost
            machineCondition = 100f
            statusBanner = "Mesin selesai diservis ke 100%!"
        }
    }

    fun upgradeSupply() {
        if (cash >= supplyCost) { cash -= supplyCost; supplyLevel++ }
    }

    fun upgradeMachine() {
        if (cash >= machineCost) { cash -= machineCost; machineLevel++ }
    }

    fun upgradeStorage() {
        if (cash >= storageCost) { cash -= storageCost; storageLevel++ }
    }

    fun upgradeEco() {
        if (cash >= ecoCost) { cash -= ecoCost; ecoLevel++ }
    }

    fun executeBulkExport() {
        if (finishedStock >= 5f) {
            val bulkValue = finishedStock * (marketPrice * 1.10)
            cash += bulkValue
            totalProfit += bulkValue
            statusBanner = "Kontrak pukal terjual: +RM ${bulkValue.toInt()}!"
            finishedStock = 0f
        }
    }
}
