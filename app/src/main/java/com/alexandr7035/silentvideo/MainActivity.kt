package com.alexandr7035.silentvideo

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*


private val LOG_TAG = "DEBUG_SV"
private lateinit var videoUriLiveData: MutableLiveData<String>

private lateinit var TEMP_FILE_PATH: String
private lateinit var TEMP_MUTED_FILE_PATH: String

private const val COPY_BUFFER_SIZE = 1024

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoUriLiveData = MutableLiveData()

        videoUriLiveData.observe(this, Observer<String> { uri ->
            if (uri != null ) {
                videoPreview.text = uri
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {

            Log.d(LOG_TAG, "update video uri")

            val selectedFile = data?.data

            if (selectedFile != null) {
                copyVideoToWorkDir(selectedFile)
                muteVideo()
            }

            videoUriLiveData.postValue(selectedFile.toString())

        }
    }


    private fun muteVideo() {

        val command: String = "-i $TEMP_FILE_PATH -c copy -an $TEMP_MUTED_FILE_PATH -y"
        Log.d(LOG_TAG, "execute FFMPEG command $command")

        val executionId = FFmpeg.executeAsync(command) { executionId, returnCode ->

                Log.d(LOG_TAG, "start")

                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.d(LOG_TAG, "Async command execution completed successfully.")
                    Log.d(LOG_TAG, "execution id $executionId")

                    Toast.makeText(this, "ffmpeg succeded", Toast.LENGTH_SHORT).show()

                }
                else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.d(LOG_TAG,"Async command execution cancelled by user.")
                    Toast.makeText(this, "ffmpeg canceled", Toast.LENGTH_SHORT).show()
                }
                else {
                    Log.d(LOG_TAG, "Async command execution failed with $returnCode")
                    Toast.makeText(this, "ffmpeg failed", Toast.LENGTH_SHORT).show()
                }
            }


    }


    private fun copyVideoToWorkDir(srcUri: Uri) {

        Log.d(LOG_TAG, "copy file to $TEMP_FILE_PATH")

        val contentResolver = getContentResolver()
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

}