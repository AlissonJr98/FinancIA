package com.financia.app.data.model

import kotlinx.serialization.Serializable

enum class TransactionType(val label: String) {
    INCOME("Receita"),
    EXPENSE("Despesa")
}

enum class TransactionCategory(val label: String, val iconName: String, val isIncomeDefault: Boolean = false) {
    ALIMENTACAO("Alimentação", "restaurant"),
    TRANSPORTE("Transporte", "directions_car"),
    MORADIA("Moradia", "home"),
    LAZER("Lazer", "movie"),
    SAUDE("Saúde", "medical_services"),
    EDUCACAO("Educação", "school"),
    COMPRAS("Compras", "shopping_bag"),
    ASSINATURAS("Assinaturas", "subscriptions"),
    SALARIO("Salário", "payments", isIncomeDefault = true),
    INVESTIMENTOS("Investimentos", "trending_up", isIncomeDefault = true),
    FREELANCE("Freelance", "work", isIncomeDefault = true),
    OUTROS("Outros", "category");

    companion object {
        fun fromLabelOrName(value: String): TransactionCategory {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull {
                it.name.lowercase() == normalized || it.label.lowercase() == normalized
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
    val createdAt: Long = System.currentTimeMillis()
) {
    val isIncome: Boolean
        get() = type.equals(TransactionType.INCOME.name, ignoreCase = true)

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
            "createdAt" to createdAt
        )
        if (id.isNotBlank()) {
            map["id"] = id.take(128)
        }
        return map
    }
}

@Serializable
data class AiExtractedTransaction(
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE",
    val category: String = "Outros",
    val paymentMethod: String = "Pix",
    val notes: String = ""
)

data class ChatMessage(
    val id: String = "",
    val role: String = "user", // "user" or "model"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "role" to role,
        "content" to content.take(8000),
        "timestamp" to timestamp
    )
}
