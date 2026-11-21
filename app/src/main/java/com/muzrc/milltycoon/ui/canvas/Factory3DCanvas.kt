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
            drawFullMillIsometricScene(
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
            color = if (controller.turboTimeLeft > 0f) Color(0xFFFF6D00) else Color(0xD9182026),
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

private fun DrawScope.drawFullMillIsometricScene(
    clock: Float,
    inboundProgress: Float,
    outboundProgress: Float,
    isProcessing: Boolean,
    isTurbo: Boolean,
    machineCondition: Float,
    tankRatio: Float,
    rawRatio: Float
) {
    val cx = size.width * 0.50f
    val cy = size.height * 0.52f
    // Skala automatik supaya keseluruhan kawasan kilang memenuhi skrin
    val scale = minOf(size.width / 340f, size.height / 245f).coerceIn(0.85f, 1.65f)

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

    // =========================================================================
    // 1. TAPAK KAWASAN KILANG & PAGAR PERIMETER
    // =========================================================================
    // Tanah luar jalan masuk hadapan
    drawIsoBox(-175f, 130f, -14f, 110f, 45f, 14f, Color(0xFF1A242B), Color(0xFF10161B), Color(0xFF141C22))

    // Tapak utama dalam pagar kilang (-165..165, -145..130)
    drawIsoBox(
        x = -165f, y = -145f, z = -14f,
        dx = 330f, dy = 275f, dz = 14f,
        topColor = Color(0xFF212C35),
        leftColor = Color(0xFF12181E),
        rightColor = Color(0xFF172027)
    )

    // Pagar Perimeter Keliling (kecuali bukaan pintu pagar utama di x = -140..-90, y = 130)
    val fenceColor = Color(0xFF607D8B)
    // Pagar belakang & kanan
    drawLine(fenceColor, iso(-163f, -143f, 8f), iso(163f, -143f, 8f), strokeWidth = 2f * scale)
    drawLine(fenceColor, iso(163f, -143f, 8f), iso(163f, 128f, 8f), strokeWidth = 2f * scale)
    // Pagar kiri
    drawLine(fenceColor, iso(-163f, -143f, 8f), iso(-163f, 128f, 8f), strokeWidth = 2f * scale)
    // Pagar hadapan (kanan pintu pagar utama)
    drawLine(fenceColor, iso(-85f, 128f, 8f), iso(163f, 128f, 8f), strokeWidth = 2f * scale)

    // =========================================================================
    // 2. LALUAN JALAN RAYA DALAMAN (DARI GATE -> TIMBANG -> RAMP -> TANGKI)
    // =========================================================================
    val roadTop = Color(0xFF323E48)
    val roadSide = Color(0xFF232C34)

    // Jalan masuk utama dari luar gate ke dalam
    drawIsoBox(-142f, 65f, 0f, 52f, 108f, 1.2f, roadTop, roadSide, roadSide)
    // Jalan utama melintang hadapan (ke kawasan pejabat & laluan keluar)
    drawIsoBox(-142f, 65f, 0f, 265f, 34f, 1.2f, roadTop, roadSide, roadSide)
    // Cabang jalan naik ke Loading Ramp (Bahan Mentah)
    drawIsoBox(-142f, -35f, 0f, 36f, 105f, 1.2f, roadTop, roadSide, roadSide)
    // Cabang jalan ke kawasan Tangki Simpanan (Eksport)
    drawIsoBox(88f, -45f, 0f, 35f, 115f, 1.2f, roadTop, roadSide, roadSide)

    // Garisan kuning tengah jalan masuk
    drawLine(Color(0xFFFFCA28), iso(-116f, 85f, 1.5f), iso(-116f, 168f, 1.5f), strokeWidth = 1.8f * scale)

    // =========================================================================
    // 3. PINTU PAGAR UTAMA, PONDOK PENGAWAL & JAMBATAN TIMBANG (HADAPAN)
    // =========================================================================
    // Pondok Pengawal Utama (Kiri pintu pagar)
    drawIsoBox(-162f, 102f, 0f, 18f, 24f, 22f, Color(0xFFECEFF1), Color(0xFF90A4AE), Color(0xFF78909C))
    // Bumbung pondok pengawal
    drawIsoBox(-164f, 100f, 22f, 22f, 28f, 4f, Color(0xFF0288D1), Color(0xFF01579B), Color(0xFF0277BD))

    // Tiang & Palang Pagar Automatik (Terangkat bila lori di pintu pagar)
    drawIsoBox(-145f, 124f, 0f, 4f, 4f, 14f, Color(0xFFFF5252), Color(0xFFD32F2F), Color(0xFFB71C1C))
    val gateOpen = (inboundProgress < 0.28f) || (outboundProgress > 0.72f)
    val barrierLift = if (gateOpen) 26f else 0f
    drawLine(
        color = Color(0xFFFF5252),
        start = iso(-143f, 126f, 11f),
        end = iso(-92f, 126f, 11f + barrierLift),
        strokeWidth = 4f * scale
    )

    // Jambatan Timbang (Weighbridge) selepas Gate
    drawIsoBox(-136f, 70f, 1.2f, 40f, 26f, 2.5f, Color(0xFF546E7A), Color(0xFF37474F), Color(0xFF263238))
    // Plat keluli timbang
    drawIsoBox(-133f, 73f, 3.7f, 34f, 20f, 1f, Color(0xFF90A4AE), Color(0xFF607D8B), Color(0xFF455A64))
    // Pondok Kerani Timbang kecil
    drawIsoBox(-90f, 102f, 0f, 16f, 16f, 18f, Color(0xFFCFD8DC), Color(0xFF90A4AE), Color(0xFF78909C))

    // =========================================================================
    // 4. PEJABAT PENTADBIRAN & MAKMAL (BAHAGIAN HADAPAN TENGAH)
    // =========================================================================
    drawIsoBox(-45f, 104f, 0f, 95f, 20f, 22f, Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFF546E7A))
    drawIsoBox(-47f, 102f, 22f, 99f, 24f, 4f, Color(0xFF37474F), Color(0xFF263238), Color(0xFF1C262B))

    // =========================================================================
    // 5. LOADING RAMP / HOPPER BAHAN MENTAH & CONVEYOR KE KILANG
    // =========================================================================
    // Platform tinggi Loading Ramp
    drawIsoBox(-145f, -95f, 0f, 45f, 58f, 16f, Color(0xFF455A64), Color(0xFF263238), Color(0xFF1C262B))
    // Longgokan bahan mentah di atas ramp
    if (rawRatio > 0.02f) {
        drawIsoBox(-138f, -88f, 16f, 31f, 44f, 22f * rawRatio, Color(0xFFFFB74D), Color(0xFFF57C00), Color(0xFFE65100))
    }
    // Conveyor bertutup menyambungkan Ramp terus ke Bangunan Kilang Utama
    drawIsoBox(-100f, -72f, 18f, 45f, 12f, 10f, Color(0xFF78909C), Color(0xFF455A64), Color(0xFF37474F))

    // =========================================================================
    // 6. BANGUNAN KILANG UTAMA & BOILER HOUSE (TENGAH BELAKANG)
    // =========================================================================
    val wallTop = if (isTurbo) Color(0xFFFFB74D) else Color(0xFF546E7A)
    val wallLeft = if (machineCondition < 50f) Color(0xFF6D3B3B) else Color(0xFF37474F)
    val wallRight = if (machineCondition < 50f) Color(0xFF522A2A) else Color(0xFF263238)

    // Blok Kilang Utama
    drawIsoBox(-55f, -120f, 0f, 105f, 125f, 52f, wallTop, wallLeft, wallRight)

    // Bumbung bergerigi 3D (Sawtooth Mill Roof)
    for (r in 0..3) {
        val rx = -50f + (r * 25f)
        drawIsoBox(rx, -116f, 52f, 20f, 117f, 10f, Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF37474F))
    }

    // Lampu Status Operasi di Bumbung
    val pulse = (sin(clock * (if (isTurbo) 14f else 6f)) + 1f) * 0.5f
    val beaconColor = when {
        !isProcessing -> Color.Gray
        isTurbo -> Color(0xFFFFD600)
        machineCondition < 50f -> Color(0xFFFF1744)
        else -> Color(0xFF00E676)
    }
    drawCircle(beaconColor.copy(alpha = 0.35f + pulse * 0.65f), radius = 8f * scale, center = iso(-5f, -55f, 68f))

    // 2 Cerobong Asap Tinggi + Kepulan Asap
    listOf(Offset(32f, -102f), Offset(32f, -72f)).forEachIndexed { idx, pos ->
        drawIsoBox(pos.x, pos.y, 52f, 11f, 11f, 36f, Color(0xFFECEFF1), Color(0xFF90A4AE), Color(0xFF607D8B))
        if (isProcessing) {
            for (s in 0..2) {
                val phase = ((clock * 0.9f) + (s * 0.33f) + (idx * 0.2f)) % 1f
                val smokeZ = 92f + (phase * 45f)
                val driftY = sin((clock + s) * 2f) * 5f
                drawCircle(
                    color = (if (isTurbo) Color(0xFFFFE082) else Color(0xFFECEFF1)).copy(alpha = (1f - phase) * 0.55f),
                    radius = (5f + phase * 10f) * scale,
                    center = iso(pos.x + 5f, pos.y + 5f + driftY, smokeZ)
                )
            }
        }
    }

    // =========================================================================
    // 7. PAIP PENGHANTARAN & TANGKI SIMPANAN BERKEMBAR (KANAN BELAKANG)
    // =========================================================================
    // Paip atas dari kilang ke tangki simpanan
    drawIsoBox(50f, -95f, 36f, 45f, 8f, 7f, Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFF546E7A))

    // Tangki Simpanan 1 & 2
    val tankPositions = listOf(Offset(75f, -125f), Offset(116f, -125f))
    tankPositions.forEach { tPos ->
        // Tapak konkrit tangki
        drawIsoBox(tPos.x, tPos.y, 0f, 34f, 48f, 6f, Color(0xFF455A64), Color(0xFF263238), Color(0xFF1C262B))
        // Dinding luar tangki separa lutsinar
        drawIsoBox(tPos.x + 2f, tPos.y + 2f, 6f, 30f, 44f, 52f, Color(0x3381D4FA), Color(0x264FC3F7), Color(0x1F0288D1))
        // Paras hasil siap di dalam tangki
        if (tankRatio > 0.01f) {
            drawIsoBox(
                tPos.x + 3f, tPos.y + 3f, 6f,
                28f, 42f, 50f * tankRatio,
                Color(0xFF4FC3F7), Color(0xFF0288D1), Color(0xFF01579B)
            )
        }
    }
    // Jambatan Gantry Pemeriksaan di atas kedua-dua tangki
    drawIsoBox(73f, -104f, 58f, 79f, 8f, 3f, Color(0xFFFFCA28), Color(0xFFF57F17), Color(0xFFE65100))

    // =========================================================================
    // 8. PERGERAKAN LORI REALISTIK (GATE -> TIMBANG -> RAMP / TANGKI -> GATE)
    // =========================================================================
    fun drawIsoLorryVertical(lx: Float, ly: Float, cargoColor: Color, facingNorth: Boolean) {
        // Bayang-bayang
        drawIsoBox(lx - 1f, ly - 1f, 1.3f, 16f, 30f, 1f, Color(0x66000000), Color.Transparent, Color.Transparent)
        if (facingNorth) {
            // Kepala lori di hadapan (arah atas/-y), kargo di belakang (+y)
            drawIsoBox(lx + 1f, ly, 2.5f, 12f, 9f, 10f, Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF90A4AE))
            drawIsoBox(lx, ly + 10f, 2.5f, 14f, 19f, 13f, cargoColor, cargoColor.copy(alpha = 0.85f), cargoColor.copy(alpha = 0.65f))
        } else {
            // Kepala lori menghadap keluar (arah bawah/+y)
            drawIsoBox(lx, ly, 2.5f, 14f, 19f, 13f, cargoColor, cargoColor.copy(alpha = 0.85f), cargoColor.copy(alpha = 0.65f))
            drawIsoBox(lx + 1f, ly + 20f, 2.5f, 12f, 9f, 10f, Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF90A4AE))
        }
    }

    fun drawIsoLorryHorizontal(lx: Float, ly: Float, cargoColor: Color, facingWest: Boolean) {
        drawIsoBox(lx - 1f, ly - 1f, 1.3f, 30f, 16f, 1f, Color(0x66000000), Color.Transparent, Color.Transparent)
        if (facingWest) {
            drawIsoBox(lx, ly + 1f, 2.5f, 9f, 12f, 10f, Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF90A4AE))
            drawIsoBox(lx + 10f, ly, 2.5f, 19f, 14f, 13f, cargoColor, cargoColor.copy(alpha = 0.85f), cargoColor.copy(alpha = 0.65f))
        } else {
            drawIsoBox(lx, ly, 2.5f, 19f, 14f, 13f, cargoColor, cargoColor.copy(alpha = 0.85f), cargoColor.copy(alpha = 0.65f))
            drawIsoBox(lx + 20f, ly + 1f, 2.5f, 9f, 12f, 10f, Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF90A4AE))
        }
    }

    // A. Lori Bahan Mentah: Masuk dari Gate (y=155) -> Jambatan Timbang (y=75) -> Loading Ramp (y=-35)
    val inLorryY = 155f - (inboundProgress * 190f)
    drawIsoLorryVertical(lx = -132f, ly = inLorryY, cargoColor = Color(0xFFFF7043), facingNorth = true)

    // B. Lori Eksport: Dari bawah Tangki Simpanan (y=-40) -> Turun ke Jalan Hadapan (y=72) -> Belok Kiri ke Timbang & Keluar Gate
    if (outboundProgress > 0f) {
        when {
            outboundProgress < 0.40f -> {
                // Fasa 1: Turun dari kawasan tangki simpanan ke jalan utama hadapan
                val p = outboundProgress / 0.40f
                val ly = -40f + (p * 112f)
                drawIsoLorryVertical(lx = 98f, ly = ly, cargoColor = Color(0xFF26A69A), facingNorth = false)
            }
            outboundProgress < 0.80f -> {
                // Fasa 2: Bergerak melintang ke arah Jambatan Timbang & Gate
                val p = (outboundProgress - 0.40f) / 0.40f
                val lx = 95f - (p * 205f)
                drawIsoLorryHorizontal(lx = lx, ly = 72f, cargoColor = Color(0xFF26A69A), facingWest = true)
            }
            else -> {
                // Fasa 3: Keluar melepasi Pintu Pagar Utama
                val p = (outboundProgress - 0.80f) / 0.20f
                val ly = 75f + (p * 85f)
                drawIsoLorryVertical(lx = -112f, ly = ly, cargoColor = Color(0xFF26A69A), facingNorth = false)
            }
        }
    }
}
