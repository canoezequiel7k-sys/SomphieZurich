package com.somphie.staff.feature.billing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somphie.staff.core.design.SomphieTheme
import com.somphie.staff.core.design.StaffColors
import com.somphie.staff.core.network.AdjustmentReasonCode
import com.somphie.staff.core.network.Bill
import com.somphie.staff.core.network.StaffOrder
import com.somphie.staff.core.network.StaffOrderItem
import com.somphie.staff.core.network.StaffSession
import com.somphie.staff.core.network.StaffSessionDetail

// Pantalla de Cobro en Caja y Resumen de Cuenta de la Mesa
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    sessionId: String,
    viewModel: BillingViewModel,
    permissions: Set<String> = emptySet(),
    onNavigateBack: () -> Unit = {},
    onSessionClosed: () -> Unit = {},
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()

    // Cargar la cuenta al iniciar la pantalla
    LaunchedEffect(sessionId) {
        viewModel.loadBill(sessionId)
    }

    BillingContent(
        uiState = uiState,
        permissions = permissions,
        onNavigateBack = onNavigateBack,
        onOpenPayDialog = { viewModel.openPayDialog() },
        onClosePayDialog = { viewModel.closePayDialog() },
        onOpenAdjustmentDialog = { viewModel.openAdjustmentDialog() },
        onCloseAdjustmentDialog = { viewModel.closeAdjustmentDialog() },
        onOpenForceCloseDialog = { viewModel.openForceCloseDialog() },
        onCloseForceCloseDialog = { viewModel.closeForceCloseDialog() },
        onConfirmPay = { amountMinor, tipMinor ->
            viewModel.processPayment(sessionId, amountMinor, tipMinor)
        },
        onConfirmDiscountPercent = { percent, reasonCode ->
            viewModel.applyDiscountPercent(sessionId, percent, reasonCode = reasonCode)
        },
        onCloseSession = {
            viewModel.closeSession(sessionId = sessionId, onClosed = onSessionClosed)
        },
        onForceCloseSession = { reason ->
            viewModel.forceCloseSession(sessionId = sessionId, reason = reason, onClosed = onSessionClosed)
        },
        showTopBar = showTopBar
    )
}

