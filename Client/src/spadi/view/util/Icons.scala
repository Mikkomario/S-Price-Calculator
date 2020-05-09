package spadi.view.util

import utopia.flow.util.FileExtensions._
import utopia.reflection.image.SingleColorIconCache

/**
 * Used for accessing icons
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object Icons
{
	// ATTRIBUTES   -----------------------
	
	private val cache = new SingleColorIconCache(Setup.resourceDirectory/"icons")
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return A search icon
	 */
	def search = cache("search.png")
}
