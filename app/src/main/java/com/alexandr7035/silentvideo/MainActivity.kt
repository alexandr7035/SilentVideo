package com.alexandr7035.silentvideo

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val LOG_TAG = "DEBUG_SV"
private lateinit var videoUriLiveData: MutableLiveData<Uri>
private lateinit var sharedPreferences: SharedPreferences
private lateinit var vibrator: Vibrator

class MainActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoUriLiveData = MutableLiveData()

        // Init SharedPreferences
        sharedPreferences = getPreferences(Context.MODE_PRIVATE)

        // Init menu
        toolbar.inflateMenu(R.menu.menu_toolbar_activity_main)
        toolbar.setOnMenuItemClickListener(this)
        onCreateOptionsMenu(toolbar.menu)

        // Folow system by default
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        videoUriLiveData.observe(this, Observer<Uri> { uri ->
            if (uri != null) {

                Log.d(LOG_TAG, uri.toString())

                val mMMR = MediaMetadataRetriever()

                // Exceptions means that selected file is not a video
                try {
                    mMMR.setDataSource(this, uri)
                }
                catch (e: IllegalArgumentException) {
                    videoUriLiveData.postValue(null)
                    showFailSnack(getString(R.string.snack_text_unsupported_file))
                    vibrate(300)
                }

                val bmp: Bitmap? = mMMR.getFrameAtTime(0L)

                videoPreview.setImageBitmap(bmp)

                chooseFileBtn.visibility = View.GONE
                resetFileBtn.visibility = View.VISIBLE
                progressBar.visibility = View.GONE

            }
            else {
                videoPreview.setImageResource(R.drawable.default_bg)
                chooseFileBtn.visibility = View.VISIBLE
                resetFileBtn.visibility = View.GONE
                progressBar.visibility = View.GONE
            }
        })

        videoUriLiveData.postValue(null)

        // Vibrator

        // Vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    }


    fun chooseFileBtn(v: View) {
        val intent: Intent
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)

        chooseFile.type = "video/*"
        intent = Intent.createChooser(chooseFile, getString(R.string.file_chooser_title))
        startActivityForResult(intent, 1)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {

            Log.d(LOG_TAG, "update video uri")

            val selectedFileUri = data?.data

            if (selectedFileUri != null) {
                videoUriLiveData.postValue(selectedFileUri)
            }

        }
    }


    fun resetFileBtn(v: View) {
        videoUriLiveData.postValue(null)
    }


    fun muteVideoBtn(v: View) {

        val selectedFile = videoUriLiveData.value

        if (selectedFile != null) {

            // FIXME Don't use globalscope
            GlobalScope.launch {
                Log.d(LOG_TAG, "start muting in background")

                val startTimeMs = System.currentTimeMillis()

                // Update ui in main thread
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.VISIBLE
                    resetFileBtn.visibility = View.GONE
                    muteVideoBtn.isEnabled = false
                }

                // Mute video
                val result = VideoMuter.muteVideo(this@MainActivity, selectedFile)

                if (result == VideoMuter.MUTING_CODE_SUCCESS) {
                    val endTimeMs = System.currentTimeMillis()

                    val time: Float = (endTimeMs - startTimeMs) / 1000F

                    // Update ui in main thread
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        muteVideoBtn.isEnabled = true
                        resetFileBtn.visibility = View.VISIBLE
                        showSuccessSnack(time)
                        vibrate(200)
                    }

                    Log.d(LOG_TAG, "finish muting video")
                }

                // If muting failed for some reason (see VideoMuter class)
                else {
                    // FIXME handle this
                    Log.d(LOG_TAG, "muting FAILED")

                    // Preform in ui thread
                    withContext(Dispatchers.Main) {

                        progressBar.visibility = View.GONE
                        muteVideoBtn.isEnabled = true

                        showFailSnack(getString(R.string.snack_text_video_muting_failed))
                        vibrate(300)

                        // Reset the video
                        videoUriLiveData.value = null
                    }
                }

            }
        }

        else {
            Toast.makeText(this, getString(R.string.toast_select_video), Toast.LENGTH_LONG).show()
            vibrate(100)
        }
    }


    private fun showSuccessSnack(time: Float) {
        val snack: Snackbar = Snackbar.make(rootView, getString(R.string.snack_text_video_muted, time), Snackbar.LENGTH_LONG)
        snack.setAction(getString(R.string.snack_action_ok), View.OnClickListener {

            Log.d(LOG_TAG, "snack action")
            snack.dismiss()
        })

        snack.show()
    }

    private fun showFailSnack(text: String) {
        val snack: Snackbar = Snackbar.make(rootView, text, Snackbar.LENGTH_LONG)
        snack.setAction(getString(R.string.snack_action_ok), View.OnClickListener {

            Log.d(LOG_TAG, "snack action")
            snack.dismiss()
        })

       // snack.setTextColor(ContextCompat.getColor(this, R.color.red_500))

        snack.show()

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        Log.d(LOG_TAG, "onCreateOptionsMenu")

        val vibrationItem = menu.findItem(R.id.item_vibration)


        vibrationItem.isChecked =  sharedPreferences.getBoolean(getString(R.string.shared_pref_key_vibration), true)

        return true
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_vibration) {
            item.isChecked = ! item.isChecked

            // Save the setting
            val prefEditor = sharedPreferences.edit()
            prefEditor.putBoolean(getString(R.string.shared_pref_key_vibration), item.isChecked)
            prefEditor.apply()

        }

        return super.onOptionsItemSelected(item)
    }


    private fun vibrate(time: Long) {

        // Return of vibration is disabled
        if (! sharedPreferences.getBoolean(getString(R.string.shared_pref_key_vibration), true)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        else {
            vibrator.vibrate(time)
        }
    }

}