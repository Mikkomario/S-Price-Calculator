package spadi.view.component

import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.reflection.component.context.{ButtonContextLike, ColorContext}
import utopia.reflection.component.drawing.immutable.ImageDrawer
import utopia.reflection.component.swing.input.TextField
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._

/**
 * Used for constructing search fields
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object SearchField
{
	/**
	 * @param prompt Prompt displayed on this field
	 * @param context Color context
	 * @return A new search field with default settings
	 */
	def default(prompt: LocalizedString)(implicit context: ColorContext) =
		custom(prompt)(context.forTextComponents().forGrayFields)
	
	/**
	 * @param prompt Prompt displayed on this field
	 * @param context Field context
	 * @return A new search field with context settings
	 */
	def custom(prompt: LocalizedString)(implicit context: ButtonContextLike) =
	{
		val field = TextField.contextual(standardFieldWidth.any.withLowPriority, prompt = Some(prompt))
		field.addCustomDrawer(ImageDrawer(Icons.search.singleColorImage, alignment = Alignment.Right))
		field
	}
}
