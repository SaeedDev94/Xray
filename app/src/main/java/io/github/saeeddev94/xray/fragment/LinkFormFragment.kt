package io.github.saeeddev94.xray.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import io.github.saeeddev94.xray.R
import io.github.saeeddev94.xray.database.Link
import java.net.URI
import kotlin.reflect.cast

class LinkFormFragment(
    private val link: Link,
    private val onAction: (confirm: Boolean) -> Unit,
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return openLink(requireActivity())
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finish()
    }

    private fun openLink(context: FragmentActivity): Dialog =
        MaterialAlertDialogBuilder(context).apply {
            val layout = context.layoutInflater.inflate(
                R.layout.layout_link_form,
                LinearLayout(context)
            )

            val typeRadioGroup = layout.findViewById<RadioGroup>(R.id.typeRadioGroup)
            val nameEditText = layout.findViewById<EditText>(R.id.nameEditText)
            val addressEditText = layout.findViewById<EditText>(R.id.addressEditText)
            val userAgentEditText = layout.findViewById<EditText>(R.id.userAgentEditText)
            val isActiveSwitch = layout.findViewById<MaterialSwitch>(R.id.isActiveSwitch)
            Link.Type.entries.forEach {
                val radio = MaterialRadioButton(context)
                radio.text = it.name
                radio.tag = it
                typeRadioGroup.addView(radio)
                if (it == link.type) typeRadioGroup.check(radio.id)
            }
            nameEditText.setText(link.name)
            addressEditText.setText(link.address)
            userAgentEditText.setText(link.userAgent)
            isActiveSwitch.isChecked = if (link.id == 0L) {
                true
            } else {
                link.isActive
            }

            setView(layout)
            setTitle(
                if (link.id == 0L) context.getString(R.string.newLink)
                else context.getString(R.string.editLink)
            )
            setPositiveButton(
                if (link.id == 0L) context.getString(R.string.createLink)
                else context.getString(R.string.updateLink)
            ) { _, _ ->
                val address = addressEditText.text.toString()
                val typeRadioButton = typeRadioGroup.findViewById<RadioButton>(
                    typeRadioGroup.checkedRadioButtonId
                )
                val uri = runCatching { URI(address) }.getOrNull()
                val invalidLink = context.getString(R.string.invalidLink)
                val onlyHttps = context.getString(R.string.onlyHttps)
                if (uri == null) {
                    Toast.makeText(context, invalidLink, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (uri.scheme != "https") {
                    Toast.makeText(context, onlyHttps, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                link.type = Link.Type::class.cast(typeRadioButton.tag)
                link.name = nameEditText.text.toString()
                link.address = address
                link.userAgent = userAgentEditText.text.toString().ifBlank { null }
                link.isActive = isActiveSwitch.isChecked
                onAction(true)
            }
            setNegativeButton(context.getString(R.string.closeLink)) { _, _ ->
                onAction(false)
            }
        }.create()
}
