package com.muzrc.milltycoon.ui.tabs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.muzrc.milltycoon.engine.MillController
import com.muzrc.milltycoon.ui.components.ActionCard

@Composable
fun NaikTarafTab(controller: MillController) {
    Text("NAIK TARAF INFRASTRUKTUR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90A4AE))

    ActionCard(
        title = "Armada Lori (Lv.${controller.supplyLevel})",
        subtitle = "+8t muatan lori & penghantaran lebih laju",
        buttonText = "RM ${"%,.0f".format(controller.supplyCost)}",
        enabled = controller.cash >= controller.supplyCost,
        onClick = { controller.upgradeSupply() }
    )

    ActionCard(
        title = "Barisan Mesin Utama (Lv.${controller.machineLevel})",
        subtitle = "+1.15t/s kelajuan proses bahan mentah",
        buttonText = "RM ${"%,.0f".format(controller.machineCost)}",
        enabled = controller.cash >= controller.machineCost,
        onClick = { controller.upgradeMachine() }
    )

    ActionCard(
        title = "Silo & Tangki Simpanan (Lv.${controller.storageLevel})",
        subtitle = "+45t kapasiti bahan mentah & hasil siap",
        buttonText = "RM ${"%,.0f".format(controller.storageCost)}",
        enabled = controller.cash >= controller.storageCost,
        onClick = { controller.upgradeStorage() }
    )

    ActionCard(
        title = "Sistem Penjimat Tenaga (Lv.${controller.ecoLevel})",
        subtitle = "Kurangkan bil elektrik & diesel sebanyak 12%",
        buttonText = "RM ${"%,.0f".format(controller.ecoCost)}",
        enabled = controller.cash >= controller.ecoCost,
        onClick = { controller.upgradeEco() }
    )
}
