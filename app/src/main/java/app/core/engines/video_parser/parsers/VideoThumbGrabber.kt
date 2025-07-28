package app.core.engines.video_parser.parsers

import lib.networks.URLUtilityKT.fetchWebPageContent
import org.jsoup.Jsoup

/**
 * Utility object responsible for extracting the thumbnail image URL from a video page.
 *
 * This parser primarily relies on Open Graph meta tags (`og:image`) present in the HTML
 * source of the video URL.
 */
object VideoThumbGrabber {
	
	/**
	 * Attempts to parse and retrieve the thumbnail image URL from a video page.
	 *
	 * @param videoUrl The URL of the video page from which to extract the thumbnail.
	 * @param userGivenHtmlBody Optional HTML content of the video page (if already fetched).
	 * @param numOfRetry Number of times to retry fetching the page in case of network failure.
	 * @return The URL of the thumbnail image if found, otherwise null.
	 */
	@JvmStatic
	fun startParsingVideoThumbUrl(
		videoUrl: String,
		userGivenHtmlBody: String? = null,
		numOfRetry: Int = 6
	): String? {
		// Fetch HTML body either from user input or by making a network call
		val htmlBody = if (userGivenHtmlBody.isNullOrEmpty()) {
			fetchWebPageContent(
				url = videoUrl,
				retry = true,
				numOfRetry = numOfRetry
			) ?: return null
		} else {
			userGivenHtmlBody
		}
		
		// Parse the HTML using Jsoup
		val document = Jsoup.parse(htmlBody)
		
		// Select meta tags with property="og:image"
		val metaTags = document.select("meta[property=og:image]")
		
		// Loop through each tag to find a valid image URL
		for (metaTag in metaTags) {
			val rawContent = metaTag.attr("content")
			val decodedUrl = org.jsoup.parser.Parser.unescapeEntities(rawContent, true)
			
			// Regular expression to match common image formats
			val regexPattern = Regex(
				pattern = """https?://[^\s'"<>]+?\.(jpeg|jpg|png|gif|webp)(\?.*)?""",
				option = RegexOption.IGNORE_CASE
			)
			
			// Return the first valid image URL match
			if (regexPattern.containsMatchIn(decodedUrl)) return decodedUrl
		}
		
		// No valid thumbnail found
		return null
	}
}