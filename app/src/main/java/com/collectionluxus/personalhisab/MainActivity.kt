package com.collectionluxus.personalhisab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

data class Entry(val type: String, val title: String, val amount: Double, val account: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PersonalHisabApp() }
    }
}

@Composable
fun PersonalHisabApp() {
    var entries by remember { mutableStateOf(listOf<Entry>()) }
    var showAdd by remember { mutableStateOf(false) }
    val received = entries.filter { it.type == "Receive" || it.type == "Sale" }.sumOf { it.amount }
    val expenses = entries.filter { it.type == "Expense" || it.type == "Personal" || it.type == "Household" }.sumOf { it.amount }
    val balance = received - expenses

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Personal Hisab") }) },
            floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Total Balance", style = MaterialTheme.typography.labelLarge)
                            Text(money(balance), style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Received: ${money(received)}")
                            Text("Expenses: ${money(expenses)}")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Accounts", style = MaterialTheme.typography.titleLarge)
                }
                item { AccountCard("Cash in Hand", entries, "Cash") }
                item { AccountCard("Union Bank — Current", entries, "Union Bank") }
                item { AccountCard("Kotak Bank — Savings", entries, "Kotak Bank") }
                item { Text("Recent Transactions", style = MaterialTheme.typography.titleLarge) }
                items(entries.reversed()) { e ->
                    ListItem(
                        headlineContent = { Text(e.title) },
                        supportingContent = { Text("${e.type} • ${e.account}") },
                        trailingContent = { Text(money(e.amount)) }
                    )
                }
            }
        }
        if (showAdd) {
            AddEntryDialog({ showAdd = false }) { entry ->
                entries = entries + entry
                showAdd = false
            }
        }
    }
}

@Composable
fun AccountCard(name: String, entries: List<Entry>, account: String) {
    val value = entries.filter { it.account == account }.sumOf {
        if (it.type == "Receive" || it.type == "Sale") it.amount else -it.amount
    }
    Card(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(name) },
            trailingContent = { Text(money(value), style = MaterialTheme.typography.titleMedium) }
        )
    }
}

@Composable
fun AddEntryDialog(onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Receive") }
    var account by remember { mutableStateOf("Cash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Description") })
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount") })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Receive", "Sale", "Expense", "Personal", "Household").forEach {
                        FilterChip(selected = type == it, onClick = { type = it }, label = { Text(it) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cash", "Union Bank", "Kotak Bank").forEach {
                        FilterChip(selected = account == it, onClick = { account = it }, label = { Text(it) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                amount.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                    onSave(Entry(type, title.ifBlank { type }, it, account))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
