package com.example.cashley

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashley.AddTransactionActivity
import com.example.cashley.EditTransactionActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {

    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var emptyStateLayout: View
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var addTransactionButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        initializeViews()
        setupBottomNavigation()
        setupSwipeRefresh()
        setupChipGroup()
        displayTransactions()
    }

    private fun initializeViews() {
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        addTransactionButton = findViewById(R.id.addTransactionButton)
        sharedPreferences = getSharedPreferences("cashley", MODE_PRIVATE)

        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        addTransactionButton.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_primary,
            R.color.accent_secondary
        )
        swipeRefreshLayout.setOnRefreshListener {
            displayTransactions()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupChipGroup() {
        filterChipGroup.setOnCheckedStateChangeListener { group, _ ->
            displayTransactions(when (group.checkedChipId) {
                R.id.incomeChip -> "Income"
                R.id.expenseChip -> "Expense"
                else -> null
            })
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_add_transaction -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    true
                }
                R.id.nav_transactions -> true // Already on transactions page
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_transactions
    }

    private fun displayTransactions(filterType: String? = null) {
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!
            .asSequence()
            .filter { filterType == null || it.split("|")[0] == filterType }
            .sortedByDescending { it.split("|")[2] }
            .map {
                val data = it.split("|")
                TransactionItem(
                    type = data[0],
                    description = data[1],
                    date = data[2],
                    amount = data[3].toFloat(),
                    rawData = it
                )
            }
            .toList()

        updateEmptyState(transactions.isEmpty())
        transactionsRecyclerView.adapter = TransactionsAdapter(
            transactions,
            onEditClick = { transaction ->
                val intent = Intent(this, EditTransactionActivity::class.java)
                intent.putExtra("transaction", transaction.rawData)
                startActivity(intent)
            },
            onDeleteClick = { transaction ->
                deleteTransaction(transaction.rawData)
                Toast.makeText(this, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
                displayTransactions(filterType)
            }
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        transactionsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun deleteTransaction(transaction: String) {
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!.toMutableSet()
        transactions.remove(transaction)
        val editor = sharedPreferences.edit()
        editor.putStringSet("transactions", transactions)

        val data = transaction.split("|")
        val amount = data[3].toFloat()
        if (data[0] == "Income") {
            val budget = sharedPreferences.getFloat("monthlyBudget", 0f) - amount
            editor.putFloat("monthlyBudget", budget)
        } else {
            val budget = sharedPreferences.getFloat("monthlyBudget", 0f) + amount
            editor.putFloat("monthlyBudget", budget)
        }

        editor.apply()
    }
}

data class TransactionItem(
    val type: String,
    val description: String,
    val date: String,
    val amount: Float,
    val rawData: String
)

class TransactionsAdapter(
    private val transactions: List<TransactionItem>,
    private val onEditClick: (TransactionItem) -> Unit,
    private val onDeleteClick: (TransactionItem) -> Unit
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.transactionTitle)
        val date: TextView = view.findViewById(R.id.transactionDate)
        val amount: TextView = view.findViewById(R.id.transactionAmount)
        val editButton: ImageButton = view.findViewById(R.id.editTransactionButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteTransactionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_list_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context

        holder.title.text = "${transaction.type}: ${transaction.description}"

        // Format date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(transaction.date)
            holder.date.text = date?.let { outputFormat.format(it) } ?: transaction.date
        } catch (e: Exception) {
            holder.date.text = transaction.date
        }

        // Format amount with color based on transaction type
        holder.amount.text = "Rs.${String.format("%.2f", transaction.amount)}"
        holder.amount.setTextColor(
            ContextCompat.getColor(
                context,
                if (transaction.type == "Income") android.R.color.holo_green_dark
                else android.R.color.holo_red_dark
            )
        )

        holder.editButton.setOnClickListener { onEditClick(transaction) }
        holder.deleteButton.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount() = transactions.size
}