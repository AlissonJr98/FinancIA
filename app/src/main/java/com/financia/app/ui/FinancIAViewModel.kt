package com.financia.app.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financia.app.core.update.AppUpdateInfo
import com.financia.app.core.update.GitHubUpdateChecker
import com.financia.app.data.model.AiExtractedTransaction
import com.financia.app.data.model.ChatMessage
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.TransactionCategory
import com.financia.app.data.model.TransactionType
import com.financia.app.data.repository.AiFinancialRepository
import com.financia.app.data.repository.AuthRepository
import com.financia.app.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class FinancIAUiState(
    val isLoadingAuth: Boolean = false,
    val authError: String? = null,
    val isProcessingAi: Boolean = false,
    val aiStatusMessage: String? = null,
    val pendingAiDraft: AiExtractedTransaction? = null,
    val isAdvisorThinking: Boolean = false,
    val selectedCategoryFilter: String? = null,
    val searchQuery: String = "",
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val isPrivacyMode: Boolean = false,
    // OTA Update States
    val updateInfo: AppUpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateProgress: Float = 0f,
    val updateProgressText: String = "",
    val downloadedApkFile: File? = null,
    val updateError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinancIAViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val aiRepository: AiFinancialRepository,
    private val updateChecker: GitHubUpdateChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancIAUiState())
    val uiState: StateFlow<FinancIAUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = authRepository.authStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser)

    val allTransactions: StateFlow<List<FinancialTransaction>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                transactionRepository.observeTransactions(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Transações filtradas pelo Mês/Ano selecionado no Dashboard (incluindo lançamentos fixos mensais).
     */
    val currentMonthTransactions: StateFlow<List<FinancialTransaction>> = combine(
        allTransactions,
        _uiState
    ) { list, state ->
        val cal = Calendar.getInstance()
        list.filter { tx ->
            cal.timeInMillis = tx.dateTimestamp
            val txMonth = cal.get(Calendar.MONTH)
            val txYear = cal.get(Calendar.YEAR)
            (txMonth == state.selectedMonth && txYear == state.selectedYear) ||
                (tx.isRecurring && (txYear < state.selectedYear || (txYear == state.selectedYear && txMonth <= state.selectedMonth)))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyBudget: StateFlow<Double> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                authRepository.observeMonthlyBudget(user.uid)
            } else {
                flowOf(0.0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val chatMessages: StateFlow<List<ChatMessage>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                transactionRepository.observeChatMessages(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkForGitHubUpdates(silent = true)
    }

    // --- GitHub OTA Auto-Update ---

    fun checkForGitHubUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            updateChecker.checkForUpdates()
                .onSuccess { info ->
                    if (!silent || info.hasUpdate) {
                        _uiState.update {
                            it.copy(
                                updateInfo = info,
                                showUpdateDialog = true,
                                updateError = null
                            )
                        }
                    } else {
                        _uiState.update { it.copy(updateInfo = info) }
                    }
                }
                .onFailure { err ->
                    if (!silent) {
                        _uiState.update {
                            it.copy(
                                updateError = err.localizedMessage ?: "Não foi possível verificar atualizações.",
                                showUpdateDialog = true
                            )
                        }
                    }
                }
        }
    }

    fun startUpdateDownload(apkUrl: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingUpdate = true,
                    updateProgress = 0f,
                    updateProgressText = "Iniciando download da atualização...",
                    updateError = null,
                    downloadedApkFile = null
                )
            }

            updateChecker.downloadApkInApp(
                apkUrl = apkUrl,
                onProgress = { progress, downloadedMb, totalMb ->
                    val pct = (progress * 100).toInt()
                    val text = String.format(Locale.US, "%.1f MB / %.1f MB (%d%%)", downloadedMb, totalMb, pct)
                    _uiState.update {
                        it.copy(
                            updateProgress = progress,
                            updateProgressText = text
                        )
                    }
                },
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            isDownloadingUpdate = false,
                            updateProgress = 1f,
                            downloadedApkFile = file
                        )
                    }
                    if (updateChecker.canRequestPackageInstalls()) {
                        updateChecker.installApk(file)
                    }
                },
                onError = { errorMsg ->
                    _uiState.update {
                        it.copy(
                            isDownloadingUpdate = false,
                            updateError = errorMsg
                        )
                    }
                }
            )
        }
    }

    fun canRequestPackageInstalls(): Boolean = updateChecker.canRequestPackageInstalls()

    fun requestInstallPermission() {
        updateChecker.openInstallPermissionSettings()
    }

    fun installDownloadedApk() {
        val file = _uiState.value.downloadedApkFile ?: return
        updateChecker.installApk(file)
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false, updateError = null) }
    }

    // --- Authentication (Email, Google Sign-In & Guest) ---

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(authError = "Preencha e-mail e senha.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            authRepository.signInWithEmail(email, pass)
                .onFailure { err ->
                    _uiState.update {
                        it.copy(authError = err.localizedMessage ?: "Falha na autenticação.")
                    }
                }
            _uiState.update { it.copy(isLoadingAuth = false) }
        }
    }

    fun signUp(name: String, email: String, pass: String) {
        if (email.isBlank() || pass.length < 6) {
            _uiState.update { it.copy(authError = "Informe um e-mail válido e senha com 6+ caracteres.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            authRepository.signUpWithEmail(name, email, pass)
                .onFailure { err ->
                    _uiState.update {
                        it.copy(authError = err.localizedMessage ?: "Não foi possível criar a conta.")
                    }
                }
            _uiState.update { it.copy(isLoadingAuth = false) }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            authRepository.signInWithGoogleIdToken(idToken)
                .onFailure { err ->
                    _uiState.update {
                        it.copy(authError = err.localizedMessage ?: "Falha ao autenticar com o Google.")
                    }
                }
            _uiState.update { it.copy(isLoadingAuth = false) }
        }
    }

    fun setAuthError(message: String) {
        _uiState.update { it.copy(authError = message, isLoadingAuth = false) }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            authRepository.signInAnonymously()
                .onFailure { err ->
                    _uiState.update {
                        it.copy(authError = err.localizedMessage ?: "Erro ao entrar como convidado.")
                    }
                }
            _uiState.update { it.copy(isLoadingAuth = false) }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    // --- Dashboard Filters, Privacy & Budget ---

    fun setCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun togglePrivacyMode() {
        _uiState.update { it.copy(isPrivacyMode = !it.isPrivacyMode) }
    }

    fun previousMonth() {
        _uiState.update { state ->
            if (state.selectedMonth == 0) {
                state.copy(selectedMonth = 11, selectedYear = state.selectedYear - 1)
            } else {
                state.copy(selectedMonth = state.selectedMonth - 1)
            }
        }
    }

    fun nextMonth() {
        _uiState.update { state ->
            if (state.selectedMonth == 11) {
                state.copy(selectedMonth = 0, selectedYear = state.selectedYear + 1)
            } else {
                state.copy(selectedMonth = state.selectedMonth + 1)
            }
        }
    }

    fun updateMonthlyBudget(newBudget: Double) {
        viewModelScope.launch {
            authRepository.updateMonthlyBudget(newBudget)
        }
    }

    // --- Transactions CRUD ---

    fun addManualTransaction(
        title: String,
        amount: Double,
        isIncome: Boolean,
        category: String,
        paymentMethod: String,
        notes: String,
        createdByAi: Boolean = false,
        isRecurring: Boolean = false,
        installmentCount: Int = 1
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            val tx = FinancialTransaction(
                title = title.trim(),
                amount = amount,
                type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                category = category,
                paymentMethod = paymentMethod,
                notes = notes.trim(),
                createdByAi = createdByAi,
                isRecurring = isRecurring,
                installmentCount = installmentCount.coerceIn(1, 48)
            )
            transactionRepository.addTransaction(tx)
            _uiState.update { it.copy(pendingAiDraft = null, aiStatusMessage = "Lançamento salvo na nuvem!") }
        }
    }

    fun updateExistingTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    // --- AI Smart Entry (Natural Language, Voice & Receipt Photo) ---

    fun processNaturalLanguageInput(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    aiStatusMessage = "Analisando com Gemini IA..."
                )
            }
            val result = aiRepository.parseNaturalLanguageTransaction(text)
            val extracted = result.getOrElse {
                aiRepository.fallbackHeuristicParse(text)
            }
            _uiState.update {
                it.copy(
                    isProcessingAi = false,
                    pendingAiDraft = extracted,
                    aiStatusMessage = "Campos preenchidos pela IA! Revise e confirme."
                )
            }
        }
    }

    fun processReceiptPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingAi = true,
                    aiStatusMessage = "Lendo comprovante com Visão Computacional Gemini..."
                )
            }
            aiRepository.parseReceiptImage(bitmap)
                .onSuccess { extracted ->
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            pendingAiDraft = extracted,
                            aiStatusMessage = "Comprovante analisado com sucesso! Revise os dados."
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isProcessingAi = false,
                            aiStatusMessage = "Não foi possível ler o comprovante: ${err.localizedMessage ?: "tente novamente"}"
                        )
                    }
                }
        }
    }

    fun autoCategorizeTitle(title: String, onCategorySuggested: (TransactionCategory) -> Unit) {
        if (title.length < 3) return
        viewModelScope.launch {
            aiRepository.suggestCategory(title)
                .onSuccess { onCategorySuggested(it) }
                .onFailure {
                    val fallback = aiRepository.fallbackHeuristicParse(title)
                    onCategorySuggested(TransactionCategory.fromLabelOrName(fallback.category))
                }
        }
    }

    fun clearPendingAiDraft() {
        _uiState.update { it.copy(pendingAiDraft = null, aiStatusMessage = null) }
    }

    // --- AI Financial Advisor Chat with Function Calling ---

    fun sendAdvisorMessage(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            val userMsg = ChatMessage(role = "user", content = question.trim())
            transactionRepository.saveChatMessage(userMsg)
            _uiState.update { it.copy(isAdvisorThinking = true) }

            val currentTx = currentMonthTransactions.value
            val budget = monthlyBudget.value
            val responseResult = aiRepository.askFinancialAdvisorWithActions(
                userQuestion = question,
                transactions = currentTx,
                monthlyBudget = budget
            )

            val answerText = responseResult.fold(
                onSuccess = { actionResponse ->
                    // Se a IA extraiu transações pedidas no chat, salva automaticamente no Firestore!
                    actionResponse.transactionsToCreate.forEach { extracted ->
                        if (extracted.amount > 0.0 && extracted.title.isNotBlank()) {
                            val tx = FinancialTransaction(
                                title = extracted.title,
                                amount = extracted.amount,
                                type = if (extracted.type.equals("INCOME", ignoreCase = true)) {
                                    TransactionType.INCOME.name
                                } else {
                                    TransactionType.EXPENSE.name
                                },
                                category = extracted.category,
                                paymentMethod = extracted.paymentMethod,
                                notes = extracted.notes.ifBlank { "Lançado via Chat Consultor IA" },
                                createdByAi = true,
                                isRecurring = extracted.isRecurring,
                                installmentCount = extracted.installmentCount.coerceIn(1, 48)
                            )
                            transactionRepository.addTransaction(tx)
                        }
                    }
                    actionResponse.reply
                },
                onFailure = {
                    // Fallback local se o usuário pediu para registrar gastos no chat e o Gemini estiver offline
                    val lower = question.lowercase()
                    if (listOf("registra", "lança", "lanca", "anota", "gastei", "paguei", "recebi").any { lower.contains(it) }) {
                        val parts = question.split(Regex("""(?i)\s+e\s+|,""")).filter { it.isNotBlank() }
                        val createdList = mutableListOf<String>()
                        parts.forEach { part ->
                            val parsed = aiRepository.fallbackHeuristicParse(part)
                            if (parsed.amount > 0.0) {
                                val tx = FinancialTransaction(
                                    title = parsed.title,
                                    amount = parsed.amount,
                                    type = parsed.type,
                                    category = parsed.category,
                                    paymentMethod = parsed.paymentMethod,
                                    notes = "Lançado via Chat IA",
                                    createdByAi = true,
                                    isRecurring = parsed.isRecurring,
                                    installmentCount = parsed.installmentCount
                                )
                                transactionRepository.addTransaction(tx)
                                createdList.add("• ${parsed.title}: R$ ${parsed.amount} (${parsed.category})")
                            }
                        }
                        if (createdList.isNotEmpty()) {
                            "Pronto! Registrei automaticamente para você no Firestore:\n${createdList.joinToString("\n")}"
                        } else {
                            "Não consegui conectar ao Gemini no momento. Verifique sua conexão ou ative o Firebase AI Logic no console."
                        }
                    } else {
                        "Não consegui conectar ao Gemini no momento. Verifique sua conexão ou ative o Firebase AI Logic no console."
                    }
                }
            )

            val aiMsg = ChatMessage(role = "model", content = answerText)
            transactionRepository.saveChatMessage(aiMsg)
            _uiState.update { it.copy(isAdvisorThinking = false) }
        }
    }

    fun clearAdvisorChat() {
        viewModelScope.launch {
            transactionRepository.clearChatHistory()
        }
    }
}
