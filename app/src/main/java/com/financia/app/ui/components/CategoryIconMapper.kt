package com.financia.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.financia.app.data.model.TransactionCategory

object CategoryIconMapper {
    fun getIcon(categoryName: String): ImageVector {
        return when (TransactionCategory.fromLabelOrName(categoryName)) {
            TransactionCategory.ALIMENTACAO -> Icons.Default.Restaurant
            TransactionCategory.TRANSPORTE -> Icons.Default.DirectionsCar
            TransactionCategory.MORADIA -> Icons.Default.Home
            TransactionCategory.EMPRESTIMOS -> Icons.Default.AccountBalance
            TransactionCategory.RENEGOCIACAO -> Icons.Default.CreditCard
            TransactionCategory.LAZER -> Icons.Default.Movie
            TransactionCategory.SAUDE -> Icons.Default.MedicalServices
            TransactionCategory.EDUCACAO -> Icons.Default.School
            TransactionCategory.COMPRAS -> Icons.Default.ShoppingBag
            TransactionCategory.ASSINATURAS -> Icons.Default.Subscriptions
            TransactionCategory.SALARIO -> Icons.Default.Payments
            TransactionCategory.INVESTIMENTOS -> Icons.Default.TrendingUp
            TransactionCategory.FREELANCE -> Icons.Default.Work
            TransactionCategory.OUTROS -> Icons.Default.Category
        }
    }
}
