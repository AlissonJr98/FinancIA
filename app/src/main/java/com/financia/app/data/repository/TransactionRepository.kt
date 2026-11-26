package com.financia.app.data.repository

import com.financia.app.data.model.ChatMessage
import com.financia.app.data.model.CreditCardItem
import com.financia.app.data.model.ExpenseNature
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.FixedBillTemplate
import com.financia.app.data.model.LoanContract
import com.financia.app.data.model.LoanType
import com.financia.app.data.model.ReceivableItem
import com.financia.app.data.model.SavingsGoal
import com.financia.app.data.model.TransactionCategory
import com.financia.app.data.model.TransactionType
import com.financia.app.data.model.WishlistItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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
    /**
     * Resolve automaticamente a raiz dos dados:
     * - Se `sharedSpaceId` estiver preenchido (ex: "CASAL-4829"), lê e salva em `/sharedSpaces/{spaceId}`
     *   (sincronizado em tempo real entre o casal / família).
     * - Caso contrário, usa `/users/{userId}` (carteira individual).
     */
    private fun rootDoc(userId: String, sharedSpaceId: String = ""): DocumentReference {
        val cleanSpace = sharedSpaceId.trim().uppercase()
        return if (cleanSpace.isNotBlank()) {
            firestore.collection("sharedSpaces").document(cleanSpace)
        } else {
            firestore.collection("users").document(userId)
        }
    }

    private fun currentAuthorName(): String {
        val user = auth.currentUser
        return user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Alisson"
    }

    suspend fun createOrJoinSharedSpace(spaceId: String, partnerNameHint: String = ""): Result<String> = runCatching {
        val user = auth.currentUser ?: error("Usuário não autenticado.")
        val code = spaceId.trim().uppercase()
        require(code.length >= 4) { "Código do Espaço Compartilhado deve ter pelo menos 4 caracteres." }

        val spaceDoc = firestore.collection("sharedSpaces").document(code)
        val meta = mutableMapOf<String, Any>(
            "spaceId" to code,
            "spaceName" to "Espaço Casal & Família ($code)",
            "updatedAt" to System.currentTimeMillis()
        )
        if (partnerNameHint.isNotBlank()) {
            meta["partnerName"] = partnerNameHint.trim().take(120)
        }
        runCatching {
            spaceDoc.set(meta, SetOptions.merge()).await()
        }

        // Vincula o perfil do usuário ao código do casal
        firestore.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "sharedSpaceId" to code,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
        code
    }

    suspend fun leaveSharedSpace(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Usuário não autenticado.")
        firestore.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "sharedSpaceId" to "",
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    fun observeTransactions(userId: String, sharedSpaceId: String = ""): Flow<List<FinancialTransaction>> = callbackFlow {
        val subscription = rootDoc(userId, sharedSpaceId)
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
                    val accountName = doc.getString("accountName") ?: "Nubank"
                    val notes = doc.getString("notes") ?: ""
                    val createdByAi = doc.getBoolean("createdByAi") ?: false
                    val isRecurring = doc.getBoolean("isRecurring") ?: false
                    val installmentCount = (doc.getLong("installmentCount") ?: 1L).toInt()
                    val installmentIndex = (doc.getLong("installmentIndex") ?: 1L).toInt()
                    val expenseNature = doc.getString("expenseNature")
                        ?: if (isRecurring) ExpenseNature.FIXED.name else ExpenseNature.VARIABLE.name
                    val fixedBillId = doc.getString("fixedBillId") ?: ""
                    val loanId = doc.getString("loanId") ?: ""
                    val authorName = doc.getString("authorName") ?: ""
                    val authorUid = doc.getString("authorUid") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: dateTimestamp

                    FinancialTransaction(
                        id = doc.id,
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        dateTimestamp = dateTimestamp,
                        paymentMethod = paymentMethod,
                        accountName = accountName,
                        notes = notes,
                        createdByAi = createdByAi,
                        isRecurring = isRecurring,
                        installmentCount = installmentCount,
                        installmentIndex = installmentIndex,
                        expenseNature = expenseNature,
                        fixedBillId = fixedBillId,
                        loanId = loanId,
                        authorName = authorName,
                        authorUid = authorUid,
                        createdAt = createdAt
                    )
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addTransaction(transaction: FinancialTransaction, sharedSpaceId: String = ""): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val author = transaction.authorName.ifBlank { currentAuthorName() }
        val colRef = rootDoc(uid, sharedSpaceId).collection("transactions")

        val totalInstallments = transaction.installmentCount.coerceIn(1, 360)
        if (totalInstallments <= 1) {
            val docRef = colRef.document()
            val withId = transaction.copy(
                id = docRef.id,
                installmentCount = 1,
                installmentIndex = 1,
                authorName = author,
                authorUid = uid,
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
                    authorName = author,
                    authorUid = uid,
                    createdAt = System.currentTimeMillis()
                )
                batch.set(docRef, instTx.toFirestoreMap())
                cal.add(Calendar.MONTH, 1)
            }
            batch.commit().await()
            firstId
        }
    }

    suspend fun updateTransaction(transaction: FinancialTransaction, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        require(transaction.id.isNotBlank()) { "ID da transação inválido." }
        rootDoc(uid, sharedSpaceId)
            .collection("transactions")
            .document(transaction.id)
            .set(transaction.toFirestoreMap())
            .await()
    }

    suspend fun deleteTransaction(transactionId: String, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        rootDoc(uid, sharedSpaceId)
            .collection("transactions")
            .document(transactionId)
            .delete()
            .await()
    }

    // --- Contas Fixas Mensais (Água, Luz, Internet...) ---

    @Suppress("UNCHECKED_CAST")
    fun observeFixedBills(userId: String, sharedSpaceId: String = ""): Flow<List<FixedBillTemplate>> = callbackFlow {
        val subscription = rootDoc(userId, sharedSpaceId)
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
                    val authorName = doc.getString("authorName") ?: ""

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
                        authorName = authorName,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveFixedBill(bill: FixedBillTemplate, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = rootDoc(uid, sharedSpaceId).collection("fixedBills")
        val docRef = if (bill.id.isBlank()) col.document() else col.document(bill.id)
        val withId = bill.copy(id = docRef.id, authorName = bill.authorName.ifBlank { currentAuthorName() })
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun payFixedBillForMonth(
        bill: FixedBillTemplate,
        monthKey: String,
        amountForThisMonth: Double,
        targetTimestamp: Long,
        sharedSpaceId: String = ""
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
        rootDoc(uid, sharedSpaceId)
            .collection("fixedBills")
            .document(bill.id)
            .set(updatedBill.toFirestoreMap())
            .await()

        val txCol = rootDoc(uid, sharedSpaceId).collection("transactions")
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
            fixedBillId = bill.id,
            authorName = currentAuthorName(),
            authorUid = uid
        )
        txDoc.set(tx.toFirestoreMap()).await()
    }

    suspend fun deleteFixedBill(billId: String, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        rootDoc(uid, sharedSpaceId)
            .collection("fixedBills")
            .document(billId)
            .delete()
            .await()
    }

    // --- Empréstimos, Financiamentos e Renegociações ---

    fun observeLoans(userId: String, sharedSpaceId: String = ""): Flow<List<LoanContract>> = callbackFlow {
        val subscription = rootDoc(userId, sharedSpaceId)
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
                    val authorName = doc.getString("authorName") ?: ""

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
                        authorName = authorName,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveLoanContract(loan: LoanContract, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = rootDoc(uid, sharedSpaceId).collection("loans")
        val docRef = if (loan.id.isBlank()) col.document() else col.document(loan.id)
        val withId = loan.copy(id = docRef.id, authorName = loan.authorName.ifBlank { currentAuthorName() })
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun payOrAmortizeLoanInstallment(
        loan: LoanContract,
        installmentsPaidNow: Int = 1,
        customPaidAmount: Double = loan.installmentAmount * installmentsPaidNow,
        monthKey: String,
        targetTimestamp: Long,
        isAmortization: Boolean = false,
        sharedSpaceId: String = ""
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val newPaidCount = (loan.paidInstallments + installmentsPaidNow).coerceAtMost(loan.totalInstallments)
        val updatedLoan = loan.copy(
            paidInstallments = newPaidCount,
            lastPaidMonthKey = if (!isAmortization) monthKey else loan.lastPaidMonthKey
        )

        rootDoc(uid, sharedSpaceId)
            .collection("loans")
            .document(loan.id)
            .set(updatedLoan.toFirestoreMap())
            .await()

        val txCol = rootDoc(uid, sharedSpaceId).collection("transactions")
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
            loanId = loan.id,
            authorName = currentAuthorName(),
            authorUid = uid
        )
        txDoc.set(tx.toFirestoreMap()).await()
    }

    suspend fun deleteLoanContract(loanId: String, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        rootDoc(uid, sharedSpaceId)
            .collection("loans")
            .document(loanId)
            .delete()
            .await()
    }

    // --- Caixinhas / Metas de Economia ---

    fun observeSavingsGoals(userId: String, sharedSpaceId: String = ""): Flow<List<SavingsGoal>> = callbackFlow {
        val subscription = rootDoc(userId, sharedSpaceId)
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

    suspend fun saveSavingsGoal(goal: SavingsGoal, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = rootDoc(uid, sharedSpaceId).collection("savingsGoals")
        val docRef = if (goal.id.isBlank()) col.document() else col.document(goal.id)
        val withId = goal.copy(id = docRef.id)
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun deleteSavingsGoal(goalId: String, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        rootDoc(uid, sharedSpaceId)
            .collection("savingsGoals")
            .document(goalId)
            .delete()
            .await()
    }

    // --- Cartões de Crédito Virtuais & Faturas ---

    fun observeCreditCards(userId: String, sharedSpaceId: String = ""): Flow<List<CreditCardItem>> = callbackFlow {
        val subscription = rootDoc(userId, sharedSpaceId)
            .collection("creditCards")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val lastDigits = doc.getString("lastDigits") ?: ""
                    val limitTotal = doc.getDouble("limitTotal") ?: 3000.0
                    val closingDay = (doc.getLong("closingDay") ?: 25L).toInt().coerceIn(1, 31)
                    val dueDay = (doc.getLong("dueDay") ?: 5L).toInt().coerceIn(1, 31)
                    val currentInvoice = doc.getDouble("currentInvoice") ?: 0.0
                    val colorHex = doc.getLong("colorHex") ?: 0xFF8A05BEL
                    val authorName = doc.getString("authorName") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    CreditCardItem(
                        id = doc.id,
                        name = name,
                        lastDigits = lastDigits,
                        limitTotal = limitTotal,
                        closingDay = closingDay,
                        dueDay = dueDay,
                        currentInvoice = currentInvoice,
                        colorHex = colorHex,
                        authorName = authorName,
                        createdAt = createdAt
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveCreditCard(card: CreditCardItem, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = rootDoc(uid, sharedSpaceId).collection("creditCards")
        val docRef = if (card.id.isBlank()) col.document() else col.document(card.id)
        val withId = card.copy(id = docRef.id, authorName = card.authorName.ifBlank { currentAuthorName() })
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun deleteCreditCard(cardId: String, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        rootDoc(uid, sharedSpaceId)
            .collection("creditCards")
            .document(cardId)
            .delete()
            .await()
    }

    suspend fun payCreditCardInvoice(
        card: CreditCardItem,
        amountToPay: Double,
        targetTimestamp: Long = System.currentTimeMillis(),
        sharedSpaceId: String = ""
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val txCol = rootDoc(uid, sharedSpaceId).collection("transactions")
        val txDoc = txCol.document()
        val author = currentAuthorName()

        val tx = FinancialTransaction(
            id = txDoc.id,
            title = "Pagamento de Fatura - ${card.name} (•••• ${card.lastDigits.ifBlank { "0000" }})",
            amount = amountToPay,
            type = TransactionType.EXPENSE.name,
            category = TransactionCategory.RENEGOCIACAO.label,
            dateTimestamp = targetTimestamp,
            paymentMethod = "Boleto / Pix",
            accountName = card.name,
            notes = "Fatura paga via app • Limite liberado",
            createdByAi = false,
            isRecurring = false,
            installmentCount = 1,
            installmentIndex = 1,
            expenseNature = ExpenseNature.VARIABLE.name,
            authorName = author,
            authorUid = uid
        )
        txDoc.set(tx.toFirestoreMap()).await()

        // Zera ou abate da fatura atual do cartão
        val newInvoice = (card.currentInvoice - amountToPay).coerceAtLeast(0.0)
        saveCreditCard(card.copy(currentInvoice = newInvoice), sharedSpaceId)
    }

    // --- Quem me deve / Valores A Receber ---

    fun observeReceivables(userId: String, sharedSpaceId: String = ""): Flow<List<ReceivableItem>> = callbackFlow {
        val subscription = rootDoc(userId, sharedSpaceId)
            .collection("receivables")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val personName = doc.getString("personName") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val amount = doc.getDouble("amount") ?: 0.0
                    val dueDateNote = doc.getString("dueDateNote") ?: ""
                    val isReceived = doc.getBoolean("isReceived") ?: false

                    ReceivableItem(
                        id = doc.id,
                        personName = personName,
                        description = description,
                        amount = amount,
                        dueDateNote = dueDateNote,
                        isReceived = isReceived,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveReceivable(item: ReceivableItem, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = rootDoc(uid, sharedSpaceId).collection("receivables")
        val docRef = if (item.id.isBlank()) col.document() else col.document(item.id)
        val withId = item.copy(id = docRef.id)
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun markReceivableAsPaid(item: ReceivableItem, targetTimestamp: Long, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val updated = item.copy(isReceived = true)
        rootDoc(uid, sharedSpaceId)
            .collection("receivables")
            .document(item.id)
            .set(updated.toFirestoreMap())
            .await()

        val txCol = rootDoc(uid, sharedSpaceId).collection("transactions")
        val txDoc = txCol.document()
        val tx = FinancialTransaction(
            id = txDoc.id,
            title = "Recebido de ${item.personName} (${item.description})",
            amount = item.amount,
            type = TransactionType.INCOME.name,
            category = TransactionCategory.OUTROS.label,
            dateTimestamp = targetTimestamp,
            paymentMethod = "Pix",
            notes = "Reembolso / valor que ${item.personName} devia",
            createdByAi = false,
            authorName = currentAuthorName(),
            authorUid = uid
        )
        txDoc.set(tx.toFirestoreMap()).await()
    }

    suspend fun deleteReceivable(itemId: String, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        rootDoc(uid, sharedSpaceId)
            .collection("receivables")
            .document(itemId)
            .delete()
            .await()
    }

    // --- Lista de Compras Planejadas (Wishlist) ---

    fun observeWishlist(userId: String, sharedSpaceId: String = ""): Flow<List<WishlistItem>> = callbackFlow {
        val subscription = rootDoc(userId, sharedSpaceId)
            .collection("wishlist")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val totalPrice = doc.getDouble("totalPrice") ?: 0.0
                    val installments = (doc.getLong("installments") ?: 1L).toInt()
                    val priority = doc.getString("priority") ?: "Média"
                    val bestMonthHint = doc.getString("bestMonthHint") ?: ""

                    WishlistItem(
                        id = doc.id,
                        title = title,
                        totalPrice = totalPrice,
                        installments = installments,
                        priority = priority,
                        bestMonthHint = bestMonthHint,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveWishlistItem(item: WishlistItem, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val col = rootDoc(uid, sharedSpaceId).collection("wishlist")
        val docRef = if (item.id.isBlank()) col.document() else col.document(item.id)
        val withId = item.copy(id = docRef.id)
        docRef.set(withId.toFirestoreMap()).await()
    }

    suspend fun deleteWishlistItem(itemId: String, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        rootDoc(uid, sharedSpaceId)
            .collection("wishlist")
            .document(itemId)
            .delete()
            .await()
    }

    // --- Chat Messages ---

    fun observeChatMessages(userId: String, sharedSpaceId: String = ""): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = rootDoc(userId, sharedSpaceId)
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

    suspend fun saveChatMessage(message: ChatMessage, sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        rootDoc(uid, sharedSpaceId)
            .collection("chatMessages")
            .add(message.toFirestoreMap())
            .await()
    }

    suspend fun clearChatHistory(sharedSpaceId: String = ""): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Usuário não autenticado.")
        val snapshot = rootDoc(uid, sharedSpaceId)
            .collection("chatMessages")
            .get()
            .await()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
