package spadi.view.controller

import java.nio.file.Path

import spadi.controller.{Log, ShopData}
import spadi.model.{BasePriceKeyMappingFromFieldsFactory, DataSource, FileReadSetting, KeyMappingFactory, ProductPriceKeyMappingFromFieldsFactory, SalesGroupKeyMappingFromFieldsFactory, ShopSetup}
import spadi.model.PriceInputType.{BasePrice, SalePrice}
import spadi.view.util.Setup._
import spadi.view.dialog.{DataSourceDialog, FileReadSettingsFrame}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.Direction1D.{Negative, Positive}
import utopia.reflection.container.swing.window.Frame

import scala.util.{Failure, Success}

/**
 * Provides an interactive user interface for specifying read settings for new files
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
object NewFileConfigurationUI
{
	// OTHER    ----------------------------
	
	def configureBlocking(newPaths: Vector[Path]) =
	{
		// Displays a settings dialog for the new paths
		val readSettingsFrame = new FileReadSettingsFrame(newPaths)
		val settings = readSettingsFrame.display().waitFor().getOrElse(Vector())
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
			val comboInputs = merged.get(true).map { _._1 }.getOrElse(Vector()).map { setting =>
				new DataSourceInput(setting, ProductPriceKeyMappingFromFieldsFactory) }
			val (newSaleSettings, newBaseSettings) = merged.get(false).map { _._1 }.getOrElse(Vector())
				.divideBy { _.inputType == BasePrice }
			val baseInputs = newBaseSettings.map { new DataSourceInput(_, BasePriceKeyMappingFromFieldsFactory) }
			val saleInputs = newSaleSettings.map { new DataSourceInput(_, SalesGroupKeyMappingFromFieldsFactory) }
			
			val allInputs = comboInputs ++ baseInputs ++ saleInputs
			
			val parentFrame = Frame.invisible()
			parentFrame.position = readSettingsFrame.bounds.center
			var nextDisplayIndex = 0
			var isCancelled = false
			while (!isCancelled && nextDisplayIndex >= 0 && nextDisplayIndex < allInputs.size)
			{
				allInputs(nextDisplayIndex).displayBlocking(parentFrame.component) match
				{
					case Some(direction) => nextDisplayIndex += 1 * direction.signModifier
					case None => isCancelled = true
				}
			}
			
			if (isCancelled)
			{
				// TODO: Read data anyway
				???
			}
			else if (nextDisplayIndex < 0)
			{
				// TODO: edit settings and repeat this process (recursion)
				???
			}
			else
			{
				// Updates shop setups based on new data
				val newComboSetups = comboInputs.flatMap { input => input.result.map { ds =>
					ShopSetup(input.setting.shop, Right(ds)) } }
				val newSplitShops = (newBaseSettings ++ newSaleSettings).map { _.shop }.toSet
				val newSplitSetups = newSplitShops.flatMap { shop =>
					val oldSetup = existingSetupsByType.get(false).flatMap { _.find { _.shop.id == shop.id } }
					baseInputs.find { _.setting.shop == shop }.flatMap { _.result }
						.orElse { oldSetup.flatMap { _.dataSource.leftOption.map { _._1 } } }
						.flatMap { baseDS =>
							saleInputs.find { _.setting.shop == shop }.flatMap { _.result }
								.orElse { oldSetup.flatMap { _.dataSource.leftOption.map { _._2 } } }
								.map { saleDS => ShopSetup(shop, Left(baseDS -> saleDS)) }
						}
				}
				// TODO: Continue
			}
			
			println("Configured following settings:")
			settings.foreach(println)
		}
	}
	
	
	// NESTED   ---------------------------
	
	private class DataSourceInput[A](val setting: FileReadSetting, mappingFactory: KeyMappingFactory[A])
	{
		// ATTRIBUTES   -------------------
		
		private var lastDialog: Option[DataSourceDialog[A]] = None
		private var lastResult: Option[DataSource[A]] = None
		
		
		// COMPUTED -----------------------
		
		def result = lastResult
		
		
		// OTHER    -----------------------
		
		// Returns next display direction. None if user cancelled process.
		def displayBlocking(parentWindow: java.awt.Window) =
		{
			// Creates a new dialog
			val newDialog = new DataSourceDialog[A](setting.path, setting.shop, mappingFactory)
			// Pre-fills some content, if possible
			lastDialog.foreach { d => newDialog.input = d.input }
			lastDialog = Some(newDialog)
			// Displays the dialog and waits for a result
			newDialog.display(parentWindow).waitFor() match
			{
				case Success(result) =>
					result match
					{
						case Right(dataSource) =>
							lastResult = Some(dataSource)
							Some(Positive)
						case Left(displayPrevious) => if (displayPrevious) Some(Negative) else None
					}
				case Failure(error) =>
					Log(error, "Unexpected error while displaying data source dialog")
					None
			}
		}
	}
}
