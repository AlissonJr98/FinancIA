package com.financia.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.financia.app.core.export.PdfReportExporter
import com.financia.app.core.notifications.BankNotificationService
import com.financia.app.core.widget.FinancIAWidgetProvider
import com.financia.app.data.model.BankAccountOption
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.FixedBillTemplate
import com.financia.app.data.model.FutureMonthProjection
import com.financia.app.data.model.LoanContract
import com.financia.app.data.model.LoanType
import com.financia.app.data.model.MonthlyBarStat
import com.financia.app.data.model.SavingsGoal
import com.financia.app.data.model.TransactionCategory
import com.financia.app.data.model.WishlistItem
import com.financia.app.ui.components.CategoryIconMapper
import com.financia.app.ui.components.FinancIADatePickerField
import com.financia.app.ui.components.FinancIADropdownField
import com.financia.app.ui.components.FinancIATextField
import com.financia.app.ui.theme.AiPurple
import com.financia.app.ui.theme.EmeraldDark
import com.financia.app.ui.theme.EmeraldPrimary
import com.financia.app.ui.theme.ExpenseRed
import com.financia.app.ui.theme.IncomeGreen
import com.financia.app.ui.theme.WarningAmber
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    userName: String,
    transactions: List<FinancialTransaction>,
    sixMonthsStats: List<MonthlyBarStat>,
    futureProjections: List<FutureMonthProjection>,
    fixedBills: List<FixedBillTemplate>,
    loans: List<LoanContract>,
    savingsGoals: List<SavingsGoal>,
    wishlist: List<WishlistItem>,
    categoryBudgets: Map<String, Double>,
    selectedMonth: Int,
    selectedYear: Int,
    searchQuery: String,
    selectedCategory: String?,
    monthlyBudget: Double,
    isPrivacyMode: Boolean,
    sharedSpaceId: String = "",
    isViewingSharedSpace: Boolean = true,
    onToggleSharedSpaceView: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onSaveMonthlyBudget: (Double) -> Unit,
    onSaveCategoryBudget: (String, Double) -> Unit,
    onSaveWishlistItem: (WishlistItem) -> Unit,
    onDeleteWishlistItem: (String) -> Unit,
    onEditTransaction: (FinancialTransaction) -> Unit,
    onDuplicateTransaction: (FinancialTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onOpenSmartAdd: () -> Unit,
    onOpenFixedAndDebts: () -> Unit,
    onConvertCardInvoiceToRenegotiation: (LoanContract) -> Unit,
    onAskAiCanIBuy: (String) -> Unit,
    onImportStatement: (Uri, String, (Int) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale("pt", "BR")) }

    val monthKey = remember(selectedMonth, selectedYear) {
        String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)
    }

    val monthNames = remember { DateFormatSymbols(Locale("pt", "BR")).months }
    val currentMonthLabel = remember(selectedMonth, selectedYear) {
        val rawMonth = monthNames.getOrElse(selectedMonth) { "Mês" }
        "${rawMonth.replaceFirstChar { it.titlecase(Locale("pt", "BR")) }} $selectedYear"
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
    }
    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCategoryBudgetDialog by remember { mutableStateOf(false) }
    var showRenegotiateCardDialog by remember { mutableStateOf(false) }
    var showCanIBuyDialog by remember { mutableStateOf(false) }
    var showImportStatementDialog by remember { mutableStateOf(false) }
    var importAccountName by remember { mutableStateOf(BankAccountOption.NUBANK.label) }
    var editingTransaction by remember { mutableStateOf<FinancialTransaction?>(null) }
    var selectedNatureFilter by remember { mutableStateOf("Todos os Tipos") }
    var selectedAccountFilter by remember { mutableStateOf("Todas as Contas") }
    var selectedSortOption by remember { mutableStateOf("Mais Recentes") }

    // Resumo de saldo por Banco / Conta
    val bankAccountsSummary = remember(transactions) {
        transactions
            .groupBy { it.accountName.ifBlank { BankAccountOption.NUBANK.label } }
            .map { (accName, list) ->
                val inc = list.filter { it.isIncome }.sumOf { it.amount }
                val exp = list.filter { !it.isIncome }.sumOf { it.amount }
                Triple(accName, inc, exp)
            }
            .sortedByDescending { it.second + it.third }
    }

    val statementFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportStatement(uri, importAccountName) { count ->
                Toast.makeText(
                    context,
                    "Importados $count lançamentos do extrato para $importAccountName!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val categoryDropdownOptions = remember {
        listOf("Todas as Categorias") + TransactionCategory.entries.map { it.label }
    }
    val accountDropdownOptions = remember {
        listOf("Todas as Contas") + BankAccountOption.entries.map { it.label }
    }
    val natureFilterOptions = remember {
        listOf(
            "Todos os Tipos",
            "Apenas Receitas (+)",
            "Apenas Despesas (-)",
            "Apenas Fixas & Empréstimos",
            "Apenas Variáveis (Dia a dia)"
        )
    }
    val sortOptions = remember {
        listOf(
            "Mais Recentes",
            "Maior Valor (R$)",
            "Menor Valor (R$)",
            "Ordem Alfabética (A-Z)"
        )
    }

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

    LaunchedEffect(balance, totalIncome, totalExpense) {
        FinancIAWidgetProvider.saveAndRefreshWidget(
            context = context,
            balance = balance,
            income = totalIncome,
            expense = totalExpense
        )
    }

    // Despesas Fixas vs. Dívidas vs. Variáveis (Regra 50-30-20)
    val debtExpense = remember(transactions) {
        transactions.filter { !it.isIncome && it.isDebtNature }.sumOf { it.amount }
    }
    val essentialFixedExpense = remember(transactions, debtExpense) {
        transactions.filter { !it.isIncome && it.isFixedOrDebt && !it.isDebtNature }.sumOf { it.amount }
    }
    val fixedAndDebtExpense = debtExpense + essentialFixedExpense
    val variableExpense = remember(totalExpense, fixedAndDebtExpense) {
        (totalExpense - fixedAndDebtExpense).coerceAtLeast(0.0)
    }
    val totalSavedInGoals = remember(savingsGoals) {
        savingsGoals.sumOf { it.currentAmount }
    }

    val creditCardTotal = remember(transactions) {
        transactions.filter {
            !it.isIncome && it.paymentMethod.contains("Crédito", ignoreCase = true)
        }.sumOf { it.amount }
    }

    val pendingFixedBillsCount = remember(fixedBills, monthKey) {
        fixedBills.count { !it.isPaidInMonth(monthKey) }
    }
    val pendingLoansCount = remember(loans, monthKey) {
        loans.count { it.remainingInstallments > 0 && !it.isPaidInMonth(monthKey) }
    }

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

    val filteredTransactions = remember(transactions, selectedCategory, searchQuery, selectedNatureFilter, selectedAccountFilter, selectedSortOption) {
        val filtered = transactions.filter { tx ->
            val matchesCat = selectedCategory == null || tx.category.equals(selectedCategory, ignoreCase = true)
            val matchesAccount = selectedAccountFilter == "Todas as Contas" ||
                tx.accountName.equals(selectedAccountFilter, ignoreCase = true)
            val matchesNature = when (selectedNatureFilter) {
                "Apenas Receitas (+)" -> tx.isIncome
                "Apenas Despesas (-)" -> !tx.isIncome
                "Apenas Fixas & Empréstimos" -> !tx.isIncome && tx.isFixedOrDebt
                "Apenas Variáveis (Dia a dia)" -> !tx.isIncome && !tx.isFixedOrDebt
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.accountName.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesAccount && matchesNature && matchesQuery
        }
        when (selectedSortOption) {
            "Maior Valor (R$)" -> filtered.sortedByDescending { it.amount }
            "Menor Valor (R$)" -> filtered.sortedBy { it.amount }
            "Ordem Alfabética (A-Z)" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered.sortedByDescending { it.dateTimestamp }
        }
    }

    // Modal de Importar Extrato Bancário (CSV / OFX)
    if (showImportStatementDialog) {
        AlertDialog(
            onDismissRequest = { showImportStatementDialog = false },
            title = { Text("Importar Extrato Bancário (CSV / OFX)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Selecione a Conta/Banco de origem e escolha o arquivo .CSV ou .OFX exportado pelo app do seu banco. O FinancIA categorizará todos os lançamentos automaticamente!",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FinancIADropdownField(
                        label = "Conta / Banco do Extrato",
                        selectedValue = importAccountName,
                        options = BankAccountOption.entries.map { it.label },
                        onOptionSelected = { importAccountName = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportStatementDialog = false
                        statementFileLauncher.launch(arrayOf("*/*"))
                    }
                ) {
                    Text("Escolher Arquivo CSV / OFX")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportStatementDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Modal: Calculadora Inteligente "Posso Comprar?" ---
    if (showCanIBuyDialog) {
        var itemName by remember { mutableStateOf("") }
        var itemTotalPriceText by remember { mutableStateOf("") }
        var itemInstallmentsText by remember { mutableStateOf("10") }

        val price = itemTotalPriceText.replace(",", ".").toDoubleOrNull() ?: 0.0
        val count = itemInstallmentsText.toIntOrNull()?.coerceIn(1, 120) ?: 1
        val monthlyInstallment = if (count > 0) price / count else price
        val projectedBalanceAfter = balance - monthlyInstallment
        val projectedExpenseWithNew = totalExpense + monthlyInstallment

        AlertDialog(
            onDismissRequest = { showCanIBuyDialog = false },
            title = { Text("Simulador: Posso Comprar?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Teste se uma nova compra à vista ou parcelada cabe no seu orçamento considerando suas contas fixas e empréstimos:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("O que você quer comprar? (ex: TV, Celular)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = itemTotalPriceText,
                            onValueChange = { itemTotalPriceText = it },
                            label = { Text("Valor Total (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = itemInstallmentsText,
                            onValueChange = { itemInstallmentsText = it },
                            label = { Text("Parcelas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    if (price > 0.0) {
                        val verdictText = when {
                            projectedBalanceAfter < 0 ->
                                "🔴 Risco Alto: A parcela de ${currencyFormatter.format(monthlyInstallment)} deixaria seu saldo mensal negativo em ${currencyFormatter.format(projectedBalanceAfter)}."
                            monthlyBudget > 0 && projectedExpenseWithNew > monthlyBudget ->
                                "🟡 Atenção: Cabe no saldo, mas ultrapassa sua Meta de Teto Mensal (${currencyFormatter.format(monthlyBudget)})."
                            else ->
                                "🟢 Cabe no Orçamento! Parcela de ${currencyFormatter.format(monthlyInstallment)}/mês • Saldo livre projetado: ${currencyFormatter.format(projectedBalanceAfter)}."
                        }
                        val verdictColor = when {
                            projectedBalanceAfter < 0 -> ExpenseRed
                            monthlyBudget > 0 && projectedExpenseWithNew > monthlyBudget -> WarningAmber
                            else -> IncomeGreen
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = verdictColor.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = verdictText,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (price > 0.0) {
                                val bestMonth = futureProjections.minByOrNull { it.totalCommitted }?.monthLabel
                                    ?: "Próximo mês com menor comprometimento"
                                onSaveWishlistItem(
                                    WishlistItem(
                                        title = itemName.ifBlank { "Item Planejado" },
                                        totalPrice = price,
                                        installments = count,
                                        bestMonthHint = "Melhor mês projetado: $bestMonth"
                                    )
                                )
                                showCanIBuyDialog = false
                                Toast.makeText(context, "Salvo na sua Lista de Desejos!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = price > 0.0
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvar Desejo")
                    }

                    Button(
                        onClick = {
                            val query = "Estou pensando em comprar '${itemName.ifBlank { "um item" }}' por ${currencyFormatter.format(price)} em ${count}x (${currencyFormatter.format(monthlyInstallment)}/mês). Analisando minhas contas fixas, empréstimos e saldo, vale a pena comprar agora ou melhor esperar?"
                            showCanIBuyDialog = false
                            onAskAiCanIBuy(query)
                        },
                        enabled = price > 0.0
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IA")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCanIBuyDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Modal de Renegociação da Fatura do Cartão
    if (showRenegotiateCardDialog) {
        var cardName by remember { mutableStateOf("Renegociação Fatura Cartão ($currentMonthLabel)") }
        var instValueText by remember { mutableStateOf(((creditCardTotal / 12.0).toInt()).toString()) }
        var countText by remember { mutableStateOf("12") }
        var dueDayText by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showRenegotiateCardDialog = false },
            title = { Text("Parcelar / Renegociar Fatura do Cartão") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transforme a fatura atual (${currencyFormatter.format(creditCardTotal)}) em um contrato de parcelamento ou renegociação na aba 'Fixas & Dívidas':",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = cardName,
                        onValueChange = { cardName = it },
                        label = { Text("Descrição do Acordo / Fatura") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text("Número de Parcelas (ex: 12, 24, 36)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instValueText,
                        onValueChange = { instValueText = it },
                        label = { Text("Valor Fixo da Parcela Mensal (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it },
                        label = { Text("Dia do Vencimento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val instAmt = instValueText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val total = countText.toIntOrNull()?.coerceIn(1, 360) ?: 12
                        val day = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 10
                        if (cardName.isNotBlank() && instAmt > 0.0) {
                            onConvertCardInvoiceToRenegotiation(
                                LoanContract(
                                    title = cardName.trim(),
                                    loanType = LoanType.RENEGOCIACAO_CARTAO.label,
                                    installmentAmount = instAmt,
                                    totalInstallments = total,
                                    paidInstallments = 0,
                                    dueDay = day
                                )
                            )
                            showRenegotiateCardDialog = false
                            onOpenFixedAndDebts()
                        }
                    }
                ) {
                    Text("Criar Renegociação")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenegotiateCardDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Meta de Orçamento Individual por Categoria
    if (showCategoryBudgetDialog) {
        var selectedCatForBudget by remember { mutableStateOf(TransactionCategory.ALIMENTACAO.label) }
        var catLimitText by remember(selectedCatForBudget) {
            mutableStateOf((categoryBudgets[selectedCatForBudget] ?: 0.0).takeIf { it > 0 }?.toString() ?: "")
        }

        AlertDialog(
            onDismissRequest = { showCategoryBudgetDialog = false },
            title = { Text("Teto de Gastos por Categoria") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Escolha a categoria na caixa abaixo e defina o limite mensal para ela (deixe 0 para remover o limite):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FinancIADropdownField(
                        label = "Categoria",
                        selectedValue = selectedCatForBudget,
                        options = TransactionCategory.entries.filter { !it.isIncomeDefault }.map { it.label },
                        onOptionSelected = {
                            selectedCatForBudget = it
                            catLimitText = (categoryBudgets[it] ?: 0.0).takeIf { v -> v > 0 }?.toString() ?: ""
                        },
                        optionIconProvider = { CategoryIconMapper.getIcon(it) }
                    )
                    FinancIATextField(
                        value = catLimitText,
                        onValueChange = { catLimitText = it },
                        label = "Limite Mensal da Categoria",
                        prefixText = "R$ ",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lim = catLimitText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onSaveCategoryBudget(selectedCatForBudget, lim)
                        showCategoryBudgetDialog = false
                    }
                ) {
                    Text("Salvar Limite da Categoria")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryBudgetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
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

    // Modal de Edição de Transação (com DatePicker e Seletor de Conta/Banco)
    if (editingTransaction != null) {
        val txToEdit = editingTransaction!!
        var editTitle by remember(txToEdit) { mutableStateOf(txToEdit.title) }
        var editAmount by remember(txToEdit) { mutableStateOf(txToEdit.amount.toString()) }
        var editCategory by remember(txToEdit) { mutableStateOf(txToEdit.category) }
        var editPayment by remember(txToEdit) { mutableStateOf(txToEdit.paymentMethod) }
        var editAccount by remember(txToEdit) { mutableStateOf(txToEdit.accountName) }
        var editDateTimestamp by remember(txToEdit) { mutableLongStateOf(txToEdit.dateTimestamp) }
        var editNotes by remember(txToEdit) { mutableStateOf(txToEdit.notes) }

        AlertDialog(
            onDismissRequest = { editingTransaction = null },
            title = { Text("Editar Lançamento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinancIATextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = "Descrição"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FinancIATextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            label = "Valor",
                            prefixText = "R$ ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        FinancIADatePickerField(
                            label = "Data",
                            selectedTimestamp = editDateTimestamp,
                            onDateSelected = { editDateTimestamp = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FinancIADropdownField(
                        label = "Categoria",
                        selectedValue = editCategory,
                        options = TransactionCategory.entries.map { it.label },
                        onOptionSelected = { editCategory = it },
                        optionIconProvider = { CategoryIconMapper.getIcon(it) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FinancIADropdownField(
                            label = "Conta / Banco",
                            selectedValue = editAccount,
                            options = BankAccountOption.entries.map { it.label },
                            onOptionSelected = { editAccount = it },
                            modifier = Modifier.weight(1f)
                        )
                        FinancIADropdownField(
                            label = "Pagamento",
                            selectedValue = editPayment,
                            options = listOf("Pix", "Cartão de Crédito", "Cartão de Débito", "Boleto", "Dinheiro"),
                            onOptionSelected = { editPayment = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FinancIATextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = "Observações",
                        singleLine = false,
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
                                accountName = editAccount.trim(),
                                dateTimestamp = editDateTimestamp,
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
        // 1. Cabeçalho Premium com Avatar/Iniciais + Saudação Dinâmica + Botão "Posso Comprar?"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val initial = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(colors = listOf(EmeraldPrimary, AiPurple))
                            )
                            .clickable { onOpenProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.clickable { onOpenProfile() }) {
                        Text(
                            text = "$greeting, ${userName.split(" ").firstOrNull() ?: "Alisson"} 👋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val quickStatus = when {
                            sharedSpaceId.isNotBlank() && isViewingSharedSpace -> "💑 Espaço do Casal: $sharedSpaceId (Toque p/ Perfil)"
                            pendingFixedBillsCount > 0 -> "Você tem $pendingFixedBillsCount conta(s) fixa(s) no mês"
                            pendingLoansCount > 0 -> "Você tem $pendingLoansCount parcela(s) ativa(s) no mês"
                            else -> "Toque aqui para abrir Perfil, Idioma & Casal"
                        }
                        Text(
                            text = quickStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sharedSpaceId.isNotBlank() && isViewingSharedSpace) Color(0xFFEC4899)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssistChip(
                    onClick = { showCanIBuyDialog = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = AiPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Posso Comprar?", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // 2. Seletor de Mês/Ano + Exportar PDF / Excel / Importar Extrato
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = {
                                PdfReportExporter.exportAndSharePdf(
                                    context = context,
                                    periodLabel = currentMonthLabel,
                                    transactions = transactions,
                                    fixedBills = fixedBills,
                                    loans = loans
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("PDF", style = MaterialTheme.typography.labelSmall) }
                        )

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
                            label = { Text("CSV", style = MaterialTheme.typography.labelSmall) }
                        )

                        AssistChip(
                            onClick = { showImportStatementDialog = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Extrato", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // 3. Cartão de Saldo Principal com Modo Privacidade (Olhinho)
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

        // 3.1 Mini-Cards Coloridos de Saldo por Banco / Conta (Nubank, Inter, Itaú, Carteira...)
        if (bankAccountsSummary.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Movimentação por Conta / Banco (Toque para filtrar)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedAccountFilter != "Todas as Contas") {
                            TextButton(onClick = { selectedAccountFilter = "Todas as Contas" }) {
                                Text("Mostrar Todas", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        bankAccountsSummary.forEach { (accName, accIncome, accExpense) ->
                            val bankOpt = BankAccountOption.fromLabel(accName)
                            val bankColor = Color(bankOpt.colorHex)
                            val isSelected = selectedAccountFilter.equals(accName, ignoreCase = true)
                            val netAcc = accIncome - accExpense

                            Card(
                                onClick = {
                                    selectedAccountFilter = if (isSelected) "Todas as Contas" else accName
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        bankColor.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                modifier = Modifier
                                    .width(155.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) bankColor else bankColor.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(bankColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = accName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = formatMoney(netAcc),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (netAcc >= 0) IncomeGreen else ExpenseRed
                                    )
                                    Text(
                                        text = "Saídas: ${formatMoney(accExpense)}",
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

        // 3.2 Projeção de Saldo Futuro (Próximos 3 Meses: Fixas + Empréstimos + Parcelas)
        if (futureProjections.any { it.totalCommitted > 0 }) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = AiPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Projeção dos Próximos 3 Meses (Fixas + Parcelas)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            futureProjections.forEach { proj ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = proj.monthLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = formatMoney(proj.totalCommitted),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (proj.finishingInstallmentsCount > 0) {
                                            Text(
                                                text = "🎉 ${proj.finishingInstallmentsCount} parc. acaba!",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = IncomeGreen
                                            )
                                        } else {
                                            Text(
                                                text = "Comprometido",
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
        }

        // 3.3 Lista de Compras / Desejos Planejados (Wishlist)
        if (wishlist.isNotEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Lista de Compras Planejadas (${wishlist.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = { showCanIBuyDialog = true }) {
                                Text("+ Simular Item", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        wishlist.forEach { wish ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${wish.title} (${wish.installments}x de ${formatMoney(wish.monthlyInstallment)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (wish.bestMonthHint.isNotBlank()) {
                                        Text(
                                            text = "💡 ${wish.bestMonthHint}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IncomeGreen
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteWishlistItem(wish.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remover desejo",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Calendário Visual de Vencimentos do Mês ("Mapa de Calor" de Contas Fixas e Parcelas)
        if (fixedBills.isNotEmpty() || loans.isNotEmpty()) {
            item {
                Card(
                    onClick = onOpenFixedAndDebts,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Calendário de Vencimentos (Fixas & Parcelas)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Lista horizontal dos dias que possuem vencimento ou linha dos 31 dias
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val dueItems = buildList {
                                fixedBills.forEach { bill ->
                                    add(Triple(bill.dueDay, bill.name, bill.isPaidInMonth(monthKey)))
                                }
                                loans.filter { it.remainingInstallments > 0 }.forEach { loan ->
                                    add(Triple(loan.dueDay, loan.title, loan.isPaidInMonth(monthKey)))
                                }
                            }.sortedBy { it.first }

                            dueItems.forEach { (day, label, isPaid) ->
                                val statusColor = when {
                                    isPaid -> IncomeGreen
                                    todayDay > day -> ExpenseRed
                                    (day - todayDay) in 0..3 -> WarningAmber
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(statusColor.copy(alpha = 0.12f))
                                        .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Dia $day",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor
                                    )
                                    Text(
                                        text = label.take(12),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Termômetro Fixas vs. Variáveis + Regra 50-30-20 Automatizada
        item {
            val fixedRatioTarget = if (totalExpense > 0) (fixedAndDebtExpense / totalExpense).toFloat().coerceIn(0f, 1f) else 0.5f
            val animatedFixedRatio by animateFloatAsState(
                targetValue = fixedRatioTarget,
                animationSpec = tween(700),
                label = "fixedRatio"
            )

            Card(
                onClick = onOpenFixedAndDebts,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Termômetro Fixas vs. Variáveis & Regra 50-30-20",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "50% Fixas Essenciais",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMoney(essentialFixedExpense),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "30% Variáveis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMoney(variableExpense),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF97316)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "20% Dívidas/Metas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMoney(debtExpense + totalSavedInGoals),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedFixedRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF8B5CF6),
                        trackColor = Color(0xFFF97316).copy(alpha = 0.35f)
                    )

                    val alertText = buildString {
                        if (pendingFixedBillsCount > 0) {
                            append("⏳ $pendingFixedBillsCount conta(s) fixa(s) aguardando valor do mês (Água/Luz...) ")
                        }
                        if (pendingLoansCount > 0) {
                            append("• $pendingLoansCount parcela(s) de empréstimo em aberto")
                        }
                        if (isEmpty()) {
                            append("Toque para abrir Contas Fixas (Água/Luz), Empréstimos, Caixinhas e Quem te Deve")
                        }
                    }
                    Text(
                        text = alertText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 6. Gráfico de Barras Comparativo (Evolução dos Últimos 6 Meses)
        if (sixMonthsStats.any { it.income > 0 || it.totalExpense > 0 }) {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Evolução Mensal (Últimos 6 Meses)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val maxBarValue = remember(sixMonthsStats) {
                            sixMonthsStats.maxOfOrNull { maxOf(it.income, it.totalExpense) }?.coerceAtLeast(1.0) ?: 1.0
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            sixMonthsStats.forEach { stat ->
                                val incomeFrac = (stat.income / maxBarValue).toFloat().coerceIn(0.04f, 1f)
                                val expFrac = (stat.totalExpense / maxBarValue).toFloat().coerceIn(0.04f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Barra de Receita
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .fillMaxHeight(if (stat.income > 0) incomeFrac else 0.03f)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(IncomeGreen)
                                        )
                                        // Barra de Despesa Total
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .fillMaxHeight(if (stat.totalExpense > 0) expFrac else 0.03f)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(ExpenseRed)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stat.monthLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receitas", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Despesas (Fixas + Variáveis)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 7. Card de Fatura do Cartão de Crédito
        if (creditCardTotal > 0.0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD97706).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Fatura Cartão de Crédito",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatMoney(creditCardTotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ExpenseRed
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showRenegotiateCardDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Renegociar / Parcelar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 8. Card de Meta de Orçamento Mensal + Score de Saúde Financeira IA
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
                        val budgetProgressTarget = (totalExpense / monthlyBudget).toFloat().coerceIn(0f, 1f)
                        val animatedBudgetProgress by animateFloatAsState(
                            targetValue = budgetProgressTarget,
                            animationSpec = tween(700),
                            label = "budgetProgress"
                        )
                        val barColor = when {
                            budgetProgressTarget >= 0.9f -> ExpenseRed
                            budgetProgressTarget >= 0.75f -> WarningAmber
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
                            progress = { animatedBudgetProgress },
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

        // 9. Card de Automação Zero-Toque (Leitura Automática de Notificações Bancárias)
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

        // 10. Gráfico de Rosca (Donut Chart) + Despesas por Categoria
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Distribuição e Tetos por Categoria",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showCategoryBudgetDialog = true }) {
                                Text("Limite Categoria", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
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
                                    val catLimit = categoryBudgets[category] ?: 0.0
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                                                text = if (catLimit > 0) {
                                                    "${formatMoney(sum)} / ${formatMoney(catLimit)}"
                                                } else {
                                                    "${formatMoney(sum)} ($pct%)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (catLimit > 0 && sum > catLimit) ExpenseRed
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (catLimit > 0) {
                                            val catRatio = (sum / catLimit).toFloat().coerceIn(0f, 1f)
                                            LinearProgressIndicator(
                                                progress = { catRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = when {
                                                    catRatio >= 1f -> ExpenseRed
                                                    catRatio >= 0.8f -> WarningAmber
                                                    else -> IncomeGreen
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 11. Busca + Filtros que abrem a Caixa para Selecionar (Dropdowns de Categoria, Tipo e Ordenação)
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Filtrar e Pesquisar Lançamentos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    FinancIATextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        label = "Pesquisar lançamento",
                        placeholder = "Digite nome, categoria ou observação...",
                        leadingIcon = Icons.Default.Search
                    )

                    // Caixa de Seleção Dropdown: Categoria + Conta/Banco
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FinancIADropdownField(
                            label = "Categoria",
                            selectedValue = selectedCategory ?: "Todas as Categorias",
                            options = categoryDropdownOptions,
                            onOptionSelected = { chosen ->
                                onSelectCategory(if (chosen == "Todas as Categorias") null else chosen)
                            },
                            optionIconProvider = { opt ->
                                if (opt == "Todas as Categorias") Icons.Default.PieChart
                                else CategoryIconMapper.getIcon(opt)
                            },
                            optionColorProvider = { opt ->
                                if (opt == "Todas as Categorias") EmeraldPrimary
                                else Color(TransactionCategory.fromLabelOrName(opt).colorHex)
                            },
                            modifier = Modifier.weight(1.1f)
                        )

                        FinancIADropdownField(
                            label = "Conta / Banco",
                            selectedValue = selectedAccountFilter,
                            options = accountDropdownOptions,
                            onOptionSelected = { selectedAccountFilter = it },
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Caixas de Seleção Lado a Lado: Tipo (Fixas/Variáveis/Receitas) + Ordenação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FinancIADropdownField(
                            label = "Tipo",
                            selectedValue = selectedNatureFilter,
                            options = natureFilterOptions,
                            onOptionSelected = { selectedNatureFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                        FinancIADropdownField(
                            label = "Ordenar por",
                            selectedValue = selectedSortOption,
                            options = sortOptions,
                            onOptionSelected = { selectedSortOption = it },
                            modifier = Modifier.weight(1f)
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
                            text = "Use a aba 'Fixas/Dívidas' para cadastrar Água, Luz e Empréstimos, ou 'Lançar IA' para gastos do dia a dia.",
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
                    onDuplicate = {
                        onDuplicateTransaction(tx)
                        Toast.makeText(context, "Lançamento '${tx.title}' duplicado para hoje!", Toast.LENGTH_SHORT).show()
                    },
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
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryEnum = TransactionCategory.fromLabelOrName(transaction.category)
    val categoryColor = Color(categoryEnum.colorHex)
    val categoryIcon = CategoryIconMapper.getIcon(transaction.category)
    val amountColor = if (transaction.isIncome) IncomeGreen else ExpenseRed

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
            // Ícone Colorido Real da Categoria
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
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
                    if (transaction.isFixedOrDebt) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Despesa Fixa ou Parcela",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                val natureTag = when {
                    transaction.isIncome -> "Receita"
                    transaction.isDebtNature -> "Empréstimo/Dívida"
                    transaction.isFixedOrDebt -> "Despesa Fixa"
                    else -> "Variável"
                }
                val authorTag = if (transaction.authorName.isNotBlank()) " • Por ${transaction.authorName}" else ""
                Text(
                    text = "${transaction.category} • ${transaction.accountName} • ${transaction.paymentMethod}$authorTag",
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
                    color = amountColor
                )
                Row {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicar para Hoje",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
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
