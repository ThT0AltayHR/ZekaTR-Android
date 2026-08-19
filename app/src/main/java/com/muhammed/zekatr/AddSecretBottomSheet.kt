package com.muhammed.zekatr

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * "Add Secret" balonu (Replit tarzi).
 * Kullanim: AddSecretBottomSheet { name, value -> ... } .show(supportFragmentManager, "vault")
 * onSaved SADECE basari/basarisizlik bilgisi tasir; ham deger disari sizmaz,
 * cunku deger zaten bu fragment icinde SecretVault'a yazilip degisken kapsamdan cikar.
 */
class AddSecretBottomSheet(
    private val onSaved: (name: String) -> Unit = {}
) : BottomSheetDialogFragment() {

    private var revealing = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottomsheet_add_secret, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nameField = view.findViewById<EditText>(R.id.editSecretName)
        val valueField = view.findViewById<EditText>(R.id.editSecretValue)
        val toggleBtn = view.findViewById<ImageButton>(R.id.btnToggleReveal)

        toggleBtn.setOnClickListener {
            revealing = !revealing
            valueField.inputType = if (revealing) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            valueField.setSelection(valueField.text.length)
        }

        view.findViewById<View>(R.id.btnCancelSecret).setOnClickListener { dismiss() }

        view.findViewById<View>(R.id.btnSubmitSecret).setOnClickListener {
            val name = nameField.text.toString().trim().uppercase().replace(" ", "_")
            val value = valueField.text.toString()
            if (name.isEmpty() || value.isEmpty()) {
                Toast.makeText(requireContext(), "Anahtar adı ve değer boş olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SecretVault(requireContext()).addOrUpdateSecret(name, value)
            // Alanları hemen temizle; değer bellekte/ekranda kalmasın
            valueField.text.clear()
            Toast.makeText(requireContext(), "🔒 '$name' kasaya kaydedildi", Toast.LENGTH_SHORT).show()
            onSaved(name)
            dismiss()
        }
    }
}
