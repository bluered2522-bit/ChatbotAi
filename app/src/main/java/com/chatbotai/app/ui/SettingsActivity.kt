package com.chatbotai.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.chatbotai.app.R
import com.chatbotai.app.util.Prefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var spModel: Spinner
    private lateinit var radioGroupProvider: RadioGroup

    // Provider yang lagi ditampilin di layar (belum tentu udah disimpan)
    private var selectedProvider: String = Prefs.PROVIDER_GEMINI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = Prefs(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etApiKey = findViewById(R.id.etApiKey)
        spModel = findViewById(R.id.spModel)
        
        radioGroupProvider = findViewById(R.id.radioGroupProvider)
        val radioGemini = findViewById<RadioButton>(R.id.radioGemini)
        val radioOpenAi = findViewById<RadioButton>(R.id.radioOpenAi)
        val radioClaude = findViewById<RadioButton>(R.id.radioClaude)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Load data tersimpan
        etApiKey.setText(prefs.apiKey)
        selectedProvider = prefs.apiProvider
        when (selectedProvider) {
            Prefs.PROVIDER_OPENAI -> radioOpenAi.isChecked = true
            Prefs.PROVIDER_CLAUDE -> radioClaude.isChecked = true
            else -> radioGemini.isChecked = true
        }
        val geminiModels = arrayOf("gemini-1.5-flash", "gemini-1.5-pro")
        val openAiModels = arrayOf("gpt-4o", "gpt-4o-mini")
        val claudeModels = arrayOf("claude-3-5-sonnet-latest")

        fun gantiModel(provider: String) {
        	    val models = when (provider) {
        	    	        Prefs.PROVIDER_OPENAI -> openAiModels
        	    	                Prefs.PROVIDER_CLAUDE -> claudeModels
        	    	                        else -> geminiModels
        	    	                            }
        	    	                                spModel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, models)
        	    	                                    val savedModel = prefs.getModelFor(provider)
        	    	                                        val pos = models.indexOf(savedModel)
        	    	                                            if (pos >= 0) spModel.setSelection(pos)
        	    	                                            }
        	    	                                            gantiModel(selectedProvider)
 

        radioGroupProvider.setOnCheckedChangeListener { _, checkedId ->
            val newProvider = when (checkedId) {
                R.id.radioOpenAi -> Prefs.PROVIDER_OPENAI
                R.id.radioClaude -> Prefs.PROVIDER_CLAUDE
                else -> Prefs.PROVIDER_GEMINI
            }
            // Tampilin model yang tersimpan buat provider yang baru dipilih (kalau belum ada, kosong + hint default)
            selectedProvider = newProvider
            gantiModel(newProvider)
            
        }

        btnSave.setOnClickListener {
            val key = etApiKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "API Key belum diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.apiKey = key
            prefs.apiProvider = selectedProvider

            val model = spModel.selectedItem.toString()
            prefs.setModelFor(selectedProvider, model)

            Toast.makeText(this, "Pengaturan disimpan — pakai ${Prefs.labelFor(selectedProvider)}", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnLogout.setOnClickListener {
            prefs.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
