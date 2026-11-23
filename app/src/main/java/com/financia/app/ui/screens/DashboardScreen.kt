package com.financia.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.financia.app.core.export.CsvReportExporter
import com.financia.app.core.notifications.BankNotificationService
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.TransactionCategory
import com.financia.app.ui.theme.AiPurple
import com.financia.app.ui.theme.EmeraldDark
import com.financia.app.ui.theme.EmeraldPrimary
import com.financia.app.ui.theme.ExpenseRed
import com.financia.app.ui.theme.IncomeGreen
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    transactions: List<FinancialTransaction>,
    selectedMonth: Int,
    selectedYear: Int,
    searchQuery: String,
    selectedCategory: String?,
    monthlyBudget: Double,
    isPrivacyMode: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onSaveMonthlyBudget: (Double) -> Unit,
    onEditTransaction: (FinancialTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onOpenSmartAdd: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale("pt", "BR")) }

    val monthNames = remember {
        DateFormatSymbols(Locale("pt", "BR")).months
    }
    val currentMonthLabel = remember(selectedMonth, selectedYear) {
        val rawMonth = monthNames.getOrElse(selectedMonth) { "Mês" }
        "${rawMonth.replaceFirstChar { it.titlecase(Locale("pt", "BR")) }} $selectedYear"
    }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<FinancialTransaction?>(null) }
    val isNotificationAccessEnabled = remember(transactions) {
        BankNotificationService.isNotificationListenerEnabled(context)
    }

    fun formatMoney(value: Double): String {
        return if (isPrivacyMode) "R$ ••••••" else currencyFormatter.format(value)
    }

    val totalIncome = remember(transactions) {
        transactions.filter { it.isIncome }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { !it.isIncome }.sumOf { it.amount }
    }
    val balance = totalIncome - totalExpense

    // Score de Saúde Financeira (0 a 100)
    val healthScore = remember(totalIncome, totalExpense, monthlyBudget) {
        when {
            totalIncome == 0.0 && totalExpense == 0.0 -> 80
            totalIncome > 0.0 -> {
                val ratio = totalExpense / totalIncome
                val base = ((1.0 - ratio) * 100).toInt() + 50
                val budgetPenalty = if (monthlyBudget > 0 && totalExpense > monthlyBudget) 20 else 0
                (base - budgetPenalty).coerceIn(10, 100)
            }
            else -> 35
        }
    }

    val categoryBreakdown = remember(transactions) {
        transactions
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
    }

    val filteredTransactions = remember(transactions, selectedCategory, searchQuery) {
        transactions.filter { tx ->
            val matchesCat = selectedCategory == null || tx.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    // Modal de Meta de Orçamento Mensal
    if (showBudgetDialog) {
        var budgetInput by remember { mutableStateOf(if (monthlyBudget > 0) monthlyBudget.toString() else "") }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Meta de Teto de Gastos Mensal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Defina quanto você planeja gastar no máximo por mês. O FinancIA exibirá alertas visuais conforme seus gastos avançarem.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Limite Mensal (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = budgetInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                    onSaveMonthlyBudget(value)
                    showBudgetDialog = false
                }) {
                    Text("Salvar Meta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Edição de Transação
    if (editingTransaction != null) {
        val txToEdit = editingTransaction!!
        var editTitle by remember(txToEdit) { mutableStateOf(txToEdit.title) }
        var editAmount by remember(txToEdit) { mutableStateOf(txToEdit.amount.toString()) }
        var editCategory by remember(txToEdit) { mutableStateOf(txToEdit.category) }
        var editPayment by remember(txToEdit) { mutableStateOf(txToEdit.paymentMethod) }
        var editNotes by remember(txToEdit) { mutableStateOf(txToEdit.notes) }

        AlertDialog(
            onDismissRequest = { editingTransaction = null },
            title = { Text("Editar Lançamento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Descrição") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Valor (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Categoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPayment,
                        onValueChange = { editPayment = it },
                        label = { Text("Forma de Pagamento") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newAmt = editAmount.replace(",", ".").toDoubleOrNull() ?: txToEdit.amount
                    if (editTitle.isNotBlank() && newAmt > 0) {
                        onEditTransaction(
                            txToEdit.copy(
                                title = editTitle.trim(),
                                amount = newAmt,
                                category = editCategory.trim(),
                                paymentMethod = editPayment.trim(),
                                notes = editNotes.trim()
                            )
                        )
                        editingTransaction = null
                    }
                }) {
                    Text("Salvar Alterações")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTransaction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Seletor de Mês/Ano + Exportar CSV
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
                    }
                    Text(
                        text = currentMonthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
                    }
                }

                AssistChip(
                    onClick = {
                        CsvReportExporter.exportAndShareCsv(
                            context = context,
                            transactions = transactions,
                            periodLabel = currentMonthLabel
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Excel/CSV", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Cartão de Saldo Principal com Modo Privacidade (Olhinho)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(colors = listOf(EmeraldPrimary, EmeraldDark)))
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saldo de $currentMonthLabel",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            IconButton(
                                onClick = onTogglePrivacyMode,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Alternar Modo Privacidade",
                                    tint = Color.White
                                )
                            }
                        }

                        Text(
                            text = formatMoney(balance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryMetricItem(
                                label = "Receitas",
                                amountText = formatMoney(totalIncome),
                                isIncome = true
                            )
                            SummaryMetricItem(
                                label = "Despesas",
                                amountText = formatMoney(totalExpense),
                                isIncome = false
                            )
                        }
                    }
                }
            }
        }

        // Card de Meta de Orçamento Mensal + Score de Saúde Financeira IA
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AiPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saúde Financeira: $healthScore/100",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { showBudgetDialog = true }) {
                            Text(if (monthlyBudget > 0) "Editar Meta" else "Definir Meta Mensal")
                        }
                    }

                    if (monthlyBudget > 0) {
                        val budgetProgress = (totalExpense / monthlyBudget).toFloat().coerceIn(0f, 1f)
                        val barColor = when {
                            budgetProgress >= 0.9f -> ExpenseRed
                            budgetProgress >= 0.75f -> Color(0xFFF59E0B)
                            else -> IncomeGreen
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gasto: ${formatMoney(totalExpense)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Teto: ${formatMoney(monthlyBudget)} (${((totalExpense / monthlyBudget) * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = barColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { budgetProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = barColor
                        )
                    } else {
                        Text(
                            text = "Toque em 'Definir Meta Mensal' para acompanhar seu limite de gastos com alertas automáticos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Card de Automação Zero-Toque (Leitura Automática de Notificações Bancárias)
        item {
            Card(
                onClick = {
                    if (!isNotificationAccessEnabled) {
                        BankNotificationService.openNotificationAccessSettings(context)
                    } else {
                        onOpenSmartAdd()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AiPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isNotificationAccessEnabled) Icons.Default.AutoAwesome else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AiPurple
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isNotificationAccessEnabled) {
                                "Automação Bancária Ativa + Lançamento IA"
                            } else {
                                "Ativar Captura Automática de Bancos (Nubank, Inter, Itaú...)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isNotificationAccessEnabled) {
                                "Notificações de Pix/Cartão são lidas automaticamente. Toque para lançar por voz ou foto!"
                            } else {
                                "Toque aqui para autorizar o FinancIA a ler notificações de Pix e compras e lançar sozinho com IA."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Gráfico de Rosca (Donut Chart) + Despesas por Categoria
        if (categoryBreakdown.isNotEmpty() && totalExpense > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Distribuição por Categoria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Donut Chart em Compose Canvas
                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(104.dp)) {
                                    var startAngle = -90f
                                    categoryBreakdown.forEach { (catName, sum) ->
                                        val sweep = ((sum / totalExpense) * 360f).toFloat()
                                        val catColor = Color(TransactionCategory.fromLabelOrName(catName).colorHex)
                                        drawArc(
                                            color = catColor,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${categoryBreakdown.size}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "categ.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categoryBreakdown.take(5).forEach { (category, sum) ->
                                    val pct = ((sum / totalExpense) * 100).toInt()
                                    val catColor = Color(TransactionCategory.fromLabelOrName(category).colorHex)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = category,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = "${formatMoney(sum)} ($pct%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Busca + Filtro por Categoria
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar lançamento por nome, categoria ou nota...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("Todas") }
                    )
                    TransactionCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.label,
                            onClick = {
                                onSelectCategory(if (selectedCategory == cat.label) null else cat.label)
                            },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nenhum lançamento em $currentMonthLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use a aba 'Lançar IA' ou peça diretamente no chat do Consultor para adicionar gastos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    formattedAmount = formatMoney(tx.amount),
                    formattedDate = dateFormatter.format(Date(tx.dateTimestamp)),
                    onEdit = { editingTransaction = tx },
                    onDelete = { onDeleteTransaction(tx.id) }
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    amountText: String,
    isIncome: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = label,
                tint = if (isIncome) Color(0xFFA7F3D0) else Color(0xFFFECACA),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TransactionRowCard(
    transaction: FinancialTransaction,
    formattedAmount: String,
    formattedDate: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val badgeColor = if (transaction.isIncome) IncomeGreen else ExpenseRed
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.createdByAi) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Criado com IA",
                            tint = AiPurple,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (transaction.isRecurring) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Gasto Fixo Mensal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "${transaction.category} • ${transaction.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transaction.isIncome) "+ " else "- ") + formattedAmount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
