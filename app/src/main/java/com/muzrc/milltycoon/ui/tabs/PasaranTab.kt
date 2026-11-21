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
fun PasaranTab(controller: MillController) {
    Text("PASARAN & JUALAN PUKAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A4AE))

    val bulkBonusPrice = controller.marketPrice * 1.10
    val bulkValue = controller.finishedStock * bulkBonusPrice

    ActionCard(
        title = "Kontrak Eksport Pukal (+10% Bonus)",
        subtitle = "Jual semua ${controller.finishedStock.toInt()}t stok siap pada RM ${"%.1f".format(bulkBonusPrice)}/t",
        buttonText = "RM ${"%,.0f".format(bulkValue)}",
        enabled = controller.finishedStock >= 5f,
        accentColor = Color(0xFF00E676),
        onClick = { controller.executeBulkExport() }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F2933), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Margin Untung Bersih", fontSize = 11.sp, color = Color.Gray)
            val margin = controller.marketPrice - controller.rawCostPerUnit
            Text(
                text = "+RM ${margin.toInt()} / tan",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF00E676)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Jumlah Hasil Terkumpul", fontSize = 11.sp, color = Color.Gray)
            Text(
                text = "RM ${"%,.0f".format(controller.totalProfit)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFFFD54F)
            )
        }
    }

    Text(
        text = "Tip Strategi: Harga pasaran berubah antara RM 11 hingga RM 17 setiap 15 saat. Tunggu harga tinggi sebelum menekan Kontrak Eksport Pukal.",
        fontSize = 11.sp,
        color = Color(0xFF90A4AE),
        lineHeight = 16.sp
    )
}
