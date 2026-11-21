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
    val prefs = remember { context.getSharedPreferences("mill_tycoon_save", Context.MODE_PRIVATE) }

    var cash by remember { mutableDoubleStateOf(prefs.getFloat("cash", 500f).toDouble()) }
    var rawStock by remember { mutableFloatStateOf(prefs.getFloat("raw", 20f)) }
    var finishedStock by remember { mutableFloatStateOf(prefs.getFloat("finished", 0f)) }

    var supplyLevel by remember { mutableIntStateOf(prefs.getInt("lvl_supply", 1)) }
    var machineLevel by remember { mutableIntStateOf(prefs.getInt("lvl_machine", 1)) }
    var storageLevel by remember { mutableIntStateOf(prefs.getInt("lvl_storage", 1)) }

    var inboundLorryX by remember { mutableFloatStateOf(0f) }
    var outboundLorryX by remember { mutableFloatStateOf(0f) }
    var machinePulse by remember { mutableFloatStateOf(0f) }

    val maxStorage = storageLevel * 50f
    val supplyCost = supplyLevel * 150.0
    val machineCost = machineLevel * 200.0
    val storageCost = storageLevel * 180.0

    val lorryBitmap = rememberVectorBitmap(R.drawable.ic_lori, 140, 70)

    LaunchedEffect(cash, supplyLevel, machineLevel, storageLevel) {
        prefs.edit()
            .putFloat("cash", cash.toFloat())
            .putFloat("raw", rawStock)
            .putFloat("finished", finishedStock)
            .putInt("lvl_supply", supplyLevel)
            .putInt("lvl_machine", machineLevel)
            .putInt("lvl_storage", storageLevel)
            .apply()
    }

    LaunchedEffect(supplyLevel, machineLevel, storageLevel) {
        var lastTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                val inboundSpeed = 0.25f + (supplyLevel * 0.08f)
                inboundLorryX += inboundSpeed * dt
                if (inboundLorryX >= 1f) {
                    inboundLorryX = 0f
                    val delivered = 10f * supplyLevel
                    rawStock = (rawStock + delivered).coerceAtMost(maxStorage)
                }

                val processRate = 4f * machineLevel * dt
                if (rawStock > 0f && finishedStock < maxStorage) {
                    val actualProcessed = minOf(rawStock, processRate, maxStorage - finishedStock)
                    rawStock -= actualProcessed
                    finishedStock += actualProcessed
                    machinePulse = (machinePulse + dt * 8f) % 1f
                }

                if (finishedStock >= 5f || outboundLorryX > 0f) {
                    val outboundSpeed = 0.30f + (supplyLevel * 0.05f)
                    outboundLorryX += outboundSpeed * dt
                    if (outboundLorryX >= 1f) {
                        outboundLorryX = 0f
                        val soldAmount = minOf(finishedStock, 15f * supplyLevel)
                        finishedStock -= soldAmount
                        cash += soldAmount * 12.0
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2429))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TUNAI KILANG", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = "RM ${"%,.0f".format(cash)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("BAHAN MENTAH: ${rawStock.toInt()}/${maxStorage.toInt()}", fontSize = 12.sp, color = Color(0xFFFFB74D))
                Text("HASIL SIAP: ${finishedStock.toInt()}/${maxStorage.toInt()}", fontSize = 12.sp, color = Color(0xFF4FC3F7))
            }
        }

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

            drawRoundRect(
                color = Color(0xFF455A64),
                topLeft = Offset(30f, roadInY - 75f),
                size = Size(60f, 30f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRect(Color(0xFFFF5252), topLeft = Offset(85f, roadInY - 40f), size = Size(8f, 35f))

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

            val activeColor = if (rawStock > 0f) Color(0xFF00E676).copy(alpha = 0.4f + machinePulse * 0.6f) else Color.DarkGray
            drawCircle(
                color = activeColor,
                radius = 28f,
                center = Offset(w / 2f, h / 2f)
            )

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

            val inX = inboundLorryX * (w * 0.65f)
            drawImage(lorryBitmap, topLeft = Offset(inX, roadInY - 35f))

            if (outboundLorryX > 0f) {
                val outX = (w * 0.35f) + outboundLorryX * (w * 0.65f)
                drawImage(lorryBitmap, topLeft = Offset(outX, roadOutY - 35f))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2429), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                subtitle = "+Kelajuan Proses Bahan Mentah",
                cost = machineCost,
                canAfford = cash >= machineCost,
                onUpgrade = {
                    cash -= machineCost
                    machineLevel++
                }
            )
            UpgradeRow(
                title = "Kapasiti Tangki & Gudang (Lv.$storageLevel)",
                subtitle = "+50 Had Simpanan Maksimum",
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
    onUpgrade: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF283037), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Text(subtitle, fontSize = 12.sp, color = Color.LightGray)
        }
        Button(
            onClick = onUpgrade,
            enabled = canAfford,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00C853),
                disabledContainerColor = Color(0xFF37474F)
            )
        ) {
            Text("RM ${"%,.0f".format(cost)}", fontWeight = FontWeight.Bold)
        }
    }
}
