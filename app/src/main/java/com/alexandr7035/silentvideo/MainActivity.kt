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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*


private val LOG_TAG = "DEBUG_SV"
private lateinit var videoUriLiveData: MutableLiveData<Uri>

private lateinit var TEMP_FILE_PATH: String
private lateinit var TEMP_MUTED_FILE_PATH: String

private const val COPY_BUFFER_SIZE = 4096

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoUriLiveData = MutableLiveData()

        videoUriLiveData.observe(this, Observer<Uri> { uri ->
            if (uri != null ) {
                //videoPreview.text = uri.toString()

                val mMMR = MediaMetadataRetriever()
                mMMR.setDataSource(this,  uri)
                val bmp: Bitmap? = mMMR.getFrameAtTime(0L)

                videoPreview.setImageBitmap(bmp)
            }
        })


        TEMP_FILE_PATH = getExternalFilesDir(null)?.absolutePath.toString() + File.separator + "video.mp4"
        TEMP_MUTED_FILE_PATH = getExternalFilesDir(null)?.absolutePath.toString() + File.separator + "video_muted.mp4"
    }


    fun chooseFileBtn(v: View) {
        val intent: Intent
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)

        chooseFile.type = "*/*"
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 1)
    }

    fun muteVideoBtn(v: View) {

        val selectedFile = videoUriLiveData.value

        if (selectedFile != null) {

            GlobalScope.launch {
                Log.d(LOG_TAG, "start muting in background")
                copyVideoToWorkDir(selectedFile)

                if (muteVideo() == RETURN_CODE_SUCCESS) {
                    saveMutedVideoToMediaStore()
                }

                Log.d(LOG_TAG, "finish muting video")
            }
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
        val videoFileName = "muted_video_" + System.currentTimeMillis() + ".mp4"

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

}