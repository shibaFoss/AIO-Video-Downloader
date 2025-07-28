package lib.process

import android.content.Context
import android.media.MediaPlayer
import java.lang.ref.WeakReference

/**
 * A utility class for handling audio playback using Android's MediaPlayer.
 *
 * This class provides a simplified interface for common audio playback operations
 * while managing MediaPlayer lifecycle and resource cleanup.
 *
 * Features include:
 * - Playback control (play, pause, resume, stop)
 * - Seeking to specific positions
 * - Volume control
 * - Playback completion and error callbacks
 * - Resource management using WeakReference for context safety
 *
 * @param context The application context (held weakly to prevent memory leaks)
 */
class AudioPlayerUtils(private val context: Context?) {
	
	// MediaPlayer instance for audio playback
	private var mediaPlayer: MediaPlayer? = null
	
	// Callback for when playback completes naturally
	private var onCompletionListener: (() -> Unit)? = null
	
	// Callback for playback errors
	private var onErrorListener: ((Int, Int) -> Unit)? = null
	
	/**
	 * Starts playback of an audio resource.
	 *
	 * @param resId The resource ID of the audio file to play
	 */
	fun play(resId: Int) {
		// Stop any existing playback first
		stop()
		
		// Use WeakReference to avoid context leaks
		WeakReference(context).get()?.let { safeContextRef ->
			mediaPlayer = MediaPlayer.create(safeContextRef, resId)?.apply {
				start()
				
				// Set completion listener to clean up after playback finishes
				setOnCompletionListener {
					onCompletionListener?.invoke()
					stop()
				}
				
				// Set error listener to handle playback errors
				setOnErrorListener { _, what, extra ->
					onErrorListener?.invoke(what, extra)
					stop()
					false // Indicates we handled the error
				}
			}
		}
	}
	
	/**
	 * Pauses the current playback.
	 * Does nothing if no playback is active or already paused.
	 */
	fun pause() {
		mediaPlayer?.takeIf { it.isPlaying }?.pause()
	}
	
	/**
	 * Resumes playback from paused state.
	 * Does nothing if playback is not paused or no media is loaded.
	 */
	fun resume() {
		mediaPlayer?.takeIf { !it.isPlaying }?.start()
	}
	
	/**
	 * Seeks to a specific position in the audio track.
	 *
	 * @param positionMs The position to seek to in milliseconds
	 */
	fun seekTo(positionMs: Int) {
		mediaPlayer?.seekTo(positionMs)
	}
	
	/**
	 * Sets the playback volume.
	 *
	 * @param leftVolume Left channel volume (0.0 to 1.0)
	 * @param rightVolume Right channel volume (0.0 to 1.0)
	 */
	fun setVolume(leftVolume: Float, rightVolume: Float) {
		mediaPlayer?.setVolume(leftVolume, rightVolume)
	}
	
	/**
	 * Gets the current playback position.
	 *
	 * @return Current position in milliseconds, or 0 if no playback active
	 */
	fun getCurrentPosition(): Int {
		return mediaPlayer?.currentPosition ?: 0
	}
	
	/**
	 * Gets the duration of the current audio track.
	 *
	 * @return Duration in milliseconds, or 0 if no media loaded
	 */
	fun getDuration(): Int = mediaPlayer?.duration ?: 0
	
	/**
	 * Stops playback and releases MediaPlayer resources.
	 * Safe to call even if no playback is active.
	 */
	fun stop() {
		mediaPlayer?.apply {
			stop()
			release()
		}
		mediaPlayer = null
	}
	
	/**
	 * Checks if audio is currently playing.
	 *
	 * @return true if playback is active, false otherwise
	 */
	fun isPlaying(): Boolean {
		return mediaPlayer?.isPlaying == true
	}
	
	/**
	 * Sets a callback for when playback completes naturally.
	 *
	 * @param listener Callback to invoke on completion
	 */
	fun setOnCompletionListener(listener: () -> Unit) {
		onCompletionListener = listener
	}
	
	/**
	 * Sets a callback for playback errors.
	 *
	 * @param listener Callback that receives error codes (what, extra)
	 */
	fun setOnErrorListener(listener: (Int, Int) -> Unit) {
		onErrorListener = listener
	}
}