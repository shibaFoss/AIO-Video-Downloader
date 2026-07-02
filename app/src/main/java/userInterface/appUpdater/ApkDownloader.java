package userInterface.appUpdater;

import androidx.annotation.NonNull;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import coreUtils.library.networks.URLUtility;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.storage.FileStorageUtility;
import coreUtils.library.strings.StringHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;

/**
 * Handles APK file downloading from a remote server with resume capability and
 * integrity verification. This class manages the complete download lifecycle for
 * application update APK files, including network requests, file I/O, SHA-256
 * hash calculation, and automatic resume support using OkHttp.
 *
 * <p><strong>Core features:</strong>
 * <ul>
 * <li>Resumable downloads – Automatically resumes partial downloads using HTTP Range.</li>
 * <li>Integrity verification – Computes SHA-256 hash for security validation.</li>
 * <li>Progress tracking – Real-time percentage and byte progress callbacks.</li>
 * <li>Error handling – Graceful fallback to full download when resume not supported.</li>
 * <li>File validation – Compares local size with server size to determine state.</li>
 * </ul>
 *
 * <p>Usage: Create instance, call {@link #startDownload(UpdateInfo, File, ProgressListener)},
 * and handle callbacks via {@link ProgressListener}.
 *
 * @see UpdateInfo
 * @see ProgressListener
 * @see OkHttpClient
 */
public class ApkDownloader {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final OkHttpClient client;
	private Call currentCall;
	
	/**
	 * Constructs a new ApkDownloader instance with a default OkHttpClient.
	 * The client is configured with default settings including connection
	 * timeouts, read/write timeouts, and cache settings provided by OkHttp's
	 * default constructor.
	 *
	 * <p>The client is used for both HEAD requests (to retrieve file metadata
	 * and check server resume support) and GET requests for downloading the
	 * actual APK file content with optional resume capabilities.
	 *
	 * <p>Custom timeouts or interceptors are not configured by default; override
	 * the client creation if advanced configuration is needed.
	 *
	 * @see OkHttpClient
	 */
	public ApkDownloader() {
		this.client = new OkHttpClient();
	}
	
