package app.core.bases

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference

/**
 * A base fragment class that provides common functionality for fragments in the app.
 * It manages fragment lifecycle events and provides a reference to the parent activity.
 */
abstract class BaseFragment : Fragment() {
	
	/**
	 * A weak reference to the parent [BaseActivity], which is retrieved lazily.
	 * This avoids strong references and helps prevent memory leaks.
	 */
	open val safeBaseActivityRef: BaseActivity? by lazy {
		WeakReference(activity).get() as BaseActivity
	}
	
	/**
	 * A weak reference to the fragment's layout view.
	 * This allows safe access to the view, preventing memory leaks if the fragment is destroyed.
	 */
	private var _fragmentLayout: View? = null
	open val safeFragmentLayoutRef: View?
		get() = WeakReference(_fragmentLayout).get()
	
	/**
	 * Flag to indicate if the fragment is currently visible and running.
	 * This is useful to manage fragment-specific tasks based on its lifecycle.
	 */
	open var isFragmentRunning: Boolean = false
	
	/**
	 * Abstract method to provide the layout resource ID for the fragment.
	 * @return The layout resource ID to be used for the fragment's UI.
	 */
	protected abstract fun getLayoutResId(): Int
	
	/**
	 * Abstract method that is called after the layout is loaded.
	 * @param layoutView The root view of the fragment's layout.
	 * @param state The saved instance state, if any.
	 */
	protected abstract fun onAfterLayoutLoad(layoutView: View, state: Bundle?)
	
	/**
	 * Abstract method to handle fragment-specific logic when the fragment resumes.
	 */
	protected abstract fun onResumeFragment()
	
	/**
	 * Abstract method to handle fragment-specific logic when the fragment pauses.
	 */
	protected abstract fun onPauseFragment()
	
	/**
	 * Called to inflate and return the fragment's layout.
	 * The layout resource is provided by [getLayoutResId].
	 *
	 * @param inflater The LayoutInflater used to inflate the layout.
	 * @param container The container view that the layout will be attached to.
	 * @param savedInstanceState The saved instance state from the previous session.
	 * @return The root view of the fragment's layout.
	 */
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? = inflater
		.inflate(getLayoutResId(), container, false).also { _fragmentLayout = it }
	
	/**
	 * Called after the view has been created and the layout has been loaded.
	 * It invokes [onAfterLayoutLoad] to allow for additional fragment setup.
	 *
	 * @param view The root view of the fragment's layout.
	 * @param bundle The saved instance state.
	 */
	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		onAfterLayoutLoad(view, bundle)
	}
	
	/**
	 * Called when the fragment becomes visible and starts interacting with the user.
	 * Sets [isFragmentRunning] to true and invokes [onResumeFragment].
	 */
	override fun onResume() {
		super.onResume()
		isFragmentRunning = true
		onResumeFragment()
	}
	
	/**
	 * Called when the fragment is no longer in the foreground.
	 * Sets [isFragmentRunning] to false and invokes [onPauseFragment].
	 */
	override fun onPause() {
		super.onPause()
		isFragmentRunning = false
		onPauseFragment()
	}
	
	/**
	 * Called when the fragment's view is destroyed.
	 * Cleans up the fragment's layout and resets [isFragmentRunning].
	 */
	override fun onDestroyView() {
		super.onDestroyView()
		isFragmentRunning = false
		_fragmentLayout = null
	}
}