package com.muzrc.milltycoon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzrc.milltycoon.data.MillStorage
import com.muzrc.milltycoon.engine.MillController
import com.muzrc.milltycoon.ui.canvas.Factory3DViewport
import com.muzrc.milltycoon.ui.components.StatusHeader
import com.muzrc.milltycoon.ui.tabs.NaikTarafTab
import com.muzrc.milltycoon.ui.tabs.OperasiTab
import com.muzrc.milltycoon.ui.tabs.PasaranTab

@Composable
fun MillTycoonApp() {
    val context = LocalContext.current
    val storage = remember { MillStorage(context) }
    val controller = remember { MillController().also { storage.loadInto(it) } }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Auto-save apabila data penting berubah
    LaunchedEffect(
        controller.cash,
        controller.supplyLevel,
        controller.machineLevel,
        controller.storageLevel,
        controller.ecoLevel,
        controller.machineCondition
    ) {
        storage.saveFrom(controller)
    }

    // Gelung Simulasi 60 FPS
    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now
                controller.updateFrame(dt)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0E1318),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF161D24), contentColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Operasi") },
                    label = { Text("Operasi", fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Naik Taraf") },
                    label = { Text("Naik Taraf", fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Pasaran") },
                    label = { Text("Pasaran", fontSize = 12.sp) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            StatusHeader(controller)

            Factory3DViewport(
                controller = controller,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.52f)
                    .background(Color(0xFF161D24), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> OperasiTab(controller)
                    1 -> NaikTarafTab(controller)
                    2 -> PasaranTab(controller)
                }
            }
        }
    }
}
