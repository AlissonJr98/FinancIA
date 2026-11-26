package com.financia.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financia.app.core.importer.BankStatementImporter
import com.financia.app.core.update.AppUpdateInfo
import com.financia.app.core.update.GitHubUpdateChecker
import com.financia.app.data.model.AchievementBadge
import com.financia.app.data.model.AiExtractedTransaction
import com.financia.app.data.model.ChatMessage
import com.financia.app.data.model.ExpenseNature
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.FixedBillTemplate
import com.financia.app.data.model.FutureMonthProjection
import com.financia.app.data.model.LoanContract
import com.financia.app.data.model.MonthlyBarStat
import com.financia.app.data.model.ReceivableItem
import com.financia.app.data.model.SavingsGoal
import com.financia.app.data.model.TransactionCategory
import com.financia.app.data.model.TransactionType
import com.financia.app.data.model.UserProfileConfig
import com.financia.app.data.model.WishlistItem
import com.financia.app.data.repository.AiFinancialRepository
import com.financia.app.data.repository.AuthRepository
import com.financia.app.data.repository.TransactionRepository
import com.financia.app.ui.theme.AppThemeMode
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
import java.text.DateFormatSymbols
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
    val themeMode: AppThemeMode = AppThemeMode.DARK_AMOLED,
    val isViewingSharedCoupleSpace: Boolean = true,
    val lastDeletedTransaction: FinancialTransaction? = null,
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

    val userProfileConfig: StateFlow<UserProfileConfig> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                authRepository.observeUserProfileConfig(user.uid)
            } else {
                flowOf(UserProfileConfig())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileConfig())

    /**
     * Resolve o `sharedSpaceId` ativo (se o usuário está conectado a um espaço de casal e com a chave ativada).
     */
    private val activeSpaceFlow: StateFlow<Pair<String?, String>> = combine(
        currentUser,
        userProfileConfig,
        _uiState
    ) { user, profile, state ->
        val uid = user?.uid
        val spaceId = if (state.isViewingSharedCoupleSpace && profile.sharedSpaceId.isNotBlank()) {
            profile.sharedSpaceId
        } else {
            ""
        }
        uid to spaceId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null to "")

    val allTransactions: StateFlow<List<FinancialTransaction>> = activeSpaceFlow
        .flatMapLatest { (uid, spaceId) ->
            if (uid != null) {
                transactionRepository.observeTransactions(uid, spaceId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val sixMonthsHistoryStats: StateFlow<List<MonthlyBarStat>> = combine(
        allTransactions,
        _uiState
    ) { list, state ->
        val shortMonths = DateFormatSymbols(Locale("pt", "BR")).shortMonths
        val cal = Calendar.getInstance()
        val stats = mutableListOf<MonthlyBarStat>()

        for (offset in 5 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, state.selectedYear)
                set(Calendar.MONTH, state.selectedMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -offset)
            }
            val m = targetCal.get(Calendar.MONTH)
            val y = targetCal.get(Calendar.YEAR)
            val label = shortMonths.getOrElse(m) { "" }.replace(".", "").take(3)
                .replaceFirstChar { it.titlecase(Locale("pt", "BR")) }

            val monthTx = list.filter { tx ->
                cal.timeInMillis = tx.dateTimestamp
                val txM = cal.get(Calendar.MONTH)
                val txY = cal.get(Calendar.YEAR)
                (txM == m && txY == y) ||
                    (tx.isRecurring && (txY < y || (txY == y && txM <= m)))
            }

            val income = monthTx.filter { it.isIncome }.sumOf { it.amount }
            val fixedDebt = monthTx.filter { !it.isIncome && it.isFixedOrDebt }.sumOf { it.amount }
            val variable = monthTx.filter { !it.isIncome && !it.isFixedOrDebt }.sumOf { it.amount }

            stats.add(
                MonthlyBarStat(
                    monthLabel = label,
                    income = income,
                    fixedAndDebt = fixedDebt,
                    variable = variable
                )
            )
        }
        stats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fixedBills: StateFlow<List<FixedBillTemplate>> = activeSpaceFlow
        .flatMapLatest { (uid, spaceId) ->
            if (uid != null) {
                transactionRepository.observeFixedBills(uid, spaceId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<LoanContract>> = activeSpaceFlow
        .flatMapLatest { (uid, spaceId) ->
            if (uid != null) {
                transactionRepository.observeLoans(uid, spaceId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoal>> = activeSpaceFlow
        .flatMapLatest { (uid, spaceId) ->
            if (uid != null) {
                transactionRepository.observeSavingsGoals(uid, spaceId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivables: StateFlow<List<ReceivableItem>> = activeSpaceFlow
        .flatMapLatest { (uid, spaceId) ->
            if (uid != null) {
                transactionRepository.observeReceivables(uid, spaceId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlist: StateFlow<List<WishlistItem>> = activeSpaceFlow
        .flatMapLatest { (uid, spaceId) ->
            if (uid != null) {
                transactionRepository.observeWishlist(uid, spaceId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyBudget: StateFlow<Double> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                authRepository.observeMonthlyBudget(user.uid)
            } else {
                flowOf(0.0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoryBudgets: StateFlow<Map<String, Double>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                authRepository.observeCategoryBudgets(user.uid)
            } else {
                flowOf(emptyMap())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val futureProjections: StateFlow<List<FutureMonthProjection>> = combine(
        allTransactions,
        fixedBills,
        loans,
        _uiState
    ) { allTx, bills, loanList, state ->
        val monthNames = DateFormatSymbols(Locale("pt", "BR")).months
        val cal = Calendar.getInstance()
        val projections = mutableListOf<FutureMonthProjection>()

        val fixedBillsSum = bills.sumOf { bill ->
            if (bill.defaultAmount > 0) bill.defaultAmount
            else bill.monthlyValues.values.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0
        }

        for (ahead in 1..3) {
            val targetCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, state.selectedYear)
                set(Calendar.MONTH, state.selectedMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, ahead)
            }
            val m = targetCal.get(Calendar.MONTH)
            val y = targetCal.get(Calendar.YEAR)
            val rawM = monthNames.getOrElse(m) { "" }
            val label = "${rawM.replaceFirstChar { it.titlecase(Locale("pt", "BR")) }}/$y"

            val activeLoansSum = loanList
                .filter { it.remainingInstallments >= ahead }
                .sumOf { it.installmentAmount }

            val finishingLoans = loanList.count { it.remainingInstallments == ahead }

            val futureTxInMonth = allTx.filter { tx ->
                cal.timeInMillis = tx.dateTimestamp
                !tx.isIncome && cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }
            val cardInstallmentsSum = futureTxInMonth.sumOf { it.amount }
            val finishingCardTx = futureTxInMonth.count { it.installmentCount > 1 && it.installmentIndex == it.installmentCount }

            projections.add(
                FutureMonthProjection(
                    monthLabel = label,
                    fixedBillsEstimate = fixedBillsSum,
                    loansInstallmentsTotal = activeLoansSum,
                    cardInstallmentsTotal = cardInstallmentsSum,
                    finishingInstallmentsCount = finishingLoans + finishingCardTx
                )
            )
        }
        projections
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Conquistas & Gamificação Financeira exibidas no Perfil.
     */
    val achievements: StateFlow<List<AchievementBadge>> = combine(
        currentMonthTransactions,
        fixedBills,
        loans,
        savingsGoals,
        userProfileConfig
    ) { monthTx, bills, loanList, goals, profile ->
        val inc = monthTx.filter { it.isIncome }.sumOf { it.amount }
        val exp = monthTx.filter { !it.isIncome }.sumOf { it.amount }
        listOf(
            AchievementBadge(
                id = "blue_month",
                title = "Mês no Azul",
                description = "Receitas maiores que as despesas no mês atual",
                emoji = "🏅",
                isUnlocked = inc > 0 && inc >= exp
            ),
            AchievementBadge(
                id = "fixed_bills_setup",
                title = "Contas Fixas Organizadas",
                description = "Cadastrou suas contas fixas (Água, Luz, Internet...)",
                emoji = "💧",
                isUnlocked = bills.isNotEmpty()
            ),
            AchievementBadge(
                id = "debt_tracker",
                title = "Estrategista de Dívidas",
                description = "Controlando ou amortizando empréstimos e financiamentos",
                emoji = "📉",
                isUnlocked = loanList.any { it.paidInstallments > 0 }
            ),
            AchievementBadge(
                id = "saver_box",
                title = "Investidor do Futuro",
                description = "Guardou dinheiro em pelo menos uma Caixinha/Meta",
                emoji = "🐷",
                isUnlocked = goals.any { it.currentAmount > 0 }
            ),
            AchievementBadge(
                id = "couple_mode",
                title = "Casal & Família Sincronizados",
                description = "Ativou o Espaço Compartilhado em tempo real",
                emoji = "💑",
                isUnlocked = profile.sharedSpaceId.isNotBlank()
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = activeSpaceFlow
        .flatMapLatest { (uid, spaceId) ->
            if (uid != null) {
                transactionRepository.observeChatMessages(uid, spaceId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkForGitHubUpdates(silent = true)
    }

    private fun currentActiveSpaceId(): String = activeSpaceFlow.value.second

    fun toggleSharedCoupleView() {
        _uiState.update { it.copy(isViewingSharedCoupleSpace = !it.isViewingSharedCoupleSpace) }
    }

    fun saveUserProfileConfig(config: UserProfileConfig) {
        viewModelScope.launch {
            authRepository.saveUserProfileConfig(config)
        }
    }

    fun createOrJoinSharedSpace(code: String, partnerName: String = "") {
        viewModelScope.launch {
            transactionRepository.createOrJoinSharedSpace(code, partnerName)
            _uiState.update { it.copy(isViewingSharedCoupleSpace = true) }
        }
    }

    fun leaveSharedSpace() {
        viewModelScope.launch {
            transactionRepository.leaveSharedSpace()
            _uiState.update { it.copy(isViewingSharedCoupleSpace = false) }
        }
    }

    fun cycleThemeMode() {
        _uiState.update { state ->
            val next = when (state.themeMode) {
                AppThemeMode.DARK_AMOLED -> AppThemeMode.LIGHT
                AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
                AppThemeMode.SYSTEM -> AppThemeMode.DARK_AMOLED
            }
            state.copy(themeMode = next)
        }
    }

    // --- OTA Auto-Update ---

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

    // --- Authentication ---

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

    fun updateCategoryBudget(categoryName: String, limit: Double) {
        viewModelScope.launch {
            authRepository.updateCategoryBudget(
                categoryName = categoryName,
                limit = limit,
                existing = categoryBudgets.value
            )
        }
    }

    private fun selectedMonthTimestamp(): Long {
        val state = _uiState.value
        val cal = Calendar.getInstance()
        val nowMonth = cal.get(Calendar.MONTH)
        val nowYear = cal.get(Calendar.YEAR)
        if (state.selectedMonth == nowMonth && state.selectedYear == nowYear) {
            return System.currentTimeMillis()
        }
        cal.set(Calendar.YEAR, state.selectedYear)
        cal.set(Calendar.MONTH, state.selectedMonth)
        cal.set(Calendar.DAY_OF_MONTH, 15)
        return cal.timeInMillis
    }

    // --- Transactions CRUD (Space-Aware) ---

    fun addManualTransaction(
        title: String,
        amount: Double,
        isIncome: Boolean,
        category: String,
        paymentMethod: String,
        accountName: String = "Nubank",
        dateTimestamp: Long = selectedMonthTimestamp(),
        notes: String,
        createdByAi: Boolean = false,
        isRecurring: Boolean = false,
        installmentCount: Int = 1,
        expenseNature: String = ExpenseNature.VARIABLE.name
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            val tx = FinancialTransaction(
                title = title.trim(),
                amount = amount,
                type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
                category = category,
                dateTimestamp = dateTimestamp,
                paymentMethod = paymentMethod,
                accountName = accountName,
                notes = notes.trim(),
                createdByAi = createdByAi,
                isRecurring = isRecurring,
                installmentCount = installmentCount.coerceIn(1, 360),
                expenseNature = expenseNature,
                authorName = userProfileConfig.value.displayName
            )
            transactionRepository.addTransaction(tx, currentActiveSpaceId())
            _uiState.update { it.copy(pendingAiDraft = null, aiStatusMessage = "Lançamento salvo na nuvem!") }
        }
    }

    fun importBankStatementFile(
        context: Context,
        uri: Uri,
        accountName: String,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val parsedList = BankStatementImporter.parseStatementUri(
                context = context,
                uri = uri,
                defaultAccountName = accountName,
                aiRepository = aiRepository
            )
            val targetTime = selectedMonthTimestamp()
            val spaceId = currentActiveSpaceId()
            parsedList.forEach { tx ->
                transactionRepository.addTransaction(
                    tx.copy(dateTimestamp = targetTime, authorName = userProfileConfig.value.displayName),
                    spaceId
                )
            }
            onComplete(parsedList.size)
        }
    }

    fun updateExistingTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction, currentActiveSpaceId())
        }
    }

    fun deleteTransaction(id: String) {
        val toDelete = allTransactions.value.firstOrNull { it.id == id }
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id, currentActiveSpaceId())
            if (toDelete != null) {
                _uiState.update { it.copy(lastDeletedTransaction = toDelete) }
            }
        }
    }

    fun undoDeleteTransaction() {
        val lastDeleted = _uiState.value.lastDeletedTransaction ?: return
        viewModelScope.launch {
            transactionRepository.addTransaction(
                lastDeleted.copy(id = "", installmentCount = 1),
                currentActiveSpaceId()
            )
            _uiState.update { it.copy(lastDeletedTransaction = null) }
        }
    }

    fun clearLastDeletedTransaction() {
        _uiState.update { it.copy(lastDeletedTransaction = null) }
    }

    fun duplicateTransaction(tx: FinancialTransaction) {
        viewModelScope.launch {
            val copyTx = tx.copy(
                id = "",
                dateTimestamp = System.currentTimeMillis(),
                installmentCount = 1,
                installmentIndex = 1,
                authorName = userProfileConfig.value.displayName,
                createdAt = System.currentTimeMillis()
            )
            transactionRepository.addTransaction(copyTx, currentActiveSpaceId())
        }
    }

    // --- Contas Fixas, Empréstimos, Caixinhas, A Receber & Wishlist ---

    fun saveFixedBill(bill: FixedBillTemplate) {
        viewModelScope.launch {
            transactionRepository.saveFixedBill(bill, currentActiveSpaceId())
        }
    }

    fun payFixedBillForMonth(bill: FixedBillTemplate, monthKey: String, amount: Double) {
        viewModelScope.launch {
            transactionRepository.payFixedBillForMonth(
                bill = bill,
                monthKey = monthKey,
                amountForThisMonth = amount,
                targetTimestamp = selectedMonthTimestamp(),
                sharedSpaceId = currentActiveSpaceId()
            )
        }
    }

    fun deleteFixedBill(billId: String) {
        viewModelScope.launch {
            transactionRepository.deleteFixedBill(billId, currentActiveSpaceId())
        }
    }

    fun saveLoanContract(loan: LoanContract) {
        viewModelScope.launch {
            transactionRepository.saveLoanContract(loan, currentActiveSpaceId())
        }
    }

    fun payOrAmortizeLoan(
        loan: LoanContract,
        installmentsPaidNow: Int,
        customPaidAmount: Double,
        monthKey: String,
        isAmortization: Boolean
    ) {
        viewModelScope.launch {
            transactionRepository.payOrAmortizeLoanInstallment(
                loan = loan,
                installmentsPaidNow = installmentsPaidNow,
                customPaidAmount = customPaidAmount,
                monthKey = monthKey,
                targetTimestamp = selectedMonthTimestamp(),
                isAmortization = isAmortization,
                sharedSpaceId = currentActiveSpaceId()
            )
        }
    }

    fun deleteLoanContract(loanId: String) {
        viewModelScope.launch {
            transactionRepository.deleteLoanContract(loanId, currentActiveSpaceId())
        }
    }

    fun saveSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            transactionRepository.saveSavingsGoal(goal, currentActiveSpaceId())
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            transactionRepository.deleteSavingsGoal(goalId, currentActiveSpaceId())
        }
    }

    fun saveReceivable(item: ReceivableItem) {
        viewModelScope.launch {
            transactionRepository.saveReceivable(item, currentActiveSpaceId())
        }
    }

    fun markReceivableAsPaid(item: ReceivableItem) {
        viewModelScope.launch {
            transactionRepository.markReceivableAsPaid(item, selectedMonthTimestamp(), currentActiveSpaceId())
        }
    }

    fun deleteReceivable(itemId: String) {
        viewModelScope.launch {
            transactionRepository.deleteReceivable(itemId, currentActiveSpaceId())
        }
    }

    fun saveWishlistItem(item: WishlistItem) {
        viewModelScope.launch {
            transactionRepository.saveWishlistItem(item, currentActiveSpaceId())
        }
    }

    fun deleteWishlistItem(itemId: String) {
        viewModelScope.launch {
            transactionRepository.deleteWishlistItem(itemId, currentActiveSpaceId())
        }
    }

    // --- AI Smart Entry ---

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

    // --- AI Financial Advisor & Couple Advisor ---

    fun sendAdvisorMessage(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            val spaceId = currentActiveSpaceId()
            val profile = userProfileConfig.value
            val userMsg = ChatMessage(role = "user", content = question.trim())
            transactionRepository.saveChatMessage(userMsg, spaceId)
            _uiState.update { it.copy(isAdvisorThinking = true) }

            val currentTx = currentMonthTransactions.value
            val currentBills = fixedBills.value
            val currentLoans = loans.value
            val budget = monthlyBudget.value

            val langInstruction = when (profile.languageCode) {
                "en-US" -> "Please reply in English (en-US)."
                "es-ES" -> "Por favor responde en Español (es-ES)."
                else -> "Responda em Português do Brasil (pt-BR)."
            }

            val responseResult = aiRepository.askFinancialAdvisorWithActions(
                userQuestion = "$question ($langInstruction)",
                transactions = currentTx,
                fixedBills = currentBills,
                loans = currentLoans,
                monthlyBudget = budget
            )

            val answerText = responseResult.fold(
                onSuccess = { actionResponse ->
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
                                dateTimestamp = selectedMonthTimestamp(),
                                paymentMethod = extracted.paymentMethod,
                                notes = extracted.notes.ifBlank { "Lançado via Chat Consultor IA" },
                                createdByAi = true,
                                isRecurring = extracted.isRecurring,
                                installmentCount = extracted.installmentCount.coerceIn(1, 360),
                                expenseNature = extracted.expenseNature,
                                authorName = profile.displayName
                            )
                            transactionRepository.addTransaction(tx, spaceId)
                        }
                    }
                    actionResponse.reply
                },
                onFailure = {
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
                                    dateTimestamp = selectedMonthTimestamp(),
                                    paymentMethod = parsed.paymentMethod,
                                    notes = "Lançado via Chat IA",
                                    createdByAi = true,
                                    isRecurring = parsed.isRecurring,
                                    installmentCount = parsed.installmentCount,
                                    expenseNature = parsed.expenseNature,
                                    authorName = profile.displayName
                                )
                                transactionRepository.addTransaction(tx, spaceId)
                                createdList.add("• ${parsed.title}: R$ ${parsed.amount} (${parsed.category})")
                            }
                        }
                        if (createdList.isNotEmpty()) {
                            "Pronto! Registrei automaticamente para você:\n${createdList.joinToString("\n")}"
                        } else {
                            "Não consegui conectar ao Gemini no momento. Verifique sua conexão."
                        }
                    } else {
                        "Não consegui conectar ao Gemini no momento. Verifique sua conexão."
                    }
                }
            )

            val aiMsg = ChatMessage(role = "model", content = answerText)
            transactionRepository.saveChatMessage(aiMsg, spaceId)
            _uiState.update { it.copy(isAdvisorThinking = false) }
        }
    }

    fun clearAdvisorChat() {
        viewModelScope.launch {
            transactionRepository.clearChatHistory(currentActiveSpaceId())
        }
    }
}
