package spadi.view.controller

import java.nio.file.Path

import spadi.view.util.Setup._
import spadi.view.dialog.FileReadSettingsFrame
import utopia.flow.async.AsyncExtensions._

/**
 * Provides an interactive user interface for specifying read settings for new files
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
object NewFileConfigurationUI
{
	def configureBlocking(newPaths: Vector[Path]) =
	{
		// Displays a settings dialog for the new paths
		val settings = new FileReadSettingsFrame(newPaths).display().waitFor().getOrElse(Vector())
		if (settings.isEmpty)
			println("No settings configured")
		else
		{
			
			
			println("Configured following settings:")
			settings.foreach(println)
		}
	}
}
