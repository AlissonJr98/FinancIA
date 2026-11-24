package com.financia.app.data.model

import kotlinx.serialization.Serializable

enum class TransactionType(val label: String) {
    INCOME("Receita"),
    EXPENSE("Despesa")
}

enum class ExpenseNature(val label: String) {
    FIXED("Despesa Fixa"),
    VARIABLE("Despesa Variável"),
    DEBT("Empréstimo / Financiamento / Renegociação")
}

enum class LoanType(val label: String) {
    EMPRESTIMO("Empréstimo Pessoal / Consignado"),
    FINANCIAMENTO("Financiamento (Veículo / Imóvel)"),
    RENEGOCIACAO_CARTAO("Renegociação de Cartão / Dívida"),
    PARCELAMENTO_FATURA("Parcelamento de Fatura")
}

enum class TransactionCategory(
    val label: String,
    val iconName: String,
    val colorHex: Long,
    val isIncomeDefault: Boolean = false,
    val defaultNature: ExpenseNature = ExpenseNature.VARIABLE
) {
    ALIMENTACAO("Alimentação", "restaurant", 0xFFF97316, defaultNature = ExpenseNature.VARIABLE),
    TRANSPORTE("Transporte", "directions_car", 0xFF3B82F6, defaultNature = ExpenseNature.VARIABLE),
    MORADIA("Moradia (Água/Luz/Aluguel)", "home", 0xFF8B5CF6, defaultNature = ExpenseNature.FIXED),
    EMPRESTIMOS("Empréstimos & Financiamentos", "account_balance", 0xFFE11D48, defaultNature = ExpenseNature.DEBT),
    RENEGOCIACAO("Renegociação / Cartão", "credit_card", 0xFFD97706, defaultNature = ExpenseNature.DEBT),
    LAZER("Lazer", "movie", 0xFFEC4899, defaultNature = ExpenseNature.VARIABLE),
    SAUDE("Saúde", "medical_services", 0xFFEF4444, defaultNature = ExpenseNature.VARIABLE),
    EDUCACAO("Educação", "school", 0xFF06B6D4, defaultNature = ExpenseNature.FIXED),
    COMPRAS("Compras", "shopping_bag", 0xFFF59E0B, defaultNature = ExpenseNature.VARIABLE),
    ASSINATURAS("Assinaturas", "subscriptions", 0xFF6366F1, defaultNature = ExpenseNature.FIXED),
    SALARIO("Salário", "payments", 0xFF10B981, isIncomeDefault = true),
    INVESTIMENTOS("Investimentos", "trending_up", 0xFF14B8A6, isIncomeDefault = true),
    FREELANCE("Freelance", "work", 0xFF22C55E, isIncomeDefault = true),
    OUTROS("Outros", "category", 0xFF64748B, defaultNature = ExpenseNature.VARIABLE);

    companion object {
        fun fromLabelOrName(value: String): TransactionCategory {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull {
                it.name.lowercase() == normalized ||
                    it.label.lowercase() == normalized ||
                    it.label.lowercase().startsWith(normalized.take(6))
            } ?: OUTROS
        }
    }
}

data class FinancialTransaction(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = TransactionCategory.OUTROS.label,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Pix",
    val notes: String = "",
    val createdByAi: Boolean = false,
    val isRecurring: Boolean = false,
    val installmentCount: Int = 1,
    val installmentIndex: Int = 1,
    val expenseNature: String = ExpenseNature.VARIABLE.name,
    val fixedBillId: String = "",
    val loanId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isIncome: Boolean
        get() = type.equals(TransactionType.INCOME.name, ignoreCase = true)

    val isDebtNature: Boolean
        get() = expenseNature == ExpenseNature.DEBT.name ||
            category.contains("Empréstimo", ignoreCase = true) ||
            category.contains("Renegociação", ignoreCase = true)

    val isFixedOrDebt: Boolean
        get() = isRecurring ||
            expenseNature == ExpenseNature.FIXED.name ||
            isDebtNature ||
            category.contains("Moradia", ignoreCase = true)

    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "title" to title.take(200),
            "amount" to amount,
            "type" to if (isIncome) "INCOME" else "EXPENSE",
            "category" to category.take(80),
            "dateTimestamp" to dateTimestamp,
            "paymentMethod" to paymentMethod.take(60),
            "notes" to notes.take(500),
            "createdByAi" to createdByAi,
            "isRecurring" to isRecurring,
            "installmentCount" to installmentCount.coerceIn(1, 480),
            "installmentIndex" to installmentIndex.coerceIn(1, 480),
            "expenseNature" to expenseNature,
            "createdAt" to createdAt
        )
        if (id.isNotBlank()) map["id"] = id.take(128)
        if (fixedBillId.isNotBlank()) map["fixedBillId"] = fixedBillId.take(128)
        if (loanId.isNotBlank()) map["loanId"] = loanId.take(128)
        return map
    }
}

