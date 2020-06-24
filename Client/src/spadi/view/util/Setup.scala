package spadi.view.util

import java.nio.file.Path

import spadi.controller.Globals
import utopia.flow.util.FileExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.util.{Ppi, Screen}
import utopia.genesis.util.DistanceExtensions._
import utopia.reflection.color.{ColorScheme, ColorSet}
import utopia.reflection.component.context.{AnimationContext, BaseContext, ScrollingContext}
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font

import scala.concurrent.ExecutionContext

/**
 * Contains view level globals / basic setup
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object Setup
{
	// ATTRIBUTES   ----------------------------
	
	implicit val ppi: Ppi = Screen.ppi
	
	val primaryColors = ColorSet.fromHexes("#455a64", "#718792", "#1c313a").get
	val secondaryColors = ColorSet.fromHexes("#ffab00", "#ffdd4b", "#c67c00").get
	val colorScheme = ColorScheme(primaryColors, secondaryColors)
	
	val resourceDirectory: Path = "resources"
	
	val margins = Margins(0.33.cm.toPixels)
	val standardFieldWidth = 320
	
	val actorHandler = ActorHandler()
	val standardFontSize = 0.5.cm.toPixels.toInt
	val baseContext = BaseContext(actorHandler,
		Font.load(resourceDirectory/"fonts/RobotoCondensed-Regular.ttf", standardFontSize)
			.getOrElse(Font("Arial", standardFontSize)), colorScheme, margins)
	
	implicit val animationContext: AnimationContext = AnimationContext(actorHandler)
	implicit val scrollingContext: ScrollingContext = ScrollingContext.withDarkRoundedBar(actorHandler,
		margins.medium.toInt, margins.medium * 6)
	
	implicit val localizer: Localizer = NoLocalization
	
	
	// COMPUTED --------------------------------
	
	implicit def exc: ExecutionContext = Globals.executionContext
}
