package com.myunit.whatsappbot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnPermission: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("BotSettings", Context.MODE_PRIVATE)

        tvStatus = findViewById(R.id.tvStatus)
        btnPermission = findViewById(R.id.btnPermission)
        val switchPrivate = findViewById<Switch>(R.id.switchPrivate)
        val switchGroup = findViewById<Switch>(R.id.switchGroup)

        switchPrivate.isChecked = prefs.getBoolean("allow_private", true)
        switchGroup.isChecked = prefs.getBoolean("allow_group", false)

        btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        switchPrivate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("allow_private", isChecked).apply()
        }

        switchGroup.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("allow_group", isChecked).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isEnabled = flat != null && flat.contains(pkgName)

        if (isEnabled) {
            tvStatus.text = "حالة الخدمة: تعمل بنجاح ✅"
            tvStatus.setTextColor(0xFF2E7D32.toInt())
            btnPermission.text = "إعدادات الصلاحية مفعلة"
            btnPermission.isEnabled = false
        } else {
            tvStatus.text = "حالة الخدمة: متوقفة (تحتاج صلاحية) ⚠️"
            tvStatus.setTextColor(0xFFC62828.toInt())
            btnPermission.text = "تفعيل صلاحية الإشعارات"
            btnPermission.isEnabled = true
        }
    }
}
