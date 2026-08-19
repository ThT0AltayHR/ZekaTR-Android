package com.muhammed.zekatr

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.muhammed.zekatr.databinding.ActivityImageGenerationBinding
import kotlinx.coroutines.launch

class ImageGenerationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageGenerationBinding
    private val engine by lazy { ImageGenerationEngine(this) }
    private var lastBytes: ByteArray? = null
    private var editUri: Uri? = null
    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        editUri = uri
        if (uri != null) { binding.textSelected.text = "Düzenlenecek görsel seçildi"; Glide.with(this).load(uri).into(binding.preview) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageGenerationBinding.inflate(layoutInflater); setContentView(binding.root)
        binding.provider.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ImageGenerationEngine.Provider.values().map { it.label })
        binding.model.setText("gpt-image-1")
        binding.size.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("1024x1024", "1792x1024", "1024x1792"))
        binding.quality.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("high", "medium", "low"))
        binding.btnBack.setOnClickListener { finish() }
        binding.btnPick.setOnClickListener { picker.launch("image/*") }
        binding.btnGenerate.setOnClickListener { generate(false) }
        binding.btnEdit.setOnClickListener { generate(true) }
        binding.btnSave.setOnClickListener { saveImage() }
    }

    private fun generate(edit: Boolean) {
        val prompt = binding.prompt.text?.toString()?.trim().orEmpty()
        if (prompt.isBlank()) { binding.prompt.error = "Ne oluşturulacağını yaz"; return }
        if (edit && editUri == null) { toast("Önce düzenlenecek görseli seç."); return }
        setBusy(true)
        lifecycleScope.launch {
            runCatching {
                val provider = ImageGenerationEngine.Provider.values()[binding.provider.selectedItemPosition]
                if (edit) engine.edit(editUri!!, prompt, binding.model.text.toString().trim().ifBlank { "gpt-image-1" }, binding.size.selectedItem.toString())
                else engine.generate(prompt, provider, binding.model.text.toString().trim().ifBlank { if (provider == ImageGenerationEngine.Provider.OPENAI) "gpt-image-1" else "imagen-3.0-generate-002" }, binding.size.selectedItem.toString(), binding.quality.selectedItem.toString())
            }.onSuccess { result ->
                lastBytes = result.bytes
                val temp = java.io.File(cacheDir, "zekatr_result.png"); temp.writeBytes(result.bytes)
                Glide.with(this@ImageGenerationActivity).load(temp).into(binding.preview)
                binding.textStatus.text = "✓ Hazır • ${result.provider.label} • ${result.model}"
                binding.btnSave.isEnabled = true
            }.onFailure { binding.textStatus.text = "Güvenli hata: ${it.message ?: "Görsel oluşturulamadı."}"; toast(it.message ?: "Görsel oluşturulamadı.") }
            setBusy(false)
        }
    }

    private fun setBusy(busy: Boolean) { binding.progress.visibility = if (busy) View.VISIBLE else View.GONE; binding.btnGenerate.isEnabled = !busy; binding.btnEdit.isEnabled = !busy; binding.textStatus.text = if (busy) "ZekaTR görseli oluşturuyor…" else binding.textStatus.text }

    private fun saveImage() {
        val bytes = lastBytes ?: return
        val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "ZekaTR_${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ZekaTR") }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return toast("Galeri kaydı başarısız.")
        contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        toast("Görsel galeriye kaydedildi.")
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
