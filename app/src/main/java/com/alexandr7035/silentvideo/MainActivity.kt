package com.alexandr7035.silentvideo

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*


private const val LOG_TAG = "DEBUG_SV"
private lateinit var videoUriLiveData: MutableLiveData<Uri>


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoUriLiveData = MutableLiveData()

        progressBar.visibility = View.GONE



        videoUriLiveData.observe(this, Observer<Uri> { uri ->
            if (uri != null) {
                //videoPreview.text = uri.toString()

                val mMMR = MediaMetadataRetriever()
                mMMR.setDataSource(this, uri)
                val bmp: Bitmap? = mMMR.getFrameAtTime(0L)

                videoPreview.setImageBitmap(bmp)

                chooseFileBtn.visibility = View.GONE
                resetFileBtn.visibility = View.VISIBLE

            }
            else {
                videoPreview.setImageResource(R.drawable.default_bg)
                chooseFileBtn.visibility = View.VISIBLE
                resetFileBtn.visibility = View.GONE
            }
        })

        videoUriLiveData.postValue(null)


        toolbar.inflateMenu(R.menu.menu_toolbar_activity_main)

    }


    fun chooseFileBtn(v: View) {
        val intent: Intent
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)

        // Fixme allow to select only a video
        chooseFile.type = "*/*"
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
                        showSuccessSnack(time)
                    }

                    Log.d(LOG_TAG, "finish muting video")
                }
                else {
                    // FIXME handle this
                    Log.d(LOG_TAG, "muting FAILED")
                }

            }
        }

        else {
            Toast.makeText(this, getString(R.string.toast_select_video), Toast.LENGTH_LONG).show()
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

    private fun showFailSnack() {

    }
}