package com.somphie.staff.feature.home.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somphie.staff.core.network.StaffProfile

//Display three: Principal panel del turno de work
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: StaffProfile,
    onNavigateToOrders: () -> Unit,
    onNavigateToTables: () -> Unit,
    onNavigateToKitchen: () -> Unit,
    onNavigateToBar: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    //Estado local para seleccionar la pestaña activa
    val selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Somphie Zúrich",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    //Chip a la recha con la estacion activa actual
                    AssistChip(
                        onClick = {},
                        label = {Text("Cocina", fontSize = 12.sp)},
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF61715B),
                            labelColor = Color(0xFFF6F1E8)
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        //Barra de navegacion inferior con 4 pestañas
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                
            }
        }
    ) { }
}