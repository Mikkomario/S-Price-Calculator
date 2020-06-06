package spadi.controller

import java.nio.file.Path
import java.time.Instant

import spadi.model.{DataSource, ProductBasePrice, ProductPrice, SalesGroup, ShopSetup}
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.CollectionExtensions._

import scala.util.Success

/**
 * Reads product related data from excel files and stores them locally
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object ReadProducts
{
	// ATTRIBUTES   --------------------------
	
	private val inputDirectory: Path = "luettavat-tiedostot"
	private val supportedFileTypes = ReadExcel.supportedFileTypes.map { _.toLowerCase }
	
	
	// COMPUTED ------------------------------
	
	private def targetFiles = inputDirectory.findDescendants { file => file.isRegularFile &&
		supportedFileTypes.contains(file.fileType.toLowerCase) }
	
	
	// OTHER    ------------------------------
	
	/**
	 * Reads product data from input excel files.
	 * @return Either <br>
	 *         - Right (Option[ Shop -> Try[...] ]): Shop -> Either product prices (right (Try[ Vector[ProductPrice] ]))
	 *         or base prices and sales groups (left (Try[ Vector[ProductBasePrice] -> Vector[SalesGroup] ]) -
	 *         Failure if reads failed. None if no data refresh was required. OR<br>
	 *         - Left (Vector[Path]): Files that require new mappings
	 */
	def apply() =
	{
		// Checks the input files, whether there were any new, removed or modified cases
		targetFiles.map { allFiles =>
				// Checks for new (unmapped) files first
				val setups = ShopData.shopSetups
				val mappedPaths = setups.flatMap { _.paths }.toSet
				val unmappedPaths = allFiles.filterNot(mappedPaths.contains)
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
		targetFiles.map { allFiles => readFiles(allFiles, ShopData.shopSetups) }
	}
	
	private def readFiles(files: Iterable[Path], setups: Vector[ShopSetup]) =
	{
		// Next checks whether any of the files were modified, deleted or added since last read
		if (FileReads.current.exists { case (path, lastReadTime) => files.find { _ == path }.forall {
			_.lastModified.toOption.forall { _ > lastReadTime } } } ||
			files.exists { !FileReads.contains(_) })
		{
			val setupsToUse = setups.filter { _.paths.forall { _.exists } }
			
			// In which case re-reads all data
			val result = setupsToUse.map { setup =>
				val readData = setup.dataSource.mapBoth { case (baseSource, saleSource) =>
					read(baseSource).flatMap { base => read(saleSource).map { sale => base -> sale } }
				} { comboSource => read(comboSource) }.mapToSingle {
					_.map[Either[(Vector[ProductBasePrice], Vector[SalesGroup]), Vector[ProductPrice]]] {
						Left(_) } } { _.map { Right(_) } }
				setup.shop -> readData
			}
			
			// Records data read (read time is set slightly into the future since file closing will
			// update file modified time
			val readTime = Instant.now() + 10.seconds
			FileReads.current = setupsToUse.flatMap { _.paths }.map { _ -> readTime }.toMap
			
			Some(result)
		}
		else
			None
	}
	
	private def read[A](source: DataSource[A]) =
	{
		val target = SheetTarget.sheetAtIndex(0, source.firstDataRowIndex -> 0,
			cellHeadersRowIndex = source.headerRowIndex)
		ReadExcel.from(source.filePath, target).flatMap { models => models.tryMap(source.mapping.apply) }
	}
	
	
	private object FileReads extends LocalContainer[Map[Path, Instant]]("read-status.json")
	{
		// IMPLEMENTED  --------------------------
		
		override protected def toJsonValue(item: Map[Path, Instant]) = Model.fromMap(
			item.map { case (path, time) => path.toJson -> time })
		
		override protected def fromJsonValue(value: Value) = Success(
			value.getModel.attributeMap.map { case (pathString, const) => (pathString: Path) -> const.value.getInstant })
		
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
	
	/*
	private object LastFileRead extends FromModelFactoryWithSchema[LastFileRead]
	{
		override val schema = ModelDeclaration("path" -> StringType, "read_time" -> InstantType)
		
		override protected def fromValidatedModel(model: Model[Constant]) = LastFileRead(
			model("path").getString, model("read_time"))
	}
	
	private case class LastFileRead(path: Path, lastReadTime: Instant) extends ModelConvertible
	{
		override def toModel = Model(Vector("path" -> path.toString, "read_time" -> lastReadTime))
	}*/
}
