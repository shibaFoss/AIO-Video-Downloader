package app.ui.others.media_player

import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod.getInstance
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.documentfile.provider.DocumentFile.fromFile
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.Builder
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioTimer
import app.core.AIOApp.Companion.downloadSystem
import app.core.AIOTimer.AIOTimerListener
import app.core.bases.BaseActivity
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.downloader.DownloadDataModel.Companion.DOWNLOAD_MODEL_ID_KEY
import app.core.engines.downloader.DownloadURLHelper.getFileInfoFromSever
import app.ui.others.media_player.dialogs.MediaInfoHtmlBuilder.buildMediaInfoHtmlString
import app.ui.others.media_player.dialogs.MediaOptionsPopup
import com.aio.R.color
import com.aio.R.drawable
import com.aio.R.id
import com.aio.R.layout
import com.aio.R.string
import com.anggrayudi.storage.file.FileFullPath
import com.anggrayudi.storage.file.getAbsolutePath
import com.google.common.io.Files.getFileExtension
import lib.device.ShareUtility
import lib.device.ShareUtility.shareMediaFile
import lib.files.FileSystemUtility.getFileFromUri
import lib.files.FileSystemUtility.isAudio
import lib.files.FileSystemUtility.isVideo
import lib.networks.URLUtility.isValidURL
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.process.LogHelperUtils
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.fromHtmlStringToSpanned
import lib.ui.MsgDialogUtils.getMessageDialog
import lib.ui.MsgDialogUtils.showMessageDialog
import lib.ui.RoundedTimeBar
import lib.ui.ViewUtility.hideView
import lib.ui.ViewUtility.matchHeightToTopCutout
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.ViewUtility.setTextColorKT
import lib.ui.ViewUtility.showView
import lib.ui.ViewUtility.toggleViewVisibility
import lib.ui.builders.ToastView.Companion.showToast
import java.io.File
import java.lang.ref.WeakReference
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Main activity for media playback functionality.
 *
 * Handles video and audio playback with features like:
 * - Playback controls (play/pause, seek, skip)
 * - Media information display
 * - Subtitle support
 * - Video to audio conversion
 * - Media sharing
 * - Fullscreen and orientation control
 *
 * Uses ExoPlayer as the underlying media playback engine.
 */
@UnstableApi
class MediaPlayerActivity : BaseActivity(), AIOTimerListener, Listener {
	
	// Weak reference to self to prevent memory leaks
	private val safeSelfReference = WeakReference(this).get()
	
	companion object {
		// Intent extra keys
		const val DOWNLOAD_MODEL_ID_REFERENCE = "DOWNLOAD_MODEL_ID_REFERENCE"
		const val STREAM_MEDIA_URL = "STREAM_MEDIA_URL"
		const val STREAM_MEDIA_TITLE = "STREAM_MEDIA_TITLE"
		const val PLAY_MEDIA_FILE_PATH = "PLAY_MEDIA_FILE_PATH"
		const val WHERE_DID_YOU_COME_FROM = "WHERE_DID_YOU_COME_FROM"
		
		// Constants for navigation origin
		const val FROM_FINISHED_DOWNLOADS_LIST = 1
		const val FROM_PRIVATE_FOLDER_LIST = 2
	}
	
	// Player components
	lateinit var exoMediaPlayer: ExoPlayer
	lateinit var exoMediaPlayerView: PlayerView
	lateinit var playerDefaultTrackSelector: DefaultTrackSelector
	
	// UI components
	lateinit var mediaOptionsPopup: MediaOptionsPopup
	lateinit var textQuickInfo: TextView
	lateinit var invisibleTouchArea: View
	lateinit var invisibleNightModeOverlay: View
	
	lateinit var deviceCutoutEmptyPadding: View
	lateinit var audioAlbumArtHolder: ImageView
	lateinit var entirePlaybackController: View
	lateinit var buttonBackActionbar: View
	lateinit var textCurrentVideoName: TextView
	lateinit var buttonOptionActionbar: View
	
	lateinit var textProgressTimer: TextView
	lateinit var textVideoDuration: TextView
	lateinit var videoProgressBar: RoundedTimeBar
	
	lateinit var buttonControllerLock: View
	lateinit var buttonVideoPlayPrevious: View
	lateinit var buttonVideoPlayPauseToggle: View
	lateinit var buttonVideoPlayNext: View
	lateinit var buttonShareMediaFile: View
	lateinit var buttonControllerUnlock: View
	
	// Player state variables
	var areVideoControllersLocked = false
	var trackPlaybackPosition: Long = 0L
	var isNightModeOn = false
	
	var isDoubleClickedInvisibleArea = 0
	val seekAmountOfVideoForwardRewind = 10000L
	
	// Handler for quick info display
	val quickInfoHandler = Handler(Looper.getMainLooper())
	var quickInfoDelayRunnable: Runnable? = null
	
	/**
	 * Sets up the activity layout and appearance.
	 * @return The layout resource ID for the activity
	 */
	override fun onRenderingLayout(): Int {
		setDarkSystemStatusBar(); setEdgeToEdgeFullscreen()
		setEdgeToEdgeCustomCutoutColor(resources.getColor(color.pure_black, theme))
		initializeAutoRotateMediaPlayer(shouldToggleAutoRotate = true)
		return layout.activity_player_1
	}
	
	/**
	 * Initializes views and starts playback after layout is rendered.
	 */
	override fun onAfterLayoutRender() {
		initLayoutViews().let {
			initVideoPlayer().let {
				initializeSwipeGestureSeeking(targetView = invisibleTouchArea)
				playVideoFromIntent().let {
					hideView(invisibleNightModeOverlay, true, 1000)
				}
			}
		}
	}
	
	/**
	 * Handles back button press with proper player cleanup.
	 */
	override fun onBackPressActivity() {
		if (areVideoControllersLocked) {
			doSomeVibration(20)
			val quickInfoText = getString(string.text_player_is_locked_unlock_first)
			showQuickPlayerInfo(quickInfoText); return
		}; stopAndReleasePlayer()

		hideView(exoMediaPlayerView, true, 1000).let {
			delay(500, object : OnTaskFinishListener {
				override fun afterDelay() {
					closeActivityWithFadeAnimation(true)
				}
			})
		}
	}
	
	/**
	 * Resumes player and loads ads when activity resumes.
	 */
	override fun onResumeActivity() {
		resumePlayer()
	}
	
	/**
	 * Pauses player when activity pauses.
	 */
	override fun onPauseActivity() = pausePlayer()
	
	/**
	 * Cleans up player resources when activity is destroyed.
	 */
	override fun onDestroy() {
		super.onDestroy()
		stopAndReleasePlayer()
	}
	
	/**
	 * Application wide timer loop used for refreshing the progress container.
	 */
	override fun onAIOTimerTick(loopCount: Double) = updateVideoProgressContainer()
	
