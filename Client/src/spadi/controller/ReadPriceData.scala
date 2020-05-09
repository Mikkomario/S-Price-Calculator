package spadi.controller

import java.nio.file.Path
import java.time.Instant

import spadi.model.{KeyMapping, Product, ProductPrice, ProductSalePrice, SalesGroup}
import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration, Value}
import utopia.flow.generic.{FromModelFactoryWithSchema, InstantType, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.CollectionExtensions._

import scala.util.{Failure, Success, Try}

/**
 * Reads product related data from excel files and stores them locally
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object ReadPriceData
{
	// ATTRIBUTES   --------------------------
	
	private val inputDirectory: Path = "input"
	
	
	// OTHER    ------------------------------
	
	/**
	 * Reads product price data from the file system + local containers
	 * @return Read products + possible errors encountered during reads. Failure if a failure made product
	 *         reading impossible
	 */
	def apply() =
	{
		// Reads the products first
		read(Price).map { case (prices, priceErrors) =>
			// Updates products to local data
			Prices.current = prices
			// Next reads the sales
			read(Sale) match
			{
				case Success(saleData) =>
					val sales = saleData._1
					val allErrors = priceErrors ++ saleData._2
					// Updates local data
					Sales.current = sales
					// Combines prices with sales
					val salePrices = prices.flatMap { basePrice =>
						val productSales = sales.filter { _.salesGroupId == basePrice.salesGroupId }
						if (productSales.nonEmpty)
							productSales.map { sale => ProductSalePrice(basePrice, Some(sale)) }
						else
							Some(ProductSalePrice(basePrice, None))
					}
					// Combines products based on id
					val products = salePrices.groupBy { _.basePrice.productId }.map { case (id, prices) =>
						Product(id, prices.toSet) }.toSet
					products -> allErrors
				case Failure(error) =>
					// If sale reading failed, returns products without sale prices
					prices.groupBy { _.productId }.map { case (id, prices) => Product(id,
						prices.map { ProductSalePrice(_, None) }.toSet) }.toSet -> (priceErrors :+ error)
			}
		}
	}
	
	private def read[A](targetType: TargetType[A]): Try[(Vector[A], Vector[Throwable])] =
	{
		// Checks which files are available
		targetType.inputDirectory.children.flatMap { files =>
			// Only handles excel files
			val excelFiles = files.filter { f => f.isRegularFile && ReadExcel.supportedFileTypes.exists { _ ~== f.fileType } }
			// If some files were deleted or modifier since last read, reads all data again
			val lastReadStatus = FileReads.current(targetType)
			val wasModified = lastReadStatus.exists { lastRead =>
				excelFiles.find { _ == lastRead.path }.forall { newVersion =>
					newVersion.lastModified.toOption.forall { _ > lastRead.lastReadTime }
				}
			}
			val readTime = Instant.now()
			if (wasModified)
			{
				val result = read(files, targetType)
				// Records new read status
				if (result.isSuccess)
					FileReads.current += targetType -> files.map { LastFileRead(_, readTime) }
				result
			}
			// Otherwise only appends data from new files
			else
			{
				val newFiles = files.filterNot { f => lastReadStatus.exists { _.path == f } }
				val existingData = targetType.container.current
				if (newFiles.isEmpty)
					Success(existingData -> Vector())
				else
				{
					read(newFiles, targetType) match
					{
						case Success(newReads) =>
							// Records read status
							FileReads.current += targetType -> (lastReadStatus ++ newFiles.map { LastFileRead(_, readTime) })
							Success(existingData ++ newReads._1 -> newReads._2)
						case Failure(readFailure) =>
							if (existingData.isEmpty)
								Failure(readFailure)
							else
								Success(existingData -> Vector(readFailure))
					}
				}
			}
		}
	}
	
	private def read[A](files: Vector[Path], targetType: TargetType[A]): Try[(Vector[A], Vector[Throwable])] =
	{
		val (failures, successes) = files.map { read(_, targetType) }.divideBy { _.isSuccess }
		val readModels = successes.flatMap { _.get }
		if (readModels.nonEmpty || failures.isEmpty)
			Success(readModels -> failures.map { _.failure.get })
		else
			Failure(failures.head.failure.get)
	}
	
	private def read[A](file: Path, targetType: TargetType[A]) =
	{
		ReadExcel.from(file, targetType.sheetTarget).flatMap { rows => tryMap(rows, targetType.mappings) }
	}
	
	private def tryMap[A](rows: Vector[Model[Constant]], mappings: IterableOnce[KeyMapping[A]]): Try[Vector[A]] =
	{
		val iter = mappings.iterator.map { tryMap(rows, _) }
		var lastResult = iter.next()
		while (lastResult.isFailure && iter.hasNext)
		{
			lastResult = iter.next()
		}
		lastResult
	}
	
	private def tryMap[A](rows: Vector[Model[Constant]], mapping: KeyMapping[A]) = rows.tryMap { mapping(_) }
	
	
	// NESTED   ------------------------------
	
	private object TargetType
	{
		val values = Vector[TargetType[_]](Price, Sale)
	}
	private sealed trait TargetType[A]
	{
		def sheetTarget: SheetTarget
		def mappings: Vector[KeyMapping[A]]
		def inputDirectory: Path
		def name: String
		def container: LocalContainer[Vector[A]]
	}
	private object Price extends TargetType[ProductPrice]
	{
		override val sheetTarget = SheetTarget.sheetAtIndex(0, 3 -> 0, cellHeadersRowIndex = 2)
		
		override def mappings = PriceKeyMappings.current
		
		override def inputDirectory = ReadPriceData.inputDirectory / "products"
		
		override def name = "product"
		
		override def container = Prices
	}
	private object Sale extends TargetType[SalesGroup]
	{
		override val sheetTarget = SheetTarget.sheetAtIndex(0, 2 -> 0, cellHeadersRowIndex = 1)
		
		override def mappings = SalesKeyMappings.current
		
		override def inputDirectory = ReadPriceData.inputDirectory / "sales"
		
		override def name = "sale"
		
		override def container = Sales
	}
	
	private object FileReads extends LocalContainer[Map[TargetType[_], Vector[LastFileRead]]]
	{
		override protected def fileName = "read-status.json"
		
		override protected def toJsonValue(item: Map[TargetType[_], Vector[LastFileRead]]) =
		{
			Model(item.map { case (target, reads) => target.name -> (reads.map { _.toModel }: Value) })
		}
		
		override protected def fromJsonValue(value: Value) =
		{
			val model = value.getModel
			val readData = TargetType.values.map { target =>
				// Ignores parsing failures within individual status rows
				val reads = model(target.name).getVector.flatMap { v => v.model.flatMap { readModel =>
					LastFileRead(readModel).toOption } }
				target -> reads
			}.toMap
			Success(readData)
		}
		
		override protected val empty = TargetType.values.map { t => t -> Vector[LastFileRead]() }.toMap
	}
	
	private object LastFileRead extends FromModelFactoryWithSchema[LastFileRead]
	{
		override val schema = ModelDeclaration("path" -> StringType, "read_time" -> InstantType)
		
		override protected def fromValidatedModel(model: Model[Constant]) = LastFileRead(
			model("path").getString, model("read_time"))
	}
	
	private case class LastFileRead(path: Path, lastReadTime: Instant) extends ModelConvertible
	{
		override def toModel = Model(Vector("path" -> path.toString, "read_time" -> lastReadTime))
	}
}
