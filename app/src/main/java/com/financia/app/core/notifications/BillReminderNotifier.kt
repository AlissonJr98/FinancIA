package com.financia.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.financia.app.MainActivity
import com.financia.app.R
import com.financia.app.data.model.FixedBillTemplate
import com.financia.app.data.model.LoanContract
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Gerenciador de Lembretes Locais Automáticos no Celular:
 * Verifica Contas Fixas (Água, Luz, Internet...) e Parcelas de Empréstimos que vencem hoje ou nos
 * próximos 2 dias e dispara uma notificação nativa no Android para o usuário não pagar juros.
 */
object BillReminderNotifier {

    private const val CHANNEL_ID = "financia_due_date_reminders"
    private const val PREFS_REMINDERS = "financia_reminders_prefs"
    private const val NOTIFICATION_ID = 4100

    fun checkAndNotifyDueBills(
        context: Context,
        fixedBills: List<FixedBillTemplate>,
        loans: List<LoanContract>
    ) {
        if (fixedBills.isEmpty() && loans.isEmpty()) return

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        val monthKey = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth + 1)
        val todaySignature = "$monthKey-$todayDay"

        val prefs = context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        if (prefs.getString("last_notified_day", "") == todaySignature) {
            return
        }

        val brl = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dueAlerts = mutableListOf<String>()

        fixedBills.forEach { bill ->
            if (!bill.isPaidInMonth(monthKey)) {
                val diff = bill.dueDay - todayDay
                when {
                    diff == 0 -> dueAlerts.add("• ${bill.name} vence HOJE (Dia ${bill.dueDay})")
                    diff in 1..2 -> dueAlerts.add("• ${bill.name} vence em $diff dia(s) (Dia ${bill.dueDay})")
                    diff < 0 -> dueAlerts.add("• ${bill.name} venceu dia ${bill.dueDay} (informe o valor!)")
                }
            }
        }

        loans.filter { it.remainingInstallments > 0 }.forEach { loan ->
            if (!loan.isPaidInMonth(monthKey)) {
                val diff = loan.dueDay - todayDay
                when {
                    diff == 0 -> dueAlerts.add("• Parcela ${loan.title} (${brl.format(loan.installmentAmount)}) vence HOJE")
                    diff in 1..2 -> dueAlerts.add("• Parcela ${loan.title} vence dia ${loan.dueDay}")
                }
            }
        }

        if (dueAlerts.isEmpty()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes de Vencimento FinancIA",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de vencimento de Água, Luz, Contas Fixas e Parcelas de Empréstimos"
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            401,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryLine = dueAlerts.first()
        val bigText = dueAlerts.joinToString("\n")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Contas & Parcelas do Mês (${dueAlerts.size})")
            .setContentText(summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            manager.notify(NOTIFICATION_ID, notification)
            prefs.edit().putString("last_notified_day", todaySignature).apply()
        }
    }
}
