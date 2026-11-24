package com.financia.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.financia.app.data.model.AiExtractedTransaction
import com.financia.app.data.model.ExpenseNature
import com.financia.app.data.model.TransactionCategory
import com.financia.app.ui.components.CategoryIconMapper
import com.financia.app.ui.components.FinancIADropdownField
import com.financia.app.ui.components.FinancIAPrimaryButton
import com.financia.app.ui.components.FinancIASecondaryButton
import com.financia.app.ui.components.FinancIATextField
import com.financia.app.ui.theme.AiPurple

@Composable
fun SmartEntryScreen(
    isProcessingAi: Boolean,
    aiStatusMessage: String?,
    pendingAiDraft: AiExtractedTransaction?,
    onAnalyzeText: (String) -> Unit,
    onAnalyzeReceiptBitmap: (Bitmap) -> Unit,
    onAutoCategorize: (String, (TransactionCategory) -> Unit) -> Unit,
    onSaveTransaction: (
        title: String,
        amount: Double,
        isIncome: Boolean,
        category: String,
        paymentMethod: String,
        notes: String,
        createdByAi: Boolean,
        isRecurring: Boolean,
        installmentCount: Int,
        expenseNature: String
    ) -> Unit,
    onTransactionSavedNavigateHome: () -> Unit
) {
    val context = LocalContext.current

    var naturalInput by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.ALIMENTACAO.label) }
    var selectedPayment by remember { mutableStateOf("Pix") }
    var notes by remember { mutableStateOf("") }
    var selectedNature by remember { mutableStateOf(ExpenseNature.VARIABLE) }
    var installmentCount by remember { mutableIntStateOf(1) }
    var customInstallmentText by remember { mutableStateOf("1") }
    var isCustomInstallment by remember { mutableStateOf(false) }
    var wasPopulatedByAi by remember { mutableStateOf(false) }

    val categoryOptions = remember { TransactionCategory.entries.map { it.label } }
    val natureOptions = remember { ExpenseNature.entries.map { it.label } }
    val paymentMethods = remember { listOf("Pix", "Cartão de Crédito", "Cartão de Débito", "Boleto", "Dinheiro") }
    val installmentDropdownOptions = remember {
        listOf(
            "À vista (1x)",
            "2x parcelas",
            "3x parcelas",
            "4x parcelas",
            "6x parcelas",
            "10x parcelas",
            "12x parcelas",
            "18x parcelas",
            "24x parcelas",
            "36x parcelas",
            "48x parcelas",
            "60x parcelas",
            "120x parcelas",
            "Personalizado (digitar nº)"
        )
    }

    LaunchedEffect(pendingAiDraft) {
        if (pendingAiDraft != null) {
            title = pendingAiDraft.title
            amountText = if (pendingAiDraft.amount > 0) pendingAiDraft.amount.toString() else ""
            isIncome = pendingAiDraft.type.equals("INCOME", ignoreCase = true)
            selectedCategory = pendingAiDraft.category
            selectedPayment = pendingAiDraft.paymentMethod
            notes = pendingAiDraft.notes
            selectedNature = runCatching {
                ExpenseNature.valueOf(pendingAiDraft.expenseNature)
            }.getOrDefault(if (pendingAiDraft.isRecurring) ExpenseNature.FIXED else ExpenseNature.VARIABLE)
            installmentCount = pendingAiDraft.installmentCount.coerceIn(1, 360)
            customInstallmentText = installmentCount.toString()
            wasPopulatedByAi = true
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                naturalInput = spokenText
                onAnalyzeText(spokenText)
            }
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            onAnalyzeReceiptBitmap(bitmap)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                onAnalyzeReceiptBitmap(bitmap)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bloco 1: Entrada Inteligente com Gemini IA
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AiPurple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lançamento Mágico com Gemini IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Digite sua frase, fale no microfone ou fotografe a conta/comprovante para preencher os seletores automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FinancIATextField(
                    value = naturalInput,
                    onValueChange = { naturalInput = it },
                    label = "Descreva seu gasto ou receita",
                    placeholder = "Ex: Conta de água 78,50 / Financiamento 18000 em 48x",
                    leadingIcon = Icons.Default.AutoAwesome,
                    singleLine = false,
                    maxLines = 3
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val samples = listOf(
                        "Conta de Água deste mês 84,90",
                        "Renegociação cartão 2400 em 24x",
                        "Gastei 45 no mercado no Pix",
                        "Recebi 3500 de salário"
                    )
                    samples.forEach { sample ->
                        AssistChip(
                            onClick = {
                                naturalInput = sample
                                onAnalyzeText(sample)
                            },
                            label = { Text(sample, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                FinancIAPrimaryButton(
                    text = if (isProcessingAi) "Interpretando com Gemini IA..." else "Interpretar Frase com IA",
                    onClick = { onAnalyzeText(naturalInput) },
                    icon = Icons.Default.AutoAwesome,
                    enabled = !isProcessingAi && naturalInput.isNotBlank(),
                    isAiStyle = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancIASecondaryButton(
                        text = "Voz",
                        icon = Icons.Default.Mic,
                        enabled = !isProcessingAi,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            runCatching {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale seu gasto ou receita...")
                                }
                                speechRecognizerLauncher.launch(intent)
                            }
                        }
                    )

                    FinancIASecondaryButton(
                        text = "Câmera",
                        icon = Icons.Default.CameraAlt,
                        enabled = !isProcessingAi,
                        modifier = Modifier.weight(1f),
                        onClick = { cameraPreviewLauncher.launch() }
                    )

                    FinancIASecondaryButton(
                        text = "Galeria",
                        icon = Icons.Default.Image,
                        enabled = !isProcessingAi,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                if (!aiStatusMessage.isNullOrBlank()) {
                    Text(
                        text = aiStatusMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = AiPurple,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bloco 2: Formulário com Caixas de Seleção Dropdown (Select Boxes)
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Dados do Lançamento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Seletor Segmentado Despesa vs Receita
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    ) {
                        Text("Despesa (-)", fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    ) {
                        Text("Receita (+)", fontWeight = FontWeight.Bold)
                    }
                }

                // Caixa de Texto: Descrição + Botão Auto-Categorizar IA
                FinancIATextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Descrição do Lançamento",
                    placeholder = "Ex: Conta de Água, Financiamento, Supermercado",
                    leadingIcon = Icons.Default.Description,
                    trailingContent = if (title.length >= 3) {
                        {
                            TextButton(
                                onClick = {
                                    onAutoCategorize(title) { suggested ->
                                        selectedCategory = suggested.label
                                        selectedNature = suggested.defaultNature
                                        if (suggested.isIncomeDefault) isIncome = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Auto-categorizar",
                                    tint = AiPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("IA", color = AiPurple, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else null
                )

                // Caixa de Texto: Valor em R$
                FinancIATextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = if (!isIncome && installmentCount > 1) {
                        "Valor Total (será dividido em ${installmentCount}x)"
                    } else {
                        "Valor do Lançamento"
                    },
                    prefixText = "R$ ",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Dropdown 1: Categoria (com ícones coloridos dentro da caixa de seleção)
                FinancIADropdownField(
                    label = "Selecionar Categoria",
                    selectedValue = selectedCategory,
                    options = categoryOptions,
                    onOptionSelected = { chosen ->
                        selectedCategory = chosen
                        val catEnum = TransactionCategory.fromLabelOrName(chosen)
                        selectedNature = catEnum.defaultNature
                    },
                    optionIconProvider = { CategoryIconMapper.getIcon(it) },
                    optionColorProvider = { Color(TransactionCategory.fromLabelOrName(it).colorHex) }
                )

                // Dropdown 2: Natureza da Despesa (Fixa, Variável ou Empréstimo/Financiamento)
                if (!isIncome) {
                    FinancIADropdownField(
                        label = "Tipo de Despesa (Fixa, Variável ou Empréstimo)",
                        selectedValue = selectedNature.label,
                        options = natureOptions,
                        leadingIcon = Icons.Default.Layers,
                        onOptionSelected = { chosenLabel ->
                            val found = ExpenseNature.entries.firstOrNull { it.label == chosenLabel }
                                ?: ExpenseNature.VARIABLE
                            selectedNature = found
                            if (found == ExpenseNature.DEBT &&
                                selectedCategory != TransactionCategory.EMPRESTIMOS.label &&
                                selectedCategory != TransactionCategory.RENEGOCIACAO.label
                            ) {
                                selectedCategory = TransactionCategory.EMPRESTIMOS.label
                            }
                        }
                    )
                }

                // Dropdown 3: Forma de Pagamento + Dropdown 4: Parcelamento Longo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinancIADropdownField(
                        label = "Forma de Pagamento",
                        selectedValue = selectedPayment,
                        options = paymentMethods,
                        leadingIcon = Icons.Default.Payment,
                        onOptionSelected = { selectedPayment = it },
                        modifier = Modifier.weight(1f)
                    )

                    if (!isIncome) {
                        val currentInstLabel = when {
                            isCustomInstallment -> "Personalizado (${installmentCount}x)"
                            installmentCount == 1 -> "À vista (1x)"
                            else -> "${installmentCount}x parcelas"
                        }
                        FinancIADropdownField(
                            label = "Parcelamento",
                            selectedValue = currentInstLabel,
                            options = installmentDropdownOptions,
                            leadingIcon = Icons.Default.CalendarViewMonth,
                            onOptionSelected = { option ->
                                if (option.startsWith("Personalizado")) {
                                    isCustomInstallment = true
                                } else {
                                    isCustomInstallment = false
                                    val digits = Regex("""(\d+)""").find(option)?.value?.toIntOrNull() ?: 1
                                    installmentCount = digits.coerceIn(1, 360)
                                    customInstallmentText = installmentCount.toString()
                                    if (installmentCount > 1 && selectedNature != ExpenseNature.DEBT) {
                                        selectedPayment = "Cartão de Crédito"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!isIncome && isCustomInstallment) {
                    FinancIATextField(
                        value = customInstallmentText,
                        onValueChange = {
                            customInstallmentText = it
                            val parsed = it.toIntOrNull()?.coerceIn(1, 360)
                            if (parsed != null) installmentCount = parsed
                        },
                        label = "Digite o número exato de parcelas (1 a 360)",
                        leadingIcon = Icons.Default.CalendarViewMonth,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Caixa de Texto: Observações
                FinancIATextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Observações (opcional)",
                    leadingIcon = Icons.Default.Notes,
                    singleLine = false,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                FinancIAPrimaryButton(
                    text = if (!isIncome && installmentCount > 1) {
                        "Salvar ${installmentCount} Parcelas no Firestore"
                    } else {
                        "Confirmar e Salvar Lançamento"
                    },
                    icon = Icons.Default.Check,
                    enabled = title.isNotBlank() && parsedAmount > 0.0,
                    onClick = {
                        onSaveTransaction(
                            title,
                            parsedAmount,
                            isIncome,
                            selectedCategory,
                            selectedPayment,
                            notes,
                            wasPopulatedByAi,
                            selectedNature == ExpenseNature.FIXED && installmentCount == 1,
                            if (isIncome) 1 else installmentCount,
                            selectedNature.name
                        )
                        onTransactionSavedNavigateHome()
                    }
                )
            }
        }
    }
}
