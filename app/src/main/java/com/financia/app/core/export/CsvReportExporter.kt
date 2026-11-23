package com.financia.app.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.financia.app.data.model.FinancialTransaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvReportExporter {

    fun exportAndShareCsv(
        context: Context,
        transactions: List<FinancialTransaction>,
        periodLabel: String
    ) {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val safePeriod = periodLabel.replace(" ", "_").replace("/", "-")
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(dir, "Relatorio_FinancIA_$safePeriod.csv")

        val csvContent = buildString {
            // UTF-8 BOM para abrir com acentos perfeitos no Excel
            append("\uFEFF")
            appendLine("Data;Descricao;Tipo;Categoria;Valor (R$);Forma de Pagamento;Fixo Mensal;Parcelas;Criado por IA;Observacoes")
            transactions.forEach { tx ->
                val dateStr = dateFormatter.format(Date(tx.dateTimestamp))
                val typeStr = if (tx.isIncome) "Receita" else "Despesa"
                val amountStr = String.format(Locale("pt", "BR"), "%.2f", tx.amount)
                val cleanTitle = tx.title.replace(";", ",")
                val cleanNotes = tx.notes.replace(";", ",").replace("\n", " ")
                val recurringStr = if (tx.isRecurring) "Sim" else "Não"
                val installmentStr = "${tx.installmentIndex}/${tx.installmentCount}"
                val aiStr = if (tx.createdByAi) "Sim" else "Não"

                appendLine("$dateStr;$cleanTitle;$typeStr;${tx.category};$amountStr;${tx.paymentMethod};$recurringStr;$installmentStr;$aiStr;$cleanNotes")
            }
        }

        csvFile.writeText(csvContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Relatório Financeiro FinancIA - $periodLabel")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Exportar Relatório Excel/CSV ($periodLabel)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
