package com.muzrc.milltycoon.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzrc.milltycoon.engine.MillController
import kotlin.math.sin

@Composable
fun Factory3DViewport(
    controller: MillController,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { controller.activateTurbo() }
                }
        ) {
            drawIsometricFactoryScene(
                clock = controller.animClock,
                inboundProgress = controller.inboundProgress,
                outboundProgress = controller.outboundProgress,
                isProcessing = controller.isProcessing,
                isTurbo = controller.turboTimeLeft > 0f,
                machineCondition = controller.machineCondition,
                tankRatio = (controller.finishedStock / controller.maxStorage).coerceIn(0f, 1f),
                rawRatio = (controller.rawStock / controller.maxStorage).coerceIn(0f, 1f)
            )
        }

        Surface(
            color = if (controller.turboTimeLeft > 0f) Color(0xFFFF6D00) else Color(0xCC1C252E),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = if (controller.turboTimeLeft > 0f) {
                    "⚡ TURBO AKTIF: ${"%.1f".format(controller.turboTimeLeft)}s"
                } else {
                    controller.statusBanner
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
    }
}

private fun DrawScope.drawIsometricFactoryScene(
    clock: Float,
    inboundProgress: Float,
    outboundProgress: Float,
    isProcessing: Boolean,
    isTurbo: Boolean,
    machineCondition: Float,
    tankRatio: Float,
    rawRatio: Float
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.54f
    val scale = (size.width / 420f).coerceIn(0.75f, 1.35f)

    fun iso(x: Float, y: Float, z: Float = 0f): Offset {
        val sx = cx + (x - y) * 0.866f * scale
        val sy = cy + (x + y) * 0.5f * scale - (z * scale)
        return Offset(sx, sy)
    }

    fun drawIsoBox(
        x: Float, y: Float, z: Float,
        dx: Float, dy: Float, dz: Float,
        topColor: Color, leftColor: Color, rightColor: Color
    ) {
        val p001 = iso(x, y, z + dz)
        val p101 = iso(x + dx, y, z + dz)
        val p111 = iso(x + dx, y + dy, z + dz)
        val p011 = iso(x, y + dy, z + dz)

        val p100 = iso(x + dx, y, z)
        val p110 = iso(x + dx, y + dy, z)
        val p010 = iso(x, y + dy, z)

        val topPath = Path().apply {
            moveTo(p001.x, p001.y); lineTo(p101.x, p101.y)
            lineTo(p111.x, p111.y); lineTo(p011.x, p011.y); close()
        }
        drawPath(topPath, topColor)

        val leftPath = Path().apply {
            moveTo(p011.x, p011.y); lineTo(p111.x, p111.y)
            lineTo(p110.x, p110.y); lineTo(p010.x, p010.y); close()
        }
        drawPath(leftPath, leftColor)

        val rightPath = Path().apply {
            moveTo(p101.x, p101.y); lineTo(p111.x, p111.y)
            lineTo(p110.x, p110.y); lineTo(p100.x, p100.y); close()
        }
        drawPath(rightPath, rightColor)
    }

    // 1. Tapak Pulau Kilang 3D
    drawIsoBox(-150f, -130f, -18f, 300f, 260f, 18f, Color(0xFF1F2A33), Color(0xFF131A20), Color(0xFF182128))
    for (g in -120..120 step 40) {
        drawLine(Color(0xFF283540), iso(g.toFloat(), -120f), iso(g.toFloat(), 120f), strokeWidth = 1.5f)
        drawLine(Color(0xFF283540), iso(-140f, g.toFloat()), iso(140f, g.toFloat()), strokeWidth = 1.5f)
    }

    // 2. Jalan Raya Masuk & Eksport
    drawIsoBox(-150f, -95f, 0f, 160f, 34f, 1.5f, Color(0xFF2F3B46), Color(0xFF222B33), Color(0xFF263038))
    drawIsoBox(-10f, 65f, 0f, 160f, 34f, 1.5f, Color(0xFF2F3B46), Color(0xFF222B33), Color(0xFF263038))
    for (dash in -130..0 step 30) {
        drawLine(Color(0xFF78909C), iso(dash.toFloat(), -78f, 2f), iso(dash + 14f, -78f, 2f), strokeWidth = 2f)
    }
    for (dash in 0..130 step 30) {
        drawLine(Color(0xFF78909C), iso(dash.toFloat(), 82f, 2f), iso(dash + 14f, 82f, 2f), strokeWidth = 2f)
    }

    // 3. Pondok Pengawal & Palang Pagar
    drawIsoBox(-115f, -125f, 0f, 24f, 22f, 24f, Color(0xFFCFD8DC), Color(0xFF90A4AE), Color(0xFF78909C))
    val gateLift = if (inboundProgress in 0.1f..0.45f) 24f else 0f
    drawLine(Color(0xFFFF5252), iso(-102f, -98f, 12f), iso(-102f, -64f, 12f + gateLift), strokeWidth = 5f * scale)

    // 4. Stor Bahan Mentah
    drawIsoBox(-95f, -35f, 0f, 45f, 55f, 12f, Color(0xFF37474F), Color(0xFF263238), Color(0xFF1C262B))
    if (rawRatio > 0.02f) {
        drawIsoBox(-88f, -28f, 12f, 31f, 41f, 28f * rawRatio, Color(0xFFFFB74D), Color(0xFFF57C00), Color(0xFFE65100))
    }

    // 5. Bangunan Utama Kilang & Bumbung Bergerigi
    val wallTop = if (isTurbo) Color(0xFFFFAB40) else Color(0xFF546E7A)
    val wallLeft = if (machineCondition < 50f) Color(0xFF6D3B3B) else Color(0xFF37474F)
    val wallRight = if (machineCondition < 50f) Color(0xFF522A2A) else Color(0xFF263238)
    drawIsoBox(-40f, -45f, 0f, 85f, 95f, 54f, wallTop, wallLeft, wallRight)

    for (r in 0..2) {
        val rx = -35f + (r * 26f)
        drawIsoBox(rx, -42f, 54f, 22f, 89f, 11f, Color(0xFF78909C), Color(0xFF455A64), Color(0xFF37474F))
    }

    val pulse = (sin(clock * (if (isTurbo) 14f else 6f)) + 1f) * 0.5f
    val beaconColor = when {
        !isProcessing -> Color.Gray
        isTurbo -> Color(0xFFFFD600)
        machineCondition < 50f -> Color(0xFFFF1744)
        else -> Color(0xFF00E676)
    }
    drawCircle(beaconColor.copy(alpha = 0.35f + pulse * 0.65f), radius = 9f * scale, center = iso(0f, 0f, 70f))

    // Cerobong & Asap Animasi
    listOf(Offset(25f, -30f), Offset(25f, -5f)).forEachIndexed { idx, pos ->
        drawIsoBox(pos.x, pos.y, 54f, 12f, 12f, 34f, Color(0xFFCFD8DC), Color(0xFF78909C), Color(0xFF546E7A))
        if (isProcessing) {
            for (s in 0..2) {
                val phase = ((clock * 0.9f) + (s * 0.33f) + (idx * 0.2f)) % 1f
                val smokeZ = 92f + (phase * 48f)
                val driftY = sin((clock + s) * 2f) * 6f
                drawCircle(
                    color = (if (isTurbo) Color(0xFFFFE082) else Color(0xFFECEFF1)).copy(alpha = (1f - phase) * 0.55f),
                    radius = (6f + phase * 11f) * scale,
                    center = iso(pos.x + 6f, pos.y + 6f + driftY, smokeZ)
                )
            }
        }
    }

    // 6. Tangki Silo Hasil Siap
    drawIsoBox(65f, -25f, 0f, 38f, 38f, 6f, Color(0xFF455A64), Color(0xFF263238), Color(0xFF1C262B))
    if (tankRatio > 0.01f) {
        drawIsoBox(68f, -22f, 6f, 32f, 32f, 59f * tankRatio, Color(0xFF4FC3F7), Color(0xFF0288D1), Color(0xFF01579B))
    }
    drawCircle(Color(0xFF81D4FA), radius = 18f * scale, center = iso(84f, -6f, 65f), style = Stroke(width = 2f))

    // 7. Lori 3D Bergerak
    fun drawIsoLorry(lx: Float, ly: Float, cargoColor: Color) {
        drawIsoBox(lx - 2f, ly - 2f, 1.5f, 34f, 18f, 1f, Color(0x66000000), Color.Transparent, Color.Transparent)
        drawIsoBox(lx, ly, 3f, 22f, 15f, 14f, cargoColor, cargoColor.copy(alpha = 0.8f), cargoColor.copy(alpha = 0.6f))
        drawIsoBox(lx + 23f, ly + 1f, 3f, 9f, 13f, 11f, Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF90A4AE))
    }

    drawIsoLorry(-145f + (inboundProgress * 115f), -85f, Color(0xFFFF7043))
    if (outboundProgress > 0f) {
        drawIsoLorry(-5f + (outboundProgress * 130f), 74f, Color(0xFF26A69A))
    }
}