	/**
	 * Starts a new download or resumes an existing partial download for an APK file.
	 * This method is the public entry point for downloading application updates.
	 * It validates the download directory, constructs the output file name, performs
	 * a HEAD request to query server file size, and then initiates or resumes the
	 * download based on local file state.
	 *
	 * <p><strong>Validation steps:</strong>
	 * <ul>
	 * <li>Ensures the download directory exists; creates it if necessary.</li>
	 * <li>Verifies that the output file is writable if it already exists.</li>
	 * <li>Sends a HEAD request to retrieve server file metadata.</li>
	 * <li>On HEAD failure, falls back to full download via
	 *     {@link #handleHeadRequestFailure(IOException, File, UpdateInfo, ProgressListener)}.</li>
	 * <li>On HEAD success, calls
	 * {@link #startOrResumeDownload(Response, File, UpdateInfo, ProgressListener)}
	 *     to determine download strategy.</li>
	 * </ul>
	 *
	 * <p>The output file is named using the pattern:
	 * "{app_name}_{versionCode}_{versionName}.apk"
	 *
	 * @param updateInfo  The update information containing the APK download URL
	 *                    and version details. Must not be null.
	 * @param downloadDir The directory where the APK file will be saved.
	 *                    Must not be null.
	 * @param listener    Progress listener for download callbacks. Must not be null.
	 */
	public void startDownload(@NotNull UpdateInfo updateInfo,
	                          @NotNull File downloadDir,
	                          @NotNull ProgressListener listener) {
		
		if (!downloadDir.exists() && !downloadDir.mkdirs()) {
			String errorMsg = "Failed to create download directory.";
			logger.error(errorMsg);
			listener.onError(errorMsg);
			return;
		}
		
		String fileName = StringHelper.getText(R.string.label_app_name_full) +
			"_" + updateInfo.getVersionCode() + "_" + updateInfo.getVersionName() + ".apk";
		
		File outputFile = new File(downloadDir, fileName);
		
		if (outputFile.exists() && !outputFile.canWrite()) {
			String errorMsg = "Cannot write to existing APK file. " +
				"The file may be protected or storage permission is denied.";
			logger.error(errorMsg);
			listener.onError(errorMsg);
			return;
		}
		
		Request headRequest = new Request.Builder()
			.url(updateInfo.getApkFileUrl())
			.head()
			.build();
		
		currentCall = client.newCall(headRequest);
		currentCall.enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException error) {
				if (call.isCanceled()) {
					logger.debug("HEAD request canceled.");
					return;
				}
				
				handleHeadRequestFailure(error, outputFile, updateInfo, listener);
			}
			
			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) {
				try {
					startOrResumeDownload(response, outputFile, updateInfo, listener);
					
				} catch (MalformedURLException error) {
					logger.error("Url malfunction is found: ", error);
				} catch (Exception error) {
					logger.error("Something went wrong: ", error);
				}
			}
		});
	}
	
	/**
	 * Initiates or resumes a download based on the state of the local file and
	 * the file size reported by the server via a HEAD request. This method compares
	 * the local file size (if it exists) with the total server file size to determine
	 * whether to start fresh, resume a partial download, or skip downloading entirely.
	 *
	 * <p><strong>Decision logic:</strong>
	 * <ul>
	 * <li>If server file size ≤ 0 → Delete local file (if exists) and start fresh.</li>
	 * <li>If local file does not exist → Start fresh download.</li>
	 * <li>If local size == server size → File already fully downloaded; trigger completion.</li>
	 * <li>If local size < server size → Resume download from local size.</li>
	 * <li>If local size > server size → Delete local file (corrupt/oversized) and restart.</li>
	 * </ul>
	 *
	 * @param response   The HEAD response containing server metadata (closed after use).
	 * @param outputFile The local output file (may or may not exist).
	 * @param updateInfo The update information containing the download URL.
	 * @param listener   Progress listener for callbacks.
	 * @throws MalformedURLException If the download URL is malformed.
	 */
	private void startOrResumeDownload(@NonNull Response response,
	                                   @NotNull File outputFile,
	                                   @NonNull UpdateInfo updateInfo,
	                                   @NonNull ProgressListener listener) throws MalformedURLException {
		String apkFileUrl = updateInfo.getApkFileUrl();
		long totalServerSize = URLUtility.getFileSizeFromUrl(new URL(apkFileUrl));
		logger.debug("Received file size form server: " +
			FileStorageUtility.humanReadableSizeOf(totalServerSize));
		
		response.close();
		
		if (totalServerSize <= 0) {
			logger.debug("File size is received from server: 0Byte");
			if (outputFile.exists()) outputFile.delete();
			executeDownloadRequest(apkFileUrl, outputFile, listener, 0);
			return;
		}
		
		if (outputFile.exists()) {
			long localSize = outputFile.length();
			logger.debug("Local file is existed, file size:" +
				FileStorageUtility.humanReadableSizeOf(localSize));
			
			if (localSize == totalServerSize) {
				logger.debug("Local file and server file size matched, " +
					"meaning no need to download");
				handleAlreadyDownloaded(outputFile, listener);
				
			} else if (localSize < totalServerSize) {
				logger.debug("Local file has not fully downloaded, " +
					"need to resume download");
				executeResumedDownload(outputFile, updateInfo,
					listener, localSize);
				
			} else {
				logger.debug("Starting over the download");
				reinitializeDownload(outputFile, updateInfo, listener);
			}
		} else {
			logger.debug("Local file is not found, redownload the file");
			executeDownloadRequest(apkFileUrl, outputFile, listener, 0);
		}
	}
	
	/**
	 * Restarts a download from scratch by deleting the existing local file and
	 * initiating a fresh download. This method is called when the local file size
	 * is larger than the server file size, indicating corruption or an outdated
	 * partial download.
	 *
	 * <p>The existing file is deleted to prevent hash verification failures and
	 * ensure a clean download state.
	 *
	 * @param outputFile The output file to delete and re-download.
	 * @param updateInfo The update information containing the download URL.
	 * @param listener   Progress listener for callbacks.
	 */
	private void reinitializeDownload(@NonNull File outputFile,
	                                  @NonNull UpdateInfo updateInfo,
	                                  @NonNull ProgressListener listener) {
		logger.warning("Local file is larger than server file. Restarting download.");
		outputFile.delete();
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, 0);
	}
	
	/**
	 * Executes a resumed download for an existing partial file. This method is
	 * called when a partial download is detected on disk. It logs the resumption
	 * point and delegates to {@link #executeDownloadRequest(String, File, ProgressListener, long)}
	 * with the local file size as the starting byte offset.
	 *
	 * <p>The server must support HTTP Range requests (returns 206 Partial Content)
	 * for resumption to succeed. If the server does not support resume, the download
	 * will restart from the beginning.
	 *
	 * @param outputFile The partial output file on disk.
	 * @param updateInfo The update information containing the download URL.
	 * @param listener   Progress listener for callbacks.
	 * @param localSize  Number of bytes already downloaded (file length).
	 */
	private void executeResumedDownload(@NonNull File outputFile,
	                                    @NonNull UpdateInfo updateInfo,
	                                    @NonNull ProgressListener listener,
	                                    long localSize) {
		logger.debug("Partial download found. Resuming from byte: " + localSize);
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, localSize);
	}
	
	/**
	 * Handles the case where a file is already fully downloaded. This method logs
	 * the completion and delegates to {@link #handleFullyDownloaded(File, ProgressListener)}
	 * to compute the file hash and notify the listener of successful completion.
	 *
	 * <p>This scenario typically occurs when the download was completed in a
	 * previous session and the file remains on disk.
	 *
	 * @param outputFile The fully downloaded file.
	 * @param listener   Progress listener for completion callback.
	 */
	private void handleAlreadyDownloaded(@NonNull File outputFile,
	                                     @NonNull ProgressListener listener) {
		logger.debug("File already fully downloaded. Triggering completion.");
		handleFullyDownloaded(outputFile, listener);
	}
	
	/**
	 * Handles a HEAD request failure by falling back to a full download. This method
	 * logs the error, deletes any existing partial file to avoid corruption, and
	 * starts a fresh download from byte 0.
	 *
	 * <p>This fallback is necessary when the server does not support HEAD requests
	 * or when network conditions prevent retrieving content length information.
	 *
	 * @param error      The exception that caused the HEAD request to fail.
	 * @param outputFile The output file (may contain partial data).
	 * @param updateInfo The update information containing the download URL.
	 * @param listener   Progress listener for error reporting.
	 */
	private void handleHeadRequestFailure(@NonNull IOException error,
	                                      @NotNull File outputFile,
	                                      @NonNull UpdateInfo updateInfo,
	                                      @NonNull ProgressListener listener) {
		logger.error("HEAD request failed, falling back to full download: ", error);
		if (outputFile.exists()) outputFile.delete();
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, 0);
	}
	
	/**
	 * Executes an asynchronous HTTP download request with optional resume support.
	 * This method constructs a request with a Range header if resuming a partial
	 * download, enqueues the request via OkHttp, and processes the response
	 * asynchronously via the provided callback.
	 *
	 * <p><strong>Request flow:</strong>
	 * <ol>
	 * <li>Determines if resuming is requested based on {@code downloadedBytes > 0}.</li>
	 * <li>Adds a "Range: bytes={downloadedBytes}-" header for resume requests.</li>
	 * <li>Enqueues the request asynchronously via {@link Call#enqueue(Callback)}.</li>
	 * <li>On response, checks for HTTP errors and server resume support (HTTP 206).</li>
	 * <li>Delegates stream processing to {@link #handleDownloadResponse}.</li>
	 * </ol>
	 *
	 * <p>If the request is canceled, the failure callback returns without reporting
	 * an error. The current call reference is stored in {@code currentCall} to allow
	 * cancellation via {@link #stopDownload()}.
	 *
	 * @param url             The download URL. Must not be null.
	 * @param outputFile      The destination file for the download.
	 * @param listener        Progress listener for callbacks.
	 * @param downloadedBytes Number of bytes already downloaded (0 for fresh download).
	 */
	private void executeDownloadRequest(@NotNull String url,
	                                    @NonNull File outputFile,
	                                    @NotNull ProgressListener listener,
	                                    long downloadedBytes) {
		boolean isResume = downloadedBytes > 0;
		Request.Builder requestBuilder = new Request.Builder().url(url);
		
		if (isResume) {
			requestBuilder.addHeader("Range", "bytes=" + downloadedBytes + "-");
		}
		
		currentCall = client.newCall(requestBuilder.build());
		currentCall.enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException error) {
				if (call.isCanceled()) {
					logger.debug("Download request canceled.");
					return;
				}
				
				logger.error("Network request failed: " + error.getMessage());
				listener.onError(error.getMessage());
			}
			
			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) {
				if (isRequestFailed(response, listener)) return;
				
				ResponseBody body = response.body();
				boolean serverSupportsResume = isResume && response.code() == 206;
				long startingBytes = serverSupportsResume ? downloadedBytes : 0;
				
				if (shouldAbortDownload(serverSupportsResume, isResume,
					outputFile, listener)) return;
				
				handleDownloadResponse(body, startingBytes,
					serverSupportsResume, outputFile, listener);
			}
		});
	}
	
	/**
	 * Checks if an HTTP response indicates a failed request. If the response is
	 * unsuccessful (HTTP status code outside the 2xx range), this method logs the
	 * error, notifies the listener via {@link ProgressListener#onError(String)},
	 * and returns {@code true}.
	 *
	 * <p>This method is typically called immediately after receiving a response
	 * to validate the request before proceeding with download stream processing.
	 *
	 * @param response The HTTP response to check. Must not be null.
	 * @param listener The progress listener to notify on error. Must not be null.
	 * @return {@code true} if the request failed, {@code false} if successful.
	 */
	private boolean isRequestFailed(@NonNull Response response,
	                                @NonNull ProgressListener listener) {
		if (!response.isSuccessful()) {
			String errorMsg = "Server returned HTTP error: " + response.code();
			logger.error(errorMsg);
			listener.onError(errorMsg);
			return true;
		}
		return false;
	}
	
	/**
	 * Determines whether a download should be aborted based on server capabilities
	 * and file writability. This method handles two abort conditions:
	 *
	 * <ol>
	 * <li>If resuming is requested but the server does not support resumable
	 *     downloads, the existing partial file is deleted (to avoid corruption)
	 *     and the download proceeds; no abort occurs.</li>
	 * <li>If the output file exists but is not writable, the download is aborted
	 *     and an error is reported to the listener.</li>
	 * </ol>
	 *
	 * @param serverSupportsResume Whether the server supports resume (supports Range headers).
	 * @param isResume             Whether this download is attempting to resume.
	 * @param outputFile           The target output file.
	 * @param listener             The progress listener for error reporting.
	 * @return {@code true} if the download should be aborted (file not writable),
	 *         {@code false} if the download can continue.
	 */
	private boolean shouldAbortDownload(boolean serverSupportsResume,
	                                    boolean isResume,
	                                    @NonNull File outputFile,
	                                    @NonNull ProgressListener listener) {
		if (isResume && !serverSupportsResume) {
			logger.warning("Server does not support resuming. Restarting download.");
			if (outputFile.exists()) {
				outputFile.delete();
			}
		}
		
		if (outputFile.exists() && !outputFile.canWrite()) {
			String errorMsg = "Download failed: target file is not writable. " +
				"Try clearing app storage or granting storage permission.";
			
			logger.error(errorMsg);
			listener.onError(errorMsg);
			return true;
		}
		return false;
	}
	
	/**
	 * Handles the HTTP response body for a file download, delegating to
	 * {@link #processDownloadStream(ResponseBody, File, ProgressListener, long, boolean)}
	 * for actual stream processing. This method manages resource cleanup via
	 * try-with-resources and provides detailed error handling for common failure
	 * scenarios including file access issues, security restrictions, and cancellation.
	 *
	 * <p><strong>Error handling:</strong>
	 * <ul>
	 * <li>{@link FileNotFoundException} – Storage permission denied or file protected.</li>
	 * <li>{@link SecurityException} – Security restriction preventing file write.</li>
	 * <li>{@link Exception} – General I/O or hash errors, including cancellation detection.</li>
	 * </ul>
	 *
	 * <p>After processing (success or error), the response body is automatically
	 * closed via the try-with-resources block.
	 *
	 * @param body                 The response body containing the download stream.
	 * @param startingBytes        Number of bytes already downloaded (for resume).
	 * @param serverSupportsResume If true, appends to existing file; otherwise overwrites.
	 * @param outputFile           The destination file for the download.
	 * @param listener             Progress listener for callbacks.
	 * @see #processDownloadStream(ResponseBody, File, ProgressListener, long, boolean)
	 */
	private void handleDownloadResponse(@NotNull ResponseBody body,
	                                    long startingBytes,
	                                    boolean serverSupportsResume,
	                                    @NonNull File outputFile,
	                                    @NonNull ProgressListener listener) {
		try (body) {
			processDownloadStream(body, outputFile, listener,
				startingBytes, serverSupportsResume);
			
		} catch (FileNotFoundException error) {
			String errorMsg = "Cannot write APK file — " +
				"storage permission may be denied or the file is protected.";
			logger.error(errorMsg);
			listener.onError(errorMsg);
			
		} catch (SecurityException error) {
			String errorMsg = "Security restriction: cannot write to download " +
				"location. Grant storage permission and try again.";
			logger.error(errorMsg);
			
			listener.onError(errorMsg);
		} catch (Exception error) {
			if (currentCall != null && currentCall.isCanceled()) {
				logger.debug("Download stream interrupted by cancellation.");
			} else {
				logger.error("Error writing file or hashing: " + error.getMessage());
				listener.onError(error.getMessage());
			}
		}
	}
	
	/**
	 * Processes the download stream from an HTTP response, writing chunks to a file
	 * while tracking progress and updating a SHA-256 hash of the downloaded content.
	 * This method supports resumable downloads by optionally appending to an existing
	 * file and pre-loading the existing file's hash.
	 *
	 * <p><strong>Processing steps:</strong>
	 * <ol>
	 * <li>If appending to an existing file, reads the existing file content into
	 *     the hash digest.</li>
	 * <li>Reads chunks from the response body and writes them to the output file.</li>
	 * <li>Updates both the file and the hash digest with each chunk.</li>
	 * <li>Reports progress via the {@link ProgressListener} after each chunk.</li>
	 * <li>On completion, calculates the final hash and notifies success.</li>
	 * </ol>
	 *
	 * @param body                   The response body containing the download stream.
	 * @param outputFile             The destination file for the download.
	 * @param listener               Progress listener for callbacks.
	 * @param alreadyDownloadedBytes Number of bytes already downloaded (for resume).
	 * @param appendToFile           If true, appends to existing file and pre-hashes content.
	 * @throws IOException              If an I/O error occurs during reading/writing.
	 * @throws NoSuchAlgorithmException If SHA-256 is not available.
	 */
	private void processDownloadStream(@NotNull ResponseBody body,
	                                   @NotNull File outputFile,
	                                   @NotNull ProgressListener listener,
	                                   long alreadyDownloadedBytes,
	                                   boolean appendToFile)
		throws IOException, NoSuchAlgorithmException {
		
		long remainingBytes = body.contentLength();
		long totalBytes = remainingBytes + alreadyDownloadedBytes;
		long currentDownloadedBytes = alreadyDownloadedBytes;
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		
		if (appendToFile && outputFile.exists()) {
			try (InputStream existingFileInputStream = new FileInputStream(outputFile)) {
				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = existingFileInputStream.read(buffer)) != -1) {
					digest.update(buffer, 0, bytesRead);
				}
			}
		}
		
		try (InputStream inputStream = body.byteStream();
		     FileOutputStream outputStream = new FileOutputStream(outputFile, appendToFile)) {
			
			byte[] buffer = new byte[8192];
			int bytesRead;
			
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				if (currentCall != null && currentCall.isCanceled()) {
					logger.debug("Download cancelled during stream processing.");
					return;
				}
				outputStream.write(buffer, 0, bytesRead);
				digest.update(buffer, 0, bytesRead);
				currentDownloadedBytes += bytesRead;
				
				if (totalBytes > 0) {
					short percentage = (short) ((currentDownloadedBytes * 100L) / totalBytes);
					listener.onProgressUpdate(percentage, currentDownloadedBytes, totalBytes);
				}
			}
			outputStream.flush();
		}
		
		String hashHex = bytesToHex(digest.digest());
		listener.onDownloadComplete(outputFile, hashHex);
	}
	
	/**
	 * Handles the scenario where a file is already fully downloaded (e.g., from a
	 * previous resumable download attempt). This method computes the SHA-256 hash
	 * of the existing file and notifies the listener of completion with 100% progress.
	 *
	 * <p>If the file does not exist or an error occurs during hashing, the listener's
	 * {@link ProgressListener#onError(String)} method is invoked with an appropriate
	 * error message.
	 *
	 * @param outputFile The existing fully downloaded file.
	 * @param listener   The progress listener to notify of completion or error.
	 */
	private void handleFullyDownloaded(File outputFile, ProgressListener listener) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream inputStream = new FileInputStream(outputFile)) {
				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					digest.update(buffer, 0, bytesRead);
				}
			}
			if (listener != null) {
				listener.onProgressUpdate((short) 100, outputFile.length(), outputFile.length());
				listener.onDownloadComplete(outputFile, bytesToHex(digest.digest()));
			}
		} catch (Exception error) {
			logger.error("Failed to hash existing file: " + error.getMessage());
			if (listener != null) listener.onError("Failed to verify existing file.");
		}
	}
	
	/**
	 * Converts a byte array into a hexadecimal string representation. Each byte is
	 * represented by two hexadecimal characters (0-9, a-f). This method is commonly
	 * used for encoding hash values (e.g., SHA-256) or binary data for logging,
	 * storage, or network transmission.
	 *
	 * <p><strong>Example:</strong>
	 * <pre>
	 * byte[] data = new byte[] { 0x1A, 0x2B, 0x3C };
	 * String hex = bytesToHex(data); // returns "1a2b3c"
	 * </pre>
	 *
	 * @param bytes The byte array to convert. Must not be null.
	 * @return A hexadecimal string of length {@code 2 * bytes.length}.
	 */
	private String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder(2 * bytes.length);
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
	
	/**
	 * Stops or cancels the currently active download operation. This method checks
	 * if a download call exists and is pending or in progress, then cancels it
	 * to release network resources and prevent further progress. The cancelled
	 * call reference is cleared to avoid reuse.
	 *
	 * <p>Calling this method when no download is active has no effect.
	 * After cancellation, the {@link ProgressListener#onError(String)} callback
	 * may be invoked depending on the cancellation timing.
	 *
	 * @see okhttp3.Call#cancel()
	 */
	public void stopDownload() {
		if (currentCall != null) {
			logger.debug("Stopping/Canceling current download call.");
			currentCall.cancel();
			currentCall = null;
		}
	}
	
	/**
	 * Callback interface for receiving download progress and completion events.
	 * Implement this interface to monitor file download status and respond to
	 * success or failure outcomes.
	 *
	 * <p><strong>Callback methods:</strong>
	 * <ul>
	 * <li>{@link #onProgressUpdate(short, long, long)} – Reports download progress
	 *     percentage and bytes downloaded.</li>
	 * <li>{@link #onDownloadComplete(File, String)} – Called when download finishes
	 *     successfully, providing the local file and its SHA-256 hash.</li>
	 * <li>{@link #onError(String)} – Called when an error occurs during download
	 *     (e.g., network failure, invalid response, file I/O error).</li>
	 * </ul>
	 */
	public interface ProgressListener {
		
		void onProgressUpdate(short percentage, long downloadedByte, long totalFileSize);
		void onDownloadComplete(File apkFile, String downloadedApkHash);
		void onError(String errorMessage);
	}
}