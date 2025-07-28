package app.core.bases.interfaces

/**
 * Interface for receiving the result of a permission request.
 * Implement this to handle the result when Android runtime permissions are requested.
 */
interface PermissionsResult {
    
    /**
     * Called when the result of a permission request is available.
     *
     * @param isGranted `true` if all requested permissions were granted, `false` otherwise.
     * @param grantedLs A list of permissions that were granted, or `null` if none.
     * @param deniedLs A list of permissions that were denied, or `null` if none.
     */
    fun onPermissionResultFound(
        isGranted: Boolean,
        grantedLs: List<String>?,
        deniedLs: List<String>?
    )
}
