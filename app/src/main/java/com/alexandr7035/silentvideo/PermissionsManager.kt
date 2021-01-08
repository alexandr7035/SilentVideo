package com.alexandr7035.silentvideo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat


class PermissionsManager {

    companion object {

        val PERMISSION_DENIED = -1
        val PERMISSION_GRANTED = 1
        val PERMISSION_NOT_NEEDED = 0

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




    }

}