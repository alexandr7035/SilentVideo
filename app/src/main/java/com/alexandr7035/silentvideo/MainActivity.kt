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

private lateinit var TEMP_FILE_PATH: String
private lateinit var TEMP_MUTED_FILE_PATH: String

private const val MUTED_VIDEO_PREFIX = "silent_video_"

private const val COPY_BUFFER_SIZE = 4096

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

        TEMP_FILE_PATH = getExternalFilesDir(null)?.absolutePath.toString() + File.separator + "video.mp4"
        TEMP_MUTED_FILE_PATH = getExternalFilesDir(null)?.absolutePath.toString() + File.separator + "video_muted.mp4"
    }


    fun chooseFileBtn(v: View) {
        val intent: Intent
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)

        // Fixme allow to select only a video
        chooseFile.type = "*/*"
        intent = Intent.createChooser(chooseFile, getString(R.string.file_chooser_title))
        startActivityForResult(intent, 1)
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

                copyVideoToWorkDir(selectedFile)

                if (muteVideo() == RETURN_CODE_SUCCESS) {
                    saveMutedVideoToMediaStore()
                }

                cleanUp()

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
        }

        else {
            Toast.makeText(this, getString(R.string.toast_select_video), Toast.LENGTH_LONG).show()
        }
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


    private fun muteVideo(): Int {

        val command = "-i $TEMP_FILE_PATH -c copy -an $TEMP_MUTED_FILE_PATH -y"
        Log.d(LOG_TAG, "execute FFMPEG command $command")

        // Execution can be synchronous because method is run inside coroutine
        return FFmpeg.execute(command)

    }


    private fun copyVideoToWorkDir(srcUri: Uri) {

        Log.d(LOG_TAG, "copy file to $TEMP_FILE_PATH")

        val contentResolver = contentResolver
        val srcStream: InputStream? = contentResolver.openInputStream(srcUri)
        val dstStream: OutputStream = FileOutputStream(File(TEMP_FILE_PATH))


        if (srcStream != null) {
            val f: Long = srcStream.copyTo(dstStream, COPY_BUFFER_SIZE)
            Log.d(LOG_TAG, "copied $f bytes")
            srcStream.close()
        }

        dstStream.close()

        Log.d(LOG_TAG, "stop copying")

    }


    private fun saveMutedVideoToMediaStore() {
        val videoFileName = MUTED_VIDEO_PREFIX + System.currentTimeMillis() + ".mp4"

        val valuesVideos = ContentValues()
        valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "SilentVideo")
        valuesVideos.put(MediaStore.Video.Media.TITLE, videoFileName)
        valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
        valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        valuesVideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
        valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 1)
        val resolver: ContentResolver = contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uriSavedVideo = resolver.insert(collection, valuesVideos)

        if (uriSavedVideo != null) {
            val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uriSavedVideo, "w")
            val out = FileOutputStream(pfd!!.fileDescriptor)

            val videoFile = File(TEMP_MUTED_FILE_PATH)
            val inputStream = FileInputStream(videoFile)

            Log.d(LOG_TAG, "copy video to galery")
            val bytes: Long = inputStream.copyTo(out, COPY_BUFFER_SIZE)

            Log.d(LOG_TAG, "copied $bytes bytes")

            inputStream.close()
            out.close()

        }
    }


    private fun cleanUp() {
        File(TEMP_MUTED_FILE_PATH).delete()
        File(TEMP_FILE_PATH).delete()
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