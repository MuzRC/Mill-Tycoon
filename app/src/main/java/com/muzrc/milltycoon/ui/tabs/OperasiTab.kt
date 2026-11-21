package com.muzrc.milltycoon.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzrc.milltycoon.engine.MillController
import com.muzrc.milltycoon.ui.components.ActionCard

@Composable
fun OperasiTab(controller: MillController) {
    Text("KAWALAN OPERASI KILANG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A4AE))

    ActionCard(
        title = "Servis & Penyelenggaraan Mesin (${controller.machineCondition.toInt()}%)",
        subtitle = if (controller.machineCondition < 50f) "KRITIKAL: Mesin perlahan! Servis segera." else "Pulihkan prestasi & elak kerosakan",
        buttonText = if (controller.machineCondition >= 98f) "OPTIMUM" else "RM ${"%,.0f".format(controller.serviceCost)}",
        enabled = controller.machineCondition < 98f && controller.cash >= controller.serviceCost,
        accentColor = Color(0xFFFF9100),
        onClick = { controller.serviceMachine() }
    )

    ActionCard(
        title = "Penggalak Operasi (Turbo 2x)",
        subtitle = "Gandakan kelajuan pemprosesan selama 5 saat",
        buttonText = if (controller.turboTimeLeft > 0f) "AKTIF" else "GUNA (PERCUMA)",
        enabled = controller.turboTimeLeft <= 0f,
        accentColor = Color(0xFF29B6F6),
        onClick = { controller.activateTurbo() }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F2933), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Kos Utiliti Semasa", fontSize = 11.sp, color = Color.Gray)
            Text(
                text = "RM ${"%.1f".format(controller.currentUtilityCost)} / saat",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFFFF8A80)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Kapasiti Pemprosesan", fontSize = 11.sp, color = Color.Gray)
            Text(
                text = "${"%.1f".format(controller.currentProcessRate)} tan / saat",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF69F0AE)
            )
        }
    }
}
