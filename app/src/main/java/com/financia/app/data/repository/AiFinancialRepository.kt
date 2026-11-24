package com.financia.app.data.repository

import android.graphics.Bitmap
import com.financia.app.data.model.AiChatActionResponse
import com.financia.app.data.model.AiExtractedTransaction
import com.financia.app.data.model.ExpenseNature
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.FixedBillTemplate
import com.financia.app.data.model.LoanContract
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
        Sua tarefa é extrair informações estruturadas de uma transação financeira descrita pelo usuário (por texto, voz, imagem de comprovante ou notificação bancária).
        Responda APENAS com um objeto JSON válido (sem blocos markdown ```json) com exatamente estas chaves:
        {
          "title": "Nome curto e claro do gasto ou receita (ex: Supermercado Extra, Conta de Água, Parcela Financiamento Moto)",
          "amount": 45.90,
          "type": "EXPENSE" ou "INCOME",
          "category": "Uma das categorias permitidas: $availableCategories",
          "paymentMethod": "Pix, Cartão de Crédito, Cartão de Débito, Dinheiro ou Boleto",
          "notes": "Resumo ou observação adicional extraída",
          "isRecurring": false,
          "installmentCount": 1,
          "expenseNature": "FIXED", "VARIABLE" ou "DEBT"
        }
        Regras:
        - Se for conta fixa mensal (Água, Luz, Energia, Internet, Aluguel, Condomínio, Gás, Assinatura), use expenseNature = "FIXED".
        - Se for empréstimo, financiamento (carro, moto, casa), renegociação de cartão ou parcelamento de fatura, use expenseNature = "DEBT" e a categoria correspondente ("Empréstimos & Financiamentos" ou "Renegociação / Cartão").
        - Para gastos do dia a dia (Mercado, Uber, Lazer, Compras), use expenseNature = "VARIABLE".
        - Se mencionar 'parcelado em X vezes' ou 'Xx' (até 360x), preencha installmentCount com X e amount com o valor TOTAL.
    """.trimIndent()

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
            appendLine("Analise a imagem deste comprovante, boleto de água/luz/empréstimo ou cupom fiscal e extraia o estabelecimento/conta (title), valor total pago (amount), categoria sugerida (category), natureza (FIXED, VARIABLE ou DEBT), forma de pagamento (paymentMethod) e detalhes em notes.")
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
     * Consultor Financeiro & Simulador de Quitação de Dívidas (Efeito Bola de Neve / Avalanche):
     * Analisa transações, contas fixas (Água/Luz) e contratos de empréstimos/renegociações.
     */
    suspend fun askFinancialAdvisorWithActions(
        userQuestion: String,
        transactions: List<FinancialTransaction>,
        fixedBills: List<FixedBillTemplate>,
        loans: List<LoanContract>,
        monthlyBudget: Double
    ): Result<AiChatActionResponse> = runCatching {
        val brlFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val fixedAndDebtExpense = transactions.filter { !it.isIncome && it.isFixedOrDebt }.sumOf { it.amount }
        val variableExpense = (totalExpense - fixedAndDebtExpense).coerceAtLeast(0.0)
        val balance = totalIncome - totalExpense

        val loansSummary = loans.joinToString("\n") { loan ->
            "• ${loan.title} [${loan.loanType}]: Parcela ${brlFormat.format(loan.installmentAmount)} (${loan.paidInstallments}/${loan.totalInstallments} pagas, restam ${loan.remainingInstallments}x = Saldo Devedor ${brlFormat.format(loan.remainingBalance)}, Juros: ${loan.interestRateMonthly}% a.m.)"
        }.ifBlank { "Nenhum empréstimo, financiamento ou renegociação ativa." }

        val fixedBillsSummary = fixedBills.joinToString("\n") { bill ->
            "• ${bill.name} (Vence dia ${bill.dueDay}): Valor base/médio ${brlFormat.format(bill.defaultAmount)}"
        }.ifBlank { "Nenhuma conta fixa cadastrada." }

        val expensesByCategory = transactions
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (cat, sum) -> "- $cat: ${brlFormat.format(sum)}" }
            .ifBlank { "Nenhuma despesa registrada neste período." }

        val model = firebaseAi.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.3f
                responseMimeType = "application/json"
            }
        )

        val fullPrompt = """
            Você é o FinancIA, um consultor financeiro pessoal especialista em orçamento doméstico, contas fixas vs. variáveis e quitação inteligente de empréstimos, financiamentos e renegociações de cartão no Brasil.
            Responda APENAS com um objeto JSON válido no formato abaixo:
            {
              "reply": "Sua resposta amigável e estratégica em Português (pt-BR).",
              "transactionsToCreate": [
                {
                  "title": "Descrição",
                  "amount": 30.0,
                  "type": "EXPENSE" ou "INCOME",
                  "category": "Uma de: $availableCategories",
                  "paymentMethod": "Pix, Cartão de Crédito, Cartão de Débito, Dinheiro ou Boleto",
                  "notes": "Criado via Chat IA",
                  "isRecurring": false,
                  "installmentCount": 1,
                  "expenseNature": "FIXED", "VARIABLE" ou "DEBT"
                }
              ]
            }

            === CONTEXTO FINANCEIRO DO PERÍODO ===
            • Saldo Disponível: ${brlFormat.format(balance)}
            • Receitas: ${brlFormat.format(totalIncome)}
            • Total Despesas: ${brlFormat.format(totalExpense)} (Fixas/Dívidas: ${brlFormat.format(fixedAndDebtExpense)} | Variáveis: ${brlFormat.format(variableExpense)})
            • Meta de Orçamento Mensal: ${if (monthlyBudget > 0) brlFormat.format(monthlyBudget) else "Não definida"}

            === EMPRÉSTIMOS, FINANCIAMENTOS E RENEGOCIAÇÕES ATIVAS ===
            $loansSummary

            === MOLDES DE CONTAS FIXAS MENSAIS (ÁGUA, LUZ, INTERNET...) ===
            $fixedBillsSummary

            === DESPESAS POR CATEGORIA ===
            $expensesByCategory

            === MENSAGEM DO USUÁRIO ===
            $userQuestion
        """.trimIndent()

        val response = model.generateContent(fullPrompt)
        val raw = response.text?.trim() ?: error("Sem resposta do Gemini.")
        val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        runCatching {
            val parsed = json.decodeFromString<AiChatActionResponse>(cleaned)
            val normalizedList = parsed.transactionsToCreate.map { item ->
                item.copy(
                    category = TransactionCategory.fromLabelOrName(item.category).label,
                    type = if (item.type.uppercase() == "INCOME") "INCOME" else "EXPENSE"
                )
            }
            parsed.copy(transactionsToCreate = normalizedList)
        }.getOrElse {
            AiChatActionResponse(reply = raw, transactionsToCreate = emptyList())
        }
    }

    private fun parseJsonSafely(rawText: String, fallbackInput: String): AiExtractedTransaction {
        val cleaned = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return runCatching {
            val parsed = json.decodeFromString<AiExtractedTransaction>(cleaned)
            val catEnum = TransactionCategory.fromLabelOrName(parsed.category)
            val normalizedType = if (parsed.type.uppercase() == "INCOME") "INCOME" else "EXPENSE"
            val nature = when (parsed.expenseNature.uppercase()) {
                "FIXED" -> ExpenseNature.FIXED.name
                "DEBT" -> ExpenseNature.DEBT.name
                else -> catEnum.defaultNature.name
            }
            parsed.copy(
                title = parsed.title.ifBlank { fallbackInput.take(40) },
                category = catEnum.label,
                type = normalizedType,
                installmentCount = parsed.installmentCount.coerceIn(1, 360),
                expenseNature = nature
            )
        }.getOrElse {
            fallbackHeuristicParse(fallbackInput)
        }
    }

    fun fallbackHeuristicParse(input: String): AiExtractedTransaction {
        val lower = input.lowercase()
        val installmentRegex = Regex("""(\d{1,3})\s*(?:x|vezes|parcelas)""", RegexOption.IGNORE_CASE)
        val installmentMatch = installmentRegex.find(input)
        val installmentCount = installmentMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 360) ?: 1

        val textWithoutInstallments = if (installmentMatch != null) {
            input.removeRange(installmentMatch.range)
        } else input

        val numberRegex = Regex("""(\d+(?:[.,]\d{1,2})?)""")
        val matchedAmount = numberRegex.findAll(textWithoutInstallments)
            .mapNotNull { it.value.replace(",", ".").toDoubleOrNull() }
            .maxOrNull() ?: 0.0

        val isIncome = listOf("recebi", "ganhei", "salário", "salario", "renda", "freela", "dividendo", "pix recebido", "transferência recebida")
            .any { lower.contains(it) }

        val isDebt = listOf("empréstimo", "emprestimo", "financiamento", "consignado", "renegociação", "renegociacao", "acordo", "parcelamento de fatura")
            .any { lower.contains(it) }

        val isFixedBill = listOf("água", "agua", "luz", "energia", "internet", "aluguel", "condomínio", "condominio", "gás", "gas", "iptu", "fixo", "mensal")
            .any { lower.contains(it) }

        val category = when {
            listOf("renegociação", "renegociacao", "acordo", "fatura").any { lower.contains(it) } ->
                TransactionCategory.RENEGOCIACAO.label
            listOf("empréstimo", "emprestimo", "financiamento", "consignado", "prestação", "prestacao").any { lower.contains(it) } ->
                TransactionCategory.EMPRESTIMOS.label
            listOf("água", "agua", "luz", "energia", "internet", "aluguel", "condomínio", "condominio", "gás", "iptu").any { lower.contains(it) } ->
                TransactionCategory.MORADIA.label
            listOf("mercado", "ifood", "restaurante", "lanche", "padaria", "almoço", "jantar", "café", "supermercado", "burger", "pizza").any { lower.contains(it) } ->
                TransactionCategory.ALIMENTACAO.label
            listOf("uber", "99", "gasolina", "combustível", "ônibus", "metrô", "estacionamento", "posto", "pedágio").any { lower.contains(it) } ->
                TransactionCategory.TRANSPORTE.label
            listOf("farmácia", "médico", "consulta", "exame", "academia", "remédio", "drogaria").any { lower.contains(it) } ->
                TransactionCategory.SAUDE.label
            listOf("netflix", "spotify", "assinatura", "prime", "youtube", "icloud", "hbo").any { lower.contains(it) } ->
                TransactionCategory.ASSINATURAS.label
            listOf("cinema", "bar", "viagem", "show", "jogo", "festa").any { lower.contains(it) } ->
                TransactionCategory.LAZER.label
            listOf("curso", "livro", "faculdade", "escola", "udemy").any { lower.contains(it) } ->
                TransactionCategory.EDUCACAO.label
            listOf("roupa", "shopping", "mercado livre", "shopee", "amazon", "magazine").any { lower.contains(it) } ->
                TransactionCategory.COMPRAS.label
            listOf("salário", "salario", "pagamento", "holerite").any { lower.contains(it) } ->
                TransactionCategory.SALARIO.label
            listOf("investimento", "cdb", "tesouro", "ação", "fii", "dividendo").any { lower.contains(it) } ->
                TransactionCategory.INVESTIMENTOS.label
            else -> TransactionCategory.OUTROS.label
        }

        val expenseNature = when {
            isDebt -> ExpenseNature.DEBT.name
            isFixedBill -> ExpenseNature.FIXED.name
            else -> ExpenseNature.VARIABLE.name
        }

        val paymentMethod = when {
            installmentCount > 1 && !isDebt || lower.contains("crédito") || lower.contains("credito") || lower.contains("cartão") -> "Cartão de Crédito"
            isDebt || isFixedBill || lower.contains("boleto") -> "Boleto"
            lower.contains("débito") || lower.contains("debito") -> "Cartão de Débito"
            lower.contains("dinheiro") -> "Dinheiro"
            else -> "Pix"
        }

        val cleanedTitle = textWithoutInstallments
            .replace(Regex("""(?i)\b(gastei|paguei|comprei|recebi|ganhei|registra|anota|no|na|de|em|por|reais|r\$|pix|cartão|cartao|crédito|credito|débito|debito)\b"""), " ")
            .replace(numberRegex, " ")
            .trim()
            .replace(Regex("""\s+"""), " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .ifBlank { if (isIncome) "Receita" else "Despesa" }

        return AiExtractedTransaction(
            title = cleanedTitle,
            amount = matchedAmount,
            type = if (isIncome) "INCOME" else "EXPENSE",
            category = category,
            paymentMethod = paymentMethod,
            notes = "Extraído de: \"$input\"",
            isRecurring = isFixedBill,
            installmentCount = installmentCount,
            expenseNature = expenseNature
        )
    }
}
