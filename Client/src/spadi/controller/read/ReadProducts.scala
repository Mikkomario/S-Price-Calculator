package spadi.controller.read

import java.nio.file.Path
import java.time.Instant

import spadi.controller.container.{LocalContainer, ShopData}
import spadi.model.cached.ProgressState
import spadi.model.cached.pricing.product.{ProductBasePrice, ProductPrice, SalesGroup}
import spadi.model.cached.pricing.shop.ShopSetup
import spadi.model.cached.read.{DataSource, SheetTarget}
import spadi.view.util.Setup._
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.reflection.localization.LocalString._

import scala.concurrent.Future
import scala.util.Success

/**
  * Reads product related data from excel files and stores them locally
  * @author Mikko Hilpinen
  * @since 9.5.2020, v1
  */
object ReadProducts
{
	// ATTRIBUTES   --------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val inputDirectory: Path = "luettavat-tiedostot"
	private val supportedFileTypes = ReadExcel.supportedFileTypes.map
	{_.toLowerCase}
	
	
	// COMPUTED ------------------------------
	
	private def targetFiles = inputDirectory.findDescendants
	{ file =>
		file.isRegularFile &&
			supportedFileTypes.contains(file.fileType.toLowerCase)
	}
	
	
	// OTHER    ------------------------------
	
	/**
	  * Reads product data from input excel files.
	  * @return A: Future with either <br>
	  *         - Right (Option[ Shop -> Try[...] ]): Shop -> Either product prices (right (Try[ Vector[ProductPrice] ]))
	  *           or base prices and sales groups (left (Try[ Vector[ProductBasePrice] -> Vector[SalesGroup] ]) -
	  *           Failure if reads failed. None if no data refresh was required. OR<br>
	  *         - Left (Vector[Path]): Files that require new mappings<br>
	  *           B: Pointer that holds current read progress
	  */
	def async() =
	{
		val progressPointer = new PointerWithEvents(ProgressState.initial("Haetaan luettavia tiedostoja"))
		
		Future
		{
			// Checks the input files, whether there were any new, removed or modified cases
			targetFiles.map
			{ allFiles =>
				// Checks for new (unmapped) files first
				val setups = ShopData.shopSetups
				val mappedPaths = setups.flatMap
				{_.paths}.toSet
				val unmappedPaths = allFiles.filterNot(mappedPaths.contains)
				
				if (unmappedPaths.nonEmpty)
				{
					progressPointer.value = ProgressState.finished("Tiedostot pitää ensin tunnistaa")
					Left(unmappedPaths)
				}
				else if (allFiles.isEmpty)
				{
					progressPointer.value = ProgressState.finished("Kansio on tyhjä")
					Right(None)
				}
				else
				{
					progressPointer.value = ProgressState(0.1,
						"%i tiedostoa löydetty".autoLocalized.interpolated(Vector(allFiles.size)))
					Right(readFiles(allFiles, setups, progressPointer))
				}
			}
		} -> progressPointer.view
	}
	
	/**
	  * Reads product data from input excel files. Will only read data from files already linked to key mappings
	  * @return A: Future that will contain Try [ Option[ Shop -> Try[...] ] ]: Shop ->
	  *         Either product prices (Right (Try[ Vector[ProductPrice] ]))
	  *         or base prices and sales groups (Left (Try[ Vector[ProductBasePrice] -> Vector[SalesGroup] ]) -
	  *         Failure if reads failed. None if no data refresh was required.<br>
	  *         B: Pointer that holds current progress
	  */
	def asyncIgnoringUnmappedFiles() =
	{
		// Checks the input files, whether there were any removed or modified cases
		val progressPointer = new PointerWithEvents(ProgressState.initial("Haetaan luettavia tiedostoja"))
		Future
		{
			targetFiles.map
			{ allFiles =>
				if (allFiles.isEmpty)
				{
					progressPointer.value = ProgressState.finished("Kansio on tyhjä")
					None
				}
				else
				{
					progressPointer.value = ProgressState(0.1,
						"%i tiedostoa löydetty".autoLocalized.interpolated(Vector(allFiles.size)))
					readFiles(allFiles, ShopData.shopSetups, progressPointer)
				}
			}
		} -> progressPointer.view
	}
	
