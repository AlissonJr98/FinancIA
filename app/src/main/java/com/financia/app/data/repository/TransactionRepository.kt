package com.financia.app.data.repository

import com.financia.app.data.model.ChatMessage
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.TransactionCategory
import com.financia.app.data.model.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
        val docRef = colRef.document()
        val withId = transaction.copy(
            id = docRef.id,
            createdAt = System.currentTimeMillis()
        )
        docRef.set(withId.toFirestoreMap()).await()
        docRef.id
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
