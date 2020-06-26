package spadi.view.controller

import java.nio.file.Path

import spadi.controller.{Log, ReadExcel, SheetTarget, ShopData}
import spadi.model.{BasePriceKeyMappingFromFieldsFactory, DataSource, FileReadSetting, KeyMappingFactory, ProductPriceKeyMappingFromFieldsFactory, SalesGroupKeyMappingFromFieldsFactory, ShopSetup}
import spadi.model.PriceInputType.{BasePrice, SaleGroup, SalePrice}
import spadi.view.util.Setup._
import spadi.view.dialog.{DataSourceDialogLike, DataSourceDialogWithSelections, DataSourceDialogWithTextFields, FileReadSettingsFrame}
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
	// ATTRIBUTES   ------------------------
	
	private val testReadTarget = SheetTarget.sheetAtIndex(0, maxRowsRead = Some(10))
	
	
	// OTHER    ----------------------------
	
	@scala.annotation.tailrec
	def configureBlocking(target: Either[Vector[Path], Vector[FileReadSetting]]): Unit =
	{
		// Displays a settings dialog for the new paths
		val readSettingsFrame = new FileReadSettingsFrame(target)
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
			
			if (!isCancelled)
			{
				// If "previous" was pressed on the first dialog, redisplays the settings view and starts the
				// process over
				if (nextDisplayIndex < 0)
					configureBlocking(Right(settings))
				else
				{
					// Updates shop setups based on new data
					val newComboSetups = comboInputs.flatMap { input =>
						input.result.map { ds =>
							ShopSetup(input.setting.shop, Right(ds))
						}
					}
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
					// Merges some data directly into already existing setups
					val mergedComboSetups = merged.get(true).map { _._2 }.getOrElse(Vector()).groupMap { _._2 } { _._1 }
						.map { case (original, settings) =>
							original.mapDataSourceIfCombo { _.copy(filePath = settings.head.path) }
						}
					val mergedSplitSetups = merged.get(false).map { _._2 }.getOrElse(Vector()).groupMap { _._2 } { _._1 }
						.map { case (original, settings) =>
							original.mapDataSourceIfSplit { (oldBase, oldSale) =>
								val newBase = settings.find { _.inputType == BasePrice } match
								{
									case Some(setting) => oldBase.copy(filePath = setting.path)
									case None => oldBase
								}
								val newSale = settings.find { _.inputType == SaleGroup } match
								{
									case Some(setting) => oldSale.copy(filePath = setting.path)
									case None => oldSale
								}
								newBase -> newSale
							}
						}
					
					// FIXME: Handle merged setups as well
					val allComboSetups = mergeSetups(existingSetupsByType.getOrElse(true, Vector()), mergedComboSetups,
						newComboSetups)
					val allSplitSetups = mergeSetups(existingSetupsByType.getOrElse(false, Vector()), mergedSplitSetups,
						newSplitSetups)
					println(s"Updating shop setups to ${allComboSetups.size} combo setups and ${allSplitSetups.size} split setups")
					allComboSetups.foreach { s => println(s"\t- ${s._2.dataSource.toOption.map { _.filePath }}") }
					// FIXME: Sets wrong setups
					ShopData.shopSetups = allComboSetups.values.toVector ++ allSplitSetups.values
				}
			}
		}
	}
	
	private def mergeSetups(existing: IterableOnce[ShopSetup], merged: Iterable[ShopSetup],
	                        newSetups: IterableOnce[ShopSetup]) =
		existing.iterator.map { setup => setup.shop.id -> setup }.toMap ++
			merged.iterator.map { setup => setup.shop.id -> setup }.toMap ++
			newSetups.iterator.map { setup => setup.shop.id -> setup }.toMap
	
	
	// NESTED   ---------------------------
	
	private class DataSourceInput[A](val setting: FileReadSetting, mappingFactory: KeyMappingFactory[A])
	{
		// ATTRIBUTES   -------------------
		
		// TODO: Log possible errors
		private lazy val sampleRows = ReadExcel.withoutHeadersFrom(setting.path, testReadTarget).toOption
			.filter { _.exists { _.nonEmpty } }
		
		private var lastDialog: Option[DataSourceDialogLike[A, _, _]] = None
		private var lastResult: Option[DataSource[A]] = None
		
		
		// COMPUTED -----------------------
		
		def result = lastResult
		
		
		// OTHER    -----------------------
		
		// Returns next display direction. None if user cancelled process.
		def displayBlocking(parentWindow: java.awt.Window) =
		{
			// Creates a new dialog
			// Tries to read some data from the excel file in order to present a user-friendly dialog
			val newDialog = sampleRows match
			{
				case Some(sampleRows) =>
					new DataSourceDialogWithSelections[A](setting.path, setting.shop, mappingFactory, sampleRows)
				case None => new DataSourceDialogWithTextFields[A](setting.path, setting.shop, mappingFactory)
			}
			
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
