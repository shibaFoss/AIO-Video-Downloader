package lib.device

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.util.Patterns
import app.core.AIOApp.Companion.INSTANCE
import java.lang.ref.WeakReference

/**
 * Utility object for fetching and processing user email account details on the device.
 */
object UserEmailFetcher {
	
	/**
	 * Retrieves the primary Google account email address of the user.
	 *
	 * @return The email address as a String, or null if unavailable or an error occurs.
	 */
	@JvmStatic
	fun getPrimaryEmail(): String? {
		return try {
			val accountManager = AccountManager.get(INSTANCE)
			val account = getAccount(accountManager)
			account?.name
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Returns all valid email addresses registered on the device.
	 *
	 * @return Array of valid email addresses as Strings.
	 */
	@JvmStatic
	fun getAllRegisteredEmailAddresses(): Array<String> {
		val emailList = mutableListOf<String>()
		try {
			val accounts = AccountManager.get(INSTANCE).accounts
			accounts.forEach { account ->
				if (isValidEmail(account.name))
					emailList.add(account.name)
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
		return emailList.toTypedArray()
	}
	
	/**
	 * Validates whether the provided email address is in a correct format.
	 *
	 * @param email Email address to validate.
	 * @return True if the email format is valid, false otherwise.
	 */
	@JvmStatic
	fun isValidEmail(email: String): Boolean {
		return Patterns.EMAIL_ADDRESS.matcher(email).matches()
	}
	
	/**
	 * Returns the first Google account available on the device.
	 *
	 * @param accountManager Instance of AccountManager.
	 * @return First found Google account or null if none exist.
	 */
	@JvmStatic
	fun getAccount(accountManager: AccountManager): Account? {
		val accounts = accountManager.getAccountsByType("com.google")
		return if (accounts.isNotEmpty()) accounts[0] else null
	}
	
	/**
	 * Tries to fetch the user's display name from the account.
	 * Uses reflection for devices running Android Q and above.
	 * Falls back to generating a display name from the email if needed.
	 *
	 * @return Display name or null if unavailable.
	 */
	@JvmStatic
	fun getDisplayName(): String? {
		return try {
			val accountManager = AccountManager.get(INSTANCE)
			val account = getAccount(accountManager)
			
			account?.let {
				val displayName = getDisplayNameUsingReflection(accountManager, it)
				displayName ?: guessDisplayName(it.name)
			}
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Fetches the user-visible name using account user data, supported from Android Q+.
	 *
	 * @param accountManager Instance of AccountManager.
	 * @param account The Account to retrieve data from.
	 * @return Display name if found, null otherwise.
	 */
	@JvmStatic
	fun getDisplayNameUsingReflection(
		accountManager: AccountManager, account: Account
	): String? {
		return if (SDK_INT >= Q) {
			try {
				val result = accountManager.getUserData(account, "display_name")
				result?.toString()
			} catch (error: Exception) {
				error.printStackTrace()
				null
			}
		} else null
	}
	
	/**
	 * Attempts to guess a user-friendly name from an email address.
	 *
	 * @param email Email address to use for guessing.
	 * @return Capitalized name string derived from email, or null if invalid.
	 */
	@JvmStatic
	fun guessDisplayName(email: String): String? {
		val atIndex = email.indexOf('@')
		return if (atIndex != -1) {
			capitalizeFirstLetter(email.substring(0, atIndex))
		} else null
	}
	
	/**
	 * Capitalizes the first letter of a string if it is not already capitalized.
	 *
	 * @param string Input string to capitalize.
	 * @return Capitalized string, or empty if input is null or empty.
	 */
	@JvmStatic
	fun capitalizeFirstLetter(string: String?): String {
		return string?.takeIf { it.isNotEmpty() }?.let {
			val first = it[0]
			if (first.isUpperCase()) it
			else first.uppercaseChar().toString() + it.substring(1)
		} ?: ""
	}
	
	/**
	 * Extracts the domain name from the primary email address.
	 *
	 * @return Domain as a string (e.g., "gmail.com"), or null if not available.
	 */
	@JvmStatic
	fun getEmailDomain(): String? {
		return getPrimaryEmail()?.let { primaryEmail ->
			val atIndex = primaryEmail.indexOf('@')
			if (atIndex != -1 && atIndex < primaryEmail.length - 1) {
				primaryEmail.substring(atIndex + 1)
			} else null
		}
	}
	
	/**
	 * Retrieves all available email accounts with their types.
	 *
	 * @param context Optional context to use for fetching AccountManager.
	 * @return List of strings with format: "email (accountType)".
	 */
	@JvmStatic
	fun getAllEmailAccounts(context: Context?): List<String> {
		return WeakReference(context).get()?.let { safeContextRef ->
			val accountsList = mutableListOf<String>()
			try {
				val accounts = AccountManager.get(safeContextRef).accounts
				accounts.forEach { account ->
					accountsList.add("${account.name} (${account.type})")
				}
			} catch (error: Exception) {
				error.printStackTrace()
			}
			accountsList
		} ?: run { emptyList() }
	}
}