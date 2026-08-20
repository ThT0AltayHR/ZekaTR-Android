package com.muhammed.zekatr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.muhammed.zekatr.databinding.ActivityVaultManagementBinding

class VaultManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVaultManagementBinding
    private lateinit var vault: SecretVault
    private lateinit var adapter: SecretAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vault = SecretVault(this)

        binding.btnBackVault.setOnClickListener { finish() }
        binding.btnAddSecret.setOnClickListener {
            AddSecretBottomSheet { refresh() }.show(supportFragmentManager, "add_secret")
        }

        adapter = SecretAdapter(
            onReveal = { name ->
                val value = vault.revealSecret(name)
                AlertDialog.Builder(this)
                    .setTitle(name)
                    .setMessage(value ?: "(bulunamadı)")
                    .setPositiveButton("Kapat", null)
                    .show()
            },
            onDelete = { name ->
                AlertDialog.Builder(this)
                    .setTitle("Silinsin mi?")
                    .setMessage("'$name' kasadan kalıcı olarak silinecek.")
                    .setPositiveButton("Sil") { _, _ -> vault.deleteSecret(name); refresh() }
                    .setNegativeButton("Vazgeç", null)
                    .show()
            }
        )
        binding.recyclerSecrets.layoutManager = LinearLayoutManager(this)
        binding.recyclerSecrets.adapter = adapter

        refresh()
    }

    private fun refresh() {
        val items = vault.listSecrets()
        adapter.submit(items)
        binding.textVaultEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}

class SecretAdapter(
    private val onReveal: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<SecretAdapter.VH>() {

    private var items: List<SecretVault.SecretMeta> = emptyList()

    fun submit(newItems: List<SecretVault.SecretMeta>) {
        items = newItems
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textSecretName)
        val preview: TextView = view.findViewById(R.id.textSecretPreview)
        val reveal: View = view.findViewById(R.id.btnRevealSecret)
        val delete: View = view.findViewById(R.id.btnDeleteSecret)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_secret, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.preview.text = item.preview
        holder.reveal.setOnClickListener { onReveal(item.name) }
        holder.delete.setOnClickListener { onDelete(item.name) }
    }

    override fun getItemCount() = items.size
}
