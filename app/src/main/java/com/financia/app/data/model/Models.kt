package com.financia.app.data.model

import kotlinx.serialization.Serializable

enum class TransactionType(val label: String) {
    INCOME("Receita"),
    EXPENSE("Despesa")
}

enum class TransactionCategory(val label: String, val iconName: String, val colorHex: Long, val isIncomeDefault: Boolean = false) {
    ALIMENTACAO("Alimentação", "restaurant", 0xFFF97316),
    TRANSPORTE("Transporte", "directions_car", 0xFF3B82F6),
    MORADIA("Moradia", "home", 0xFF8B5CF6),
    LAZER("Lazer", "movie", 0xFFEC4899),
    SAUDE("Saúde", "medical_services", 0xFFEF4444),
    EDUCACAO("Educação", "school", 0xFF06B6D4),
    COMPRAS("Compras", "shopping_bag", 0xFFF59E0B),
    ASSINATURAS("Assinaturas", "subscriptions", 0xFF6366F1),
    SALARIO("Salário", "payments", 0xFF10B981, isIncomeDefault = true),
    INVESTIMENTOS("Investimentos", "trending_up", 0xFF14B8A6, isIncomeDefault = true),
    FREELANCE("Freelance", "work", 0xFF22C55E, isIncomeDefault = true),
    OUTROS("Outros", "category", 0xFF64748B);

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
    val isRecurring: Boolean = false,
    val installmentCount: Int = 1,
    val installmentIndex: Int = 1,
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
            "isRecurring" to isRecurring,
            "installmentCount" to installmentCount.coerceIn(1, 120),
            "installmentIndex" to installmentIndex.coerceIn(1, 120),
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
    val notes: String = "",
    val isRecurring: Boolean = false,
    val installmentCount: Int = 1
)

@Serializable
data class AiChatActionResponse(
    val reply: String = "",
    val transactionsToCreate: List<AiExtractedTransaction> = emptyList()
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
