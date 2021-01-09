package com.alexandr7035.silentvideo

import android.content.Intent
import android.graphics.Insets
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity


class PermissionExplanationDialog(private val explanationString: String): DialogFragment(), View.OnClickListener {

    val LOG_TAG = "DEBUG_SV"

    private lateinit var btnCancel: TextView
    private lateinit var btnSettings: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(LOG_TAG, "")

        val dialogView: View = inflater.inflate(R.layout.dialog_permission_explanation, container, false)

        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.background_dialog))

        isCancelable = false

        val explanationView: TextView = dialogView.findViewById(R.id.explanationTextView)
        explanationView.text = explanationString

        val goToSettingsView: TextView = dialogView.findViewById(R.id.goToSettingsTextView)
        goToSettingsView.text = getString(R.string.grant_permission_in_settings)

        setStyle(STYLE_NORMAL, R.style.DialogTheme)

        btnCancel = dialogView.findViewById(R.id.btnCancel)
        btnSettings = dialogView.findViewById(R.id.btnSettings)
        btnCancel.setOnClickListener(this)
        btnSettings.setOnClickListener(this)

        return dialogView
    }

    override fun onResume() {
        super.onResume()
        val window = dialog!!.window

        if (window != null) {
            window.setLayout((getScreenWidth(getActivity()) * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
    }


    private fun getScreenWidth(activity: FragmentActivity?): Int {

        if (activity == null ) {
            return 0
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())

            return windowMetrics.bounds.width() - insets.left - insets.right

        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.widthPixels
        }
    }

    override fun onClick(v: View) {
        if (v.id == btnCancel.id) {
            dialog?.dismiss()
        }

        else if (v.id == btnSettings.id) {
            Log.d(LOG_TAG, "redirect to app settings")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", activity?.packageName, null)
            intent.data = uri
            activity?.startActivityForResult(intent, 134)
        }
    }

}