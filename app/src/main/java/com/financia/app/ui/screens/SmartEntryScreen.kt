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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.financia.app.data.model.AiExtractedTransaction
import com.financia.app.data.model.TransactionCategory
import com.financia.app.ui.theme.AiPurple

@OptIn(ExperimentalLayoutApi::class)
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
        createdByAi: Boolean
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
    var wasPopulatedByAi by remember { mutableStateOf(false) }

    // Preenche formulário quando a IA extrai dados de texto/voz/foto
    LaunchedEffect(pendingAiDraft) {
        if (pendingAiDraft != null) {
            title = pendingAiDraft.title
            amountText = if (pendingAiDraft.amount > 0) pendingAiDraft.amount.toString() else ""
            isIncome = pendingAiDraft.type.equals("INCOME", ignoreCase = true)
            selectedCategory = pendingAiDraft.category
            selectedPayment = pendingAiDraft.paymentMethod
            notes = pendingAiDraft.notes
            wasPopulatedByAi = true
        }
    }

    // Reconhecimento de Voz Nativo do Android
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

    // Câmera Rápida para Foto de Comprovante / Nota Fiscal
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            onAnalyzeReceiptBitmap(bitmap)
        }
    }

    // Galeria para Foto de Comprovante / Nota Fiscal
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

    val paymentMethods = listOf("Pix", "Cartão de Crédito", "Cartão de Débito", "Dinheiro", "Boleto")

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
            shape = RoundedCornerShape(20.dp),
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
                    text = "Descreva seu gasto/receita naturalmente, use o microfone ou tire foto do comprovante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = naturalInput,
                    onValueChange = { naturalInput = it },
                    label = { Text("Ex: Gastei 45,90 no mercado no Pix") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Exemplos clicáveis rápidos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val samples = listOf(
                        "Gastei 45 no mercado no Pix",
                        "Uber 24,90 no cartão de crédito",
                        "Recebi 3500 de salário",
                        "Almoço restaurante 38,50"
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAnalyzeText(naturalInput) },
                        enabled = !isProcessingAi && naturalInput.isNotBlank(),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (isProcessingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Interpretar")
                        }
                    }

                    OutlinedButton(
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
                        },
                        enabled = !isProcessingAi
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Falar")
                    }

                    OutlinedButton(
                        onClick = { cameraPreviewLauncher.launch() },
                        enabled = !isProcessingAi
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Câmera Comprovante")
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingAi
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Galeria Comprovante")
                    }
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

        // Bloco 2: Confirmação / Edição Manual com Auto-Categorização
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
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
                    text = "Dados do Lançamento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Seletor Despesa vs Receita
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Despesa (-)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Receita (+)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descrição (ex: Mercado, Uber, Salário)") },
                    trailingIcon = {
                        if (title.length >= 3) {
                            TextButton(
                                onClick = {
                                    onAutoCategorize(title) { suggested ->
                                        selectedCategory = suggested.label
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
                                Text("IA", color = AiPurple)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory.equals(category.label, ignoreCase = true),
                            onClick = { selectedCategory = category.label },
                            label = { Text(category.label) }
                        )
                    }
                }

                Text(
                    text = "Forma de Pagamento",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        FilterChip(
                            selected = selectedPayment.equals(method, ignoreCase = true),
                            onClick = { selectedPayment = method },
                            label = { Text(method) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        onSaveTransaction(
                            title,
                            parsedAmount,
                            isIncome,
                            selectedCategory,
                            selectedPayment,
                            notes,
                            wasPopulatedByAi
                        )
                        onTransactionSavedNavigateHome()
                    },
                    enabled = title.isNotBlank() && parsedAmount > 0.0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Lançamento no Firestore")
                }
            }
        }
    }
}
