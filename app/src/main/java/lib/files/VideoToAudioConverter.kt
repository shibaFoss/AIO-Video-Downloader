package lib.files

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import lib.process.LogHelperUtils
import java.io.File
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

/**
 * Converts video files to audio files by extracting the audio track from a video file.
 * This class handles the extraction process and provides progress updates.
 */
class VideoToAudioConverter {
    
    private val logger = LogHelperUtils.from(javaClass)
    
    @Volatile
    private var isCancelled = false
    
    /**
     * Extracts audio from the provided video file and saves it to the output file.
     * Provides progress updates via the [ConversionListener].
     *
     * @param inputFile The path to the input video file.
     * @param outputFile The path where the extracted audio will be saved.
     * @param listener A [ConversionListener] for receiving progress updates, success, and failure notifications.
     */
    
    fun extractAudio(inputFile: String, outputFile: String, listener: ConversionListener) {
        WeakReference(listener).get()?.let { safeListener ->
            try {
                logger.d("Starting audio extraction from video: $inputFile")

                val extractor = MediaExtractor()
                extractor.setDataSource(inputFile)

                var audioTrackIndex = -1
                var format: MediaFormat? = null
                
                // Loop through tracks to find the audio track.
                for (i in 0 until extractor.trackCount) {
                    format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    logger.d("Checking track $i: MIME = $mime")
                    
                    if (mime?.startsWith("audio/") == true) {
                        audioTrackIndex = i
                        extractor.selectTrack(i)
                        logger.d("Selected audio track index: $audioTrackIndex")
                        break
                    }
                }
                
                // If no audio track is found, notify failure.
                if (audioTrackIndex == -1 || format == null) {
                    val errorMsg = "No audio track found in video"
                    logger.d(errorMsg)
                    safeListener.onFailure(errorMsg)
                    return
                }
                
                // Initialize MediaMuxer to save the audio track.
                logger.d("Initializing MediaMuxer with output file: $outputFile")
                val muxer = MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val newTrackIndex = muxer.addTrack(format)
                muxer.start()
                
                // Buffer and MediaCodec setup for reading and writing samples.
                val buffer = ByteBuffer.allocate(4096)
                val bufferInfo = MediaCodec.BufferInfo()
                
                val fileSize = File(inputFile).length().toFloat()
                var extractedSize = 0L
                
                logger.d("Starting extraction loop...")
                while (!isCancelled) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                    
                    muxer.writeSampleData(newTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                    
                    extractedSize += sampleSize
                    val progress = ((extractedSize / fileSize) * 100).toInt()
                    safeListener.onProgress(progress)
                }
                
                // Handle cancellation during extraction.
                if (isCancelled) {
                    logger.d("Extraction cancelled")
                    safeListener.onFailure("Audio extraction cancelled")
                    return
                }
                
                // Finalize and release resources.
                muxer.stop()
                muxer.release()
                extractor.release()
                
                logger.d("Audio extraction completed successfully: $outputFile")
                safeListener.onSuccess(outputFile)
            } catch (e: Exception) {
                val errorMsg = "Conversion failed: ${e.message}"
                logger.d(errorMsg)
                safeListener.onFailure(errorMsg)
            }
        }
    }
    
    /**
     * Cancels the audio extraction process if it is in progress.
     */
    fun cancel() {
        logger.d("Cancelling audio extraction...")
        isCancelled = true
    }
    
    /**
     * Interface for receiving updates during the audio extraction process.
     */
    interface ConversionListener {
        /**
         * Called to report the progress of the extraction.
         *
         * @param progress An integer between 0 and 100 representing the progress of the extraction.
         */
        fun onProgress(progress: Int)
        
        /**
         * Called when the extraction is successful.
         *
         * @param outputFile The path to the extracted audio file.
         */
        fun onSuccess(outputFile: String)
        
        /**
         * Called when the extraction fails.
         *
         * @param errorMessage A message describing the error that occurred.
         */
        fun onFailure(errorMessage: String)
    }
}
