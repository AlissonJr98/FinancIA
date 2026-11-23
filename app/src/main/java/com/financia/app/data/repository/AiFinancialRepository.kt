package com.financia.app.data.repository

import android.graphics.Bitmap
import com.financia.app.data.model.AiExtractedTransaction
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.TransactionCategory
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiFinancialRepository @Inject constructor(
    private val firebaseAi: FirebaseAI
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        private const val MODEL_NAME = "gemini-2.5-flash"
    }

    private val availableCategories = TransactionCategory.entries.joinToString(", ") { it.label }

    private val extractionSystemPrompt = """
        Você é o motor de inteligência financeira do aplicativo FinancIA.
        Sua tarefa é extrair informações estruturadas de uma transação financeira descrita pelo usuário (por texto, voz ou imagem de comprovante/nota fiscal).
        Responda APENAS com um objeto JSON válido (sem blocos markdown ```json) com exatamente estas chaves:
        {
          "title": "Nome curto e claro do gasto ou receita (ex: Supermercado Extra, Uber, Salário)",
          "amount": 45.90,
          "type": "EXPENSE" ou "INCOME",
          "category": "Uma das categorias permitidas: $availableCategories",
          "paymentMethod": "Pix, Cartão de Crédito, Cartão de Débito, Dinheiro ou Boleto",
          "notes": "Resumo ou observação adicional extraída"
        }
        Regras:
        - Se o usuário disser 'gastei', 'paguei', 'comprei', type deve ser "EXPENSE".
        - Se o usuário disser 'recebi', 'ganhei', 'salário', 'pix recebido', type deve ser "INCOME".
        - Escolha sempre a categoria mais adequada da lista: $availableCategories.
        - O campo amount deve ser um número positivo (ex: 45.50).
    """.trimIndent()

    /**
     * Extrai transação a partir de linguagem natural (texto ou transcrição de voz).
     */
    suspend fun parseNaturalLanguageTransaction(userInput: String): Result<AiExtractedTransaction> = runCatching {
        val model = firebaseAi.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
            }
        )

        val prompt = """
            $extractionSystemPrompt

            Entrada do usuário: "$userInput"
        """.trimIndent()

        val response = model.generateContent(prompt)
        val rawText = response.text?.trim() ?: error("A IA não retornou resposta.")
        parseJsonSafely(rawText, fallbackInput = userInput)
    }

    /**
     * Extrai transação a partir de foto de comprovante ou nota fiscal (Visão Computacional).
     */
    suspend fun parseReceiptImage(bitmap: Bitmap, optionalHint: String = ""): Result<AiExtractedTransaction> = runCatching {
        val model = firebaseAi.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
            }
        )

        val promptText = buildString {
            appendLine(extractionSystemPrompt)
            appendLine("Analise a imagem deste comprovante, cupom fiscal ou recibo e extraia o estabelecimento (title), valor total pago (amount), categoria sugerida (category), forma de pagamento (paymentMethod) e itens principais em notes.")
            if (optionalHint.isNotBlank()) {
                appendLine("Observação do usuário: $optionalHint")
            }
        }

        val response = model.generateContent(
            content {
                image(bitmap)
                text(promptText)
            }
        )
        val rawText = response.text?.trim() ?: error("Não foi possível ler o comprovante.")
        parseJsonSafely(rawText, fallbackInput = "Comprovante")
    }

    /**
     *Sugere automaticamente a categoria com base apenas no título/descrição.
     */
    suspend fun suggestCategory(title: String): Result<TransactionCategory> = runCatching {
        val model = firebaseAi.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.0f
            }
        )
        val prompt = """
            Classifique a transação "$title" em EXATAMENTE UMA das seguintes categorias:
            $availableCategories
            Responda apenas com o nome exato da categoria.
        """.trimIndent()

        val responseText = model.generateContent(prompt).text?.trim() ?: "Outros"
        TransactionCategory.fromLabelOrName(responseText)
    }

    /**
     * Assistente Financeiro (Chat IA) com contexto em tempo real das transações do usuário.
     */
    suspend fun askFinancialAdvisor(
        userQuestion: String,
        transactions: List<FinancialTransaction>
    ): Result<String> = runCatching {
        val brlFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val expensesByCategory = transactions
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (cat, sum) -> "- $cat: ${brlFormat.format(sum)}" }
            .ifBlank { "Nenhuma despesa registrada ainda." }

        val recentTxSummary = transactions.take(20).joinToString("\n") { tx ->
            val sign = if (tx.isIncome) "+" else "-"
            "• ${tx.title} (${tx.category}): $sign ${brlFormat.format(tx.amount)} [${tx.paymentMethod}]"
        }.ifBlank { "Nenhuma transação recente." }

        val model = firebaseAi.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.4f
            }
        )

        val fullPrompt = """
            Você é o FinancIA, um consultor financeiro pessoal inteligente, amigável, direto e prático no Brasil.
            Responda em Português do Brasil (pt-BR), usando formatação clara com tópicos curtos quando útil.

            === CONTEXTO FINANCEIRO ATUAL DO USUÁRIO ===
            • Saldo Atual: ${brlFormat.format(balance)}
            • Total de Receitas: ${brlFormat.format(totalIncome)}
            • Total de Despesas: ${brlFormat.format(totalExpense)}

            === DESPESAS POR CATEGORIA ===
            $expensesByCategory

            === ÚLTIMAS TRANSAÇÕES ===
            $recentTxSummary

            === PERGUNTA DO USUÁRIO ===
            $userQuestion
        """.trimIndent()

        val response = model.generateContent(fullPrompt)
        response.text?.trim() ?: "Não consegui gerar uma análise no momento. Tente novamente!"
    }

    private fun parseJsonSafely(rawText: String, fallbackInput: String): AiExtractedTransaction {
        val cleaned = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return runCatching {
            val parsed = json.decodeFromString<AiExtractedTransaction>(cleaned)
            val validCategory = TransactionCategory.fromLabelOrName(parsed.category).label
            val normalizedType = if (parsed.type.uppercase() == "INCOME") "INCOME" else "EXPENSE"
            parsed.copy(
                title = parsed.title.ifBlank { fallbackInput.take(40) },
                category = validCategory,
                type = normalizedType
            )
        }.getOrElse {
            fallbackHeuristicParse(fallbackInput)
        }
    }

    /**
     * Fallback local caso o serviço de IA ainda não esteja habilitado no console ou sem internet.
     */
    fun fallbackHeuristicParse(input: String): AiExtractedTransaction {
        val lower = input.lowercase()
        val numberRegex = Regex("""(\d+[.,]?\d*)""")
        val matchedAmount = numberRegex.find(input)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

        val isIncome = listOf("recebi", "ganhei", "salário", "salario", "renda", "freela", "dividendo")
            .any { lower.contains(it) }

        val category = when {
            listOf("mercado", "ifood", "restaurante", "lanche", "padaria", "almoço", "jantar", "café").any { lower.contains(it) } ->
                TransactionCategory.ALIMENTACAO.label
            listOf("uber", "99", "gasolina", "combustível", "ônibus", "metrô", "estacionamento").any { lower.contains(it) } ->
                TransactionCategory.TRANSPORTE.label
            listOf("aluguel", "luz", "água", "internet", "condomínio", "energia").any { lower.contains(it) } ->
                TransactionCategory.MORADIA.label
            listOf("farmácia", "médico", "consulta", "exame", "academia", "remédio").any { lower.contains(it) } ->
                TransactionCategory.SAUDE.label
            listOf("netflix", "spotify", "assinatura", "prime", "youtube").any { lower.contains(it) } ->
                TransactionCategory.ASSINATURAS.label
            listOf("cinema", "bar", "viagem", "show", "jogo").any { lower.contains(it) } ->
                TransactionCategory.LAZER.label
            listOf("curso", "livro", "faculdade", "escola").any { lower.contains(it) } ->
                TransactionCategory.EDUCACAO.label
            listOf("salário", "salario", "pagamento").any { lower.contains(it) } ->
                TransactionCategory.SALARIO.label
            listOf("investimento", "cdb", "tesouro", "ação", "fii").any { lower.contains(it) } ->
                TransactionCategory.INVESTIMENTOS.label
            else -> TransactionCategory.OUTROS.label
        }

        val paymentMethod = when {
            lower.contains("crédito") || lower.contains("credito") || lower.contains("cartão") -> "Cartão de Crédito"
            lower.contains("débito") || lower.contains("debito") -> "Cartão de Débito"
            lower.contains("dinheiro") -> "Dinheiro"
            lower.contains("boleto") -> "Boleto"
            else -> "Pix"
        }

        val cleanedTitle = input
            .replace(Regex("""(?i)\b(gastei|paguei|comprei|recebi|ganhei|no|na|de|reais|r\$)\b"""), " ")
            .replace(numberRegex, " ")
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .ifBlank { if (isIncome) "Nova Receita" else "Nova Despesa" }

        return AiExtractedTransaction(
            title = cleanedTitle,
            amount = matchedAmount,
            type = if (isIncome) "INCOME" else "EXPENSE",
            category = category,
            paymentMethod = paymentMethod,
            notes = "Extraído de: \"$input\""
        )
    }
}