	/**
	 * Handles player state changes.
	 * @param playbackState The new playback state
	 */
	override fun onPlaybackStateChanged(playbackState: Int) {
		if (playbackState == Player.STATE_ENDED) onPlaybackCompleted()
		else takeIf { isPlayerBufferingOrGettingReady(playbackState) }
			?.let { updateVideoProgressContainer() }
	}
	
	/**
	 * Checks if player is buffering or ready to play.
	 * @param playbackState Current playback state
	 * @return True if player is buffering or ready
	 */
	private fun isPlayerBufferingOrGettingReady(playbackState: Int): Boolean {
		return playbackState == STATE_READY || playbackState == STATE_BUFFERING
	}
	
	/**
	 * Handles configuration changes (e.g., orientation changes).
	 * @param newConfig The new configuration
	 */
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		if (newConfig.orientation == ORIENTATION_PORTRAIT) handlePortraitMode()
		else if (newConfig.orientation == ORIENTATION_LANDSCAPE) handleLandscapeMode()
	}
	
	/**
	 * Handles player errors.
	 * @param error The playback error
	 */
	override fun onPlayerError(error: PlaybackException) {
		error.printStackTrace()
		showMessageDialog(
			baseActivityInf = safeSelfReference,
			titleText = getString(string.title_couldnt_play_media),
			isTitleVisible = true,
			isCancelable = true,
			isNegativeButtonVisible = false,
			titleTextViewCustomize = { it.setTextColor(resources.getColor(color.color_error, theme)) },
			messageTextViewCustomize = { it.setText(string.text_error_media_cant_be_played) },
			positiveButtonTextCustomize = {
				it.text = getString(string.title_exit_the_player)
				it.setLeftSideDrawable(drawable.ic_button_exit)
			}, negativeButtonText = getString(string.title_delete_file),
			negativeButtonTextCustomize = { it.setLeftSideDrawable(drawable.ic_button_delete) }
		)?.let { dialogBuilder ->
			dialogBuilder.setOnClickForNegativeButton { dialogBuilder.close(); deleteMediaFile() }
			dialogBuilder.setOnClickForPositiveButton { dialogBuilder.close(); closeActivityWithFadeAnimation() }
		}
	}
	
	/**
	 * Checks if currently playing a streaming video.
	 * @return True if streaming video is playing
	 */
	fun isPlayingStreamingVideo(): Boolean {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			intent.getParcelableExtra(STREAM_MEDIA_URL, Uri::class.java)?.let { return true }
		else intent.getStringExtra(STREAM_MEDIA_URL)?.let { if (isValidURL(it)) return true }
		return false
	}
	
	/**
	 * Handles play/pause state changes.
	 * @param isPlaying True if player is currently playing
	 */
	override fun onIsPlayingChanged(isPlaying: Boolean) {
		safeSelfReference?.let { safeActivityRef ->
			let {
				if (isPlaying) aioTimer.register(safeActivityRef)
				else aioTimer.unregister(safeActivityRef)
			}.apply {
				val incomingVideoTitle = intent.getStringExtra(STREAM_MEDIA_TITLE)
				if (incomingVideoTitle.isNullOrEmpty()) updateCurrentVideoName()
				updateIconOfVideoPlayPauseButton(isPlaying)
			}
		}
	}
	
	/**
	 * Sets dark theme for system bars.
	 */
	fun setDarkSystemStatusBar() {
		setSystemBarsColors(
			statusBarColorResId = color.pure_black,
			navigationBarColorResId = color.pure_black,
			isLightStatusBar = false,
			isLightNavigationBar = false
		)
	}
	
	/**
	 * Handles landscape mode changes.
	 */
	fun handleLandscapeMode() {
		deviceCutoutEmptyPadding.visibility = GONE
		delay(timeInMile = 400, listener = object : OnTaskFinishListener {
			override fun afterDelay() = hideEntirePlaybackControllers()
		})
	}
	
	/**
	 * Handles portrait mode changes.
	 */
	fun handlePortraitMode() {
		deviceCutoutEmptyPadding.visibility = VISIBLE
		delay(timeInMile = 400, listener = object : OnTaskFinishListener {
			override fun afterDelay() = hideEntirePlaybackControllers()
		})
	}
	
	/**
	 * Shows quick player info message.
	 * @param msgText The message to display
	 */
	fun showQuickPlayerInfo(msgText: String) {
		textQuickInfo.apply { visibility = VISIBLE; text = msgText }
		quickInfoDelayRunnable?.let { quickInfoHandler.removeCallbacks(it) }
		quickInfoDelayRunnable = Runnable {
			textQuickInfo.apply { visibility = GONE; text = "" }
		}; quickInfoHandler.postDelayed(quickInfoDelayRunnable!!, 1500)
	}
	
	/**
	 * Initializes all layout views and sets up click listeners.
	 */
	private fun initLayoutViews() {
		safeSelfReference?.let { _ ->
			// Initialize ExoPlayer view and other UI components
			exoMediaPlayerView = findViewById(id.video_player_view)
			textQuickInfo = findViewById(id.txt_video_quick_info)
			invisibleNightModeOverlay = findViewById(id.night_mode_invisible_area)
			invisibleNightModeOverlay.visibility = GONE
			invisibleTouchArea = findViewById(id.invisible_touch_area)
			
			// Set up device cutout handling
			deviceCutoutEmptyPadding = findViewById(id.device_cutout_padding_view)
			deviceCutoutEmptyPadding.matchHeightToTopCutout()
			
			// Initialize playback controls
			entirePlaybackController = findViewById(id.container_player_controller)
			entirePlaybackController.visibility = GONE
			
			// Initialize album art holder for audio files
			audioAlbumArtHolder = findViewById(id.img_audio_album_art)
			
			// Set up action bar buttons
			buttonBackActionbar = findViewById(id.btn_actionbar_back)
			buttonBackActionbar.apply { setOnClickListener { onBackPressActivity() } }
			
			textCurrentVideoName = findViewById(id.txt_video_file_name)
			textCurrentVideoName.apply { isSelected = true; text = getDownloadModelFromIntent()?.fileName ?: "" }
			
			buttonOptionActionbar = findViewById(id.btn_actionbar_option)
			buttonOptionActionbar.apply { setOnClickListener { showOptionMenuPopup() } }
			
			// Initialize progress display
			textProgressTimer = findViewById(id.text_video_progress_timer)
			videoProgressBar = findViewById(id.video_progress_bar)
			videoProgressBar.apply { addListener(generateOnScrubberListener()) }
			
			textVideoDuration = findViewById(id.text_video_duration)
			
			// Initialize control buttons
			buttonControllerLock = findViewById(id.btn_video_controllers_lock)
			buttonControllerLock.apply { setOnClickListener { lockEntirePlaybackControllers() } }
			
			buttonVideoPlayPrevious = findViewById(id.btn_video_previous)
			buttonVideoPlayPrevious.apply { setOnClickListener { playPreviousMedia() } }
			
			buttonVideoPlayPauseToggle = findViewById(id.btn_video_play_pause_toggle)
			buttonVideoPlayPauseToggle.apply { setOnClickListener { toggleVideoPlayback() } }
			
			buttonVideoPlayNext = findViewById(id.btn_video_next)
			buttonVideoPlayNext.apply { setOnClickListener { playNextMedia() } }
			
			buttonShareMediaFile = findViewById(id.btn_video_file_share)
			buttonShareMediaFile.apply { setOnClickListener { shareMediaFile() } }
			
			buttonControllerUnlock = findViewById(id.btn_video_unlock_overlay)
			buttonControllerUnlock.apply { setOnClickListener { unlockEntirePlaybackControllers() } }
		}
	}
	
	/**
	 * Initializes the ExoPlayer instance with proper configuration.
	 */
	private fun initVideoPlayer() {
		safeSelfReference?.let { safeActivityRef ->
			// Configure renderers factory
			val defaultRenderersFactory = DefaultRenderersFactory(safeActivityRef)
				.setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
				.forceEnableMediaCodecAsynchronousQueueing()
				.setEnableDecoderFallback(true)
			
			// Set up track selector
			playerDefaultTrackSelector = DefaultTrackSelector(safeActivityRef)
			exoMediaPlayer =
				ExoPlayer.Builder(safeActivityRef, defaultRenderersFactory)
					.setTrackSelector(playerDefaultTrackSelector)
					.build().apply { addListener(safeActivityRef) }
			exoMediaPlayer.setForegroundMode(false)
			
			// Configure player view
			exoMediaPlayerView.player = exoMediaPlayer
			exoMediaPlayerView.subtitleView?.apply {
				setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
				setApplyEmbeddedStyles(true)
				setApplyEmbeddedFontSizes(false)
				setCues(emptyList())
				val playerStyle = CaptionStyleCompat(
					getColor(safeActivityRef, color.transparent_white),
					getColor(safeActivityRef, color.transparent),
					getColor(safeActivityRef, color.transparent),
					EDGE_TYPE_NONE,
					getColor(safeActivityRef, color.color_primary_variant),
					Typeface.DEFAULT_BOLD
				); setStyle(playerStyle)
			}
		}
	}
	
	/**
	 * Generates scrubber listener for progress bar.
	 * @return Configured VideoScrubberListener instance
	 */
	private fun generateOnScrubberListener(): VideoScrubberListener {
		return object : VideoScrubberListener() {
			override fun onScrubStart(timeBar: TimeBar, position: Long) = Unit
			
			override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
				if (!canceled) exoMediaPlayer.seekTo(position)
			}
		}
	}
	
	/**
	 * Updates the current video name display.
	 */
	private fun updateCurrentVideoName() {
		val currentItem = exoMediaPlayer.currentMediaItem
		val currentMediaUri = currentItem?.localConfiguration?.uri.toString()
		if (currentMediaUri.isEmpty()) return
		
		val currentFileName = currentMediaUri.toUri().lastPathSegment ?: return
		updateMediaTitleWith(currentFileName)
	}
	
	/**
	 * Updates media title display.
	 * @param videoName The name to display
	 */
	private fun updateMediaTitleWith(videoName: CharSequence) {
		::textCurrentVideoName.isInitialized.takeIf { it }.let {
			if (textCurrentVideoName.text != videoName) textCurrentVideoName.text = videoName
		}
	}
	
	/**
	 * Gets current playback position.
	 * @return Current position in milliseconds
	 */
	private fun getCurrentPosition(): Long = exoMediaPlayer.currentPosition
	
	/**
	 * Gets media duration.
	 * @return Duration in milliseconds
	 */
	private fun getDuration(): Long = exoMediaPlayer.duration
	
	/**
	 * Initializes swipe gesture seeking on target view.
	 * @param targetView The view to attach gesture detector to
	 */
	private fun initializeSwipeGestureSeeking(targetView: View) {
		var isSeeking = false
		var initialSeekPosition = 0L
		var wasPlayerPlaying = false
		var isScrolling = false
		var isLongPressActive = false
		val longPressTimeout = 500L
		val longPressHandler = Handler(Looper.getMainLooper())
		
		// Set up gesture detector for seeking
		val gestureDetector = GestureDetector(
			targetView.context,
			object : GestureDetector.SimpleOnGestureListener() {
				override fun onDown(e: MotionEvent): Boolean {
					isScrolling = false; isLongPressActive = false
					longPressHandler.postDelayed({
						if (!isScrolling && !isSeeking) {
							wasPlayerPlaying = exoMediaPlayer.isPlaying
							if (wasPlayerPlaying) {
								exoMediaPlayer.pause(); isLongPressActive = true
							}
						}
					}, longPressTimeout); return true
				}
				
				override fun onScroll(
					e1: MotionEvent?, e2: MotionEvent,
					distanceX: Float, distanceY: Float,
				): Boolean {
					longPressHandler.removeCallbacksAndMessages(null)
					if (!isScrolling) {
						isScrolling = true; wasPlayerPlaying = exoMediaPlayer.isPlaying
						if (wasPlayerPlaying) exoMediaPlayer.pause()
						isSeeking = true; initialSeekPosition = getCurrentPosition()
					}
					
					val duration = getDuration()
					if (isSeeking && e1 != null && duration > 0) {
						val deltaX = e2.x - e1.x
						val threshold = 120
						
						if (abs(deltaX) > threshold && abs(distanceX) > abs(distanceY)) {
							val seekOffset = ((deltaX / targetView.width) * 25000).toLong()
							val newSeekPosition = (initialSeekPosition + seekOffset).coerceIn(0, getDuration())
							videoProgressBar.setPosition(newSeekPosition)
							exoMediaPlayer.seekTo(newSeekPosition)
							showQuickPlayerInfo(formatTimeDuration(newSeekPosition))
							return true
						}
					}; return false
				}
				
				override fun onSingleTapUp(e: MotionEvent): Boolean {
					toggleVisibilityOfPlaybackController(shouldTogglePlayback = false); return false
				}
				
				override fun onDoubleTap(e: MotionEvent): Boolean {
					toggleVideoPlayback(); return true
				}
			})
		
		// Set touch listener to handle gestures
		targetView.setOnTouchListener { touchedView, event ->
			gestureDetector.onTouchEvent(event)
			when (event.action) {
				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					longPressHandler.removeCallbacksAndMessages(null)
					if (isSeeking) {
						isSeeking = false; if (wasPlayerPlaying) exoMediaPlayer.play()
					} else if (isLongPressActive) {
						isLongPressActive = false; exoMediaPlayer.play()
					}
					touchedView.performClick()
				}
			}; true
		}
	}
	
	/**
	 * Formats time duration for display.
	 * @param milliseconds Duration in milliseconds
	 * @return Formatted time string (MM:SS)
	 */
	fun formatTimeDuration(milliseconds: Long): String {
		val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
		val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
		return String.format(Locale.US, "%02d:%02d", minutes, seconds)
	}
	
	/**
	 * Handles playback completion.
	 */
	private fun onPlaybackCompleted() {
		if (isPlayingStreamingVideo()) return
		
		val currentItem = exoMediaPlayer.currentMediaItem
		val currentMediaUri = currentItem?.localConfiguration?.uri.toString()
		if (currentMediaUri.isEmpty()) return
		
		val mediaFiles = getAllMediaRelatedDownloadDataModels()
		val matchedIndex = getFirstMatchingModelIndex(currentMediaUri, mediaFiles)
		if (matchedIndex == -1) return
		trackPlaybackPosition = 0; playVideoFromDownloadModel(mediaFiles[matchedIndex])
	}
	
	/**
	 * Starts playback based on intent data.
	 */
	private fun playVideoFromIntent() {
		getDownloadModelFromIntent()?.let { playVideoFromDownloadModel(it) } ?: run {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				intent.getParcelableExtra(STREAM_MEDIA_URL, Uri::class.java)?.let { streamVideoWithUri(it); return }
			} else intent.getStringExtra(STREAM_MEDIA_URL)?.let { streamVideoWithURL(it) }
		}
	}
	
	/**
	 * Plays streaming video from URI.
	 * @param fileUri The video URI
	 */
	private fun streamVideoWithUri(fileUri: Uri) {
		val incomingVideoTitle = intent.getStringExtra(STREAM_MEDIA_TITLE)
		if (!incomingVideoTitle.isNullOrEmpty()) updateMediaTitleWith(videoName = incomingVideoTitle)
		else getFileFromUri(fileUri)?.let { updateMediaTitleWith(videoName = it.name) }
		
		val mediaItem = MediaItem.fromUri(fileUri)
		exoMediaPlayer.setMediaItem(mediaItem)
		exoMediaPlayer.prepare()
		resumePlayer()
		
		val incomingIntent = getDownloadModelFromIntent()
		val audioMediaFie = incomingIntent?.getDestinationDocumentFile()
		ifAudioShowAlbumArt(audioMediaFie)
	}
	
	/**
	 * Gets download model from intent.
	 * @return DownloadDataModel or null if not found
	 */
	private fun getDownloadModelFromIntent(): DownloadDataModel? {
		intent?.let { intent ->
			val downloadModelID = intent.getIntExtra(DOWNLOAD_MODEL_ID_KEY, -1)
			if (downloadModelID > -1) downloadSystem.finishedDownloadDataModels
				.find { it.id == downloadModelID }?.let { return it }
		}; return null
	}
	
	/**
	 * Plays video from download model.
	 * @param downloadModel The model containing video info
	 */
	private fun playVideoFromDownloadModel(downloadModel: DownloadDataModel) {
		val mediaFile = fromFile(File("${downloadModel.fileDirectory}/${downloadModel.fileName}"))
		if (mediaFile.exists()) playVideoFromFile(mediaFile = mediaFile, subtitleFile = null)
		else showQuickPlayerInfo(msgText = getString(string.text_media_file_is_not_existed))
	}
	
	/**
	 * Plays video from file with optional subtitle.
	 * @param mediaFile The media file to play
	 * @param subtitleFile Optional subtitle file
	 */
	private fun playVideoFromFile(mediaFile: DocumentFile, subtitleFile: DocumentFile?) {
		mediaFile.canWrite().let {
			stopAndReleasePlayer()
			initVideoPlayer()
			
			subtitleFile?.let {
				val combineMediaFileWithSubtitle = combineMediaFileWithSubtitle(mediaFile, subtitleFile)
				exoMediaPlayer.setMediaItem(combineMediaFileWithSubtitle).apply { prepareMediaAndPlay(mediaFile) }
			} ?: run {
				val generateMediaItemWithURI = generateMediaItemWithURI(mediaFile.uri)
				exoMediaPlayer.setMediaItem(generateMediaItemWithURI).apply { prepareMediaAndPlay(mediaFile) }
			}
		}
	}
	
	/**
	 * Plays streaming video from URL.
	 * @param fileUrl The video URL
	 */
	private fun streamVideoWithURL(fileUrl: String) {
		safeSelfReference?.let { safeActivityRef ->
			if (!isValidURL(fileUrl)) {
				getMessageDialog(
					baseActivityInf = safeActivityRef, isCancelable = false,
					titleText = getString(string.title_invalid_streaming_link),
					isNegativeButtonVisible = false, isTitleVisible = true,
					titleTextViewCustomize = { it.setTextColor(resources.getColor(color.color_error, theme)) },
					messageTextViewCustomize = { it.text = getString(string.text_streaming_link_invalid) },
					positiveButtonText = getString(string.title_exit_the_player),
					positiveButtonTextCustomize = { it.setLeftSideDrawable(drawable.ic_button_exit) },
					dialogBuilderCustomize = { dialogBuilder ->
						dialogBuilder.setOnClickForPositiveButton {
							dialogBuilder.close(); closeActivityWithFadeAnimation()
						}
					}
				)?.show(); return
			}
			
			val incomingVideoTitle = intent.getStringExtra(STREAM_MEDIA_TITLE)
			if (!incomingVideoTitle.isNullOrEmpty()) {
				updateMediaTitleWith(videoName = incomingVideoTitle)
			} else updateTitleFromUrl(fileUrl)
			
			val mediaItem = MediaItem.fromUri(fileUrl)
			exoMediaPlayer.setMediaItem(mediaItem)
			exoMediaPlayer.prepare()
			resumePlayer()
			
			val incomingIntent = getDownloadModelFromIntent()
			val audioMediaFie = incomingIntent?.getDestinationDocumentFile()
			ifAudioShowAlbumArt(audioMediaFie)
		}
	}
	
	/**
	 * Updates title from URL metadata.
	 * @param fileUrl The media URL
	 */
	private fun updateTitleFromUrl(fileUrl: String) {
		ThreadsUtility.executeInBackground(codeBlock = {
			val modelId = intent.getIntExtra(DOWNLOAD_MODEL_ID_REFERENCE, -1)
			downloadSystem.activeDownloadDataModels.firstOrNull { it.id == modelId }?.let {
				executeOnMainThread { textCurrentVideoName.text = it.fileName }
			} ?: run {
				getFileInfoFromSever(URL(fileUrl)).let { fileInfo ->
					if (fileInfo.fileName.isNotEmpty()) executeOnMainThread {
						textCurrentVideoName.text = fileInfo.fileName
					}
				}
			}
		})
	}
	
	/**
	 * Generates media item from URI.
	 * @param mediaUri The media URI
	 * @return Configured MediaItem
	 */
	private fun generateMediaItemWithURI(mediaUri: Uri): MediaItem = Builder().setUri(mediaUri).build()
	
	/**
	 * Prepares media and starts playback.
	 * @param mediaFile The media file
	 */
	private fun prepareMediaAndPlay(mediaFile: DocumentFile) {
		exoMediaPlayer.prepare(); resumePlayer(); ifAudioShowAlbumArt(mediaFile)
	}
	
	/**
	 * Combines media file with subtitle.
	 * @param mediaFile The media file
	 * @param subtitleFile The subtitle file
	 * @return Combined MediaItem
	 */
	private fun combineMediaFileWithSubtitle(mediaFile: DocumentFile, subtitleFile: DocumentFile): MediaItem {
		val listOfSubtitles = listOf(
			SubtitleConfiguration.Builder(subtitleFile.uri)
				.setMimeType(MimeTypes.APPLICATION_SUBRIP)
				.setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
		); return Builder().setUri(mediaFile.uri).setSubtitleConfigurations(listOfSubtitles).build()
	}
	
	/**
	 * Checks if player has active subtitles.
	 * @param player The ExoPlayer instance
	 * @return True if subtitles are active
	 */
	private fun hasSubtitles(player: ExoPlayer): Boolean {
		val trackGroups = player.currentTracks.groups
		for (trackGroup in trackGroups)
			if (trackGroup.type == C.TRACK_TYPE_TEXT && trackGroup.isSelected)
				return true; return false
	}
	
	/**
	 * Shows album art for audio files.
	 * @param mediaFile The media file
	 */
	private fun ifAudioShowAlbumArt(mediaFile: DocumentFile?) {
		safeSelfReference?.let { safeActivityRef ->
			mediaFile?.let {
				val retriever = MediaMetadataRetriever()
				val file = mediaFile.getAbsolutePath(safeActivityRef)
				retriever.setDataSource(file)
				val width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)
				val height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)
				
				if (width == null && height == null) {
					showDefaultAudioAlbumArt(mediaFile); return
				}
				
				if (!isAudio(mediaFile)) {
					audioAlbumArtHolder.visibility = GONE; return
				}; showDefaultAudioAlbumArt(mediaFile)
			}
		}
	}
	
	/**
	 * Shows default album art for audio files.
	 * @param mediaFile The audio file
	 */
	private fun showDefaultAudioAlbumArt(mediaFile: DocumentFile) {
		safeSelfReference?.let { safeActivityRef ->
			audioAlbumArtHolder.visibility = VISIBLE
			setAlbumArt(mediaFile.getAbsolutePath(safeActivityRef), audioAlbumArtHolder)
		}
	}
	
	/**
	 * Sets album art for audio file.
	 * @param audioFilePath Path to audio file
	 * @param imageView ImageView to display album art
	 */
	private fun setAlbumArt(audioFilePath: String?, imageView: ImageView) {
		val retriever = MediaMetadataRetriever()
		try {
			retriever.setDataSource(audioFilePath)
			val art = retriever.embeddedPicture
			if (art != null) {
				val albumArt = decodeByteArray(art, 0, art.size)
				imageView.setImageBitmap(albumArt)
			} else imageView.setImageResource(drawable.image_audio_thumb)
		} catch (error: Exception) {
			LogHelperUtils.from(javaClass).e(error)
			imageView.setImageResource(drawable.image_audio_thumb)
		} finally {
			retriever.release()
		}
	}
	
	/**
	 * Stops and releases player resources.
	 */
	private fun stopAndReleasePlayer() {
		if (!isFinishing && !isDestroyed) {
			exoMediaPlayer.stop(); exoMediaPlayer.release()
		}
	}
	
	/**
	 * Pauses player playback.
	 */
	fun pausePlayer() {
		if (!isFinishing && !isDestroyed) {
			exoMediaPlayer.playWhenReady = false
			exoMediaPlayer.playbackState; exoMediaPlayer.pause()
		}
	}
	
	/**
	 * Resumes player playback.
	 */
	fun resumePlayer() {
		if (exoMediaPlayer.isPlaying) return
		
		exoMediaPlayer.playWhenReady = true
		exoMediaPlayer.playbackState; exoMediaPlayer.play()
		exoMediaPlayer.seekTo(trackPlaybackPosition)
	}
	
	/**
	 * Toggles between play and pause states.
	 */
	private fun toggleVideoPlayback() {
		val playbackEnded = exoMediaPlayer.contentPosition >= (exoMediaPlayer.duration - 500)
		if (playbackEnded) {
			exoMediaPlayer.seekTo(0); exoMediaPlayer.playWhenReady = true
			trackPlaybackPosition = 0
		} else {
			if (exoMediaPlayer.isPlaying) pausePlayer() else resumePlayer()
		}
	}
	
	/**
	 * Plays next media in playlist.
	 */
	private fun playNextMedia() {
		if (!::exoMediaPlayer.isInitialized) return
		
		if (isPlayingStreamingVideo()) {
			val infoText = getString(string.text_no_next_item_to_play)
			showQuickPlayerInfo(infoText); return
		}
		
		val currentMediaUri = exoMediaPlayer.currentMediaItem?.localConfiguration?.uri.toString()
		if (currentMediaUri.isEmpty()) return
		
		intent.getIntExtra(WHERE_DID_YOU_COME_FROM, -2).let { result ->
			if (result == -2) return
			if (result == FROM_FINISHED_DOWNLOADS_LIST) {
				playNextFromFinishedDownloads(currentMediaUri); return
			}
			
			if (result == FROM_PRIVATE_FOLDER_LIST) {
				//todo: implement next playback from the private folder
			}
		}
	}
	
	/**
	 * Plays next media from finished downloads.
	 * @param currentMediaUri URI of current media
	 */
	private fun playNextFromFinishedDownloads(currentMediaUri: String) {
		val mediaFiles = getAllMediaRelatedDownloadDataModels()
		val matchedIndex = getFirstMatchingModelIndex(currentMediaUri, mediaFiles)
		if (matchedIndex != -1) {
			val nextIndex = matchedIndex + 1
			if (mediaFiles.size > nextIndex) {
				val nextDownloadDataModel = mediaFiles[nextIndex]
				playVideoFromDownloadModel(nextDownloadDataModel)
			} else {
				val infoText = getString(string.text_no_next_item_to_play)
				showQuickPlayerInfo(infoText)
			}
		}
	}
	
	/**
	 * Plays previous media in playlist.
	 */
	private fun playPreviousMedia() {
		if (!::exoMediaPlayer.isInitialized) return
		
		if (isPlayingStreamingVideo()) {
			val infoText = getString(string.text_no_previous_item_to_play)
			showQuickPlayerInfo(infoText); return
		}
		
		val currentItem = exoMediaPlayer.currentMediaItem
		val currentMediaUri = currentItem?.localConfiguration?.uri.toString()
		if (currentMediaUri.isEmpty()) return
		
		intent.getIntExtra(WHERE_DID_YOU_COME_FROM, -2).let { result ->
			if (result == -2) return
			if (result == FROM_FINISHED_DOWNLOADS_LIST) {
				playPreviousFromFinishedDownloads(currentMediaUri)
				return
			}
			
			@Suppress("ControlFlowWithEmptyBody")
			if (result == FROM_PRIVATE_FOLDER_LIST) {
				//Todo: implement previous playback from the private folder
			}
		}
	}
	
	/**
	 * Plays previous media from finished downloads.
	 * @param currentMediaUri URI of current media
	 */
	private fun playPreviousFromFinishedDownloads(currentMediaUri: String) {
		val mediaFiles = getAllMediaRelatedDownloadDataModels()
		val matchedIndex = getFirstMatchingModelIndex(currentMediaUri, mediaFiles)
		if (matchedIndex != -1) {
			val previousIndex = matchedIndex - 1
			if (previousIndex > -1) {
				val nextDownloadDataModel = mediaFiles[previousIndex]
				playVideoFromDownloadModel(nextDownloadDataModel)
			} else {
				val infoText = getString(string.text_no_previous_item_to_play)
				showQuickPlayerInfo(infoText)
			}
		}
	}
	
	/**
	 * Gets index of first matching model for media URI.
	 * @param mediaUri The media URI to match
	 * @param models List of DownloadDataModels to search
	 * @return Index of matching model or -1 if not found
	 */
	private fun getFirstMatchingModelIndex(mediaUri: String, models: List<DownloadDataModel>): Int {
		val currentMediaFilePath = mediaUri.toUri().path
		val matchedIndex = models.indexOfFirst { model ->
			val modelFilePath = Uri.fromFile(model.getDestinationFile()).path
			modelFilePath == currentMediaFilePath
		}; return matchedIndex
	}
	
	/**
	 * Gets all media-related download models.
	 * @return List of DownloadDataModels for media files
	 */
	private fun getAllMediaRelatedDownloadDataModels(): List<DownloadDataModel> {
		return downloadSystem.finishedDownloadDataModels
			.filter { model ->
				val modelDestinationFile = model.getDestinationDocumentFile()
				modelDestinationFile.exists() && isMediaFile(modelDestinationFile)
			}.distinctBy { it.getDestinationDocumentFile().uri }
	}
	
	/**
	 * Checks if file is media (audio or video).
	 * @param modelDestinationFile The file to check
	 * @return True if file is audio or video
	 */
	private fun isMediaFile(modelDestinationFile: DocumentFile): Boolean {
		return (isAudio(modelDestinationFile) || isVideo(modelDestinationFile))
	}
	
	/**
	 * Updates video progress display.
	 */
	private fun updateVideoProgressContainer() {
		textProgressTimer.text = formatTimeDuration(exoMediaPlayer.currentPosition)
		val tmpTimer = formatTimeDuration(exoMediaPlayer.duration)
		if (!tmpTimer.startsWith("-")) textVideoDuration.text = tmpTimer
		else textVideoDuration.text = getString(string.text_00_00)
		
		val duration = exoMediaPlayer.duration
		val position = exoMediaPlayer.currentPosition
		val bufferedPosition = exoMediaPlayer.bufferedPosition
		trackPlaybackPosition = position
		
		videoProgressBar.setDuration(duration)
		videoProgressBar.setPosition(position)
		videoProgressBar.setBufferedPosition(bufferedPosition)
	}
	
	/**
	 * Initializes auto-rotate for media player.
	 * @param shouldToggleAutoRotate True to enable auto-rotate
	 */
	fun initializeAutoRotateMediaPlayer(shouldToggleAutoRotate: Boolean = true) {
		val orientation = if (shouldToggleAutoRotate)
			SCREEN_ORIENTATION_UNSPECIFIED else SCREEN_ORIENTATION_LOCKED
		requestedOrientation = orientation
	}
	
	/**
	 * Toggles playback controller visibility.
	 * @param shouldTogglePlayback True to toggle playback state
	 */
	private fun toggleVisibilityOfPlaybackController(shouldTogglePlayback: Boolean = true) {
		if (areVideoControllersLocked) {
			toggleViewVisibility(buttonControllerUnlock, true)
		} else {
			isDoubleClickedInvisibleArea++
			if (isDoubleClickedInvisibleArea > 1) {
				if (shouldTogglePlayback) toggleVideoPlayback()
				isDoubleClickedInvisibleArea = 0
			} else toggleViewVisibility(entirePlaybackController, true)
			
			delay(timeInMile = 300, listener = object : OnTaskFinishListener {
				override fun afterDelay() {
					isDoubleClickedInvisibleArea = 0
				}
			})
		}
	}
	
	/**
	 * Shows playback controllers.
	 */
	private fun visibleEntirePlaybackControllers() {
		showView(shouldAnimate = true, targetView = entirePlaybackController)
	}
	
	/**
	 * Hides playback controllers.
	 */
	private fun hideEntirePlaybackControllers() {
		hideView(shouldAnimate = true, targetView = entirePlaybackController)
	}
	
	/**
	 * Updates play/pause button icon.
	 * @param isPlaying True if player is currently playing
	 */
	private fun updateIconOfVideoPlayPauseButton(isPlaying: Boolean) {
		val iconResId = if (isPlaying) drawable.ic_button_media_pause
		else drawable.ic_button_media_play
		val buttonViewId = id.btn_img_video_play_pause_toggle
		(findViewById<ImageView>(buttonViewId)).setImageResource(iconResId)
	}
	
	/**
	 * Locks playback controllers.
	 */
	private fun lockEntirePlaybackControllers() {
		requestedOrientation = SCREEN_ORIENTATION_LOCKED
		hideEntirePlaybackControllers()
		showView(targetView = buttonControllerUnlock, shouldAnimate = true)
		areVideoControllersLocked = true
		
		delay(timeInMile = 1500, listener = object : OnTaskFinishListener {
			override fun afterDelay() {
				hideView(targetView = buttonControllerUnlock, shouldAnimate = true)
			}
		})
	}
	
	/**
	 * Unlocks playback controllers.
	 */
	private fun unlockEntirePlaybackControllers() {
		requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
		hideView(targetView = buttonControllerUnlock, shouldAnimate = true)
		visibleEntirePlaybackControllers()
		areVideoControllersLocked = false
	}
	
	/**
	 * Shares current media file.
	 */
	fun shareMediaFile() {
		safeSelfReference?.let { safeActivityRef ->
			if (isPlayingStreamingVideo()) {
				showMessageDialog(
					baseActivityInf = safeActivityRef,
					isTitleVisible = true,
					isNegativeButtonVisible = false,
					titleTextViewCustomize = { titleView ->
						titleView.setText(string.text_unavailable_for_streaming)
						titleView.setTextColorKT(color.color_error)
					}, positiveButtonTextCustomize = { positiveButton ->
						positiveButton.setLeftSideDrawable(drawable.ic_button_checked_circle)
						positiveButton.setText(string.title_okay)
					}, messageTextViewCustomize = { it.setText(string.text_share_stream_media_unavailable) }
				); return
			} else {
				val currentItem = exoMediaPlayer.currentMediaItem
				val currentMediaUri = currentItem?.localConfiguration?.uri.toString()
				if (currentMediaUri.isEmpty()) {
					invalidMediaFileToast(); return
				}
				
				downloadSystem.finishedDownloadDataModels.find {
					it.getDestinationFile().path == currentMediaUri.toUri().path
				}?.let { downloadDataModel ->
					shareMediaFile(safeActivityRef, downloadDataModel.getDestinationFile())
					return
				}; invalidMediaFileToast()
			}
			
			invalidMediaFileToast()
		}
	}
	
	/**
	 * Shows toast for invalid media file.
	 */
	private fun invalidMediaFileToast() {
		doSomeVibration(timeInMillis = 50)
		showToast(msg = getString(string.text_invalid_media_file))
	}
	
	/**
	 * Opens and syncs subtitle file.
	 */
	fun openAndSyncSubtitle() {
		safeSelfReference?.let { safeActivityRef ->
			getMessageDialog(
				baseActivityInf = safeActivityRef,
				messageTxt = getString(string.text_select_subtitle_from_file_manager),
				positiveButtonText = getString(string.title_select_file)
			)?.let { dialogBuilder ->
				dialogBuilder.positiveButtonView.setOnClickListener {
					dialogBuilder.close(); addExternalSubtitleToPlayer()
				}.apply { dialogBuilder.show() }
			}
		}
	}
	
	/**
	 * Adds external subtitle to player.
	 */
	private fun addExternalSubtitleToPlayer() {
		try {
			pausePlayer()
			requestToPickSubtitleFile()
			validateSelectedSubtitleFileByUser()
		} catch (error: Exception) {
			error.printStackTrace()
			warnUserAboutSubtitleSelectionFailure()
		}
	}
	
	/**
	 * Warns user about subtitle selection failure.
	 */
	private fun warnUserAboutSubtitleSelectionFailure() {
		val msgText = getString(string.text_subtitle_file_not_readable)
		showMessageDialog(baseActivityInf = safeSelfReference, messageTxt = msgText)
	}
	
	/**
	 * Validates user-selected subtitle file.
	 */
	private fun validateSelectedSubtitleFileByUser() {
		scopedStorageHelper?.onFileSelected = { _, files ->
			if (!isFileReadable(files)) {
				val errorMsg = getString(string.text_file_not_readable)
				throw Exception(errorMsg)
			}
			
			val subtitleFile = files.first()
			val fileExtension = subtitleFile.let {
				subtitleFile.name?.let { fileName -> getFileExtension(fileName) ?: "" }
			}
			
			if (fileExtension.isNullOrEmpty()) throw Exception(getString(string.text_file_not_readable))
			if (!isSubtitleFileExtension(fileExtension)) showUnsupportedSubtitleFileMessage() else {
				getDownloadModelFromIntent()?.let { downloadDataModel ->
					val videoFile = downloadDataModel.getDestinationDocumentFile()
					playVideoWithSubtitle(videoFile, subtitleFile)
				}
			}
		}
	}
	
	/**
	 * Plays video with subtitle.
	 * @param videoFile The video file
	 * @param subtitleFile The subtitle file
	 */
	private fun playVideoWithSubtitle(videoFile: DocumentFile, subtitleFile: DocumentFile) {
		playVideoFromFile(videoFile, subtitleFile); resumePlayer()
	}
	
	/**
	 * Shows message about unsupported subtitle file.
	 */
	private fun showUnsupportedSubtitleFileMessage() {
		val msgText = getString(string.text_subtitle_file_not_supported)
		showMessageDialog(baseActivityInf = safeSelfReference, messageTxt = msgText)
	}
	
	/**
	 * Checks if file extension is for subtitle.
	 * @param fileExt The file extension
	 * @return True if extension is for subtitle file
	 */
	private fun isSubtitleFileExtension(fileExt: String): Boolean {
		var isValid = false
		if (fileExt.isNotEmpty() || fileExt.isBlank()) {
			if (fileExt.lowercase(Locale.getDefault()) == "srt" ||
				fileExt.lowercase(Locale.getDefault()) == "vtt"
			) isValid = true
		}; return isValid
	}
	
	/**
	 * Checks if file is readable.
	 * @param files List of files to check
	 * @return True if first file is readable
	 */
	private fun isFileReadable(files: List<DocumentFile>): Boolean {
		return files.isNotEmpty() && files.first().canRead()
	}
	
	/**
	 * Requests user to pick subtitle file.
	 */
	private fun requestToPickSubtitleFile() {
		safeSelfReference?.let { safeActivityRef ->
			val pubicDownloadFolder = INSTANCE.getPublicDownloadDir()?.getAbsolutePath(INSTANCE)
			val defaultAIOFolder = getText(string.text_default_aio_download_folder_path)
			val pathToPick = pubicDownloadFolder ?: defaultAIOFolder
			val initialPath = FileFullPath(context = safeActivityRef, fullPath = pathToPick.toString())
			scopedStorageHelper?.openFilePicker(allowMultiple = false, initialPath = initialPath)
		}
	}
	
	/**
	 * Rewinds video player by 10 seconds.
	 */
	private fun rewindVideoPlayer() {
		if (exoMediaPlayer.isPlaying) {
			val currentPosition = exoMediaPlayer.currentPosition
			val newPosition = (currentPosition - 10000).coerceIn(0, exoMediaPlayer.duration)
			exoMediaPlayer.seekTo(newPosition)
		}
	}
	
	/**
	 * Forwards video player by 10 seconds.
	 */
	private fun forwardVideoPlayer() {
		if (exoMediaPlayer.isPlaying) {
			val currentPosition = exoMediaPlayer.currentPosition
			val newPosition = (currentPosition + 10000).coerceAtMost(exoMediaPlayer.duration)
			exoMediaPlayer.seekTo(newPosition)
		}
	}
	
	/**
	 * Shows options menu popup.
	 */
	private fun showOptionMenuPopup() {
		if (!::mediaOptionsPopup.isInitialized)
			mediaOptionsPopup = MediaOptionsPopup(safeSelfReference); mediaOptionsPopup.show()
	}
	
	/**
	 * Toggles fullscreen mode.
	 */
	@Suppress("DEPRECATION")
	private fun toggleFullscreen() {
		safeSelfReference?.let { safeActivityRef ->
			var newUiOptions = safeActivityRef.window.decorView.systemUiVisibility
			newUiOptions = newUiOptions xor SYSTEM_UI_FLAG_HIDE_NAVIGATION
			newUiOptions = newUiOptions xor SYSTEM_UI_FLAG_FULLSCREEN
			newUiOptions = newUiOptions xor SYSTEM_UI_FLAG_IMMERSIVE
			safeActivityRef.window.decorView.systemUiVisibility = newUiOptions
		}
	}
	
	/**
	 * Deletes current media file.
	 */
	fun deleteMediaFile() {
		if (!::exoMediaPlayer.isInitialized) return
		
		val currentMediaUri = exoMediaPlayer.currentMediaItem?.localConfiguration?.uri.toString()
		if (currentMediaUri.isEmpty()) return
		
		val audioAndVideoFiles = getAllMediaRelatedDownloadDataModels()
		val matchedIndex = getFirstMatchingModelIndex(currentMediaUri, audioAndVideoFiles)
		
		ThreadsUtility.executeInBackground(codeBlock = {
			playNextOrPreviousFromFinishedDownloads(matchedIndex, audioAndVideoFiles)
			deleteActualMediaFileAndClearDatabase(audioAndVideoFiles, matchedIndex)
		})
	}
	
	/**
	 * Deletes media file and clears database entry.
	 * @param audioAndVideoFiles List of media files
	 * @param matchedIndex Index of file to delete
	 */
	private fun deleteActualMediaFileAndClearDatabase(
		audioAndVideoFiles: List<DownloadDataModel>, matchedIndex: Int,
	) {
		audioAndVideoFiles[matchedIndex].getDestinationFile().delete()
		val deletedDownloadDataModel = audioAndVideoFiles[matchedIndex]
		downloadSystem.finishedDownloadDataModels.remove(deletedDownloadDataModel)
		downloadSystem.sortFinishedDownloadDataModels()
		deletedDownloadDataModel.deleteModelFromDisk()
		executeOnMainThread { showToast(getString(string.text_media_file_has_deleted)) }
	}
	
	/**
	 * Plays next or previous media after deletion.
	 * @param matchedIndex Index of deleted file
	 * @param audioAndVideoFiles List of media files
	 */
	private fun playNextOrPreviousFromFinishedDownloads(
		matchedIndex: Int, audioAndVideoFiles: List<DownloadDataModel>,
	) {
		executeOnMainThread {
			if (matchedIndex != -1) {
				if (audioAndVideoFiles.size > (matchedIndex + 1)) {
					playVideoFromDownloadModel(audioAndVideoFiles[(matchedIndex + 1)])
				} else {
					if ((matchedIndex - 1) > -1)
						playVideoFromDownloadModel(audioAndVideoFiles[(matchedIndex - 1)]) else {
						stopAndReleasePlayer(); closeActivityWithSwipeAnimation()
					}
				}
			}
		}
	}
	
	/**
	 * Gets current playing download model.
	 * @return Current DownloadDataModel or null
	 */
	fun getCurrentPlayingDownloadModel(): DownloadDataModel? {
		safeSelfReference?.let { _ ->
			try {
				if (!::exoMediaPlayer.isInitialized) return null
				
				val currentMediaUri = exoMediaPlayer.currentMediaItem?.localConfiguration?.uri.toString()
				if (currentMediaUri.isEmpty()) return null
				
				val downloadDataModelList = getAllMediaRelatedDownloadDataModels()
				val matchedIndex = getFirstMatchingModelIndex(currentMediaUri, downloadDataModelList)
				val currentPlayingDownloadModel = downloadDataModelList[matchedIndex]
				return currentPlayingDownloadModel
			} catch (error: Exception) {
				error.printStackTrace()
				return null
			}
		} ?: run { return null }
	}
	
	/**
	 * Opens current media file in external app.
	 */
	fun openMediaFile() {
		safeSelfReference?.let { safeActivityRef ->
			if (isPlayingStreamingVideo()) {
				val msgText = getString(string.text_open_stream_media_unavailable)
				showMessageDialog(
					baseActivityInf = safeActivityRef,
					messageTxt = msgText, isNegativeButtonVisible = false
				); return
			}
			
			if (!::exoMediaPlayer.isInitialized) return
			
			val currentMediaUri = exoMediaPlayer.currentMediaItem?.localConfiguration?.uri.toString()
			if (currentMediaUri.isEmpty()) return
			
			val mediaFiles = getAllMediaRelatedDownloadDataModels()
			val matchedIndex = getFirstMatchingModelIndex(currentMediaUri, mediaFiles)
			val mediaFile = mediaFiles[matchedIndex].getDestinationFile()
			ShareUtility.openFile(mediaFile, safeActivityRef)
		}
	}
	
	/**
	 * Opens media file information dialog.
	 */
	fun openMediaFileInfo() {
		safeSelfReference?.let { safeActivityRef ->
			if (isPlayingStreamingVideo()) {
				showMessageDialog(
					baseActivityInf = safeActivityRef,
					isTitleVisible = true,
					isNegativeButtonVisible = false,
					titleTextViewCustomize = { titleView ->
						titleView.setText(string.text_unavailable_for_streaming)
						titleView.setTextColorKT(color.color_error)
					}, positiveButtonTextCustomize = { positiveButton ->
						positiveButton.setLeftSideDrawable(drawable.ic_button_checked_circle)
						positiveButton.setText(string.title_okay)
					}, messageTextViewCustomize = { it.setText(string.text_video_stream_info_unavailable) }
				); return
			}
			
			if (!::exoMediaPlayer.isInitialized) return
			
			val currentItem = exoMediaPlayer.currentMediaItem
			val currentMediaUri = currentItem?.localConfiguration?.uri.toString()
			if (currentMediaUri.isEmpty()) return
			
			val mediaFiles = getAllMediaRelatedDownloadDataModels()
			val matchedIndex = getFirstMatchingModelIndex(currentMediaUri, mediaFiles)
			val downloadModel = mediaFiles[matchedIndex]
			
			getMessageDialog(
				baseActivityInf = safeActivityRef,
				isCancelable = true,
				isTitleVisible = true,
				titleText = getText(string.title_media_file_info),
				messageTextViewCustomize = {
					it.gravity = Gravity.START
					it.linksClickable = true
					val htmlString = buildMediaInfoHtmlString(downloadModel)
					it.text = fromHtmlStringToSpanned(htmlString)
					it.movementMethod = getInstance()
				},
				positiveButtonText = getString(string.title_okay),
				isNegativeButtonVisible = false,
				positiveButtonTextCustomize = { positiveButton: TextView ->
					val drawable = ContextCompat.getDrawable(applicationContext, drawable.ic_button_checked_circle)
					drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
					positiveButton.setCompoundDrawables(drawable, null, null, null)
				}
			)?.show()
		}
	}
	
	/**
	 * Abstract class for video scrubber listeners.
	 */
	abstract class VideoScrubberListener : TimeBar.OnScrubListener {
		override fun onScrubMove(timeBar: TimeBar, position: Long) {
			//Override this function to implement the function.
		}
	}
}