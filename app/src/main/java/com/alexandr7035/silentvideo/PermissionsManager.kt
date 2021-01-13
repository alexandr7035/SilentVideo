package com.alexandr7035.silentvideo

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class PermissionsManager {

    companion object {

        val PERMISSION_DENIED = -1
        val PERMISSION_GRANTED = 1
        val PERMISSION_NOT_NEEDED = 0

        val LOG_TAG = "DEBUG_SV"

        fun checkForWriteExternalStoragePermission(context: Context): Int {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return PERMISSION_NOT_NEEDED
            }
            else {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    return PERMISSION_GRANTED
                }
                else {
                    return PERMISSION_DENIED
                }
            }

        }


        fun requestWriteExternalStoragePermission(activity: AppCompatActivity, sharedPreferences: SharedPreferences) {

            // 2 cases when ActivityCompat.shouldShowRequestPermissionRationalble() is FALSE:
            // 1) When user has denied the permission previously AND never ask again checkbox was selected.
            // 2) When user is requesting permission for the first time
            if (! ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                if (sharedPreferences.getBoolean(activity.getString(R.string.shared_pref_key_first_req_perm_write_external_storage), true)) {

                    val prefEditor = sharedPreferences.edit()
                    prefEditor.putBoolean(activity.getString(R.string.shared_pref_key_first_req_perm_write_external_storage), false)
                    prefEditor.apply()

                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                }

                // Means that "never ask again" checkbox was selected
                // Request dialog will not be shown
                // Show explanation dialog and Redirect user to app settings
                else {

                    val fm = activity.supportFragmentManager

                    val dialog = PermissionExplanationDialog(activity.getString(R.string.permission_explanation_write_external_storage))
                    dialog.show(fm, "tag")

                    /*
                    Log.d(LOG_TAG, "redirect to app settings")
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivityForResult(intent, 134)
                    */

                }

            }

            // Means the user has denied the permission previously but has not checked the "Never Ask Again" checkbox.
            // So request permission again
            else {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
            }


        }

    }

}