// Contenido principal desacoplado para vista previa en Android Studio (@Preview)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingContent(
    uiState: BillingUiState,
    permissions: Set<String> = emptySet(),
    onNavigateBack: () -> Unit = {},
    onOpenPayDialog: () -> Unit = {},
    onClosePayDialog: () -> Unit = {},
    onOpenAdjustmentDialog: () -> Unit = {},
    onCloseAdjustmentDialog: () -> Unit = {},
    onOpenForceCloseDialog: () -> Unit = {},
    onCloseForceCloseDialog: () -> Unit = {},
    onConfirmPay: (amountMinor: Long, tipMinor: Long) -> Unit = { _, _ -> },
    onConfirmDiscountPercent: (percent: Int, reasonCode: String) -> Unit = { _, _ -> },
    onCloseSession: () -> Unit = {},
    onForceCloseSession: (reason: String) -> Unit = {},
    showTopBar: Boolean = true
) {
    // Visibilidad gobernada por permisos de /me (no por rol hardcodeado)
    val canPay = permissions.isEmpty() || "payment.register_cash" in permissions
    val canAdjust = permissions.isEmpty() || ("adjustment.apply" in permissions || "adjustment.apply_any" in permissions)
    val canClose = permissions.isEmpty() || "session.close" in permissions
    val canForceClose = permissions.isEmpty() || "session.force_close" in permissions

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Cobro en Caja",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = StaffColors.Coffee
                )
            } else if (uiState.sessionDetail != null) {
                val detail = uiState.sessionDetail
                val bill = detail.bill

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Mensaje de éxito
                    if (uiState.paymentSuccessMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StaffColors.Sage),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.paymentSuccessMessage,
                                color = Color.White,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Mensaje de error (Problem+JSON o red)
                    if (uiState.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Encabezado con número de mesa
                    Text(
                        text = detail.session.tableLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = StaffColors.Coffee
                    )

                    // Tarjeta con el Resumen Financiero de la Cuenta
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Resumen de Cuenta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = StaffColors.Coffee
                            )

                            HorizontalDivider()

                            BillSummaryRow("Cargos totales:", formatMoney(bill.chargesMinor, bill.currency))

                            if (bill.adjustmentsMinor != 0L) {
                                BillSummaryRow("Descuentos / Ajustes:", formatMoney(bill.adjustmentsMinor, bill.currency))
                            }

                            BillSummaryRow("Pagado:", formatMoney(bill.paidMinor, bill.currency))

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saldo Pendiente:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = StaffColors.Coffee
                                )
                                Text(
                                    text = formatMoney(bill.balanceMinor, bill.currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = if (bill.balanceMinor > 0) StaffColors.Coffee else StaffColors.Sage
                                )
                            }
                        }
                    }

                    // Detalle de Consumos
                    Text(
                        text = "Detalle de Consumos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StaffColors.Coffee
                    )

                    val allItems = detail.orders.flatMap { it.items }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allItems) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.quantity} x ${item.productName}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = formatMoney(item.totalMinor, bill.currency),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Botones de acción gobernados por permisos
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Botón 1: Cobrar en Caja
                        if (canPay) {
                            Button(
                                onClick = onOpenPayDialog,
                                enabled = bill.balanceMinor > 0 && !uiState.isProcessingPayment,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StaffColors.Coffee,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cobrar (${formatMoney(bill.balanceMinor, bill.currency)})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Botón 2: Aplicar Descuento / Cortesía
                        if (canAdjust) {
                            OutlinedButton(
                                onClick = onOpenAdjustmentDialog,
                                enabled = bill.balanceMinor > 0 && !uiState.isProcessingPayment,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.Percent, contentDescription = null, tint = StaffColors.Caramel)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Aplicar Descuento / Cortesía",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = StaffColors.Coffee
                                )
                            }
                        }

                        // Botón 3: Cerrar Mesa normal
                        if (canClose) {
                            OutlinedButton(
                                onClick = onCloseSession,
                                enabled = bill.balanceMinor == 0L && !uiState.isProcessingPayment,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StaffColors.Sage)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cerrar Mesa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Botón 4: Cierre Forzado con motivo
                        if (canForceClose && bill.balanceMinor > 0L) {
                            TextButton(
                                onClick = onOpenForceCloseDialog,
                                enabled = !uiState.isProcessingPayment,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cierre Forzado con Motivo",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Diálogo emergente para registrar pago
            if (uiState.showPayDialog && uiState.sessionDetail != null) {
                PayDialog(
                    balanceMinor = uiState.sessionDetail.bill.balanceMinor,
                    currency = uiState.sessionDetail.bill.currency,
                    isProcessing = uiState.isProcessingPayment,
                    onDismiss = onClosePayDialog,
                    onConfirm = onConfirmPay
                )
            }

            // Diálogo emergente para aplicar descuento / cortesía
            if (uiState.showAdjustmentDialog && uiState.sessionDetail != null) {
                AdjustmentDialog(
                    isProcessing = uiState.isProcessingPayment,
                    onDismiss = onCloseAdjustmentDialog,
                    onConfirmPercent = onConfirmDiscountPercent
                )
            }

            // Diálogo emergente para cierre forzado
            if (uiState.showForceCloseDialog && uiState.sessionDetail != null) {
                ForceCloseDialog(
                    isProcessing = uiState.isProcessingPayment,
                    onDismiss = onCloseForceCloseDialog,
                    onConfirm = onForceCloseSession
                )
            }
        }
    }
}

