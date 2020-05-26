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
	 * @return A closing cross icon
	 */
	def close = cache("close.png")
	
	/**
	 * @return A plus icon for adding new items
	 */
	def plus = cache("plus.png")
	
	/**
	 * @return An edit icon
	 */
	def edit = cache("edit.png")
	
	/**
	 * @return A search icon
	 */
	def search = cache("search.png")
	
	/**
	 * @return A circular icon with a check sign
	 */
	def checkCircle = cache("check-circle.png")
	
	/**
	 * @return A trash can icon for deletion
	 */
	def delete = cache("delete.png")
	
	/**
	 * @return A drop down icon
	 */
	def dropDown = cache("drop-down.png")
	
	/**
	 * @return A file icon
	 */
	def file = cache("file.png")
	
	/**
	 * @return A warning icon
	 */
	def warning = cache("warning.png")
}