data class FixedBillTemplate(
    val id: String = "",
    val name: String = "",
    val category: String = TransactionCategory.MORADIA.label,
    val dueDay: Int = 10,
    val defaultAmount: Double = 0.0,
    val isFixedValue: Boolean = false,
    val monthlyValues: Map<String, Double> = emptyMap(),
    val paidMonths: Map<String, Boolean> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun amountForMonth(monthKey: String): Double {
        return monthlyValues[monthKey] ?: defaultAmount
    }

    fun isPaidInMonth(monthKey: String): Boolean {
        return paidMonths[monthKey] == true
    }

    fun averagePreviousAmount(excludeMonthKey: String): Double {
        val otherValues = monthlyValues.filterKeys { it != excludeMonthKey }.values.filter { it > 0.0 }
        return if (otherValues.isNotEmpty()) otherValues.average() else defaultAmount
    }

    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "name" to name.take(120),
            "category" to category.take(80),
            "dueDay" to dueDay.coerceIn(1, 31),
            "defaultAmount" to defaultAmount.coerceAtLeast(0.0),
            "isFixedValue" to isFixedValue,
            "monthlyValues" to monthlyValues,
            "paidMonths" to paidMonths,
            "createdAt" to createdAt
        )
        if (id.isNotBlank()) map["id"] = id.take(128)
        return map
    }
}

data class LoanContract(
    val id: String = "",
    val title: String = "",
    val loanType: String = LoanType.EMPRESTIMO.label,
    val installmentAmount: Double = 0.0,
    val totalInstallments: Int = 48,
    val paidInstallments: Int = 0,
    val dueDay: Int = 15,
    val interestRateMonthly: Double = 0.0,
    val notes: String = "",
    val lastPaidMonthKey: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingInstallments: Int
        get() = (totalInstallments - paidInstallments).coerceAtLeast(0)

    val remainingBalance: Double
        get() = remainingInstallments * installmentAmount

    val progressFraction: Float
        get() = if (totalInstallments > 0) {
            (paidInstallments.toFloat() / totalInstallments.toFloat()).coerceIn(0f, 1f)
        } else 0f

    fun isPaidInMonth(monthKey: String): Boolean = lastPaidMonthKey == monthKey

    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "title" to title.take(150),
            "loanType" to loanType.take(80),
            "installmentAmount" to installmentAmount,
            "totalInstallments" to totalInstallments.coerceIn(1, 480),
            "paidInstallments" to paidInstallments.coerceIn(0, 480),
            "dueDay" to dueDay.coerceIn(1, 31),
            "interestRateMonthly" to interestRateMonthly.coerceAtLeast(0.0),
            "notes" to notes.take(500),
            "lastPaidMonthKey" to lastPaidMonthKey.take(20),
            "createdAt" to createdAt
        )
        if (id.isNotBlank()) map["id"] = id.take(128)
        return map
    }
}

data class SavingsGoal(
    val id: String = "",
    val title: String = "",
    val targetAmount: Double = 1000.0,
    val currentAmount: Double = 0.0,
    val deadlineNote: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "title" to title.take(120),
            "targetAmount" to targetAmount.coerceAtLeast(1.0),
            "currentAmount" to currentAmount.coerceAtLeast(0.0),
            "deadlineNote" to deadlineNote.take(100),
            "createdAt" to createdAt
        )
        if (id.isNotBlank()) map["id"] = id.take(128)
        return map
    }
}

/**
 * Quem me deve / Valores A Receber (ex: Emprestei cartão ou Pix para um amigo/familiar).
 */
data class ReceivableItem(
    val id: String = "",
    val personName: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val dueDateNote: String = "",
    val isReceived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "personName" to personName.take(120),
            "description" to description.take(200),
            "amount" to amount.coerceAtLeast(0.01),
            "dueDateNote" to dueDateNote.take(100),
            "isReceived" to isReceived,
            "createdAt" to createdAt
        )
        if (id.isNotBlank()) map["id"] = id.take(128)
        return map
    }
}

/**
 * Resumo de barra mensal para o Gráfico Comparativo dos últimos 6 meses.
 */
data class MonthlyBarStat(
    val monthLabel: String,
    val income: Double,
    val fixedAndDebt: Double,
    val variable: Double
) {
    val totalExpense: Double
        get() = fixedAndDebt + variable
}

@Serializable
data class AiExtractedTransaction(
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE",
    val category: String = "Outros",
    val paymentMethod: String = "Pix",
    val notes: String = "",
    val isRecurring: Boolean = false,
    val installmentCount: Int = 1,
    val expenseNature: String = "VARIABLE"
)

@Serializable
data class AiChatActionResponse(
    val reply: String = "",
    val transactionsToCreate: List<AiExtractedTransaction> = emptyList()
)

data class ChatMessage(
    val id: String = "",
    val role: String = "user",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "role" to role,
        "content" to content.take(8000),
        "timestamp" to timestamp
    )
}
