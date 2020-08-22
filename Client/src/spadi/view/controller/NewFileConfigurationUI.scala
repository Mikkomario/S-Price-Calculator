package spadi.view.controller

import java.nio.file.Path

import spadi.controller.{Globals, Log}
import spadi.controller.database.access.multi.{DbPriceKeyMappings, DbSaleKeyMappings}
import spadi.controller.read.{DataProcessor, ReadExcel}
import spadi.model.cached.ProgressState
import spadi.model.cached.read._
import spadi.model.enumeration.PriceInputType.{SaleGroup, SalePrice}
import spadi.model.enumeration.PriceType.{Base, Net}
import spadi.model.partial.pricing.{SaleGroupData, ShopProductData}
import spadi.model.partial.reading.{PriceKeyMappingData, SaleKeyMappingData}
import spadi.view.component.Fields
import spadi.view.dialog._
import spadi.view.util.Setup._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.parse.CsvReader
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.reflection.localization.LocalString._

import scala.collection.immutable.VectorBuilder
import scala.io.Codec
import scala.util.{Failure, Success}

/**
 * Provides an interactive user interface for specifying read settings for new files
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
object NewFileConfigurationUI
{
	// ATTRIBUTES   ------------------------
	
	private implicit val languageCode: String = "fi"
	private implicit val encoding: Codec = Codec.UTF8
	
	
	// OTHER    ----------------------------
	
	/**
	  * Scans the input file, configures found files and inports new data
	  */
	def readAndConfigureBlocking() =
	{
		val acceptedFileTypes = ReadExcel.supportedFileTypes.map { _.toLowerCase }.toSet + "csv"
		Globals.fileInputDirectory.allRegularFileChildren.map { _.filter { file =>
			acceptedFileTypes.contains(file.fileType.toLowerCase) } } match
		{
			case Success(inputFiles) =>
				if (inputFiles.nonEmpty)
					configureBlocking(Left(inputFiles))
			case Failure(error) =>
				Log(error, "Failed to scan through input directory")
				Fields.errorDialog("Luettavien tiedostojen etsiminen epäonnistui.\nVirheilmoitus: %s"
					.autoLocalized.interpolated(Vector(error.getLocalizedMessage))).displayBlocking()
		}
	}
	
	@scala.annotation.tailrec
	def configureBlocking(target: Either[Vector[Path], Vector[FileReadSetting]]): Unit =
	{
		// Displays a settings dialog for the new paths
		val readSettingsFrame = new FileReadSettingsFrame(target)
		val settings = readSettingsFrame.display().waitFor().getOrElse(Vector())
		if (settings.nonEmpty)
		{
			// Checks which of the settings still require key mappings
			val (existingSaleMappings, existingPriceMappings) = connectionPool.tryWith { implicit c =>
				// Reverses resulting vectors in order to read data from most recent to oldest
				val saleMappings = DbSaleKeyMappings.all.reverse
				val priceMappings = DbPriceKeyMappings.all.reverse
				saleMappings -> priceMappings
			} match
			{
				case Success(data) => data
				case Failure(error) =>
					// TODO: Display error message
					Log(error, "Failed to read existing key mappings")
					Vector() -> Vector()
			}
			
			val (priceSettings, saleSettings) = settings.divideBy { _.inputType == SaleGroup }
			
			// Groups settings based on whether existing key mappings may be used
			// TODO: WET WET
			val saleSettingsWithExistingMappings = saleSettings.map { setting =>
				setting -> existingSaleMappings.filter { _.shopId == setting.shop.id }
			}
			val priceSettingsWithExistingMappings = priceSettings.map { setting =>
				setting -> existingPriceMappings.filter { mapping => mapping.shopId == setting.shop.id &&
					mapping.priceType.matches(setting.inputType) }
			}
			val (saleSettingsWithoutMappings, saleSettingsWithMappings) = saleSettingsWithExistingMappings
				.dividedWith { case (setting, mappings) =>
					if (mappings.isEmpty) Left(setting) else Right(setting -> mappings)
				}
			val (priceSettingsWithoutMappings, priceSettingsWithMappings) = priceSettingsWithExistingMappings
				.dividedWith { case (setting, mappings) =>
					if (mappings.isEmpty) Left(setting) else Right(setting -> mappings)
				}
			
			// Creates input dialogs for settings which don't yet have a suitable mapping
			val saleInputs = saleSettingsWithoutMappings.map { setting =>
				new DataSourceInput[SaleGroupData, SaleKeyMappingData](setting,
					SaleKeyMappingFromFieldsFactory(setting.shop.id))({ case (p, m) => DataProcessor.forSaleGroups(p, m)})
			}
			val priceInputs = priceSettingsWithoutMappings.map { setting =>
				val (factory, priceType) = setting.inputType match
				{
					case SalePrice => PriceKeyMappingFromFieldsFactory.forNetPricesInShopWithId(setting.shop.id) -> Net
					case _ => PriceKeyMappingFromFieldsFactory.forBasePricesInShopWithId(setting.shop.id) -> Base
				}
				new DataSourceInput[ShopProductData, PriceKeyMappingData](setting, factory)({ case (p, m) =>
					DataProcessor.forPrices(p, m, priceType, setting.isProperlySorted) })
			}
			val allInputs = saleInputs ++ priceInputs
			
			var nextDisplayIndex = 0
			var isCancelled = false
			while (!isCancelled && nextDisplayIndex >= 0 && nextDisplayIndex < allInputs.size)
			{
				allInputs(nextDisplayIndex).displayBlocking() match
				{
					case Some(direction) => nextDisplayIndex += 1 * direction.modifier
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
					// Displays a loading view during this process
					val progressPointer = new PointerWithEvents(
						ProgressState.initial("Tallennetaan uusia asetuksia"))
					val loadCompletion = new LoadingView(progressPointer).display()
					
					connectionPool.tryWith { implicit connection =>
						// Inserts new mappings to the database
						if (saleInputs.nonEmpty || priceInputs.nonEmpty)
						{
							val progressPerInput = 0.1 / (saleInputs.size + priceInputs.size)
							println(s"Saving ${saleInputs.size} sale read settings")
							saleInputs.foreach { input =>
								input.result.foreach { processor => DbSaleKeyMappings.insert(processor.mapping) }
								progressPointer.update { p => p.copy(progress = p.progress + progressPerInput) }
							}
							println(s"Saving ${priceInputs.size} price read settings")
							priceInputs.foreach { input =>
								input.result.foreach { processor => DbPriceKeyMappings.insert(processor.mapping) }
								progressPointer.update { p => p.copy(progress = p.progress + progressPerInput) }
							}
						}
						
						// Processes data from all recognized files, then moves the files to the history folder
						// TODO: Add a version that uses different mappings until one succeeds
						//  (now uses the most recent mapping)
						val existingSaleProcessors = saleSettingsWithMappings.map { case (setting, mappings) =>
							DataProcessor.forSaleGroups(setting.path, mappings.head: SaleKeyMappingData) }
						// TODO: Specify whether document is ordered and contains all rows in range (now true)
						val existingPriceProcessors = priceSettingsWithMappings.map { case (setting, mappings) =>
							DataProcessor.forPrices(setting.path, mappings.head: PriceKeyMappingData,
								if (setting.inputType == SalePrice) Net else Base)
						}
						val allProcessors = allInputs.flatMap { _.result } ++ existingSaleProcessors ++
							existingPriceProcessors
						println(s"Using ${allProcessors.size} data processors")
						
						val failuresBuilder = new VectorBuilder[(Path, Throwable)]()
						allProcessors.zipWithIndex.foreach { case (processor, index) =>
							progressPointer.value = ProgressState(0.1 + (index / allProcessors.size.toDouble) * 0.9,
								"Käsitellään tiedostoa ${file} (${current}/${max})".autoLocalized
									.interpolated(Map("file" -> processor.filePath.fileName,
										"current" -> (index + 1).toString, "max" -> allProcessors.size.toString)))
							println(s"Processing ${processor.filePath}")
							processor().flatMap { _ => processor.filePath.moveTo(Globals.fileHistoryDirectory) }
								.failure.foreach { failuresBuilder += processor.filePath -> _ }
						}
						println("Processing completed")
						progressPointer.value = ProgressState.finished("Kaikki tiedostot käsitelty")
						
						// Shows an error message if some reads failed
						val failures = failuresBuilder.result()
						failures.foreach { case (path, error) => Log(error, s"Failed to process input file $path") }
						if (failures.nonEmpty)
						{
							Fields.errorDialog("${errorCount} tiedoston käsittely epännistui.\nTiedostot: ${files}"
								.autoLocalized.interpolated(Map("errorCount" -> failures.size,
								"files" -> failures.map { _._1.fileName }.mkString(", ")))).display().waitFor()
						}
					}.failure.foreach { error =>
						progressPointer.value = ProgressState.finished("Käsittely keskeytyi")
						Log(error, "Failed to import or configure target files")
						Fields.errorDialog("Tiedostojen käsittely epäonnistui.\nVirheilmoitus: %s"
							.autoLocalized.interpolated(Vector(error.getLocalizedMessage))).display().waitFor()
					}
					
					loadCompletion.waitFor()
				}
			}
		}
	}
	
	
	// NESTED   ---------------------------
	
	private class DataSourceInput[A, M <: KeyMapping[A]](val setting: FileReadSetting,
														 mappingFactory: KeyMappingFactory[A, M])
									(makeProcessor: (Path, M) => DataProcessor[A, M])
	{
		// ATTRIBUTES   -------------------
		
		// Finds the first row in target document that is suitable for serving as the header
		private lazy val headerRow: Option[Vector[String]] =
		{
			val requiredColumnCount = mappingFactory.fieldNames.count { _._2 }
			println(s"Requires $requiredColumnCount columns in ${setting.path.fileName}")
			/*(*/setting.path.fileType.toLowerCase match
			{
					// TODO: Remove test prints
				case "csv" =>
					println("Csv file")
					CsvReader.iterateRawRowsIn(setting.path) { rowsIter =>
						if (rowsIter.isEmpty)
							println("Empty row iterator")
						rowsIter.find { _.count { _.nonEmpty } >= requiredColumnCount }
					} match
					{
						case Success(result) =>
							println(s"Csv headers: $result")
							result
						case Failure(error) =>
							Log(error, s"Headers search failed in ${setting.path}")
							None
					}
				case _ =>
					println("Excel file")
					ReadExcel.withoutHeadersFromSheetAtIndex(setting.path, 0) match
					{
						case Success(rows) =>
							println(s"Found ${rows.size} rows")
							val targetRow = rows.find { _.count { _.string.exists { _.nonEmpty } } >= requiredColumnCount }
							println(s"Header row: $targetRow")
							targetRow.map { _.map { _.getString } }
						case Failure(error) =>
							Log(error, s"Failed to read headers from ${setting.path}")
							None
					}
			}// ).map { _.map(CleanInput.apply) }
		}
		
		private var lastDialog: Option[DataProcessorWindowLike[A, M, _]] = None
		private var lastResult: Option[DataProcessor[A, M]] = None
		
		
		// COMPUTED -----------------------
		
		def result = lastResult
		
		
		// OTHER    -----------------------
		
		// Returns next display direction. None if user cancelled process
		def displayBlocking() =
		{
			// Creates a new dialog
			// Tries to read some data from the excel file in order to present a user-friendly dialog
			val progressPointer = new PointerWithEvents(ProgressState.initial(
				"Luetaan tiedostoa %s".autoLocalized.interpolated(Vector(setting.path.fileName))))
			val loadCompletion = new LoadingView(progressPointer).display()
			val headers = headerRow
			progressPointer.value = ProgressState(0.85, "Valmistellaan dialogia")
			val newDialog = headers match
			{
				case Some(headers) =>
					new DataProcessorWindowWithSelections[A, M](setting.path, setting.shop, mappingFactory, headers)(makeProcessor)
				case None => new DataProcessorWindowWithTextFields[A, M](setting.path, setting.shop, mappingFactory)(makeProcessor)
			}
			
			// Pre-fills some content, if possible
			lastDialog.foreach { d => newDialog.input = d.input }
			lastDialog = Some(newDialog)
			
			progressPointer.value = ProgressState.finished("Käyttöliittymä valmis")
			
			// Displays the dialog and waits for a result
			loadCompletion.waitFor()
			newDialog.display().waitFor() match
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
