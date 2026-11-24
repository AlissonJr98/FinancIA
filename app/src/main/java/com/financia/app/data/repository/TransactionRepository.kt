package com.financia.app.data.repository

import com.financia.app.data.model.ChatMessage
import com.financia.app.data.model.ExpenseNature
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.FixedBillTemplate
import com.financia.app.data.model.LoanContract
import com.financia.app.data.model.LoanType
import com.financia.app.data.model.SavingsGoal
import com.financia.app.data.model.TransactionCategory
import com.financia.app.data.model.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    fun observeTransactions(userId: String): Flow<List<FinancialTransaction>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .collection("transactions")
            .orderBy("dateTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val amount = doc.getDouble("amount") ?: 0.0
                    val type = doc.getString("type") ?: TransactionType.EXPENSE.name
                    val category = doc.getString("category") ?: TransactionCategory.OUTROS.label
                    val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                    val paymentMethod = doc.getString("paymentMethod") ?: "Pix"
                    val notes = doc.getString("notes") ?: ""
                    val createdByAi = doc.getBoolean("createdByAi") ?: false
                    val isRecurring = doc.getBoolean("isRecurring") ?: false
                    val installmentCount = (doc.getLong("installmentCount") ?: 1L).toInt()
                    val installmentIndex = (doc.getLong("installmentIndex") ?: 1L).toInt()
                    val expenseNature = doc.getString("expenseNature")
                        ?: if (isRecurring) ExpenseNature.FIXED.name else ExpenseNature.VARIABLE.name
                    val fixedBillId = doc.getString("fixedBillId") ?: ""
                    val loanId = doc.getString("loanId") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: dateTimestamp

                    FinancialTransaction(
                        id = doc.id,
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        dateTimestamp = dateTimestamp,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        createdByAi = createdByAi,
                        isRecurring = isRecurring,
                        installmentCount = installmentCount,
                        installmentIndex = installmentIndex,
                        expenseNature = expenseNature,
                        fixedBillId = fixedBillId,
                        loanId = loanId,
                        createdAt = createdAt
                    )
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addTransaction(transaction: FinancialTransaction): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val colRef = firestore.collection("users").document(uid).collection("transactions")

        val totalInstallments = transaction.installmentCount.coerceIn(1, 360)
        if (totalInstallments <= 1) {
            val docRef = colRef.document()
            val withId = transaction.copy(
                id = docRef.id,
                installmentCount = 1,
                installmentIndex = 1,
                createdAt = System.currentTimeMillis()
            )
            docRef.set(withId.toFirestoreMap()).await()
            docRef.id
        } else {
            val batch = firestore.batch()
            val perInstallmentAmount = (transaction.amount / totalInstallments * 100.0).toInt() / 100.0
            var firstId = ""
            val cal = Calendar.getInstance().apply { timeInMillis = transaction.dateTimestamp }

            for (idx in 1..totalInstallments) {
                val docRef = colRef.document()
                if (idx == 1) firstId = docRef.id
                val txDate = cal.timeInMillis
                val instTitle = "${transaction.title} ($idx/$totalInstallments)"
                val instTx = transaction.copy(
                    id = docRef.id,
                    title = instTitle.take(200),
                    amount = if (perInstallmentAmount > 0) perInstallmentAmount else transaction.amount,
                    dateTimestamp = txDate,
                    installmentCount = totalInstallments,
                    installmentIndex = idx,
                    createdAt = System.currentTimeMillis()
                )
                batch.set(docRef, instTx.toFirestoreMap())
                cal.add(Calendar.MONTH, 1)
            }
            batch.commit().await()
            firstId
        }
    }

    suspend fun updateTransaction(transaction: FinancialTransaction): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        require(transaction.id.isNotBlank()) { "ID da transação inválido." }
        firestore.collection("users")
            .document(uid)
            .collection("transactions")
            .document(transaction.id)
            .set(transaction.toFirestoreMap())
            .await()
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        firestore.collection("users")
            .document(uid)
            .collection("transactions")
            .document(transactionId)
            .delete()
            .await()
    }

    // --- Contas Fixas Mensais com Valor Variável (Água, Luz, Internet, Gás, Aluguel) ---

    @Suppress("UNCHECKED_CAST")
    fun observeFixedBills(userId: String): Flow<List<FixedBillTemplate>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .collection("fixedBills")
            .orderBy("dueDay", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val category = doc.getString("category") ?: TransactionCategory.MORADIA.label
                    val dueDay = (doc.getLong("dueDay") ?: 10L).toInt()
                    val defaultAmount = doc.getDouble("defaultAmount") ?: 0.0
                    val isFixedValue = doc.getBoolean("isFixedValue") ?: false

                    val rawMonthly = doc.get("monthlyValues") as? Map<String, Any> ?: emptyMap()
                    val monthlyValues = rawMonthly.mapNotNull { (k, v) ->
                        val num = (v as? Number)?.toDouble() ?: return@mapNotNull null
                        k to num
                    }.toMap()

                    val rawPaid = doc.get("paidMonths") as? Map<String, Any> ?: emptyMap()
                    val paidMonths = rawPaid.mapNotNull { (k, v) ->
                        val b = v as? Boolean ?: return@mapNotNull null
                        k to b
                    }.toMap()

                    FixedBillTemplate(
                        id = doc.id,
                        name = name,
                        category = category,
                        dueDay = dueDay,
                        defaultAmount = defaultAmount,
                        isFixedValue = isFixedValue,
                        monthlyValues = monthlyValues,
                        paidMonths = paidMonths,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveFixedBill(bill: FixedBillTemplate): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = firestore.collection("users").document(uid).collection("fixedBills")
        val docRef = if (bill.id.isBlank()) col.document() else col.document(bill.id)
        val withId = bill.copy(id = docRef.id)
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun payFixedBillForMonth(
        bill: FixedBillTemplate,
        monthKey: String,
        amountForThisMonth: Double,
        targetTimestamp: Long
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val updatedMonthly = bill.monthlyValues.toMutableMap().apply {
            put(monthKey, amountForThisMonth)
        }
        val updatedPaid = bill.paidMonths.toMutableMap().apply {
            put(monthKey, true)
        }
        val updatedBill = bill.copy(
            monthlyValues = updatedMonthly,
            paidMonths = updatedPaid
        )
        // 1. Atualiza o molde da conta fixa com o valor daquele mês e status Pago
        firestore.collection("users")
            .document(uid)
            .collection("fixedBills")
            .document(bill.id)
            .set(updatedBill.toFirestoreMap())
            .await()

        // 2. Registra a despesa fixa correspondente naquele mês
        val txCol = firestore.collection("users").document(uid).collection("transactions")
        val txDoc = txCol.document()
        val tx = FinancialTransaction(
            id = txDoc.id,
            title = "${bill.name} (Conta Fixa $monthKey)",
            amount = amountForThisMonth,
            type = TransactionType.EXPENSE.name,
            category = bill.category,
            dateTimestamp = targetTimestamp,
            paymentMethod = "Boleto / Pix",
            notes = "Conta fixa mensal vencimento dia ${bill.dueDay}",
            createdByAi = false,
            isRecurring = false,
            installmentCount = 1,
            installmentIndex = 1,
            expenseNature = ExpenseNature.FIXED.name,
            fixedBillId = bill.id
        )
        txDoc.set(tx.toFirestoreMap()).await()
    }

    suspend fun deleteFixedBill(billId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        firestore.collection("users")
            .document(uid)
            .collection("fixedBills")
            .document(billId)
            .delete()
            .await()
    }

    // --- Empréstimos, Financiamentos e Renegociações de Cartão (Parcelas Longas) ---

    fun observeLoans(userId: String): Flow<List<LoanContract>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .collection("loans")
            .orderBy("dueDay", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val loanType = doc.getString("loanType") ?: LoanType.EMPRESTIMO.label
                    val installmentAmount = doc.getDouble("installmentAmount") ?: 0.0
                    val totalInstallments = (doc.getLong("totalInstallments") ?: 12L).toInt()
                    val paidInstallments = (doc.getLong("paidInstallments") ?: 0L).toInt()
                    val dueDay = (doc.getLong("dueDay") ?: 15L).toInt()
                    val interestRateMonthly = doc.getDouble("interestRateMonthly") ?: 0.0
                    val notes = doc.getString("notes") ?: ""
                    val lastPaidMonthKey = doc.getString("lastPaidMonthKey") ?: ""

                    LoanContract(
                        id = doc.id,
                        title = title,
                        loanType = loanType,
                        installmentAmount = installmentAmount,
                        totalInstallments = totalInstallments,
                        paidInstallments = paidInstallments,
                        dueDay = dueDay,
                        interestRateMonthly = interestRateMonthly,
                        notes = notes,
                        lastPaidMonthKey = lastPaidMonthKey,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveLoanContract(loan: LoanContract): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = firestore.collection("users").document(uid).collection("loans")
        val docRef = if (loan.id.isBlank()) col.document() else col.document(loan.id)
        val withId = loan.copy(id = docRef.id)
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun payOrAmortizeLoanInstallment(
        loan: LoanContract,
        installmentsPaidNow: Int = 1,
        customPaidAmount: Double = loan.installmentAmount * installmentsPaidNow,
        monthKey: String,
        targetTimestamp: Long,
        isAmortization: Boolean = false
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val newPaidCount = (loan.paidInstallments + installmentsPaidNow).coerceAtMost(loan.totalInstallments)
        val updatedLoan = loan.copy(
            paidInstallments = newPaidCount,
            lastPaidMonthKey = if (!isAmortization) monthKey else loan.lastPaidMonthKey
        )

        firestore.collection("users")
            .document(uid)
            .collection("loans")
            .document(loan.id)
            .set(updatedLoan.toFirestoreMap())
            .await()

        val txCol = firestore.collection("users").document(uid).collection("transactions")
        val txDoc = txCol.document()
        val labelSuffix = if (isAmortization) {
            "Amortização +${installmentsPaidNow}x (${newPaidCount}/${loan.totalInstallments})"
        } else {
            "Parcela ${newPaidCount}/${loan.totalInstallments}"
        }
        val category = if (loan.loanType.contains("Renegociação", ignoreCase = true) ||
            loan.loanType.contains("Cartão", ignoreCase = true)
        ) {
            TransactionCategory.RENEGOCIACAO.label
        } else {
            TransactionCategory.EMPRESTIMOS.label
        }

        val tx = FinancialTransaction(
            id = txDoc.id,
            title = "${loan.title} - $labelSuffix",
            amount = customPaidAmount,
            type = TransactionType.EXPENSE.name,
            category = category,
            dateTimestamp = targetTimestamp,
            paymentMethod = "Boleto / Débito",
            notes = "${loan.loanType} • Restam ${updatedLoan.remainingInstallments} parcelas",
            createdByAi = false,
            isRecurring = false,
            installmentCount = loan.totalInstallments,
            installmentIndex = newPaidCount,
            expenseNature = ExpenseNature.DEBT.name,
            loanId = loan.id
        )
        txDoc.set(tx.toFirestoreMap()).await()
    }

    suspend fun deleteLoanContract(loanId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        firestore.collection("users")
            .document(uid)
            .collection("loans")
            .document(loanId)
            .delete()
            .await()
    }

    // --- Caixinhas / Metas de Economia ---

    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .collection("savingsGoals")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val targetAmount = doc.getDouble("targetAmount") ?: 1000.0
                    val currentAmount = doc.getDouble("currentAmount") ?: 0.0
                    val deadlineNote = doc.getString("deadlineNote") ?: ""

                    SavingsGoal(
                        id = doc.id,
                        title = title,
                        targetAmount = targetAmount,
                        currentAmount = currentAmount,
                        deadlineNote = deadlineNote,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveSavingsGoal(goal: SavingsGoal): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = firestore.collection("users").document(uid).collection("savingsGoals")
        val docRef = if (goal.id.isBlank()) col.document() else col.document(goal.id)
        val withId = goal.copy(id = docRef.id)
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun deleteSavingsGoal(goalId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        firestore.collection("users")
            .document(uid)
            .collection("savingsGoals")
            .document(goalId)
            .delete()
            .await()
    }

    // --- Chat Messages ---

    fun observeChatMessages(userId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .collection("chatMessages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val role = doc.getString("role") ?: "model"
                    val content = doc.getString("content") ?: return@mapNotNull null
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    ChatMessage(
                        id = doc.id,
                        role = role,
                        content = content,
                        timestamp = timestamp
                    )
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveChatMessage(message: ChatMessage): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        firestore.collection("users")
            .document(uid)
            .collection("chatMessages")
            .add(message.toFirestoreMap())
            .await()
    }

    suspend fun clearChatHistory(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("chatMessages")
            .get()
            .await()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
