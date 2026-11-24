package com.financia.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.financia.app.data.model.FixedBillTemplate
import com.financia.app.data.model.LoanContract
import com.financia.app.data.model.LoanType
import com.financia.app.data.model.SavingsGoal
import com.financia.app.data.model.TransactionCategory
import com.financia.app.ui.theme.AiPurple
import com.financia.app.ui.theme.ExpenseRed
import com.financia.app.ui.theme.IncomeGreen
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@Composable
fun FixedAndDebtsScreen(
    selectedMonth: Int,
    selectedYear: Int,
    fixedBills: List<FixedBillTemplate>,
    loans: List<LoanContract>,
    savingsGoals: List<SavingsGoal>,
    onSaveFixedBill: (FixedBillTemplate) -> Unit,
    onPayFixedBillForMonth: (FixedBillTemplate, String, Double) -> Unit,
    onDeleteFixedBill: (String) -> Unit,
    onSaveLoanContract: (LoanContract) -> Unit,
    onPayOrAmortizeLoan: (LoanContract, Int, Double, String, Boolean) -> Unit,
    onDeleteLoanContract: (String) -> Unit,
    onSaveSavingsGoal: (SavingsGoal) -> Unit,
    onDeleteSavingsGoal: (String) -> Unit,
    onSimulatePayoffInAiChat: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val monthKey = remember(selectedMonth, selectedYear) {
        String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)
    }
    val monthNames = remember { DateFormatSymbols(Locale("pt", "BR")).months }
    val currentMonthLabel = remember(selectedMonth, selectedYear) {
        val raw = monthNames.getOrElse(selectedMonth) { "Mês" }
        "${raw.replaceFirstChar { it.titlecase(Locale("pt", "BR")) }} $selectedYear"
    }

    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }

    var activeSection by remember { mutableIntStateOf(0) } // 0 = Contas Fixas, 1 = Empréstimos/Renegociação, 2 = Caixinhas

    var showAddFixedBillDialog by remember { mutableStateOf(false) }
    var billToPayDialog by remember { mutableStateOf<FixedBillTemplate?>(null) }

    var showAddLoanDialog by remember { mutableStateOf(false) }
    var loanToAmortizeDialog by remember { mutableStateOf<LoanContract?>(null) }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToDepositDialog by remember { mutableStateOf<SavingsGoal?>(null) }

    // --- Modal: Nova Conta Fixa (Água, Luz, Internet, Aluguel...) ---
    if (showAddFixedBillDialog) {
        var billName by remember { mutableStateOf("") }
        var billDueDay by remember { mutableStateOf("10") }
        var billDefaultAmt by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFixedBillDialog = false },
            title = { Text("Nova Conta Fixa Mensal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ex: Água, Energia Elétrica, Internet, Gás, Condomínio ou Aluguel. Ela ficará fixa todo mês e você só informa o valor que veio na fatura daquele mês!",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Água", "Energia (Luz)", "Internet", "Aluguel").forEach { preset ->
                            AssistChip(
                                onClick = { billName = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = billName,
                        onValueChange = { billName = it },
                        label = { Text("Nome da Conta (ex: Água Sabesp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = billDueDay,
                        onValueChange = { billDueDay = it },
                        label = { Text("Dia do Vencimento (1 a 31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = billDefaultAmt,
                        onValueChange = { billDefaultAmt = it },
                        label = { Text("Valor Médio/Base Estimado (R$ - opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (billName.isNotBlank()) {
                            val day = billDueDay.toIntOrNull()?.coerceIn(1, 31) ?: 10
                            val baseAmt = billDefaultAmt.replace(",", ".").toDoubleOrNull() ?: 0.0
                            onSaveFixedBill(
                                FixedBillTemplate(
                                    name = billName.trim(),
                                    category = TransactionCategory.MORADIA.label,
                                    dueDay = day,
                                    defaultAmount = baseAmt
                                )
                            )
                            showAddFixedBillDialog = false
                        }
                    }
                ) {
                    Text("Criar Conta Fixa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFixedBillDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Modal: Informar Valor do Mês e Pagar Conta Fixa (ex: Água deste mês) ---
    if (billToPayDialog != null) {
        val bill = billToPayDialog!!
        val initialVal = bill.amountForMonth(monthKey).takeIf { it > 0 }?.toString() ?: ""
        var monthAmountText by remember(bill) { mutableStateOf(initialVal) }
        val avgPrev = bill.averagePreviousAmount(monthKey)

        AlertDialog(
            onDismissRequest = { billToPayDialog = null },
            title = { Text("Valor de ${bill.name} em $currentMonthLabel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Qual foi o valor da conta de ${bill.name} neste mês? Ao confirmar, ela será marcada como Paga ✅ e somada às despesas fixas de $currentMonthLabel.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (avgPrev > 0) {
                        Text(
                            text = "Média/Base anterior: ${currencyFormatter.format(avgPrev)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AiPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedTextField(
                        value = monthAmountText,
                        onValueChange = { monthAmountText = it },
                        label = { Text("Valor deste mês (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = monthAmountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onPayFixedBillForMonth(bill, monthKey, amt)
                            billToPayDialog = null
                        }
                    }
                ) {
                    Text("Confirmar e Lançar no Mês")
                }
            },
            dismissButton = {
                TextButton(onClick = { billToPayDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Modal: Novo Empréstimo, Financiamento ou Renegociação de Cartão ---
    if (showAddLoanDialog) {
        var loanTitle by remember { mutableStateOf("") }
        var selectedLoanType by remember { mutableStateOf(LoanType.EMPRESTIMO.label) }
        var installmentAmtText by remember { mutableStateOf("") }
        var totalInstallmentsText by remember { mutableStateOf("48") }
        var paidInstallmentsText by remember { mutableStateOf("0") }
        var dueDayText by remember { mutableStateOf("15") }
        var interestText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddLoanDialog = false },
            title = { Text("Novo Empréstimo / Financiamento / Renegociação") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ideal para contratos de parcelas longas (12x, 36x, 48x, 60x, 120x, 360x) ou acordos de cartão de crédito.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    LoanType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedLoanType == type.label,
                            onClick = { selectedLoanType = type.label },
                            label = { Text(type.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    OutlinedTextField(
                        value = loanTitle,
                        onValueChange = { loanTitle = it },
                        label = { Text("Descrição (ex: Financiamento Carro / Renegociação Nubank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = installmentAmtText,
                        onValueChange = { installmentAmtText = it },
                        label = { Text("Valor Fixo da Parcela Mensal (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = totalInstallmentsText,
                            onValueChange = { totalInstallmentsText = it },
                            label = { Text("Total Parcelas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paidInstallmentsText,
                            onValueChange = { paidInstallmentsText = it },
                            label = { Text("Já Pagas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { dueDayText = it },
                            label = { Text("Dia Venc.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = interestText,
                            onValueChange = { interestText = it },
                            label = { Text("Juros % a.m. (opc.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val instAmt = installmentAmtText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val totalInst = totalInstallmentsText.toIntOrNull()?.coerceIn(1, 480) ?: 12
                        val paidInst = paidInstallmentsText.toIntOrNull()?.coerceIn(0, totalInst) ?: 0
                        val dueDay = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 15
                        val interest = interestText.replace(",", ".").toDoubleOrNull() ?: 0.0

                        if (loanTitle.isNotBlank() && instAmt > 0.0) {
                            onSaveLoanContract(
                                LoanContract(
                                    title = loanTitle.trim(),
                                    loanType = selectedLoanType,
                                    installmentAmount = instAmt,
                                    totalInstallments = totalInst,
                                    paidInstallments = paidInst,
                                    dueDay = dueDay,
                                    interestRateMonthly = interest
                                )
                            )
                            showAddLoanDialog = false
                        }
                    }
                ) {
                    Text("Salvar Contrato")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLoanDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Modal: Amortizar / Antecipar Parcelas com Desconto ---
    if (loanToAmortizeDialog != null) {
        val loan = loanToAmortizeDialog!!
        var extraCountText by remember(loan) { mutableStateOf("1") }
        var paidValueText by remember(loan) { mutableStateOf(loan.installmentAmount.toString()) }

        AlertDialog(
            onDismissRequest = { loanToAmortizeDialog = null },
            title = { Text("Amortizar / Antecipar Parcelas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Antecipou parcelas do final de '${loan.title}' com desconto? Informe quantas parcelas foram abatidas e o valor total pago com desconto:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = extraCountText,
                        onValueChange = {
                            extraCountText = it
                            val c = it.toIntOrNull() ?: 1
                            paidValueText = (loan.installmentAmount * c).toString()
                        },
                        label = { Text("Quantidade de parcelas amortizadas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paidValueText,
                        onValueChange = { paidValueText = it },
                        label = { Text("Valor Efetivamente Pago com Desconto (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = extraCountText.toIntOrNull()?.coerceIn(1, loan.remainingInstallments.coerceAtLeast(1)) ?: 1
                        val valuePaid = paidValueText.replace(",", ".").toDoubleOrNull() ?: (loan.installmentAmount * count)
                        onPayOrAmortizeLoan(loan, count, valuePaid, monthKey, true)
                        loanToAmortizeDialog = null
                    }
                ) {
                    Text("Confirmar Amortização")
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToAmortizeDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Modal: Nova Caixinha / Meta de Economia ---
    if (showAddGoalDialog) {
        var goalTitle by remember { mutableStateOf("") }
        var targetAmtText by remember { mutableStateOf("") }
        var currentAmtText by remember { mutableStateOf("0") }
        var deadlineNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Nova Caixinha / Meta Financeira") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Objetivo (ex: Reserva de Emergência, Quitar Moto)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetAmtText,
                        onValueChange = { targetAmtText = it },
                        label = { Text("Valor da Meta (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentAmtText,
                        onValueChange = { currentAmtText = it },
                        label = { Text("Já Guardado (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deadlineNote,
                        onValueChange = { deadlineNote = it },
                        label = { Text("Prazo / Observação (ex: Até Dezembro)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetAmtText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val curr = currentAmtText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (goalTitle.isNotBlank() && target > 0.0) {
                            onSaveSavingsGoal(
                                SavingsGoal(
                                    title = goalTitle.trim(),
                                    targetAmount = target,
                                    currentAmount = curr,
                                    deadlineNote = deadlineNote.trim()
                                )
                            )
                            showAddGoalDialog = false
                        }
                    }
                ) {
                    Text("Criar Caixinha")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Modal: Guardar Dinheiro na Caixinha ---
    if (goalToDepositDialog != null) {
        val goal = goalToDepositDialog!!
        var addValueText by remember(goal) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { goalToDepositDialog = null },
            title = { Text("Guardar em ${goal.title}") },
            text = {
                OutlinedTextField(
                    value = addValueText,
                    onValueChange = { addValueText = it },
                    label = { Text("Valor para adicionar (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val add = addValueText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (add > 0.0) {
                            onSaveSavingsGoal(goal.copy(currentAmount = goal.currentAmount + add))
                            goalToDepositDialog = null
                        }
                    }
                ) {
                    Text("Adicionar Saldo")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDepositDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val totalMonthlyDebtCommitment = remember(loans) {
        loans.filter { it.remainingInstallments > 0 }.sumOf { it.installmentAmount }
    }
    val totalRemainingDebtBalance = remember(loans) {
        loans.sumOf { it.remainingBalance }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Abas Internas: Contas Fixas (Água/Luz) | Empréstimos & Renegociações | Caixinhas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 },
                    label = { Text("Contas Fixas (${fixedBills.size})") }
                )
                FilterChip(
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 },
                    label = { Text("Dívidas & Parcelas (${loans.size})") }
                )
                FilterChip(
                    selected = activeSection == 2,
                    onClick = { activeSection = 2 },
                    label = { Text("Caixinhas (${savingsGoals.size})") }
                )
            }
        }

        // =========================================================================
        // SEÇÃO 0: CONTAS FIXAS MENSAIS COM VALOR VARIÁVEL (ÁGUA, LUZ, INTERNET...)
        // =========================================================================
        if (activeSection == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Contas Fixas de $currentMonthLabel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Suas contas fixas (Água, Luz, Internet, Aluguel) ficam salvas aqui todo mês. Quando a conta chegar, toque em 'Informar Valor' para lançar o valor exato daquele mês!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showAddFixedBillDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nova Fixa")
                        }
                    }
                }
            }

            if (fixedBills.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma conta fixa cadastrada ainda",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toque em '+ Nova Fixa' acima para cadastrar Água, Luz, Internet ou Aluguel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(fixedBills, key = { it.id }) { bill ->
                    val isPaid = bill.isPaidInMonth(monthKey)
                    val monthVal = bill.amountForMonth(monthKey)
                    val avgPrev = bill.averagePreviousAmount(monthKey)
                    val diffFromAvg = if (monthVal > 0 && avgPrev > 0) monthVal - avgPrev else 0.0

                    val statusBadgeText = when {
                        isPaid -> "🟢 Paga em $currentMonthLabel"
                        todayDay > bill.dueDay -> "🔴 Venceu dia ${bill.dueDay}"
                        (bill.dueDay - todayDay) in 0..3 -> "🟡 Vence dia ${bill.dueDay} (em breve)"
                        else -> "⏳ Vence dia ${bill.dueDay}"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bill.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = statusBadgeText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (monthVal > 0) currencyFormatter.format(monthVal) else "Valor pendente",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) IncomeGreen else ExpenseRed
                                    )
                                    IconButton(onClick = { onDeleteFixedBill(bill.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remover molde",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (abs(diffFromAvg) >= 1.0 && isPaid) {
                                val comparisonText = if (diffFromAvg > 0) {
                                    "💡 Veio ${currencyFormatter.format(diffFromAvg)} acima da média anterior (${currencyFormatter.format(avgPrev)})"
                                } else {
                                    "🎉 Você economizou ${currencyFormatter.format(abs(diffFromAvg))} comparado à média (${currencyFormatter.format(avgPrev)})!"
                                }
                                Text(
                                    text = comparisonText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (diffFromAvg > 0) ExpenseRed else IncomeGreen
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { billToPayDialog = bill },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isPaid) "Atualizar Valor de $currentMonthLabel"
                                        else "Informar Valor / Pagar Mês"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SEÇÃO 1: EMPRÉSTIMOS, FINANCIAMENTOS E RENEGOCIAÇÃO DE CARTÃO (LONGO PRAZO)
        // =========================================================================
        if (activeSection == 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Empréstimos, Financiamentos & Renegociações",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Parcelas mensais comprometidas: ${currencyFormatter.format(totalMonthlyDebtCommitment)}\nSaldo devedor total restante: ${currencyFormatter.format(totalRemainingDebtBalance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = { showAddLoanDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adicionar")
                            }
                        }

                        if (loans.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onSimulatePayoffInAiChat,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AiPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simular Plano de Quitação Rápida com Gemini IA")
                            }
                        }
                    }
                }
            }

            if (loans.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum empréstimo ou financiamento cadastrado",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toque em '+ Adicionar' para controlar financiamentos longos (carro, moto, casa), empréstimos ou renegociações de cartão.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(loans, key = { it.id }) { loan ->
                    val isPaidThisMonth = loan.isPaidInMonth(monthKey)
                    val pct = (loan.progressFraction * 100).toInt()

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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = loan.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${loan.loanType} • Vence dia ${loan.dueDay}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteLoanContract(loan.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Excluir contrato"
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Parcela: ${currencyFormatter.format(loan.installmentAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ExpenseRed
                                )
                                Text(
                                    text = "Pagas: ${loan.paidInstallments}/${loan.totalInstallments} ($pct%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { loan.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Faltam ${loan.remainingInstallments} parcelas • Saldo restante: ${currencyFormatter.format(loan.remainingBalance)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (loan.remainingInstallments > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onPayOrAmortizeLoan(
                                                loan,
                                                1,
                                                loan.installmentAmount,
                                                monthKey,
                                                false
                                            )
                                        },
                                        enabled = !isPaidThisMonth,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (isPaidThisMonth) "Paga no Mês ✅"
                                            else "Pagar Parcela ${loan.paidInstallments + 1}"
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { loanToAmortizeDialog = loan },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Amortizar")
                                    }
                                }
                            } else {
                                Text(
                                    text = "🎉 Contrato 100% Quitado! Parabéns!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SEÇÃO 2: CAIXINHAS / METAS DE ECONOMIA
        // =========================================================================
        if (activeSection == 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Caixinhas & Metas de Economia",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Acompanhe sua Reserva de Emergência, fundo para amortizar dívidas ou objetivos pessoais.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showAddGoalDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nova Meta")
                        }
                    }
                }
            }

            if (savingsGoals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma caixinha criada ainda",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Crie metas como 'Reserva de Emergência' ou 'Quitar Cartão' para visualizar seu progresso.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(savingsGoals, key = { it.id }) { goal ->
                    val pct = (goal.progressFraction * 100).toInt()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (goal.deadlineNote.isNotBlank()) {
                                        Text(
                                            text = goal.deadlineNote,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteSavingsGoal(goal.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir meta")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Guardado: ${currencyFormatter.format(goal.currentAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                                Text(
                                    text = "Meta: ${currencyFormatter.format(goal.targetAmount)} ($pct%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LinearProgressIndicator(
                                progress = { goal.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = IncomeGreen
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { goalToDepositDialog = goal },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Guardar Valor")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
