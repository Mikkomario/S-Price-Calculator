package spadi.view.controller

import java.nio.file.Path

import spadi.controller.ShopData
import spadi.model.PriceInputType.SalePrice
import spadi.view.util.Setup._
import spadi.view.dialog.FileReadSettingsFrame
import utopia.flow.async.AsyncExtensions._
import utopia.flow.util.CollectionExtensions._

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
			// Checks which of the settings still require key mappings
			val existingSetups = ShopData.shopSetups
			// Is combo -> setups
			val existingSetupsByType = existingSetups.groupBy { _.dataSource.isRight }
			// Is combo -> settings
			val settingsByType = settings.groupBy { _.inputType == SalePrice }
			
			val merged = settingsByType.map { case (isCombo, settings) =>
				val setups = existingSetupsByType.getOrElse(isCombo, Vector())
				val (settingsWithoutSetup, settingsWithSetup) = settings.dividedWith { setting =>
					setups.find { _.shop.id == setting.shop.id } match
					{
						case Some(matchingSetup) => Right(setting -> matchingSetup)
						case None => Left(setting)
					}
				}
				isCombo -> (settingsWithoutSetup -> settingsWithSetup)
			}
			
			// Requests key mapping data for the settings without setups
			
			/*
			val (missingTwoPartSettings, readyTwoPartSettings) = twoPartSettings.dividedWith { setting =>
				twoPartSetups.find { _.shop.id == setting.shop.id } match
				{
					case Some(matchingSetup) => Right(setting -> matchingSetup)
					case None => Left(setting)
				}
			}*/
			
			println("Configured following settings:")
			settings.foreach(println)
		}
	}
}
