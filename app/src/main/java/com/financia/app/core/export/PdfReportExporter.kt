package com.financia.app.core.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.FixedBillTemplate
import com.financia.app.data.model.LoanContract
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gerador nativo de Relatório Financeiro Mensal em PDF com Cabeçalho Esmeralda,
 * Resumo Executivo, Contas Fixas, Empréstimos e Tabela de Lançamentos!
 */
object PdfReportExporter {

    fun exportAndSharePdf(
        context: Context,
        periodLabel: String,
        transactions: List<FinancialTransaction>,
        fixedBills: List<FixedBillTemplate>,
        loans: List<LoanContract>
    ) {
        val brl = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val fixedAndDebt = transactions.filter { !it.isIncome && it.isFixedOrDebt }.sumOf { it.amount }
        val variableExpense = (totalExpense - fixedAndDebt).coerceAtLeast(0.0)
        val balance = totalIncome - totalExpense

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00796B")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2F1")
            textSize = 12f
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 10.5f
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#16A34A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // 1. Cabeçalho Esmeralda
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 92f, headerPaint)
        canvas.drawText("FinancIA — Relatório Financeiro Mensal", 32f, 42f, titlePaint)
        canvas.drawText("Período: $periodLabel • Gerado em ${dateFormat.format(Date())}", 32f, 66f, subtitlePaint)

        // 2. Cards de Resumo Executivo
        var y = 115f
        canvas.drawRoundRect(RectF(32f, y, pageWidth - 32f, y + 72f), 12f, 12f, cardPaint)
        canvas.drawText("Saldo do Mês", 48f, y + 24f, bodyPaint)
        canvas.drawText(brl.format(balance), 48f, y + 48f, sectionTitlePaint)

        canvas.drawText("Total Receitas", 210f, y + 24f, bodyPaint)
        canvas.drawText(brl.format(totalIncome), 210f, y + 48f, incomePaint)

        canvas.drawText("Total Despesas", 360f, y + 24f, bodyPaint)
        canvas.drawText(brl.format(totalExpense), 360f, y + 48f, expensePaint)

        y += 90f
        canvas.drawText(
            "Divisão de Gastos: Fixas & Empréstimos: ${brl.format(fixedAndDebt)}  |  Variáveis (Dia a dia): ${brl.format(variableExpense)}",
            32f,
            y,
            boldBodyPaint
        )

        // 3. Resumo de Empréstimos / Financiamentos Ativos
        if (loans.isNotEmpty()) {
            y += 26f
            canvas.drawText("Empréstimos, Financiamentos & Renegociações Ativas", 32f, y, sectionTitlePaint)
            y += 16f
            loans.take(5).forEach { loan ->
                canvas.drawText(
                    "• ${loan.title} (${loan.loanType}): Parcela ${brl.format(loan.installmentAmount)} • Pagas ${loan.paidInstallments}/${loan.totalInstallments} (Restante: ${brl.format(loan.remainingBalance)})",
                    36f,
                    y,
                    bodyPaint
                )
                y += 15f
            }
        }

        // 4. Resumo de Contas Fixas Cadastradas
        if (fixedBills.isNotEmpty()) {
            y += 14f
            canvas.drawText("Moldes de Contas Fixas Mensais (Água, Luz, Internet...)", 32f, y, sectionTitlePaint)
            y += 16f
            fixedBills.take(5).forEach { bill ->
                canvas.drawText(
                    "• ${bill.name} (Vencimento dia ${bill.dueDay}) — Valor base/médio: ${brl.format(bill.defaultAmount)}",
                    36f,
                    y,
                    bodyPaint
                )
                y += 15f
            }
        }

        // 5. Tabela de Lançamentos do Mês
        y += 22f
        canvas.drawText("Extrato de Lançamentos ($periodLabel)", 32f, y, sectionTitlePaint)
        y += 18f

        canvas.drawRect(32f, y - 12f, pageWidth - 32f, y + 6f, cardPaint)
        canvas.drawText("Data", 38f, y, boldBodyPaint)
        canvas.drawText("Descrição", 105f, y, boldBodyPaint)
        canvas.drawText("Categoria / Conta", 285f, y, boldBodyPaint)
        canvas.drawText("Valor (R$)", 465f, y, boldBodyPaint)
        y += 20f

        transactions.take(25).forEach { tx ->
            if (y > pageHeight - 45f) return@forEach
            val dStr = dateFormat.format(Date(tx.dateTimestamp))
            val sign = if (tx.isIncome) "+ " else "- "
            canvas.drawText(dStr, 38f, y, bodyPaint)
            canvas.drawText(tx.title.take(28), 105f, y, bodyPaint)
            canvas.drawText("${tx.category.take(16)} (${tx.accountName.take(10)})", 285f, y, bodyPaint)
            canvas.drawText(
                "$sign${brl.format(tx.amount)}",
                465f,
                y,
                if (tx.isIncome) incomePaint else expensePaint
            )
            y += 17f
        }

        pdfDocument.finishPage(page)

        val safePeriod = periodLabel.replace(" ", "_").replace("/", "-")
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(dir, "Relatorio_FinancIA_$safePeriod.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Relatório PDF FinancIA - $periodLabel")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Compartilhar Relatório PDF ($periodLabel)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
