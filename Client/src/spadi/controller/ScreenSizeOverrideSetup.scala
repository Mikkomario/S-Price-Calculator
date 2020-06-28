package spadi.controller

import spadi.view.component.Fields
import spadi.view.dialog.RealResolutionWindow
import utopia.flow.async.AsyncExtensions._
import utopia.flow.container.ObjectFileContainer
import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.FileExtensions._
import utopia.genesis.generic.GenesisValue._
import utopia.genesis.shape.shape2D.Size
import spadi.view.util.Setup._
import utopia.genesis.util.Screen
import utopia.reflection.container.swing.window.Frame

import scala.util.Success

/**
 * Used for setting up a possible screen size override
 * @author Mikko Hilpinen
 * @since 24.6.2020, v1.1
 */
object ScreenSizeOverrideSetup
{
	// ATTRIBUTES   ----------------------
	
	private implicit val languageCode: String = "fi"
	
	private val status = new ObjectFileContainer("data/screen-size-override.json", SizeOverrideSettings)(
		SizeOverrideSettings(areConfigured = false))
	
	
	// OTHER    --------------------------
	
	/**
	 * Loads the current size override
	 */
	def load() = status.current.sizeOverride.foreach(Screen.registerRealScreenSize)
	
	/**
	 * Prepares the real screen size. Blocks. May request for user input.
	 */
	def prepareBlocking() =
	{
		if (status.current.areConfigured)
			status.current.sizeOverride.foreach(Screen.registerRealScreenSize)
		else
		{
			// TODO: Remove invisible background frame
			val frame = Frame.invisible()
			frame.startEventGenerators(actorHandler)
			val (shouldClose, shouldPrompt) = new RealResolutionWindow().displayOver(frame.component).waitFor().getOrElse(Left(false)) match
			{
				case Right(sizeOverride) =>
					status.current = SizeOverrideSettings(areConfigured = true, Some(sizeOverride))
					Screen.registerRealScreenSize(sizeOverride)
					true -> true
				case Left(wasCorrect) =>
					if (wasCorrect) status.current = SizeOverrideSettings(areConfigured = true)
					!wasCorrect -> false
			}
			// May close the program on completion
			if (shouldClose)
			{
				if (shouldPrompt)
					Fields.messageDialog("Tarvitaan uudelleenkäynnistys",
						"Käynnistäisitkö ohjelman uudelleen muutosten viimeistelemiseksi?").displayOver(frame.component)
						.waitFor()
				frame.close()
				System.exit(0)
			}
			else
				frame.close()
		}
	}
	
	
	// NESTED   --------------------------
	
	private object SizeOverrideSettings extends FromModelFactory[SizeOverrideSettings]
	{
		override def apply(model: template.Model[Property]) =
		{
			val size = model("size_override").size
			Success(SizeOverrideSettings(model("is_configured").booleanOr(size.isDefined), size))
		}
	}
	
	private case class SizeOverrideSettings(areConfigured: Boolean, sizeOverride: Option[Size] = None)
		extends ModelConvertible
	{
		override def toModel = Model(Vector("is_configured" -> areConfigured,
			"size_override" -> sizeOverride))
	}
}
