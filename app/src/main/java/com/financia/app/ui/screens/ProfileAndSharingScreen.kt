package com.financia.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.financia.app.core.notifications.BankNotificationService
import com.financia.app.data.model.AchievementBadge
import com.financia.app.data.model.AppCurrency
import com.financia.app.data.model.AppLanguage
import com.financia.app.data.model.CoupleSplitMode
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.UserProfileConfig
import com.financia.app.ui.components.FinancIADropdownField
import com.financia.app.ui.components.FinancIAPrimaryButton
import com.financia.app.ui.components.FinancIASecondaryButton
import com.financia.app.ui.components.FinancIATextField
import com.financia.app.ui.theme.AiPurple
import com.financia.app.ui.theme.AppThemeMode
import com.financia.app.ui.theme.EmeraldDark
import com.financia.app.ui.theme.EmeraldPrimary
import com.financia.app.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndSharingScreen(
    profileConfig: UserProfileConfig,
    monthTransactions: List<FinancialTransaction>,
    achievements: List<AchievementBadge>,
    isViewingSharedSpace: Boolean,
    isPrivacyMode: Boolean,
    isBiometricEnabled: Boolean,
    themeMode: AppThemeMode,
    onSaveProfile: (UserProfileConfig) -> Unit,
    onCreateOrJoinSharedSpace: (String, String) -> Unit,
    onLeaveSharedSpace: () -> Unit,
    onToggleSharedSpaceView: () -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onToggleBiometric: () -> Unit,
    onCycleThemeMode: () -> Unit,
    onRunCoupleAiMeeting: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currency = remember(profileConfig.currencyCode) {
        AppCurrency.fromCode(profileConfig.currencyCode)
    }
    val currencyFormatter = remember(currency) {
        NumberFormat.getCurrencyInstance(currency.locale)
    }

    var displayName by remember(profileConfig.displayName) { mutableStateOf(profileConfig.displayName) }
    var selectedLanguageLabel by remember(profileConfig.languageCode) {
        mutableStateOf(AppLanguage.fromCode(profileConfig.languageCode).let { "${it.flagEmoji} ${it.label}" })
    }
    var selectedCurrencyLabel by remember(profileConfig.currencyCode) {
        mutableStateOf(AppCurrency.fromCode(profileConfig.currencyCode).label)
    }
    var baseSalaryText by remember(profileConfig.baseSalary) {
        mutableStateOf(if (profileConfig.baseSalary > 0) profileConfig.baseSalary.toString() else "")
    }
    var partnerSalaryText by remember(profileConfig.partnerSalary) {
        mutableStateOf(if (profileConfig.partnerSalary > 0) profileConfig.partnerSalary.toString() else "")
    }
    var payDayText by remember(profileConfig.payDay) {
        mutableStateOf(profileConfig.payDay.toString())
    }
    var selectedSplitModeLabel by remember(profileConfig.coupleSplitMode) {
        val mode = runCatching { CoupleSplitMode.valueOf(profileConfig.coupleSplitMode) }
            .getOrDefault(CoupleSplitMode.FIFTY_FIFTY)
        mutableStateOf(mode.label)
    }

    var inviteCodeInput by remember(profileConfig.sharedSpaceId) {
        mutableStateOf(profileConfig.sharedSpaceId)
    }
    var newCustomAccountInput by remember { mutableStateOf("") }
    var newCustomCategoryInput by remember { mutableStateOf("") }

    LaunchedEffect(profileConfig) {
        displayName = profileConfig.displayName
        inviteCodeInput = profileConfig.sharedSpaceId
    }

    val isNotificationAccessEnabled = remember(monthTransactions) {
        BankNotificationService.isNotificationListenerEnabled(context)
    }

    // Cálculo Automático de Divisão de Despesas do Casal (50/50 vs. Proporcional à Renda)
    val totalMonthExpenses = remember(monthTransactions) {
        monthTransactions.filter { !it.isIncome }.sumOf { it.amount }
    }
    val expensesByAuthor = remember(monthTransactions) {
        monthTransactions
            .filter { !it.isIncome }
            .groupBy { it.authorName.ifBlank { profileConfig.displayName } }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    val mySalary = baseSalaryText.replace(",", ".").toDoubleOrNull() ?: profileConfig.baseSalary
    val partnerSalary = partnerSalaryText.replace(",", ".").toDoubleOrNull() ?: profileConfig.partnerSalary
    val totalHouseholdSalary = mySalary + partnerSalary

    val myShareFraction = remember(selectedSplitModeLabel, mySalary, partnerSalary) {
        if (selectedSplitModeLabel.startsWith("Proporcional") && totalHouseholdSalary > 0.0) {
            (mySalary / totalHouseholdSalary).toFloat().coerceIn(0.05f, 0.95f)
        } else {
            0.5f
        }
    }
    val partnerShareFraction = 1f - myShareFraction

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // 1. CARTÃO PRINCIPAL DO PERFIL DO USUÁRIO
        // =========================================================================
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
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A"
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName.ifBlank { "Usuário FinancIA" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (profileConfig.email.isNotBlank()) {
                                Text(
                                    text = profileConfig.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (profileConfig.sharedSpaceId.isNotBlank()) {
                                    "💑 Modo Casal Ativo: ${profileConfig.sharedSpaceId}"
                                } else {
                                    "👤 Modo Individual • ${AppLanguage.fromCode(profileConfig.languageCode).flagEmoji} ${profileConfig.currencyCode}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. MODO CASAL / FAMÍLIA COMPARTILHADO EM TEMPO REAL
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEC4899)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo Casal & Família (Compartilhamento)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Conecte dois ou mais celulares ao mesmo espaço financeiro! Basta gerar um código abaixo e enviar para seu parceiro(a) colar no aplicativo dele(a). Todos os gastos, contas fixas (água/luz) e empréstimos sincronizam na hora!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (profileConfig.sharedSpaceId.isNotBlank()) {
                        // Painel quando o Casal já está conectado
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFEC4899).copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Código do Casal Conectado",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = profileConfig.sharedSpaceId,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFEC4899)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isViewingSharedSpace) "Vendo Casal" else "Vendo Pessoal",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = isViewingSharedSpace,
                                            onCheckedChange = { onToggleSharedSpaceView() }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FinancIASecondaryButton(
                                        text = "Convidar WhatsApp",
                                        icon = Icons.Default.Share,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            val msg = "Amor, criei nosso espaço financeiro compartilhado no FinancIA! 💑\n\n1. Baixe o app: https://github.com/AlissonJr98/FinancIA/releases/latest/download/FinancIA.apk\n2. Vá na aba 'Perfil' e digite nosso código: *${profileConfig.sharedSpaceId}*"
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, msg)
                                            }
                                            context.startActivity(
                                                Intent.createChooser(sendIntent, "Convidar parceiro(a) no WhatsApp").apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }
                                    )

                                    TextButton(onClick = onLeaveSharedSpace) {
                                        Text("Desconectar", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    } else {
                        // Conectar ou Gerar Novo Código de Casal
                        FinancIATextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it.uppercase() },
                            label = "Código do Espaço do Casal (ex: CASAL-4829)",
                            leadingIcon = Icons.Default.GroupAdd
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FinancIASecondaryButton(
                                text = "Gerar Código Novo",
                                icon = Icons.Default.AutoAwesome,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val randomNum = Random.nextInt(1000, 9999)
                                    val newCode = "CASAL-$randomNum"
                                    inviteCodeInput = newCode
                                    onCreateOrJoinSharedSpace(newCode, "")
                                    Toast.makeText(context, "Espaço $newCode criado e ativado!", Toast.LENGTH_SHORT).show()
                                }
                            )

                            FinancIAPrimaryButton(
                                text = "Entrar no Código",
                                icon = Icons.Default.Check,
                                enabled = inviteCodeInput.trim().length >= 4,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onCreateOrJoinSharedSpace(inviteCodeInput.trim(), "")
                                    Toast.makeText(context, "Conectado ao espaço ${inviteCodeInput.trim()}!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Divisão Justa das Contas do Casal (50/50 ou Proporcional à Renda)
                    Text(
                        text = "Calculadora de Divisão Justa do Casal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    FinancIADropdownField(
                        label = "Regra de Divisão das Despesas",
                        selectedValue = selectedSplitModeLabel,
                        options = CoupleSplitMode.entries.map { it.label },
                        onOptionSelected = { selectedSplitModeLabel = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinancIATextField(
                            value = baseSalaryText,
                            onValueChange = { baseSalaryText = it },
                            label = "Minha Renda",
                            prefixText = "${currency.symbol} ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        FinancIATextField(
                            value = partnerSalaryText,
                            onValueChange = { partnerSalaryText = it },
                            label = "Renda Parceiro(a)",
                            prefixText = "${currency.symbol} ",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (totalMonthExpenses > 0.0) {
                        val myTargetContribution = totalMonthExpenses * myShareFraction
                        val partnerTargetContribution = totalMonthExpenses * partnerShareFraction
                        val myPct = (myShareFraction * 100).toInt()
                        val partnerPct = 100 - myPct

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Total de Despesas do Mês: ${currencyFormatter.format(totalMonthExpenses)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• Sua parte ($myPct%): ${currencyFormatter.format(myTargetContribution)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• Parceiro(a) ($partnerPct%): ${currencyFormatter.format(partnerTargetContribution)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFEC4899),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (expensesByAuthor.size >= 1) {
                                    val breakdownText = expensesByAuthor.entries.joinToString("  |  ") { (author, paid) ->
                                        "Pago por $author: ${currencyFormatter.format(paid)}"
                                    }
                                    Text(
                                        text = breakdownText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    FinancIAPrimaryButton(
                        text = "Fazer Reunião Financeira do Casal com Gemini IA",
                        icon = Icons.Default.AutoAwesome,
                        isAiStyle = true,
                        onClick = onRunCoupleAiMeeting
                    )
                }
            }
        }

        // =========================================================================
        // 3. IDIOMAS, MOEDA, DADOS DO PERFIL & DIA DO PAGAMENTO
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Idioma, Moeda & Dados Pessoais",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FinancIATextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Seu Nome / Apelido (aparece em 'Lançado por...')",
                        leadingIcon = Icons.Default.Person
                    )

                    FinancIADropdownField(
                        label = "Idioma do Aplicativo e da IA (Language)",
                        selectedValue = selectedLanguageLabel,
                        options = AppLanguage.entries.map { "${it.flagEmoji} ${it.label}" },
                        leadingIcon = Icons.Default.Language,
                        onOptionSelected = { selectedLanguageLabel = it }
                    )

                    FinancIADropdownField(
                        label = "Moeda Principal (Currency)",
                        selectedValue = selectedCurrencyLabel,
                        options = AppCurrency.entries.map { it.label },
                        leadingIcon = Icons.Default.Payments,
                        onOptionSelected = { selectedCurrencyLabel = it }
                    )

                    FinancIATextField(
                        value = payDayText,
                        onValueChange = { payDayText = it },
                        label = "Dia do Recebimento do Salário (1 a 31)",
                        leadingIcon = Icons.Default.Payments,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    FinancIAPrimaryButton(
                        text = "Salvar Configurações do Perfil",
                        icon = Icons.Default.Check,
                        onClick = {
                            val chosenLang = AppLanguage.entries.firstOrNull {
                                selectedLanguageLabel.contains(it.label)
                            } ?: AppLanguage.PT_BR
                            val chosenCurr = AppCurrency.entries.firstOrNull {
                                selectedCurrencyLabel.contains(it.code)
                            } ?: AppCurrency.BRL
                            val chosenSplit = CoupleSplitMode.entries.firstOrNull {
                                it.label == selectedSplitModeLabel
                            } ?: CoupleSplitMode.FIFTY_FIFTY

                            val updated = profileConfig.copy(
                                displayName = displayName.trim().ifBlank { "Alisson" },
                                languageCode = chosenLang.code,
                                currencyCode = chosenCurr.code,
                                baseSalary = baseSalaryText.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                partnerSalary = partnerSalaryText.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                payDay = payDayText.toIntOrNull()?.coerceIn(1, 31) ?: 5,
                                coupleSplitMode = chosenSplit.name
                            )
                            onSaveProfile(updated)
                            Toast.makeText(context, "Perfil e idioma atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // =========================================================================
        // 4. CONTAS BANCÁRIAS E CATEGORIAS PERSONALIZADAS
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contas Bancárias & Categorias Personalizadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Adicione contas personalizadas (ex: 'Nubank Esposa', 'Cartão XP', 'Cofre Casal') para aparecerem nas caixas de seleção:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FinancIATextField(
                            value = newCustomAccountInput,
                            onValueChange = { newCustomAccountInput = it },
                            label = "Nova Conta / Cartão",
                            modifier = Modifier.weight(1f)
                        )
                        FinancIASecondaryButton(
                            text = "Adicionar",
                            icon = Icons.Default.Add,
                            enabled = newCustomAccountInput.isNotBlank(),
                            onClick = {
                                val updatedList = (profileConfig.customAccounts + newCustomAccountInput.trim()).distinct()
                                onSaveProfile(profileConfig.copy(customAccounts = updatedList))
                                newCustomAccountInput = ""
                            }
                        )
                    }

                    if (profileConfig.customAccounts.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            profileConfig.customAccounts.forEach { acc ->
                                AssistChip(
                                    onClick = {
                                        val next = profileConfig.customAccounts - acc
                                        onSaveProfile(profileConfig.copy(customAccounts = next))
                                    },
                                    label = { Text("$acc ✕", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FinancIATextField(
                            value = newCustomCategoryInput,
                            onValueChange = { newCustomCategoryInput = it },
                            label = "Nova Categoria",
                            modifier = Modifier.weight(1f)
                        )
                        FinancIASecondaryButton(
                            text = "Adicionar",
                            icon = Icons.Default.Add,
                            enabled = newCustomCategoryInput.isNotBlank(),
                            onClick = {
                                val updatedList = (profileConfig.customCategories + newCustomCategoryInput.trim()).distinct()
                                onSaveProfile(profileConfig.copy(customCategories = updatedList))
                                newCustomCategoryInput = ""
                            }
                        )
                    }

                    if (profileConfig.customCategories.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            profileConfig.customCategories.forEach { cat ->
                                AssistChip(
                                    onClick = {
                                        val next = profileConfig.customCategories - cat
                                        onSaveProfile(profileConfig.copy(customCategories = next))
                                    },
                                    label = { Text("$cat ✕", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. CONQUISTAS & NÍVEL FINANCEIRO (GAMIFICAÇÃO)
        // =========================================================================
        item {
            val unlockedCount = achievements.count { it.isUnlocked }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
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
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Conquistas Financeiras ($unlockedCount/${achievements.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = {
                            if (achievements.isNotEmpty()) unlockedCount.toFloat() / achievements.size.toFloat()
                            else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFF59E0B)
                    )

                    achievements.forEach { badge ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (badge.isUnlocked) IncomeGreen.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = badge.emoji,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (badge.isUnlocked) IncomeGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = badge.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (badge.isUnlocked) "Desbloqueada ✅" else "Bloqueada 🔒",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 6. CENTRAL DE CONTROLES RÁPIDOS (SEGURANÇA, TEMA, PRIVACIDADE & BANCOS)
        // =========================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Controles do Sistema & Segurança",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Bloqueio por Biometria / Digital", fontWeight = FontWeight.SemiBold)
                                Text("Exigir digital ao abrir o aplicativo", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Switch(checked = isBiometricEnabled, onCheckedChange = { onToggleBiometric() })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Modo Privacidade (Ocultar Valores)", fontWeight = FontWeight.SemiBold)
                                Text("Esconder saldos com •••••• na tela inicial", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Switch(checked = isPrivacyMode, onCheckedChange = { onTogglePrivacyMode() })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AiPurple)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Captura Automática de Notificações Bancárias", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (isNotificationAccessEnabled) "Ativa (Nubank, Inter, Itaú...)"
                                    else "Toque para autorizar leitura de Pix e Cartões",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Switch(
                            checked = isNotificationAccessEnabled,
                            onCheckedChange = {
                                BankNotificationService.openNotificationAccessSettings(context)
                            }
                        )
                    }

                    FinancIASecondaryButton(
                        text = "Alternar Tema Visual (Atual: ${themeMode.label})",
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCycleThemeMode
                    )

                    FinancIASecondaryButton(
                        text = "Sair da Conta (Logout)",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSignOut
                    )
                }
            }
        }
    }
}
