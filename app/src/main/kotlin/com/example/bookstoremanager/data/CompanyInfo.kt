package com.example.bookstoremanager.data

import android.content.Context
import android.content.SharedPreferences

data class CompanyInfo(
    var name: String = "",
    var activity: String = "",
    var address: String = "",
    var phoneFax: String = "",
    var rcNumber: String = "", // رقم السجل التجاري
    var nifNumber: String = "", // الرقم الضريبي
    var articleNumber: String = "", // رقم المادة
    var logoBase64: String = "" // الشعار
)

class CompanyInfoManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("CompanyPrefs", Context.MODE_PRIVATE)

    fun saveCompanyInfo(info: CompanyInfo) {
        prefs.edit().apply {
            putString("comp_name", info.name)
            putString("comp_activity", info.activity)
            putString("comp_address", info.address)
            putString("comp_phone", info.phoneFax)
            putString("comp_rc", info.rcNumber)
            putString("comp_nif", info.nifNumber)
            putString("comp_article", info.articleNumber)
            putString("comp_logo", info.logoBase64)
            apply()
        }
    }

    fun getCompanyInfo(): CompanyInfo {
        return CompanyInfo(
            name = prefs.getString("comp_name", "") ?: "",
            activity = prefs.getString("comp_activity", "") ?: "",
            address = prefs.getString("comp_address", "") ?: "",
            phoneFax = prefs.getString("comp_phone", "") ?: "",
            rcNumber = prefs.getString("comp_rc", "") ?: "",
            nifNumber = prefs.getString("comp_nif", "") ?: "",
            articleNumber = prefs.getString("comp_article", "") ?: "",
            logoBase64 = prefs.getString("comp_logo", "") ?: ""
        )
    }
}
