package com.somphie.staff.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somphie.staff.R
import com.somphie.staff.core.design.SomphieTheme
import com.somphie.staff.core.design.StaffColors
import com.somphie.staff.core.network.StaffProfile

// Pantalla de Perfil de Usuario y Ajustes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: StaffProfile,
    activeStationName: String = "Cocina",
    onChangeStation: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
    showTopBar: Boolean = false
) {
    var selectedStationName by remember(activeStationName) { mutableStateOf(activeStationName) }

    Scaffold(
        // Barra superior opcional (solo si showTopBar es true)
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.profile_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    // Icono de engranaje/configuracion a la izquierda
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar circular con las iniciales del usuario
            val initials = getInitials(profile.displayName)
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(StaffColors.BadgeBrown, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Nombre y Rol del usuario
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nombre completo del usuario
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = StaffColors.Coffee
                )

                // Rol del usuario (ej. Cocinero, Mozo, Dueño)
                val roleName = getRoleDisplayName(profile.roles)
                Text(
                    text = roleName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card contenedora con la lista de opciones (Cambiar estacion, Ayuda, Cerrar sesion)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Opcion 1: Cambiar estacion
                    ProfileOptionRow(
                        icon = Icons.Default.Sync,
                        title = stringResource(R.string.change_station_label),
                        valueText = "$selectedStationName >",
                        onClick = {
                            selectedStationName = if (selectedStationName == "Cocina") "Barra" else "Cocina"
                            onChangeStation()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Opcion 2: Ayuda
                    ProfileOptionRow(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(R.string.help_label),
                        onClick = onHelpClick
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Opcion 3: Cerrar sesion
                    ProfileOptionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(R.string.logout_action),
                        titleColor = StaffColors.Coffee,
                        onClick = onSignOut
                    )
                }
            }
        }
    }
}

// Fila individual de opción del perfil
@Composable
private fun ProfileOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueText: String? = null,
    titleColor: Color = StaffColors.Coffee,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la opción
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = StaffColors.Coffee,
                modifier = Modifier.size(22.dp)
            )
            // Titulo de la opción
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                fontSize = 16.sp
            )
        }

        // Valor a la derecha o flecha chevron
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (valueText != null) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Obtener las iniciales a partir del nombre completo (ej: "Lucas Gómez" -> "LC")
private fun getInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.isNotEmpty() && parts[0].length >= 2 -> parts[0].substring(0, 2).uppercase()
        parts.isNotEmpty() -> parts[0].first().toString().uppercase()
        else -> "US"
    }
}

// Formatear el nombre del rol a mostrar (ej: "KITCHEN" -> "Cocinero")
private fun getRoleDisplayName(roles: List<String>): String {
    return when {
        roles.contains("OWNER") -> "Dueño"
        roles.contains("WAITER") -> "Mozo"
        roles.contains("KITCHEN") -> "Cocinero"
        roles.contains("BAR") -> "Barista"
        else -> "Personal"
    }
}

// Preview para Android Studio (Split / Design tab)
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    SomphieTheme {
        ProfileScreen(
            profile = StaffProfile(
                id = "demo-1",
                email = "lucas@somphie.example",
                displayName = "Ezequiel Cano",
                roles = listOf("KITCHEN"),
                permissions = emptyList(),
                stations = emptyList(),
                mustChangePassword = false
            ),
            activeStationName = "Cocina",
            showTopBar = true
        )
    }
}
