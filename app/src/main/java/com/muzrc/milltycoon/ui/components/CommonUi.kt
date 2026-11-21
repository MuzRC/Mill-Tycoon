package com.muzrc.milltycoon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzrc.milltycoon.engine.MillController

@Composable
fun StatusHeader(controller: MillController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161D24))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("MODAL & TUNAI", fontSize = 10.sp, color = Color(0xFF90A4AE))
            Text(
                text = "RM ${"%,.0f".format(controller.cash)}",
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (controller.cash < 50) Color(0xFFFF5252) else Color(0xFF00E676)
            )
            Text(
                text = "Harga Jual: RM ${controller.marketPrice.toInt()}/t • Kos: RM 6/t",
                fontSize = 11.sp,
                color = Color(0xFF4DD0E1)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "MENTAH: ${controller.rawStock.toInt()}/${controller.maxStorage.toInt()}t",
                fontSize = 11.sp,
                color = Color(0xFFFFB74D)
            )
            Text(
                text = "SIAP: ${controller.finishedStock.toInt()}/${controller.maxStorage.toInt()}t",
                fontSize = 11.sp,
                color = Color(0xFF4FC3F7)
            )
            Text(
                text = "MESIN: ${controller.machineCondition.toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (controller.machineCondition < 50f) Color(0xFFFF5252) else Color(0xFFA5D6A7)
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    buttonText: String,
    enabled: Boolean,
    accentColor: Color = Color(0xFF00C853),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F2933), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFFB0BEC5))
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = Color(0xFF2E3B47)
            )
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
        }
    }
}
