package spadi.view.util

import utopia.flow.caching.multi.ReleasingCache
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Size
import utopia.genesis.util.DistanceExtensions._
import utopia.reflection.color.TextColorStandard.{Dark, Light}
import utopia.reflection.component.context.{ButtonContextLike, ColorContextLike}
import utopia.reflection.image.SingleColorIconCache

/**
 * Used for accessing icons
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object Icons
{
	// ATTRIBUTES   -----------------------

	private val iconsDirectory = Setup.resourceDirectory/"icons"

	private val cache = new SingleColorIconCache(iconsDirectory, Some(Size.square(1.cm.toScreenPixels)))
	
	
	// COMPUTED ---------------------------

	/**
	 * @return An access point to larger icons
	 */
	def large = Large
	
	/**
	  * @return A burger menu icon
	  */
	def menu = cache("menu.png")
	
	/**
	 * @return A closing cross icon
	 */
	def close = cache("close.png")
	
	/**
	 * @return An arrow icon pointing right
	 */
	def next = cache("arrow-forward.png")
	
	/**
	 * @return An arrow icon pointing left
	 */
	def previous = next.map { _.flippedHorizontally }
	
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
	
	/**
	 * @return An info icon
	 */
	def info = cache("info.png")
	
	/**
	  * @return A help icon
	  */
	def help = cache("help.png")
	
	/**
	  * @return A more icon
	  */
	def more = cache("more.png")
	
	/**
	  * @return A cleaning icon
	  */
	def clean = cache("clean.png")


	// NESTED   ----------------------------

	object Large
	{
		// ATTRIBUTES   --------------------
		
		private val sizeLimit = Size.square(2.cm.toScreenPixels)
		
		private val imageCache = ReleasingCache[String, Image](3.minutes) { fileName =>
			Image.readOrEmpty(iconsDirectory/fileName).smallerThan(sizeLimit)
		}
		
		private val iconCache = new SingleColorIconCache(iconsDirectory, Some(sizeLimit))

		
		// COMPUTED -----------------------
		
		/**
		 * @return An info icon (large)
		 */
		def info = iconCache("info.png")
		
		/**
		 * @return A warning icon (large)
		 */
		def warning = iconCache("warning.png")
		
		/**
		 * @return A light loading icon (multi-color)
		 */
		def loadingLight = imageCache("loading-blue.png")

		/**
		 * @return A darker loading icon (multi-color)
		 */
		def loadingDark = imageCache("loading-blue-dark.png")
		
		/**
		 * @param context Component context (implicit)
		 * @return A loading icon suitable for the specified context
		 */
		def loading(implicit context: ColorContextLike) =
		{
			val background = context match
			{
				case btnC: ButtonContextLike => btnC.buttonColor
				case _ => context.containerBackground
			}
			background.textColorStandard match
			{
				case Light => loadingLight
				case Dark => loadingDark
			}
		}
	}
}
