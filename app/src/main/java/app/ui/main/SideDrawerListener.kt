package app.ui.main

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener

/**
 * Interface for listening to navigation drawer events.
 *
 * Provides empty default implementations for all methods of [DrawerListener],
 * allowing implementers to override only the methods they need.
 *
 * This interface is typically used to respond to navigation drawer state changes
 * while avoiding the need to implement all methods of the base [DrawerListener].
 */
interface SideDrawerListener : DrawerListener {
	
	/**
	 * Called when the drawer's position changes.
	 *
	 * @param drawerView The child view that was moved
	 * @param slideOffset The new offset of this drawer within its range, from 0-1
	 */
	override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
	
	/**
	 * Called when the drawer has settled in a completely open state.
	 *
	 * @param drawerView The drawer view that is now open
	 */
	override fun onDrawerOpened(drawerView: View) {}
	
	/**
	 * Called when the drawer has settled in a completely closed state.
	 *
	 * @param drawerView The drawer view that is now closed
	 */
	override fun onDrawerClosed(drawerView: View) {}
	
	/**
	 * Called when the drawer motion state changes.
	 *
	 * @param newState The new drawer motion state:
	 * - 0: IDLE (not moving)
	 * - 1: DRAGGING (user is actively dragging)
	 * - 2: SETTLING (open/close animation is settling)
	 */
	override fun onDrawerStateChanged(newState: Int) {}
}