	/**
	  * Reads product data from input excel files.
	  * @return Either <br>
	  *         - Right (Option[ Shop -> Try[...] ]): Shop -> Either product prices (right (Try[ Vector[ProductPrice] ]))
	  *           or base prices and sales groups (left (Try[ Vector[ProductBasePrice] -> Vector[SalesGroup] ]) -
	  *           Failure if reads failed. None if no data refresh was required. OR<br>
	  *         - Left (Vector[Path]): Files that require new mappings
	  */
	def apply() =
	{
		// Checks the input files, whether there were any new, removed or modified cases
		targetFiles.map
		{ allFiles =>
			println(s"${allFiles.size} input files available")
			allFiles.foreach
			{ f => println(s"\t- $f") }
			// Checks for new (unmapped) files first
			val setups = ShopData.shopSetups
			val mappedPaths = setups.flatMap
			{_.paths}.toSet
			println(s"Mapped paths (${mappedPaths.size}):")
			mappedPaths.foreach
			{ f => println(s"\t- $f") }
			val unmappedPaths = allFiles.filterNot(mappedPaths.contains)
			println(s"${unmappedPaths.size} unmapped files")
			if (unmappedPaths.nonEmpty)
				Left(unmappedPaths)
			else
				Right(readFiles(allFiles, setups))
		}
	}
	
	/**
	  * Reads product data from input excel files. Will only read data from files already linked to key mappings
	  * @return Try [ Option[ Shop -> Try[...] ] ]: Shop -> Either product prices (Right (Try[ Vector[ProductPrice] ]))
	  *         or base prices and sales groups (Left (Try[ Vector[ProductBasePrice] -> Vector[SalesGroup] ]) -
	  *         Failure if reads failed. None if no data refresh was required.
	  */
	def ignoringUnmappedFiles() =
	{
		// Checks the input files, whether there were any removed or modified cases
		targetFiles.map
		{ allFiles => readFiles(allFiles, ShopData.shopSetups) }
	}
	
	private def readFiles(files: Iterable[Path], setups: Vector[ShopSetup], progressPointer: PointerWithEvents[ProgressState]) =
	{
		// Next checks whether any of the files were modified, deleted or added since last read
		if (FileReads.current.exists
		{ case (path, lastReadTime) => files.find
		{_ == path}.forall
		{
			_.lastModified.toOption.forall
			{_ > lastReadTime}
		}
		} || files.exists
		{!FileReads.contains(_)})
		{
			val setupsToUse = setups.filter
			{
				_.paths.forall
				{_.exists}
			}
			
			// Updates progress after files have been recognized (10%)
			val startProgress = progressPointer.value.progress
			val methodProgress = 1 - startProgress
			val fileCheckProgress = methodProgress * 0.1
			val progressPerFile = (methodProgress * 0.9 - fileCheckProgress) / files.size
			progressPointer.value = ProgressState(startProgress + fileCheckProgress,
				"Luetaan %i tukun tiedot".autoLocalized.interpolated(Vector(setupsToUse.size)))
			
			// Re-reads all data (90% progress)
			val result = setupsToUse.map
			{ setup =>
				val readData = setup.dataSource
					.mapBoth
					{ case (baseSource, saleSource) =>
						read(baseSource, progressPointer, progressPerFile)
							.flatMap
							{ base =>
								read(saleSource, progressPointer, progressPerFile)
									.map
									{ sale => base -> sale }
							}
					}
					{ comboSource =>
						read(comboSource)
					}.mapToSingle
				{
					_.map[Either[(Vector[ProductBasePrice], Vector[SalesGroup]), Vector[ProductPrice]]]
						{Left(_)}
				}
				{
					_.map
					{Right(_)}
				}
				setup.shop -> readData
			}
			
			// Records data read (read time is set slightly into the future since file closing will
			// update file modified time (10% progress)
			val readTime = Instant.now() + 10.seconds
			FileReads.current = setupsToUse.flatMap
			{_.paths}.map
			{_ -> readTime}.toMap
			
			progressPointer.value = ProgressState.finished("Kaikki tiedostot käsitelty ja status tallennettu")
			Some(result)
		}
		else
		{
			progressPointer.value = ProgressState.finished("Tiedostoja ei tarvinnut lukea uudelleen")
			None
		}
	}
	
