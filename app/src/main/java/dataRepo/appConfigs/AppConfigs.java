package dataRepo.appConfigs;

import com.nextgen.R;

import java.io.Serializable;

import coreUtils.library.strings.StringHelper;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Entity class representing application-wide configuration settings persisted
 * in the ObjectBox database. This singleton entity stores user preferences,
 * feature toggles, and runtime flags that must survive app restarts.
 *
 * <p>The configuration is identified by a fixed {@code entityId} (typically 1)
 * and accessed via {@link AppConfigsRepo#getConfig()}. Changes are automatically
 * synchronized with the database and notify all registered observers via
 * {@link AppConfigsObserver#onConfigChanged(AppConfigs)}.
 *
 * <p><strong>Common configuration fields include:</strong>
 * <ul>
 * <li>Crash recovery flags for detecting app crashes across sessions.</li>
 * <li>UI theme settings (light/dark/follow system).</li>
 * <li>Notification permission grant state.</li>
 * <li>Storage location preference (private folder vs. system gallery).</li>
 * <li>Media sorting options (date, name, size, type).</li>
 * </ul>
 *
 * <p>Implements {@link Serializable} to allow configuration objects to be passed
 * via Intents or saved in {@link android.os.Bundle} when needed.
 *
 * @see AppConfigsRepo
 * @see AppConfigsObserver
 * @see io.objectbox.annotation.Entity
 */
@Entity public class AppConfigs implements Serializable {
	
	@Id(assignable = true)
	public long entityId = 0L;
	
	public boolean isLocaleConfigured = false;
	public boolean isTermsConditionsAgreed = false;
	public boolean isAppReviewCompleted = false;
    public boolean isUserNeedToRemindForAppUpdate = false;
	public boolean hasSkippedBatteryOptimization = false;
	public boolean hasAppCrashedRecently = false;
	public String lastProcessedClipboardText = "";
	
	public int defaultDownloadLocationType = SYSTEM_GALLERY;
	public String selectedDownloadDirectoryPath = "";
	
	public String selectedLanguageCode = "EN";
	public String selectedRegionCode = "IN";
	public int completedDownloadsSortOrder = SORT_DATE_NEWEST_FIRST;
	public int activeDownloadsSortOrder = SORT_DATE_NEWEST_FIRST;
	
	public boolean isCrashReportingEnabled = false;
	public boolean isAnalyticsEnabled = false;
	public boolean isPersonalizedRecommendationsEnabled = false;

	public boolean isDailySuggestionsEnabled = true;
	public boolean useYtdlpAsDefaultDownloader = true;
	public boolean isNewPipeUnavailable = false;
	
	public int totalSuccessfulDownloadCount = 0;
	public float totalAppUsageDurationMs = 0f;
	public String formattedTotalUsageDuration = "";
	public float foregroundAppUsageDurationMs = 0f;
	public String formattedForegroundUsageDuration = "";
	
	public int pushNotificationClickCount = 0;
	public int pushNotificationReceivedCount = 0;
	public int languageChangeButtonClickCount = 0;
	public int mediaPlaybackButtonClickCount = 0;
	public int howToGuideButtonClickCount = 0;
	public int videoUrlEditorButtonClickCount = 0;
	public int homeHistoryMenuItemClickCount = 0;
	public int homeBookmarksMenuItemClickCount = 0;
	public int recentDownloadsMenuItemClickCount = 0;
	public int homeFaviconMenuItemClickCount = 0;
	public int versionCheckMenuItemClickCount = 0;
	public int interstitialAdClickCount = 0;
	public int interstitialAdImpressionCount = 0;
	public int rewardedAdClickCount = 0;
	public int rewardedAdImpressionCount = 0;
	public int userInitiatedDownloadCount = 0;
	
	public boolean isDownloadThumbnailHidden = false;
	public boolean isDownloadCompletionSoundEnabled = true;
	public boolean isDownloadNotificationHidden = false;
	public boolean isDownloadOpenedOnSingleClick = true;
	public boolean isDownloadAutoResumeEnabled = true;
	public int maxDownloadAutoResumeLimit = 35;
	public long maxDownloadSpeedLimit = 0L;
	public boolean wifiOnlyDownloadEnabled = false;
	public String httpUserAgentForDownloads = "";
	public String httpProxyServerForDownloads = "";
	public int downloadConcurrencyLimit = 1;
	
	public String browserDefaultHomepageUrl =
		StringHelper.getText(R.string.https_youtube_com);
	
	public boolean isDesktopBrowsingEnabled = false;
	public boolean isPrivateBrowsingEnabled = false;
	public boolean isAdBlockerEnabled = true;
	public boolean isJavaScriptEnabled = true;
	public boolean isImageLoadingEnabled = true;
	public boolean isPopupBlockerEnabled = true;
	public boolean isVideoGrabberEnabled = true;
	public String httpUserAgentForBrowser = "";
	
	public static final int PRIVATE_FOLDER = 1;
	public static final int SYSTEM_GALLERY = 2;
	
	public static final int SORT_DATE_NEWEST_FIRST = 3;
	public static final int SORT_DATE_OLDEST_FIRST = 4;
	public static final int SORT_NAME_A_TO_Z = 5;
	public static final int SORT_NAME_Z_TO_A = 6;
	public static final int SORT_SIZE_SMALLEST_FIRST = 7;
	public static final int SORT_SIZE_LARGEST_FIRST = 8;
	public static final int SORT_TYPE_VIDEOS_FIRST = 9;
	public static final int SORT_TYPE_MUSIC_FIRST = 10;
	
	/**
	 * Persists the current configuration instance to the database. This method is a
	 * convenience wrapper around {@link AppConfigsRepo#save(AppConfigs)} that passes
	 * {@code this} as the argument. Call this method after modifying any configuration
	 * fields to ensure changes are stored persistently and to trigger notification
	 * of all registered observers via {@link AppConfigsObserver#onConfigChanged(AppConfigs)}.
	 *
	 * <p><strong>Example usage:</strong>
	 * <pre>
	 * AppConfigs config = AppConfigsRepo.getConfig();
	 * config.setThemeMode(ThemeMode.DARK);
	 * config.save();
	 * </pre>
	 *
	 * @see AppConfigsRepo#save(AppConfigs)
	 * @see AppConfigsRepo#getConfig()
	 */
	public void save() {
		AppConfigsRepo.save(this);
	}
}