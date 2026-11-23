package com.financia.app.core.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.financia.app.data.model.FinancialTransaction
import com.financia.app.data.model.TransactionType
import com.financia.app.data.repository.AiFinancialRepository
import com.financia.app.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Serviço inteligente que detecta notificações de bancos brasileiros (Nubank, Inter, Itaú, Bradesco,
 * Santander, C6, PicPay, Mercado Pago, Caixa, Banco do Brasil) e registra automaticamente a
 * despesa ou receita usando o motor Gemini IA!
 */
@AndroidEntryPoint
class BankNotificationService : NotificationListenerService() {

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var aiRepository: AiFinancialRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastProcessedSignature: String = ""
    private var lastProcessedTime: Long = 0L

    private val supportedBankPackages = setOf(
        "com.nu.production",                // Nubank
        "br.com.intermedium",               // Banco Inter
        "com.itau",                         // Itaú
        "com.bradesco",                     // Bradesco
        "com.santander.app",                // Santander
        "com.c6bank.app",                   // C6 Bank
        "com.picpay",                       // PicPay
        "com.mercadopago.wallet",           // Mercado Pago
        "br.com.bb.android",                // Banco do Brasil
        "br.com.gabba.Caixa",               // Caixa
        "br.com.xp.cartoes",                // XP
        "com.btgpactual.pactualdigital"     // BTG
    )

    private val financialKeywords = listOf(
        "compra aprovada",
        "pix enviado",
        "pix recebido",
        "transferência enviada",
        "transferência recebida",
        "pagamento realizado",
        "você recebeu um pix",
        "compra no débito",
        "compra no crédito",
        "r$"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return
        if (pkg == applicationContext.packageName) return
        if (auth.currentUser == null) return

        val extras = notification.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val fullMessage = "$title - $text".trim()

        if (fullMessage.length < 10) return
        val lower = fullMessage.lowercase()

        // Verifica se vem de um app bancário ou se contém padrão explícito de transação com R$
        val isBankPackage = supportedBankPackages.any { pkg.contains(it, ignoreCase = true) }
        val hasCurrencyAndKeyword = lower.contains("r$") && financialKeywords.any { lower.contains(it) }

        if (!isBankPackage && !hasCurrencyAndKeyword) return
        if (!lower.contains("r$") && !Regex("""\d+[.,]\d{2}""").containsMatchIn(lower)) return

        // Evita duplicidade em notificações repetidas em menos de 15 segundos
        val signature = "$pkg|$fullMessage"
        val now = System.currentTimeMillis()
        if (signature == lastProcessedSignature && (now - lastProcessedTime) < 15_000L) return
        lastProcessedSignature = signature
        lastProcessedTime = now

        serviceScope.launch {
            val extracted = aiRepository.parseNaturalLanguageTransaction(fullMessage)
                .getOrElse { aiRepository.fallbackHeuristicParse(fullMessage) }

            if (extracted.amount > 0.0) {
                val tx = FinancialTransaction(
                    title = extracted.title.ifBlank { title.ifBlank { "Transação Bancária" } },
                    amount = extracted.amount,
                    type = if (extracted.type.equals("INCOME", ignoreCase = true)) {
                        TransactionType.INCOME.name
                    } else {
                        TransactionType.EXPENSE.name
                    },
                    category = extracted.category,
                    paymentMethod = extracted.paymentMethod,
                    notes = "Capturado automaticamente da notificação: \"$fullMessage\"".take(500),
                    createdByAi = true,
                    isRecurring = extracted.isRecurring,
                    installmentCount = extracted.installmentCount.coerceIn(1, 48)
                )
                transactionRepository.addTransaction(tx)
            }
        }
    }

    companion object {
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val componentName = ComponentName(context, BankNotificationService::class.java)
            return flat.contains(componentName.flattenToString())
        }

        fun openNotificationAccessSettings(context: Context) {
            runCatching {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}
