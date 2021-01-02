package com.alexandr7035.silentvideo

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.*

class VideoMuter {

    companion object {

        private lateinit var TEMP_FILE_PATH: String
        private lateinit var TEMP_MUTED_FILE_PATH: String

        private val MUTED_VIDEO_PREFIX = "silent_video_"
        private val COPY_BUFFER_SIZE = 4096

        val LOG_TAG = "DEBUG_SV"

        private lateinit var videoUri: Uri;

        const val MUTING_CODE_SUCCESS = 0
        const val MUTING_CODE_FAIL = 1


        // Returns execution code
        fun muteVideo(context: Context, videoUri: Uri): Int {
            this.videoUri = videoUri

            TEMP_FILE_PATH = context.getExternalFilesDir(null)?.absolutePath.toString() + File.separator + "video.mp4"
            TEMP_MUTED_FILE_PATH = context.getExternalFilesDir(null)?.absolutePath.toString() + File.separator + "video_muted.mp4"

            try {
                copyVideoToWorkDir(context)

                if (removeAudio() == Config.RETURN_CODE_SUCCESS) {
                    saveMutedVideoToMediaStore(context)
                } else {
                    return MUTING_CODE_FAIL
                }
            }

            // If any exception occurred (f.e. when copyTo() method called)
            catch (e: IOException) {
                return MUTING_CODE_FAIL
            }


            // Clean up
            File(TEMP_FILE_PATH).delete()
            File(TEMP_MUTED_FILE_PATH).delete()

            return MUTING_CODE_SUCCESS
        }


        private fun copyVideoToWorkDir(context: Context) {

            Log.d(LOG_TAG, "copy file to $TEMP_FILE_PATH")

            val contentResolver = context.contentResolver
            val srcStream: InputStream? = contentResolver.openInputStream(videoUri)
            val dstStream: OutputStream = FileOutputStream(File(TEMP_FILE_PATH))


            if (srcStream != null) {
                val f: Long = srcStream.copyTo(dstStream, COPY_BUFFER_SIZE)
                Log.d(LOG_TAG, "copied $f bytes")
                srcStream.close()
            }

            dstStream.close()

            Log.d(LOG_TAG, "stop copying")

        }


        private fun removeAudio(): Int {

            val command = "-i $TEMP_FILE_PATH -c copy -an $TEMP_MUTED_FILE_PATH -y"
            Log.d(LOG_TAG, "execute FFMPEG command $command")

            // Execution can be synchronous because method is run inside coroutine
            return FFmpeg.execute(command)

        }


        private fun saveMutedVideoToMediaStore(context: Context) {
            val videoFileName = MUTED_VIDEO_PREFIX + System.currentTimeMillis() + ".mp4"

            val valuesVideos = ContentValues()
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "SilentVideo")
            valuesVideos.put(MediaStore.Video.Media.TITLE, videoFileName)
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            valuesVideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 1)
            val resolver: ContentResolver = context.contentResolver
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uriSavedVideo = resolver.insert(collection, valuesVideos)

            if (uriSavedVideo != null) {
                val pfd: ParcelFileDescriptor? = resolver.openFileDescriptor(uriSavedVideo, "w")
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



}