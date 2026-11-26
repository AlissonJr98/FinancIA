package com.financia.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.financia.app.core.notifications.BillReminderNotifier
import com.financia.app.core.security.BiometricAuthHelper
import com.financia.app.ui.FinancIAViewModel
import com.financia.app.ui.components.UpdateDialog
import com.financia.app.ui.screens.AdvisorChatScreen
import com.financia.app.ui.screens.AuthScreen
import com.financia.app.ui.screens.DashboardScreen
import com.financia.app.ui.screens.FixedAndDebtsScreen
import com.financia.app.ui.screens.SmartEntryScreen
import com.financia.app.ui.theme.AppThemeMode
import com.financia.app.ui.theme.FinancIATheme
import dagger.hilt.android.AndroidEntryPoint

enum class MainTab(val title: String) {
    DASHBOARD("Visão Geral"),
    FIXED_AND_DEBTS("Fixas & Dívidas"),
    SMART_ADD("Lançar IA"),
    ADVISOR("Consultor IA")
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinancIAViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            FinancIATheme(themeMode = uiState.themeMode) {
                FinancIAAppRoot(activity = this, viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancIAAppRoot(
    activity: FragmentActivity,
    viewModel: FinancIAViewModel
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val monthTransactions by viewModel.currentMonthTransactions.collectAsState()
    val sixMonthsStats by viewModel.sixMonthsHistoryStats.collectAsState()
    val futureProjections by viewModel.futureProjections.collectAsState()
    val fixedBills by viewModel.fixedBills.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val receivables by viewModel.receivables.collectAsState()
    val wishlist by viewModel.wishlist.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Solicita permissão de notificação no Android 13+ e verifica Contas Fixas/Parcelas a vencer
    LaunchedEffect(currentUser, fixedBills, loans) {
        if (currentUser != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            BillReminderNotifier.checkAndNotifyDueBills(
                context = context,
                fixedBills = fixedBills,
                loans = loans
            )
        }
    }

    // Snackbar de Desfazer Exclusão
    LaunchedEffect(uiState.lastDeletedTransaction) {
        val deleted = uiState.lastDeletedTransaction
        if (deleted != null) {
            val result = snackbarHostState.showSnackbar(
                message = "'${deleted.title}' excluído",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteTransaction()
            } else {
                viewModel.clearLastDeletedTransaction()
            }
        }
    }

    var isBiometricEnabled by remember {
        mutableStateOf(BiometricAuthHelper.isBiometricEnabled(context))
    }
    var isAppUnlocked by rememberSaveable {
        mutableStateOf(!BiometricAuthHelper.isBiometricEnabled(context))
    }

    LaunchedEffect(currentUser, isBiometricEnabled) {
        if (currentUser != null && isBiometricEnabled && !isAppUnlocked) {
            BiometricAuthHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = { isAppUnlocked = true },
                onError = {}
            )
        }
    }

    if (uiState.showUpdateDialog && uiState.updateInfo != null) {
        UpdateDialog(
            updateInfo = uiState.updateInfo!!,
            isDownloading = uiState.isDownloadingUpdate,
            downloadProgress = uiState.updateProgress,
            downloadProgressText = uiState.updateProgressText,
            downloadedFile = uiState.downloadedApkFile,
            updateError = uiState.updateError,
            canInstall = viewModel.canRequestPackageInstalls(),
            onDismiss = viewModel::dismissUpdateDialog,
            onDownload = viewModel::startUpdateDownload,
            onInstall = viewModel::installDownloadedApk,
            onRequestPermission = viewModel::requestInstallPermission
        )
    }

    if (currentUser == null) {
        AuthScreen(
            isLoading = uiState.isLoadingAuth,
            errorMessage = uiState.authError,
            onSignIn = viewModel::signIn,
            onSignUp = viewModel::signUp,
            onGoogleSignIn = viewModel::signInWithGoogle,
            onGoogleSignInError = viewModel::setAuthError,
            onGuestSignIn = viewModel::signInAsGuest,
            onClearError = viewModel::clearAuthError
        )
        return
    }

    if (isBiometricEnabled && !isAppUnlocked) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometria",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "FinancIA Protegido",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Confirme sua digital ou PIN para acessar seus dados financeiros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        BiometricAuthHelper.showBiometricPrompt(
                            activity = activity,
                            onSuccess = { isAppUnlocked = true },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Desbloquear com Biometria")
                }
            }
        }
        return
    }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    val displayUserName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore("@")
        ?: "Alisson"

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FinancIA • ${currentTab.title}",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.cycleThemeMode() }) {
                        Icon(
                            imageVector = when (uiState.themeMode) {
                                AppThemeMode.DARK_AMOLED -> Icons.Default.DarkMode
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.SYSTEM -> Icons.Default.AutoAwesome
                            },
                            contentDescription = "Alternar Tema (${uiState.themeMode.label})"
                        )
                    }

                    IconButton(
                        onClick = {
                            if (!BiometricAuthHelper.isBiometricAvailable(context)) {
                                Toast.makeText(
                                    context,
                                    "Configure uma digital ou bloqueio de tela nas configurações do Android.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val nextState = !isBiometricEnabled
                                BiometricAuthHelper.setBiometricEnabled(context, nextState)
                                isBiometricEnabled = nextState
                                isAppUnlocked = true
                                val msg = if (nextState) {
                                    "Proteção por Biometria ATIVADA!"
                                } else {
                                    "Proteção por Biometria DESATIVADA."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isBiometricEnabled) Icons.Default.Fingerprint else Icons.Default.Lock,
                            contentDescription = "Bloqueio Biométrico",
                            tint = if (isBiometricEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    IconButton(onClick = { viewModel.checkForGitHubUpdates(silent = false) }) {
                        BadgedBox(
                            badge = {
                                if (uiState.updateInfo?.hasUpdate == true) {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Verificar Atualização do App"
                            )
                        }
                    }

                    IconButton(onClick = viewModel::signOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sair da conta"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == MainTab.DASHBOARD,
                    onClick = { currentTab = MainTab.DASHBOARD },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Início") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.FIXED_AND_DEBTS,
                    onClick = { currentTab = MainTab.FIXED_AND_DEBTS },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    label = { Text("Fixas/Dívidas") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.SMART_ADD,
                    onClick = { currentTab = MainTab.SMART_ADD },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("Lançar IA") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ADVISOR,
                    onClick = { currentTab = MainTab.ADVISOR },
                    icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                    label = { Text("Consultor") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                MainTab.DASHBOARD -> {
                    DashboardScreen(
                        userName = displayUserName,
                        transactions = monthTransactions,
                        sixMonthsStats = sixMonthsStats,
                        futureProjections = futureProjections,
                        fixedBills = fixedBills,
                        loans = loans,
                        savingsGoals = savingsGoals,
                        wishlist = wishlist,
                        categoryBudgets = categoryBudgets,
                        selectedMonth = uiState.selectedMonth,
                        selectedYear = uiState.selectedYear,
                        searchQuery = uiState.searchQuery,
                        selectedCategory = uiState.selectedCategoryFilter,
                        monthlyBudget = monthlyBudget,
                        isPrivacyMode = uiState.isPrivacyMode,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onSelectCategory = viewModel::setCategoryFilter,
                        onTogglePrivacyMode = viewModel::togglePrivacyMode,
                        onSaveMonthlyBudget = viewModel::updateMonthlyBudget,
                        onSaveCategoryBudget = viewModel::updateCategoryBudget,
                        onSaveWishlistItem = viewModel::saveWishlistItem,
                        onDeleteWishlistItem = viewModel::deleteWishlistItem,
                        onEditTransaction = viewModel::updateExistingTransaction,
                        onDuplicateTransaction = viewModel::duplicateTransaction,
                        onDeleteTransaction = viewModel::deleteTransaction,
                        onOpenSmartAdd = { currentTab = MainTab.SMART_ADD },
                        onOpenFixedAndDebts = { currentTab = MainTab.FIXED_AND_DEBTS },
                        onConvertCardInvoiceToRenegotiation = viewModel::saveLoanContract,
                        onAskAiCanIBuy = { prompt ->
                            currentTab = MainTab.ADVISOR
                            viewModel.sendAdvisorMessage(prompt)
                        },
                        onImportStatement = { uri, accName, onDone ->
                            viewModel.importBankStatementFile(context, uri, accName, onDone)
                        }
                    )
                }

                MainTab.FIXED_AND_DEBTS -> {
                    FixedAndDebtsScreen(
                        selectedMonth = uiState.selectedMonth,
                        selectedYear = uiState.selectedYear,
                        fixedBills = fixedBills,
                        loans = loans,
                        savingsGoals = savingsGoals,
                        receivables = receivables,
                        onSaveFixedBill = viewModel::saveFixedBill,
                        onPayFixedBillForMonth = viewModel::payFixedBillForMonth,
                        onDeleteFixedBill = viewModel::deleteFixedBill,
                        onSaveLoanContract = viewModel::saveLoanContract,
                        onPayOrAmortizeLoan = viewModel::payOrAmortizeLoan,
                        onDeleteLoanContract = viewModel::deleteLoanContract,
                        onSaveSavingsGoal = viewModel::saveSavingsGoal,
                        onDeleteSavingsGoal = viewModel::deleteSavingsGoal,
                        onSaveReceivable = viewModel::saveReceivable,
                        onMarkReceivableAsPaid = viewModel::markReceivableAsPaid,
                        onDeleteReceivable = viewModel::deleteReceivable,
                        onSimulatePayoffInAiChat = {
                            currentTab = MainTab.ADVISOR
                            viewModel.sendAdvisorMessage(
                                "Analise meus empréstimos, financiamentos e renegociações ativas e monte um plano estratégico de quitação e amortização (Efeito Bola de Neve vs. Juros) considerando meu saldo e minhas contas fixas."
                            )
                        }
                    )
                }

                MainTab.SMART_ADD -> {
                    SmartEntryScreen(
                        isProcessingAi = uiState.isProcessingAi,
                        aiStatusMessage = uiState.aiStatusMessage,
                        pendingAiDraft = uiState.pendingAiDraft,
                        onAnalyzeText = viewModel::processNaturalLanguageInput,
                        onAnalyzeReceiptBitmap = viewModel::processReceiptPhoto,
                        onAutoCategorize = viewModel::autoCategorizeTitle,
                        onSaveTransaction = viewModel::addManualTransaction,
                        onTransactionSavedNavigateHome = {
                            viewModel.clearPendingAiDraft()
                            currentTab = MainTab.DASHBOARD
                        }
                    )
                }

                MainTab.ADVISOR -> {
                    AdvisorChatScreen(
                        messages = chatMessages,
                        isThinking = uiState.isAdvisorThinking,
                        onSendMessage = viewModel::sendAdvisorMessage,
                        onClearChat = viewModel::clearAdvisorChat
                    )
                }
            }
        }
    }
}
