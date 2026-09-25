package com.collectionluxus.personalhisab

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

data class Party(val id: Long, val name: String, val mobile: String, val address: String, val openingBalance: Double, val kind: String)

data class Entry(
    val id: Long, val date: String, val type: String, val title: String,
    val party: String, val item: String, val cost: Double, val amount: Double,
    val received: Double, val pending: Double, val account: String,
    val fromAccount: String, val toAccount: String, val notes: String
)

class HisabDb(context: android.content.Context) :
    SQLiteOpenHelper(context, "personal_hisab.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE parties(
                id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, mobile TEXT, address TEXT,
                opening_balance REAL NOT NULL DEFAULT 0, kind TEXT NOT NULL DEFAULT 'Customer'
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE entries(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL, type TEXT NOT NULL, title TEXT,
                party TEXT, item TEXT, cost REAL NOT NULL DEFAULT 0,
                amount REAL NOT NULL DEFAULT 0, received REAL NOT NULL DEFAULT 0,
                pending REAL NOT NULL DEFAULT 0, account TEXT,
                from_account TEXT, to_account TEXT, notes TEXT
            )
        """.trimIndent())
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE entries ADD COLUMN cost REAL NOT NULL DEFAULT 0")
        if (oldVersion < 3) db.execSQL("""CREATE TABLE IF NOT EXISTS parties(
            id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, mobile TEXT, address TEXT,
            opening_balance REAL NOT NULL DEFAULT 0, kind TEXT NOT NULL DEFAULT 'Customer')""")
    }
    fun addParty(p: Party) {
        writableDatabase.insertWithOnConflict("parties", null, ContentValues().apply {
            put("name", p.name); put("mobile", p.mobile); put("address", p.address)
            put("opening_balance", p.openingBalance); put("kind", p.kind)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun parties(): List<Party> {
        val out = mutableListOf<Party>()
        readableDatabase.query("parties", null, null, null, null, null, "name COLLATE NOCASE ASC").use { c ->
            while (c.moveToNext()) out += Party(c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")), c.getString(c.getColumnIndexOrThrow("mobile")) ?: "",
                c.getString(c.getColumnIndexOrThrow("address")) ?: "", c.getDouble(c.getColumnIndexOrThrow("opening_balance")),
                c.getString(c.getColumnIndexOrThrow("kind")) ?: "Customer")
        }
        return out
    }
    fun insert(e: Entry) {
        writableDatabase.insert("entries", null, ContentValues().apply {
            put("date", e.date); put("type", e.type); put("title", e.title)
            put("party", e.party); put("item", e.item); put("cost", e.cost)
            put("amount", e.amount); put("received", e.received); put("pending", e.pending)
            put("account", e.account); put("from_account", e.fromAccount)
            put("to_account", e.toAccount); put("notes", e.notes)
        })
    }
    fun all(): List<Entry> {
        val result = mutableListOf<Entry>()
        readableDatabase.query("entries", null, null, null, null, null, "id DESC").use { c ->
            while (c.moveToNext()) result += Entry(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("date")),
                c.getString(c.getColumnIndexOrThrow("type")),
                c.getString(c.getColumnIndexOrThrow("title")) ?: "",
                c.getString(c.getColumnIndexOrThrow("party")) ?: "",
                c.getString(c.getColumnIndexOrThrow("item")) ?: "",
                c.getDouble(c.getColumnIndexOrThrow("cost")),
                c.getDouble(c.getColumnIndexOrThrow("amount")),
                c.getDouble(c.getColumnIndexOrThrow("received")),
                c.getDouble(c.getColumnIndexOrThrow("pending")),
                c.getString(c.getColumnIndexOrThrow("account")) ?: "",
                c.getString(c.getColumnIndexOrThrow("from_account")) ?: "",
                c.getString(c.getColumnIndexOrThrow("to_account")) ?: "",
                c.getString(c.getColumnIndexOrThrow("notes")) ?: ""
            )
        }
        return result
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var db: HisabDb
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = HisabDb(this)
        setContent { PersonalHisabApp(db) }
    }
    override fun onDestroy() { db.close(); super.onDestroy() }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PersonalHisabApp(db: HisabDb) {
    var entries by remember { mutableStateOf(db.all()) }
    var showAdd by remember { mutableStateOf(false) }
    var showParty by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    var parties by remember { mutableStateOf(db.parties()) }
    val refresh = { entries = db.all() }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF006B4F),
            secondary = Color(0xFFB58A2A),
            tertiary = Color(0xFF15845F),
            background = Color(0xFFF5F8F6),
            surface = Color.White,
            error = Color(0xFFD9534F)
        )
    ) {
        Scaffold(
            topBar = { TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically) { MoneyLogo(); Spacer(Modifier.width(10.dp)); Column { Text("Muhaasba", style = MaterialTheme.typography.titleLarge); Text("Apne Hisab Ka Aasaan Saathi", style = MaterialTheme.typography.labelSmall) } } }) },
            bottomBar = {
                NavigationBar {
                    listOf("Home", "Parties", "Ledger", "Reports").forEachIndexed { i, label ->
                        NavigationBarItem(selected = tab == i, onClick = { tab = i },
                            icon = { Text(listOf("⌂", "♟", "₹", "▤")[i]) },
                            label = { Text(label) })
                    }
                }
            },
            floatingActionButton = {
                if (tab == 0 || tab == 1) FloatingActionButton(onClick = { if (tab == 0) showAdd = true else showParty = true }) { Text("+") }
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(entries, parties, padding, onAddParty = { showParty = true }, onAddEntry = { showAdd = true }, onReports = { tab = 3 })
                1 -> PartiesScreen(parties, entries, padding, onAddParty = { showParty = true })
                2 -> LedgerScreen(entries, padding)
                else -> ReportsScreen(entries, padding)
            }
        }
        if (showAdd) AddEntryDialog(
            onDismiss = { showAdd = false },
            onSave = { db.insert(it); entries = db.all(); showAdd = false }
        )
        if (showParty) AddPartyDialog(
            onDismiss = { showParty = false },
            onSave = { db.addParty(it); parties = db.parties(); showParty = false }
        )
    }
}

@Composable
fun HomeScreen(entries: List<Entry>, parties: List<Party>, padding: PaddingValues, onAddParty: () -> Unit, onAddEntry: () -> Unit, onReports: () -> Unit) {
    val sales = entries.filter { it.type == "Sale" }.sumOf { it.amount }
    val profit = entries.filter { it.type == "Sale" }.sumOf { it.amount - it.cost }
    val received = entries.filter { it.type == "Receive" || it.type == "Sale" }.sumOf { it.received }
    val expenses = entries.filter { it.type in listOf("Expense", "Personal", "Household") }.sumOf { it.amount }
    val pending = entries.filter { it.type == "Sale" }.sumOf { it.pending }
    LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total Balance")
                    Text(money(accountBalance(entries, null)), style = MaterialTheme.typography.headlineMedium)
                    Text("Sales: " + money(sales))
                    Text("Profit: " + money(profit))
                    Text("Received: " + money(received))
                    Text("Expenses: " + money(expenses))
                    Text("Party Pending: " + money(pending))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddEntry, modifier = Modifier.weight(1f)) { Text("＋ Entry") }
                OutlinedButton(onClick = onAddParty, modifier = Modifier.weight(1f)) { Text("＋ Party") }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Quick Overview", style = MaterialTheme.typography.titleMedium)
                    Text("Parties: " + parties.size + "   •   Transactions: " + entries.size)
                    Text("Pending to collect: " + money(pending))
                }
            }
            Text("Accounts", style = MaterialTheme.typography.titleLarge)
        }
        item { AccountCard("Cash in Hand", entries, "Cash") }
        item { AccountCard("Union Bank — Current", entries, "Union Bank") }
        item { AccountCard("Kotak Bank — Savings", entries, "Kotak Bank") }
        item { Text("Recent Transactions", style = MaterialTheme.typography.titleLarge) }
        if (entries.isEmpty()) item { Text("No transactions yet. Tap + to add your first entry.") }
        items(entries.take(30), key = { it.id }) { e ->
            ListItem(
                headlineContent = { Text(e.title.ifBlank { e.type }) },
                supportingContent = { Text(e.date + " • " + e.type + if (e.party.isNotBlank()) " • " + e.party else "") },
                trailingContent = { Text(money(e.amount)) }
            )
        }
    }
}

@Composable
fun LedgerScreen(entries: List<Entry>, padding: PaddingValues) {
    var query by remember { mutableStateOf("") }
    var selectedParty by remember { mutableStateOf<String?>(null) }
    val parties = entries.map { it.party.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    val filtered = entries.filter {
        val q = query.trim().lowercase()
        (q.isBlank() || listOf(it.title, it.party, it.item, it.type, it.account, it.notes).any { s -> s.lowercase().contains(q) }) &&
        (selectedParty == null || it.party == selectedParty)
    }
    LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Party Ledger", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(query, { query = it }, label = { Text("Search party / item / type") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = selectedParty == null, onClick = { selectedParty = null }, label = { Text("All") })
                parties.take(8).forEach { p ->
                    FilterChip(selected = selectedParty == p, onClick = { selectedParty = if (selectedParty == p) null else p }, label = { Text(p) })
                }
            }
        }
        if (selectedParty != null) {
            item {
                val p = filtered.filter { it.type == "Sale" }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(selectedParty!!, style = MaterialTheme.typography.titleLarge)
                        Text("Sales: " + money(p.sumOf { it.amount }))
                        Text("Cost: " + money(p.sumOf { it.cost }))
                        Text("Profit: " + money(p.sumOf { it.amount - it.cost }))
                        Text("Received: " + money(p.sumOf { it.received }))
                        Text("Pending: " + money(p.sumOf { it.pending }))
                    }
                }
            }
        }
        items(filtered, key = { it.id }) { e ->
            ListItem(
                headlineContent = { Text(e.party.ifBlank { e.title }) },
                supportingContent = { Text(e.date + " • " + e.type + if (e.item.isNotBlank()) " • " + e.item else "") },
                trailingContent = { Text(money(if (e.type == "Sale") e.amount else if (e.type == "Transfer") 0.0 else e.amount)) }
            )
        }
    }
}

@Composable
fun ReportsScreen(entries: List<Entry>, padding: PaddingValues) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val monthStart = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    var custom by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf(monthStart) }
    var to by remember { mutableStateOf(today) }
    val filtered = entries.filter { it.date >= from && it.date <= to }
    val sales = filtered.filter { it.type == "Sale" }
    val businessExpense = filtered.filter { it.type == "Expense" }.sumOf { it.amount }
    val personal = filtered.filter { it.type == "Personal" }.sumOf { it.amount }
    val household = filtered.filter { it.type == "Household" }.sumOf { it.amount }
    val totalExpense = businessExpense + personal + household
    val received = filtered.filter { it.type == "Receive" || it.type == "Sale" }.sumOf { it.received }
    val profit = sales.sumOf { it.amount - it.cost }
    val byItem = sales.groupBy { it.item.ifBlank { "Other" } }
    val byParty = sales.groupBy { it.party.ifBlank { "Other" } }

    LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item {
            Text("Monthly Review", style = MaterialTheme.typography.headlineSmall)
            Text("Sale, expense, profit aur received kisi bhi date range me dekho.")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !custom, onClick = {
                    custom = false
                    from = monthStart
                    to = today
                }, label = { Text("Monthly") })
                FilterChip(selected = custom, onClick = { custom = true }, label = { Text("Custom Date") })
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Date Range", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(from, { from = it }, label = { Text("From (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(to, { to = it }, label = { Text("To (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text(if (custom) "Custom range selected" else "Current month selected")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Review: $from → $to", style = MaterialTheme.typography.titleMedium)
                    Text("Total Sale: " + money(sales.sumOf { it.amount }))
                    Text("Total Purchase/Cost: " + money(sales.sumOf { it.cost }))
                    Text("Total Profit: " + money(profit))
                    Text("Money Received: " + money(received))
                    Text("Business Expense: " + money(businessExpense))
                    Text("Personal Expense: " + money(personal))
                    Text("Household Expense: " + money(household))
                    Text("Total Expense: " + money(totalExpense))
                    Text("Party Pending: " + money(sales.sumOf { it.pending }))
                }
            }
        }
        item { Text("Item-wise Profit", style = MaterialTheme.typography.titleLarge) }
        items(byItem.entries.toList(), key = { it.key }) { (item, rows) ->
            ListItem(
                headlineContent = { Text(item) },
                supportingContent = { Text("Sale " + money(rows.sumOf { it.amount }) + " • Cost " + money(rows.sumOf { it.cost })) },
                trailingContent = { Text("Profit " + money(rows.sumOf { it.amount - it.cost })) }
            )
        }
        item { Text("Party-wise Profit", style = MaterialTheme.typography.titleLarge) }
        items(byParty.entries.toList(), key = { it.key }) { (party, rows) ->
            ListItem(
                headlineContent = { Text(party) },
                supportingContent = { Text("Received " + money(rows.sumOf { it.received }) + " • Pending " + money(rows.sumOf { it.pending })) },
                trailingContent = { Text(money(rows.sumOf { it.amount - it.cost })) }
            )
        }
    }
}

@Composable
fun SummaryCard(a: String, av: Double, b: String, bv: Double, c: String, cv: Double) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(a + ": " + money(av))
            Text(b + ": " + money(bv))
            Text(c + ": " + money(cv))
        }
    }
}

fun accountBalance(entries: List<Entry>, wanted: String?): Double =
    entries.sumOf { e ->
        when (e.type) {
            "Receive" -> if (wanted == null || e.account == wanted) e.amount else 0.0
            "Sale" -> if (wanted == null || e.account == wanted) e.received else 0.0
            "Expense", "Personal", "Household" -> if (wanted == null || e.account == wanted) -e.amount else 0.0
            "Transfer" -> when {
                wanted == null -> 0.0
                e.fromAccount == wanted -> -e.amount
                e.toAccount == wanted -> e.amount
                else -> 0.0
            }
            else -> 0.0
        }
    }

@Composable
fun MoneyLogo() {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF5EF)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(7.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(Color(0xFF006B4F), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .15f, h * .15f))
            drawRoundRect(Color(0xFFB58A2A), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .15f, h * .15f), style = Stroke(width = w * .06f))
            drawCircle(Color(0xFFFFD86A), radius = w * .16f, center = androidx.compose.ui.geometry.Offset(w * .30f, h * .72f))
            drawCircle(Color(0xFFFFE69A), radius = w * .12f, center = androidx.compose.ui.geometry.Offset(w * .52f, h * .82f))
            drawCircle(Color(0xFFFFD86A), radius = w * .10f, center = androidx.compose.ui.geometry.Offset(w * .70f, h * .66f))
        }
        Text("₹", color = Color.White)
    }
}

@Composable
fun AccountCard(name: String, entries: List<Entry>, account: String) {
    Card(Modifier.fillMaxWidth()) {
        ListItem(headlineContent = { Text(name) }, trailingContent = { Text(money(accountBalance(entries, account)), style = MaterialTheme.typography.titleMedium) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEntryDialog(onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    val types = listOf("Receive", "Sale", "Expense", "Personal", "Household", "Transfer")
    val accounts = listOf("Cash", "Union Bank", "Kotak Bank")
    var title by remember { mutableStateOf("") }
    var party by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var received by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Receive") }
    var account by remember { mutableStateOf("Cash") }
    var toAccount by remember { mutableStateOf("Union Bank") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Quick Add") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    types.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) }) }
                }
                OutlinedTextField(title, { title = it }, label = { Text("Description / Reason") }, singleLine = true)
                if (type == "Sale") {
                    OutlinedTextField(party, { party = it }, label = { Text("Party / Customer") }, singleLine = true)
                    OutlinedTextField(item, { item = it }, label = { Text("Item") }, singleLine = true)
                    OutlinedTextField(cost, { cost = it }, label = { Text("Purchase / Cost") }, singleLine = true)
                    OutlinedTextField(amount, { amount = it }, label = { Text("Total Sale Amount") }, singleLine = true)
                    OutlinedTextField(received, { received = it }, label = { Text("Amount Received") }, singleLine = true)
                } else OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, singleLine = true)
                if (type != "Transfer") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        accounts.forEach { a -> FilterChip(selected = account == a, onClick = { account = a }, label = { Text(a) }) }
                    }
                } else {
                    Text("From")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { accounts.forEach { a -> FilterChip(selected = account == a, onClick = { account = a }, label = { Text(a) }) } }
                    Text("To")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { accounts.forEach { a -> FilterChip(selected = toAccount == a, onClick = { toAccount = a }, label = { Text(a) }) } }
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val total = amount.toDoubleOrNull() ?: return@Button
                if (total <= 0 || (type == "Transfer" && account == toAccount)) return@Button
                val rec = if (type == "Sale") (received.toDoubleOrNull() ?: 0.0).coerceIn(0.0, total) else total
                onSave(Entry(0, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()), type, title.ifBlank { type }, party, item, if (type == "Sale") (cost.toDoubleOrNull() ?: 0.0) else 0.0, total, rec, if (type == "Sale") total - rec else 0.0, account, if (type == "Transfer") account else "", if (type == "Transfer") toAccount else "", notes))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
fun PartiesScreen(parties: List<Party>, entries: List<Entry>, padding: PaddingValues, onAddParty: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = parties.filter { it.name.contains(query.trim(), ignoreCase = true) || it.mobile.contains(query.trim()) }
    LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Parties", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(query, { query = it }, label = { Text("Search party") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAddParty, modifier = Modifier.fillMaxWidth()) { Text("＋ Add Party") }
        }
        items(filtered, key = { it.id }) { p ->
            val sales = entries.filter { it.type == "Sale" && it.party.equals(p.name, true) }
            val received = sales.sumOf { it.received }
            val pending = sales.sumOf { it.pending }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleLarge)
                    Text(p.kind + if (p.mobile.isNotBlank()) " • " + p.mobile else "")
                    if (p.address.isNotBlank()) Text(p.address)
                    Text("Sales: " + money(sales.sumOf { it.amount }) + " • Received: " + money(received))
                    Text("Pending: " + money(pending + p.openingBalance))
                }
            }
        }
        if (filtered.isEmpty()) item { Text("No parties yet. Tap Add Party.") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPartyDialog(onDismiss: () -> Unit, onSave: (Party) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("Customer") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Party") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Party Name") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Customer", "Supplier").forEach { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k) })
                    }
                }
                OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Address") }, singleLine = true)
                OutlinedTextField(opening, { opening = it }, label = { Text("Opening Balance") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(Party(0, name.trim(), mobile.trim(), address.trim(), opening.toDoubleOrNull() ?: 0.0, kind))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
