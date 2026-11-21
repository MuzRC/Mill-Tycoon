package com.muzrc.milltycoon

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.pow
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MillTycoonScreen()
            }
        }
    }
}

@Composable
fun rememberVectorBitmap(resId: Int, widthPx: Int, heightPx: Int): ImageBitmap {
    val context = LocalContext.current
    return remember(resId, widthPx, heightPx) {
        val drawable = ContextCompat.getDrawable(context, resId)!!
        drawable.toBitmap(width = widthPx, height = heightPx).asImageBitmap()
    }
}

@Composable
fun MillTycoonScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mill_tycoon_v2", Context.MODE_PRIVATE) }

    // State permainan
    var cash by remember { mutableDoubleStateOf(prefs.getFloat("cash", 350f).toDouble()) }
    var rawStock by remember { mutableFloatStateOf(prefs.getFloat("raw", 15f)) }
    var finishedStock by remember { mutableFloatStateOf(prefs.getFloat("finished", 0f)) }
    var machineCondition by remember { mutableFloatStateOf(prefs.getFloat("condition", 100f)) }

    // Tahap Upgrade
    var supplyLevel by remember { mutableIntStateOf(prefs.getInt("lvl_supply", 1)) }
    var machineLevel by remember { mutableIntStateOf(prefs.getInt("lvl_machine", 1)) }
    var storageLevel by remember { mutableIntStateOf(prefs.getInt("lvl_storage", 1)) }

    // Pasaran & Animasi
    var marketPrice by remember { mutableDoubleStateOf(13.0) }
    var marketTimer by remember { mutableFloatStateOf(0f) }
    var inboundLorryX by remember { mutableFloatStateOf(0f) }
    var outboundLorryX by remember { mutableFloatStateOf(0f) }
    var machinePulse by remember { mutableFloatStateOf(0f) }
    var statusBanner by remember { mutableStateOf("Operasi kilang berjalan normal") }

    // Formula Ekonomi & Kos Eksponen
    val rawCostPerUnit = 6.0
    val maxStorage = storageLevel * 40f
    val supplyCost = 220.0 * 1.65.pow(supplyLevel - 1)
    val machineCost = 300.0 * 1.70.pow(machineLevel - 1)
    val storageCost = 250.0 * 1.60.pow(storageLevel - 1)
    val serviceCost = (100f - machineCondition) * 1.8 * machineLevel

    val lorryBitmap = rememberVectorBitmap(R.drawable.ic_lori, 140, 70)

    // Auto-save
    LaunchedEffect(cash, supplyLevel, machineLevel, storageLevel, machineCondition) {
        prefs.edit()
            .putFloat("cash", cash.toFloat())
            .putFloat("raw", rawStock)
            .putFloat("finished", finishedStock)
            .putFloat("condition", machineCondition)
            .putInt("lvl_supply", supplyLevel)
            .putInt("lvl_machine", machineLevel)
            .putInt("lvl_storage", storageLevel)
            .apply()
    }

    // Game Loop 60 FPS
    LaunchedEffect(supplyLevel, machineLevel, storageLevel) {
        var lastTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                // Turun naik harga pasaran setiap 15 saat
                marketTimer += dt
                if (marketTimer >= 15f) {
                    marketTimer = 0f
                    marketPrice = Random.nextInt(11, 17).toDouble()
                }

                // 1. Lori Bahan Mentah (Perlu bayar pembekal RM6/unit)
                val spaceLeft = maxStorage - rawStock
                if (spaceLeft >= 2f && cash >= rawCostPerUnit) {
                    val inboundSpeed = 0.18f + (supplyLevel * 0.04f)
                    inboundLorryX += inboundSpeed * dt
                    if (inboundLorryX >= 1f) {
                        inboundLorryX = 0f
                        val maxOrder = 8f * supplyLevel
                        val affordableUnits = (cash / rawCostPerUnit).toFloat()
                        val boughtUnits = minOf(maxOrder, spaceLeft, affordableUnits)
                        if (boughtUnits >= 1f) {
                            cash -= boughtUnits * rawCostPerUnit
                            rawStock += boughtUnits
                            statusBanner = "Beli ${boughtUnits.toInt()}t mentah (-RM ${(boughtUnits * rawCostPerUnit).toInt()})"
                        }
                    }
                } else if (cash < rawCostPerUnit && rawStock < 1f) {
                    statusBanner = "Tunai tidak cukup untuk beli bahan mentah!"
                }

                // 2. Proses Mesin + Kos Operasi + Haus Mesin
                val efficiency = (machineCondition / 100f).coerceIn(0.25f, 1f)
                val baseProcessRate = (2.2f + (machineLevel * 1.1f)) * efficiency
                if (rawStock > 0f && finishedStock < maxStorage) {
                    val actualProcessed = minOf(rawStock, baseProcessRate * dt, maxStorage - finishedStock)
                    rawStock -= actualProcessed
                    finishedStock += actualProcessed
                    machinePulse = (machinePulse + dt * (6f * efficiency)) % 1f

                    // Kos elektrik/diesel semasa mesin berjalan
                    val operatingCost = (2.8 * machineLevel) * dt
                    cash = (cash - operatingCost).coerceAtLeast(0.0)

                    // Condition mesin menurun perlahan
                    machineCondition = (machineCondition - (1.4f * dt)).coerceAtLeast(20f)
                }

                // 3. Lori Eksport Jual Hasil Siap
                if (finishedStock >= 6f || outboundLorryX > 0f) {
                    val outboundSpeed = 0.22f + (supplyLevel * 0.03f)
                    outboundLorryX += outboundSpeed * dt
                    if (outboundLorryX >= 1f) {
                        outboundLorryX = 0f
                        val soldAmount = minOf(finishedStock, 10f * supplyLevel)
                        if (soldAmount > 0f) {
                            finishedStock -= soldAmount
                            val revenue = soldAmount * marketPrice
                            cash += revenue
                            statusBanner = "Jual ${soldAmount.toInt()}t hasil (+RM ${revenue.toInt()})"
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121619))
    ) {
        // BAR STATUS ATAS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2429))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TUNAI KILANG", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = "RM ${"%,.0f".format(cash)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (cash < 50) Color(0xFFFF5252) else Color(0xFF00E676)
                )
                Text(
                    text = "Pasaran: RM ${marketPrice.toInt()}/t (Kos: RM 6/t)",
                    fontSize = 11.sp,
                    color = Color(0xFF80CBC4)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("MENTAH: ${rawStock.toInt()}/${maxStorage.toInt()}t", fontSize = 12.sp, color = Color(0xFFFFB74D))
                Text("HASIL SIAP: ${finishedStock.toInt()}/${maxStorage.toInt()}t", fontSize = 12.sp, color = Color(0xFF4FC3F7))
                Text(
                    text = "MESIN: ${machineCondition.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (machineCondition < 50f) Color(0xFFFF5252) else Color(0xFFA5D6A7)
                )
            }
        }

        // LOG AKTIVITI RINGKAS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B1F))
                .padding(vertical = 6.dp, horizontal = 16.dp)
        ) {
            Text(statusBanner, fontSize = 11.sp, color = Color.LightGray)
        }

        // KANVAS SIMULASI KILANG 2D
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height

            drawRect(Color(0xFF1B2226))

            val roadInY = h * 0.22f
            val roadOutY = h * 0.78f
            drawRect(Color(0xFF2C353D), topLeft = Offset(0f, roadInY - 40f), size = Size(w, 80f))
            drawRect(Color(0xFF2C353D), topLeft = Offset(0f, roadOutY - 40f), size = Size(w, 80f))

            // Pondok Pengawal & Palang Pagar
            drawRoundRect(
                color = Color(0xFF455A64),
                topLeft = Offset(30f, roadInY - 75f),
                size = Size(60f, 30f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRect(Color(0xFFFF5252), topLeft = Offset(85f, roadInY - 40f), size = Size(8f, 35f))

            // Bangunan Mesin Utama
            val plantWidth = w * 0.45f
            val plantHeight = h * 0.28f
            val plantLeft = (w - plantWidth) / 2f
            val plantTop = (h - plantHeight) / 2f

            drawRoundRect(
                color = Color(0xFF37474F),
                topLeft = Offset(plantLeft, plantTop),
                size = Size(plantWidth, plantHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Lampu status mesin (Hijau = sihat, Oren/Merah = perlu servis)
            val statusColor = when {
                rawStock <= 0f -> Color.DarkGray
                machineCondition < 50f -> Color(0xFFFF5252)
                else -> Color(0xFF00E676)
            }
            drawCircle(
                color = statusColor.copy(alpha = 0.4f + machinePulse * 0.6f),
                radius = 28f,
                center = Offset(w / 2f, h / 2f)
            )

            // Tangki Simpanan
            val tankFillRatio = (finishedStock / maxStorage).coerceIn(0f, 1f)
            drawRoundRect(
                color = Color(0xFF263238),
                topLeft = Offset(plantLeft + plantWidth + 24f, plantTop + 20f),
                size = Size(50f, plantHeight - 40f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            val fillHeight = (plantHeight - 40f) * tankFillRatio
            drawRoundRect(
                color = Color(0xFF29B6F6),
                topLeft = Offset(plantLeft + plantWidth + 24f, plantTop + 20f + (plantHeight - 40f - fillHeight)),
                size = Size(50f, fillHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Lori Bahan Mentah
            val inX = inboundLorryX * (w * 0.65f)
            drawImage(lorryBitmap, topLeft = Offset(inX, roadInY - 35f))

            // Lori Eksport
            if (outboundLorryX > 0f) {
                val outX = (w * 0.35f) + outboundLorryX * (w * 0.65f)
                drawImage(lorryBitmap, topLeft = Offset(outX, roadOutY - 35f))
            }
        }

        // PANEL KAWALAN & UPGRADE BAWAH
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2429), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Butang Servis Mesin
            UpgradeRow(
                title = "Penyelenggaraan Mesin (${machineCondition.toInt()}%)",
                subtitle = if (machineCondition < 50f) "AMARAN: Kecekapan rendah!" else "Pulihkan prestasi mesin ke 100%",
                cost = serviceCost,
                canAfford = machineCondition < 95f && cash >= serviceCost,
                buttonColor = Color(0xFFFF9100),
                onUpgrade = {
                    cash -= serviceCost
                    machineCondition = 100f
                    statusBanner = "Mesin selesai diservis ke 100%"
                }
            )
            UpgradeRow(
                title = "Armada Lori (Lv.$supplyLevel)",
                subtitle = "+Muatan & Kelajuan Penghantaran",
                cost = supplyCost,
                canAfford = cash >= supplyCost,
                onUpgrade = {
                    cash -= supplyCost
                    supplyLevel++
                }
            )
            UpgradeRow(
                title = "Mesin Pemprosesan (Lv.$machineLevel)",
                subtitle = "+Kelajuan Proses (+Kos Utiliti)",
                cost = machineCost,
                canAfford = cash >= machineCost,
                onUpgrade = {
                    cash -= machineCost
                    machineLevel++
                }
            )
            UpgradeRow(
                title = "Kapasiti Tangki & Gudang (Lv.$storageLevel)",
                subtitle = "+40t Had Simpanan Maksimum",
                cost = storageCost,
                canAfford = cash >= storageCost,
                onUpgrade = {
                    cash -= storageCost
                    storageLevel++
                }
            )
        }
    }
}

@Composable
fun UpgradeRow(
    title: String,
    subtitle: String,
    cost: Double,
    canAfford: Boolean,
    buttonColor: Color = Color(0xFF00C853),
    onUpgrade: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF283037), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color.LightGray)
        }
        Button(
            onClick = onUpgrade,
            enabled = canAfford,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                disabledContainerColor = Color(0xFF37474F)
            )
        ) {
            Text("RM ${"%,.0f".format(cost)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
