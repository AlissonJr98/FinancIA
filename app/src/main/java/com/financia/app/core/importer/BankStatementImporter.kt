package com.financia.app.core.importer

import android.content.Context
import android.net.Uri
import com.financia.app.data.model.BankAccountOption
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.TransactionType
import com.financia.app.data.repository.AiFinancialRepository
import kotlin.math.abs

/**
 * Importador Inteligente de Extratos Bancários (arquivos CSV ou OFX do Nubank, Inter, Itaú, BB, Santander, etc.).
 */
object BankStatementImporter {

    fun parseStatementUri(
        context: Context,
        uri: Uri,
        defaultAccountName: String,
        aiRepository: AiFinancialRepository
    ): List<FinancialTransaction> {
        val rawText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
            it.readText()
        } ?: return emptyList()

        return if (rawText.contains("<OFX>", ignoreCase = true) || rawText.contains("<STMTTRN>", ignoreCase = true)) {
            parseOfxContent(rawText, defaultAccountName, aiRepository)
        } else {
            parseCsvContent(rawText, defaultAccountName, aiRepository)
        }
    }

    private fun parseOfxContent(
        rawText: String,
        accountName: String,
        aiRepository: AiFinancialRepository
    ): List<FinancialTransaction> {
        val results = mutableListOf<FinancialTransaction>()
        val blocks = rawText.split("<STMTTRN>", ignoreCase = true).drop(1)

        blocks.forEach { block ->
            val amtMatch = Regex("""<TRNAMT>([-+]?\d+[.,]?\d*)""", RegexOption.IGNORE_CASE).find(block)
            val memoMatch = Regex("""<(?:MEMO|NAME)>([^\r\n<]+)""", RegexOption.IGNORE_CASE).find(block)

            val rawAmount = amtMatch?.groupValues?.getOrNull(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            val title = memoMatch?.groupValues?.getOrNull(1)?.trim() ?: "Lançamento OFX"

            if (rawAmount != 0.0) {
                val isIncome = rawAmount > 0.0
                val absAmount = abs(rawAmount)
                val heuristic = aiRepository.fallbackHeuristicParse(title)

                results.add(
                    FinancialTransaction(
                        title = title.take(150),
                        amount = absAmount,
                        type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                        category = heuristic.category,
                        paymentMethod = "Extrato Bancário",
                        accountName = accountName,
                        notes = "Importado via Extrato OFX ($accountName)",
                        createdByAi = true,
                        expenseNature = heuristic.expenseNature
                    )
                )
            }
        }
        return results
    }

    private fun parseCsvContent(
        rawText: String,
        accountName: String,
        aiRepository: AiFinancialRepository
    ): List<FinancialTransaction> {
        val results = mutableListOf<FinancialTransaction>()
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        // Pula cabeçalho e analisa linhas CSV (separadas por ; ou ,)
        lines.drop(1).take(120).forEach { line ->
            val delimiter = if (line.count { it == ';' } >= line.count { it == ',' }) ';' else ','
            val cols = line.split(delimiter).map { it.trim().removeSurrounding("\"") }

            // Procura a coluna que tem número monetário e a coluna que tem descrição textual
            var amountVal: Double? = null
            var descriptionVal = ""

            cols.forEach { col ->
                val cleanNum = col.replace("R$", "", ignoreCase = true).trim().replace(",", ".")
                val parsed = cleanNum.toDoubleOrNull()
                if (parsed != null && amountVal == null && parsed != 0.0) {
                    amountVal = parsed
                } else if (col.length > 3 && !col.matches(Regex("""\d{2}[/-]\d{2}[/-]\d{2,4}.*""")) && descriptionVal.isBlank()) {
                    descriptionVal = col
                }
            }

            val finalAmt = amountVal
            if (finalAmt != null && finalAmt != 0.0) {
                val isIncome = finalAmt > 0 && (line.lowercase().contains("receita") || line.lowercase().contains("recebido") || line.lowercase().contains("salário"))
                val absAmt = abs(finalAmt)
                val title = descriptionVal.ifBlank { "Lançamento Importado" }
                val heuristic = aiRepository.fallbackHeuristicParse(title)

                results.add(
                    FinancialTransaction(
                        title = title.take(150),
                        amount = absAmt,
                        type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                        category = heuristic.category,
                        paymentMethod = "Extrato CSV",
                        accountName = accountName,
                        notes = "Importado via CSV ($accountName)",
                        createdByAi = true,
                        expenseNature = heuristic.expenseNature
                    )
                )
            }
        }
        return results
    }
}