	private def readFiles(files: Iterable[Path], setups: Vector[ShopSetup]) =
	{
		// Next checks whether any of the files were modified, deleted or added since last read
		if (FileReads.current.exists
		{ case (path, lastReadTime) => files.find
		{_ == path}.forall
		{
			_.lastModified.toOption.forall
			{_ > lastReadTime}
		}
		} || files.exists
		{!FileReads.contains(_)})
		{
			println("Some files were modified, deleted or added")
			
			val setupsToUse = setups.filter
			{
				_.paths.forall
				{_.exists}
			}
			
			println(s"Reading data from ${setupsToUse.size} setups")
			
			// In which case re-reads all data
			val result = setupsToUse.map
			{ setup =>
				val readData = setup.dataSource.mapBoth
				{ case (baseSource, saleSource) =>
					read(baseSource).flatMap
					{ base => read(saleSource).map
					{ sale => base -> sale }
					}
				}
				{ comboSource => read(comboSource) }.mapToSingle
				{
					_.map[Either[(Vector[ProductBasePrice], Vector[SalesGroup]), Vector[ProductPrice]]]
						{
							Left(_)
						}
				}
				{
					_.map
					{Right(_)}
				}
				setup.shop -> readData
			}
			
			// Records data read (read time is set slightly into the future since file closing will
			// update file modified time
			val readTime = Instant.now() + 10.seconds
			FileReads.current = setupsToUse.flatMap
			{_.paths}.map
			{_ -> readTime}.toMap
			
			Some(result)
		}
		else
			None
	}
	
	private def read[A](source: DataSource[A], progressPointer: PointerWithEvents[ProgressState], progressAdvance: Double) =
	{
		val startProgress = progressPointer.value.progress
		val fileName = source.filePath.fileName
		
		val target = SheetTarget.sheetAtIndex(0, source.firstDataRowIndex -> 0,
			cellHeadersRowIndex = source.headerRowIndex)
		// Reads all models from the excel file (90% of progress)
		val result = ReadExcel.from(source.filePath, target).flatMap
		{ models =>
			progressPointer.value = ProgressState(startProgress + progressAdvance * 0.9,
				"${amount} riviä luettu tiedostosta ${fileName}".autoLocalized.interpolated(
					Map("amount" -> models.size, "fileName" -> fileName)))
			// Then parses the model data (10% of progress)
			models.tryMap(source.mapping.apply)
		}
		if (result.isSuccess)
			progressPointer.value = ProgressState(startProgress + progressAdvance,
				"%s käsitelty".autoLocalized.interpolated(Vector(fileName)))
		else
			progressPointer.value = ProgressState(startProgress + progressAdvance,
				"%s käsittely epäonnistui".autoLocalized.interpolated(Vector(fileName)))
		result
	}
	
	private def read[A](source: DataSource[A]) =
	{
		val target = SheetTarget.sheetAtIndex(0, source.firstDataRowIndex -> 0,
			cellHeadersRowIndex = source.headerRowIndex)
		ReadExcel.from(source.filePath, target).flatMap
		{ models => models.tryMap(source.mapping.apply) }
	}
	
	
	// NESTED   --------------------------
	
	private object FileReads extends LocalContainer[Map[Path, Instant]]("read-status.json")
	{
		// IMPLEMENTED  --------------------------
		
		override protected def toJsonValue(item: Map[Path, Instant]) = Model.fromMap(
			item.map
			{ case (path, time) => path.toJson -> time })
		
		override protected def fromJsonValue(value: Value) = Success(
			value.getModel.attributeMap.map
			{ case (pathString, const) => (pathString: Path) -> const.value.getInstant })
		
		override protected def empty = Map()
		
		
		// OTHER    ------------------------------
		
		/**
		  * @param path A path
		  * @return Last read time for that path. None if the file wasn't read
		  */
		def apply(path: Path) = current.get(path)
		
		/**
		  * @param path A path
		  * @return Whether that path has been read
		  */
		def contains(path: Path) = current.contains(path)
	}
	
}
