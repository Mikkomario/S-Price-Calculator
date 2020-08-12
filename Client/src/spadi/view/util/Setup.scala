package spadi.view.util

import java.nio.file.Path

import spadi.controller.{Globals, ScreenSizeOverrideSetup}
import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.parse.JsonParser
import utopia.flow.util.FileExtensions._
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.util.{Ppi, Screen}
import utopia.genesis.util.DistanceExtensions._
import utopia.reflection.color.{ColorScheme, ColorSet}
import utopia.reflection.component.context.{AnimationContext, BaseContext, ScrollingContext}
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font
import utopia.vault.database.ConnectionPool

import scala.concurrent.ExecutionContext

/**
 * Contains view level globals / basic setup
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object Setup
{
	// INITIAL CODE ----------------------------
	
	GenesisDataType.setup()
	
	implicit val localizer: Localizer = NoLocalization
	implicit val jsonParser: JsonParser = JsonBunny
	
	val resourceDirectory: Path = "resources"
	
	ScreenSizeOverrideSetup.load()
	
	
	// ATTRIBUTES   ----------------------------
	
	implicit val ppi: Ppi = Screen.ppi
	
	val primaryColors = ColorSet.fromHexes("#455a64", "#718792", "#1c313a").get
	val secondaryColors = ColorSet.fromHexes("#ffc400", "#fff64f", "#c79400").get
	val grayColors = ColorSet.fromHexes("#424242", "#6d6d6d", "#1b1b1b").get
	val colorScheme = ColorScheme(primaryColors, secondaryColors, grayColors)
	
	val margins = Margins(3.mm.toPixels)
	val standardFieldWidth = 5.cm.toPixels
	val standardSwitchWidth = 1.5.cm.toPixels
	
	val actorHandler = ActorHandler()
	val standardFontSize = 0.5.cm.toPixels.toInt
	val baseContext = BaseContext(actorHandler,
		Font.load(resourceDirectory/"fonts/RobotoCondensed-Regular.ttf", standardFontSize)
			.getOrElse(Font("Arial", standardFontSize)), colorScheme, margins)
	
	implicit val animationContext: AnimationContext = AnimationContext(actorHandler)
	implicit val scrollingContext: ScrollingContext = ScrollingContext.withLightRoundedBar(actorHandler,
		margins.medium.toInt, margins.medium * 6)
	
	
	// COMPUTED --------------------------------
	
	implicit def exc: ExecutionContext = Globals.executionContext
	
	implicit def connectionPool: ConnectionPool = Globals.connectionPool
}
