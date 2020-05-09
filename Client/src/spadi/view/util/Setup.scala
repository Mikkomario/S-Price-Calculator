package spadi.view.util

import java.nio.file.Path

import utopia.flow.util.FileExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.color.{ColorScheme, ColorSet}
import utopia.reflection.component.context.BaseContext
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font

/**
 * Contains view level globals / basic setup
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object Setup
{
	// ATTRIBUTES   ----------------------------
	
	val primaryColors = ColorSet.fromHexes("#455a64", "#718792", "#1c313a").get
	val secondaryColors = ColorSet.fromHexes("#ffab00", "#ffdd4b", "#c67c00").get
	val colorScheme = ColorScheme(primaryColors, secondaryColors)
	
	val resourceDirectory: Path = "resources"
	
	val margins = Margins(12)
	val actorHandler = ActorHandler()
	val baseContext = BaseContext(actorHandler,
		Font.load(resourceDirectory/"fonts/RobotoCondensed-Regular.ttf", 16).getOrElse(Font("Arial", 16)),
		colorScheme, margins)
}