// Fila del resumen financiero
@Composable
private fun BillSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// Diálogo para aplicar Descuento / Cortesía con enum contractual y botones protegidos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustmentDialog(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirmPercent: (percent: Int, reasonCode: String) -> Unit
) {
    var percentValue by remember { mutableIntStateOf(10) }
    var selectedReason by remember { mutableStateOf(AdjustmentReasonCode.SERVICE_ISSUE) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Text("Aplicar Descuento / Cortesía", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Seleccione el porcentaje de descuento:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { p ->
                        OutlinedButton(
                            onClick = { percentValue = p },
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (percentValue == p) StaffColors.Caramel else Color.Transparent,
                                contentColor = if (percentValue == p) Color.White else StaffColors.Coffee
                            )
                        ) {
                            Text("$p%")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Motivo del ajuste (según contrato):",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { if (!isProcessing) dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedReason.label,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isProcessing,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        AdjustmentReasonCode.entries.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason.label) },
                                onClick = {
                                    selectedReason = reason
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPercent(percentValue, selectedReason.code) },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = StaffColors.Coffee)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(18.dp).width(18.dp))
                } else {
                    Text("Aplicar $percentValue%", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Cancelar")
            }
        }
    )
}

// Diálogo de Pago en Efectivo con botones deshabilitados al procesar
@Composable
private fun PayDialog(
    balanceMinor: Long,
    currency: String,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amountMinor: Long, tipMinor: Long) -> Unit
) {
    var amountText by remember { mutableStateOf((balanceMinor / 100.0).toString()) }
    var tipText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Text("Registrar Pago en Caja (Efectivo)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    enabled = !isProcessing,
                    label = { Text("Monto en efectivo ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tipText,
                    onValueChange = { tipText = it },
                    enabled = !isProcessing,
                    label = { Text("Propina voluntaria ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val tipMinor = ((tipText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    if (amountMinor > 0) {
                        onConfirm(amountMinor, tipMinor)
                    }
                },
                enabled = !isProcessing && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = StaffColors.Coffee)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(18.dp).width(18.dp))
                } else {
                    Text("Confirmar Pago", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Cancelar")
            }
        }
    )
}

// Diálogo de Cierre Forzado con motivo
@Composable
private fun ForceCloseDialog(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reasonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Text("Cierre Forzado de Mesa", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ingrese el motivo del cierre forzado (se auditará en el comprobante):",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    enabled = !isProcessing,
                    label = { Text("Motivo") },
                    placeholder = { Text("Ej: El cliente se retiró sin pagar") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reasonText.trim()) },
                enabled = !isProcessing && reasonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(18.dp).width(18.dp))
                } else {
                    Text("Confirmar Cierre Forzado", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Cancelar")
            }
        }
    )
}

// Preview para Android Studio
@Preview
@Composable
fun BillingScreenPreview() {
    SomphieTheme {
        BillingContent(
            uiState = BillingUiState(
                sessionDetail = StaffSessionDetail(
                    session = StaffSession(
                        id = "sess-1",
                        status = "OPEN",
                        tableLabel = "Mesa 4",
                        openedAt = "12:00"
                    ),
                    orders = listOf(
                        StaffOrder(
                            id = "ord-1",
                            code = "ORD-001",
                            sessionId = "sess-1",
                            tableLabel = "Mesa 4",
                            status = "ACCEPTED",
                            createdAt = "12:05",
                            totalMinor = 1090,
                            items = listOf(
                                StaffOrderItem(
                                    id = "item-1",
                                    productId = "p-1",
                                    productName = "Latte",
                                    quantity = 2,
                                    unitPriceMinor = 320,
                                    totalMinor = 640,
                                    status = "READY"
                                )
                            )
                        )
                    ),
                    bill = Bill(
                        currency = "ARS",
                        chargesMinor = 1090,
                        adjustmentsMinor = -109,
                        paidMinor = 0,
                        balanceMinor = 981,
                        tipsMinor = 0
                    )
                )
            )
        )
    }
}
