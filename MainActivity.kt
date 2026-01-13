package com.example.moneycounter2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.moneycounter2.ui.theme.MoneyCounter2Theme
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "money_counter"
)

private val BALANCE_KEY = doublePreferencesKey("balance")
private val TRANSACTIONS_KEY = stringPreferencesKey("transactions")


// Data Models
data class Transaction(
    val id: Long = System.currentTimeMillis(),
    val type: TransactionType,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TransactionType {
    ADD, REMOVE
}
class MoneyCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore
    private val gson = Gson()

    var balance by mutableStateOf(0.0)
        private set

    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()

            balance = prefs[BALANCE_KEY] ?: 0.0

            prefs[TRANSACTIONS_KEY]?.let { json ->
                val type = object : TypeToken<List<Transaction>>() {}.type
                transactions = gson.fromJson(json, type)
            }
        }
    }

    private fun saveData() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[BALANCE_KEY] = balance
                prefs[TRANSACTIONS_KEY] = gson.toJson(transactions)
            }
        }
    }

    fun addMoney(amount: Double) {
        balance += amount
        transactions = listOf(
            Transaction(type = TransactionType.ADD, amount = amount)
        ) + transactions.take(9)

        saveData()
    }

    fun removeMoney(amount: Double) {
        balance -= amount
        transactions = listOf(
            Transaction(type = TransactionType.REMOVE, amount = amount)
        ) + transactions.take(9)

        saveData()
    }

    fun clearAll() {
        balance = 0.0
        transactions = emptyList()
        saveData()
    }
}

// Theme
@Composable
fun MoneyCounterTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFFBB86FC),
        secondary = Color(0xFF03DAC6),
        tertiary = Color(0xFFCF6679),
        background = Color(0xFF000000),  // Pure black for AMOLED
        surface = Color(0xFF000000),      // Pure black for AMOLED
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyCounterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneyCounterApp()
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyCounterApp(
    viewModel: MoneyCounterViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    var amountInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Money Counter",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C1C1E)  // Dark gray
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)  // Light gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "د.إ ${String.format("%.2f", viewModel.balance)}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBB86FC)  // Purple accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Card
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFBB86FC),
                    unfocusedBorderColor = Color(0xFF3A3A3C),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFBB86FC)
                )
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C1C1E)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Enter Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                amountInput.toDoubleOrNull()?.let { amount ->
                                    if (amount > 0) {
                                        viewModel.addMoney(amount)
                                        amountInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add", fontSize = 16.sp)
                        }

                        Button(
                            onClick = {
                                amountInput.toDoubleOrNull()?.let { amount ->
                                    if (amount > 0) {
                                        viewModel.removeMoney(amount)
                                        amountInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF5350)
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove", fontSize = 16.sp)
                        }
                    }

                    if (viewModel.transactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { viewModel.clearAll() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear All")
                        }
                    }
                }
            }
            if (viewModel.transactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1C1C1E)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.transactions) { transaction ->
                                TransactionItem(transaction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    val isAdd = transaction.type == TransactionType.ADD
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2C2C2E),  // Slightly lighter dark gray
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isAdd) Color(0xFF1C3A1F) else Color(0xFF3A1C1C),  // Darker tints
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isAdd) Icons.Default.Add else Icons.Default.Delete,
                        contentDescription = null,
                        tint = if (isAdd) Color(0xFF4CAF50) else Color(0xFFEF5350),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "${if (isAdd) "+" else "-"}$${String.format("%.2f", transaction.amount)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = timeFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}
