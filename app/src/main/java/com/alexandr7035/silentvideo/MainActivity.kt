package com.alexandr7035.silentvideo

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



            val outputDir = getExternalFilesDir(null)?.absolutePath.toString()
            val outputFilePath = outputDir + File.separator + "video.mp4"

            if (selectedFile != null) {
                copyVideoToWorkDir(selectedFile, outputFilePath)
            }

            videoUriLiveData.postValue(selectedFile.toString())

            val file: File = File(selectedFile.toString())
            Log.d(LOG_TAG, file.absolutePath)



            //muteVideo("")

        }
    }


    // We need to use paths to file because ffmpeg doesn't support content ids
    private fun muteVideo(videoPath: String) {

        val outputDir = getExternalFilesDir(null)?.absolutePath.toString()

        val videoPath = outputDir + File.separator + "video.mp4"
        val outputPath = outputDir + File.separator + "video_muted.mp4"

        val command: String = "-i $videoPath -c copy -an $outputPath -y"

        Log.d(LOG_TAG, command)



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


    private fun copyVideoToWorkDir(srcUri: Uri, outputPath: String) {

        Log.d(LOG_TAG, "copy file to $outputPath")

        //val source = File(srcUri.toString())
        //val dest = File(outputPath)

        val outputDir = getExternalFilesDir(null)?.absolutePath.toString()
        val videoPath = outputDir + File.separator + "video.mp4"
        val outputPath = outputDir + File.separator + "video_muted.mp4"

        val source = File(videoPath)
        val dest = File(outputPath)

        if(! dest.exists()){
            dest.createNewFile();
        }

        Log.d(LOG_TAG, "start copying")


            val src: InputStream = FileInputStream(source)
            val dst: OutputStream = FileOutputStream(dest)
            // Copy the bits from instream to outstream
            val buf = ByteArray(1024)


            val f: Long = src.copyTo(dst, 1024)

            Log.d(LOG_TAG, "copied $f bytes")

            src.close()
            dst.close()


        Log.d(LOG_TAG, "stop copying")

    }

}