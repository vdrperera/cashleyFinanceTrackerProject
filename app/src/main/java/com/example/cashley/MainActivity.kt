package com.example.cashley

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashley.AddBudgetActivity
import com.example.cashley.AddTransactionActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var budgetText: TextView
    private lateinit var addBudgetButton: View
    private lateinit var recentTransactionsRecyclerView: RecyclerView
    private lateinit var incomeBarChart: BarChart
    private lateinit var expenseBarChart: BarChart
    private lateinit var notificationsCard: MaterialCardView
    private lateinit var addTransactionCard: MaterialCardView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        setupCharts()
        updateDashboard()
    }

    private fun initializeViews() {
        budgetText = findViewById(R.id.budgetText)
        addBudgetButton = findViewById(R.id.addBudgetButton)
        recentTransactionsRecyclerView = findViewById(R.id.recentTransactionsRecyclerView)
        incomeBarChart = findViewById(R.id.incomeBarChart)
        expenseBarChart = findViewById(R.id.expenseBarChart)
        notificationsCard = findViewById(R.id.notificationsCard)
        addTransactionCard = findViewById(R.id.addTransactionCard)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        sharedPreferences = getSharedPreferences("cashley", MODE_PRIVATE)

        recentTransactionsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        addBudgetButton.setOnClickListener {
            startActivity(Intent(this, AddBudgetActivity::class.java))
        }

        notificationsCard.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        addTransactionCard.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Already on home
                R.id.nav_add_transaction -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    true
                }
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
        bottomNavigationView.selectedItemId = R.id.nav_home
    }

    private fun setupCharts() {
        listOf(incomeBarChart, expenseBarChart).forEach { chart ->
            chart.apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setBorderColor(Color.WHITE)
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = ContextCompat.getColor(context, R.color.accent_secondary)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    textColor = ContextCompat.getColor(context, R.color.accent_secondary)
                }

                axisRight.isEnabled = false
                legend.isEnabled = true
                legend.textColor = ContextCompat.getColor(context, R.color.accent_secondary)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun updateDashboard() {
        displayBudget()
        displayRecentTransactions()
        displayBarCharts()
    }

    private fun displayBudget() {
        val budget = sharedPreferences.getFloat("monthlyBudget", 0f)
        budgetText.text = String.format("Rs.%.2f", budget)
    }
    private fun displayRecentTransactions() {
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!
            .asSequence()
            .map { it.split("|") }
            .sortedByDescending { it[2] } // Sort by date
            .take(5)
            .map { DashboardTransactionItem(it[0], it[1], it[2], it[3].toFloat()) }
            .toList()

        recentTransactionsRecyclerView.adapter = RecentTransactionsAdapter(transactions)
    }

    private fun displayBarCharts() {
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!
            .map { it.split("|") }

        val incomeByCategory = transactions
            .filter { it[0] == "Income" }
            .groupBy { it[4] } // Group by category
            .mapValues { it.value.sumOf { transaction -> transaction[3].toFloat().toDouble() } }

        val expenseByCategory = transactions
            .filter { it[0] == "Expense" }
            .groupBy { it[4] }
            .mapValues { it.value.sumOf { transaction -> transaction[3].toFloat().toDouble() } }

        setupBarChart(incomeBarChart, incomeByCategory, "Income by Category",
            ContextCompat.getColor(this, android.R.color.holo_green_light))
        setupBarChart(expenseBarChart, expenseByCategory, "Expense by Category",
            ContextCompat.getColor(this, android.R.color.holo_red_light))
    }

    private fun setupBarChart(chart: BarChart, data: Map<String, Double>, label: String, color: Int) {
        val entries = data.values.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value.toFloat())
        }

        val dataSet = BarDataSet(entries, label).apply {
            this.color = color
            valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.accent_secondary)
            valueTextSize = 12f
        }

        chart.apply {
            this.data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(data.keys.toList())
            xAxis.labelRotationAngle = 45f
            notifyDataSetChanged()
            invalidate()
        }
    }
}

data class DashboardTransactionItem(
    val type: String,
    val description: String,
    val date: String,
    val amount: Float
)

class RecentTransactionsAdapter(private val transactions: List<DashboardTransactionItem>) :
    RecyclerView.Adapter<RecentTransactionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.transactionTitle)
        val date: TextView = view.findViewById(R.id.transactionDate)
        val amount: TextView = view.findViewById(R.id.transactionAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.title.text = "${transaction.type}: ${transaction.description}"

        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(transaction.date)
            holder.date.text = date?.let { outputFormat.format(it) } ?: transaction.date
        } catch (e: Exception) {
            holder.date.text = transaction.date
        }

        holder.amount.text = String.format("Rs.%.2f", transaction.amount)
        holder.amount.setTextColor(
            ContextCompat.getColor(
                holder.amount.context,
                if (transaction.type == "Income") android.R.color.holo_green_dark
                else android.R.color.holo_red_dark
            )
        )
    }

    override fun getItemCount() = transactions.size
}