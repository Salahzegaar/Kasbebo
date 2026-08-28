package com.example.bookstoremanager.ui

import com.example.bookstoremanager.*
import com.example.bookstoremanager.data.*
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ==========================================
// 1. نموذج بيانات الموظف
// ==========================================
data class Employee(
    val id: String,
    var name: String,
    var phone: String,
    var role: String,
    var salary: Double,
    var notes: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("phone", phone)
        put("role", role)
        put("salary", salary)
        put("notes", notes)
    }

    companion object {
        fun fromJson(json: JSONObject): Employee = Employee(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.getString("name"),
            phone = json.optString("phone", ""),
            role = json.optString("role", "كاشير"),
            salary = json.optDouble("salary", 0.0),
            notes = json.optString("notes", "")
        )
    }
}

// ==========================================
// 2. مدير بيانات الموظفين (EmployeesManager)
// ==========================================
class EmployeesManager(context: Context) {
    private val prefs = context.getSharedPreferences("EmployeesPrefs", Context.MODE_PRIVATE)

    fun getEmployeeList(): List<Employee> {
        val jsonStr = prefs.getString("employees_list_json", "[]") ?: "[]"
        val list = mutableListOf<Employee>()
        try {
            val jArray = JSONArray(jsonStr)
            for (i in 0 until jArray.length()) {
                list.add(Employee.fromJson(jArray.getJSONObject(i)))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    private fun saveEmployeeList(list: List<Employee>) {
        val jArray = JSONArray()
        list.forEach { jArray.put(it.toJson()) }
        prefs.edit().putString("employees_list_json", jArray.toString()).apply()
    }

    fun addEmployee(employee: Employee) {
        val list = getEmployeeList().toMutableList()
        val index = list.indexOfFirst { it.id == employee.id }
        if (index != -1) {
            list[index] = employee
        } else {
            list.add(employee)
        }
        saveEmployeeList(list)
    }

    fun removeEmployee(id: String) {
        val list = getEmployeeList().toMutableList()
        list.removeAll { it.id == id }
        saveEmployeeList(list)
    }
}

// ==========================================
// 3. واجهة إدارة الموظفين (EmployeesTab)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmployeesTab(employeesManager: EmployeesManager, isDark: Boolean, onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddEmployee by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("كاشير") }
    var newSalary by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }

    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("") }
    var editSalary by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }

    var refreshTrigger by remember { mutableStateOf(0) }
    val employees = remember(refreshTrigger, searchQuery) {
        employeesManager.getEmployeeList().filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.role.contains(searchQuery, ignoreCase = true)
        }
    }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = AppTheme.GoldPrimary)
                    }
                    Text("👥 إدارة الموظفين والصلاحيات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.text(isDark))
                }
            }

            PremiumTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = "بحث عن موظف أو دور...", isDark = isDark, icon = Icons.Default.Search)

            if (employees.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("لا يوجد موظفون مسجلون. اضغط على زر + لإضافة موظف جديد.", color = AppTheme.subText(isDark), fontSize = 12.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().weight(1f)) {
                    itemsIndexed(employees, key = { index, emp -> "${emp.id}_$index" }) { _, emp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppTheme.card(isDark)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    editName = emp.name
                                    editPhone = emp.phone
                                    editRole = emp.role
                                    editSalary = emp.salary.toString()
                                    editNotes = emp.notes
                                    employeeToEdit = emp
                                }
                            )
                        ) {
                            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = emp.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppTheme.text(isDark))
                                    Text(text = "الوظيفة: ${emp.role}", fontSize = 12.sp, color = AppTheme.GoldPrimary)
                                    if (emp.phone.isNotBlank()) {
                                        Text(text = "هاتف: ${emp.phone}", fontSize = 11.sp, color = AppTheme.subText(isDark))
                                    }
                                    if (emp.salary > 0) {
                                        Text(text = "الراتب: ${emp.salary} دج", fontSize = 11.sp, color = AppTheme.ColorSuccess)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    IconButton(
                                        onClick = {
                                            editName = emp.name
                                            editPhone = emp.phone
                                            editRole = emp.role
                                            editSalary = emp.salary.toString()
                                            editNotes = emp.notes
                                            employeeToEdit = emp
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "تعديل", tint = AppTheme.text(isDark), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    TextButton(onClick = {
                                            employeesManager.removeEmployee(emp.id)
                                            refreshTrigger++
                                            Toast.makeText(context, "تم حذف الموظف", Toast.LENGTH_SHORT).show()
                                    }) { Text("حذف", color = AppTheme.ColorDanger, fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddEmployee = true },
            containerColor = AppTheme.GoldPrimary,
            contentColor = AppTheme.GoldPrimaryText,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, null)
        }
    }

    if (showAddEmployee) {
        AlertDialog(
            onDismissRequest = { showAddEmployee = false }, containerColor = AppTheme.card(isDark),
            title = { Text("إضافة موظف جديد", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumTextField(value = newName, onValueChange = { newName = it }, label = "اسم الموظف (إجباري)", isDark = isDark)
                    PremiumTextField(value = newRole, onValueChange = { newRole = it }, label = "الدور / الوظيفة (مثل: كاشير)", isDark = isDark)
                    PremiumTextField(value = newPhone, onValueChange = { newPhone = it }, label = "رقم الهاتف", isNumber = true, isDark = isDark)
                    PremiumTextField(value = newSalary, onValueChange = { newSalary = it }, label = "الراتب الشهري", isNumber = true, isDark = isDark)
                    PremiumTextField(value = newNotes, onValueChange = { newNotes = it }, label = "ملاحظات إضافية", isDark = isDark)
                }
            },
            confirmButton = {
                PrimaryAppButton(
                    text = "حفظ الموظف",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val salVal = newSalary.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (newName.isNotBlank()) {
                            employeesManager.addEmployee(
                                Employee(
                                    id = UUID.randomUUID().toString(),
                                    name = newName.trim(),
                                    phone = newPhone.trim(),
                                    role = if(newRole.isBlank()) "كاشير" else newRole.trim(),
                                    salary = salVal,
                                    notes = newNotes.trim()
                                )
                            )
                            newName = ""; newPhone = ""; newRole = "كاشير"; newSalary = ""; newNotes = ""
                            showAddEmployee = false
                            refreshTrigger++
                            Toast.makeText(context, "تمت إضافة الموظف بنجاح! 👥", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "الرجاء إدخال اسم الموظف على الأقل", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            dismissButton = { TextButton(onClick = { showAddEmployee = false }) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
        )
    }

    if (employeeToEdit != null) {
        AlertDialog(
            onDismissRequest = { employeeToEdit = null }, containerColor = AppTheme.card(isDark),
            title = { Text("تعديل بيانات الموظف", color = AppTheme.text(isDark), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumTextField(value = editName, onValueChange = { editName = it }, label = "اسم الموظف", isDark = isDark)
                    PremiumTextField(value = editRole, onValueChange = { editRole = it }, label = "الدور / الوظيفة", isDark = isDark)
                    PremiumTextField(value = editPhone, onValueChange = { editPhone = it }, label = "رقم الهاتف", isNumber = true, isDark = isDark)
                    PremiumTextField(value = editSalary, onValueChange = { editSalary = it }, label = "الراتب الشهري", isNumber = true, isDark = isDark)
                    PremiumTextField(value = editNotes, onValueChange = { editNotes = it }, label = "ملاحظات إضافية", isDark = isDark)
                }
            },
            confirmButton = {
                PrimaryAppButton(
                    text = "حفظ التعديلات",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val salVal = editSalary.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (editName.isNotBlank()) {
                            val emp = employeeToEdit!!
                            emp.name = editName.trim()
                            emp.role = editRole.trim()
                            emp.phone = editPhone.trim()
                            emp.salary = salVal
                            emp.notes = editNotes.trim()
                        }
                    }
                )
            },
            dismissButton = { TextButton(onClick = { employeeToEdit = null }) { Text("إلغاء", color = AppTheme.subText(isDark)) } }
        )
    }
}
