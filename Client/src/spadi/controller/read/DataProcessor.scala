package spadi.controller.read

import java.nio.file.Path

import spadi.controller.database.access.multi.DbProducts
import spadi.controller.database.access.single.DbSaleGroup
import spadi.model.cached.read.KeyMapping
import spadi.model.enumeration.PriceType
import spadi.model.partial.pricing.{SaleGroupData, ShopProductData}
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.parse.CsvReader
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.vault.database.Connection

import scala.io.Codec
import scala.util.{Failure, Success, Try}

object DataProcessor
{
	/**
	  * Creates a new data processor that targets sale group documents
	  * @param filePath Document path
	  * @param mapping Mapping for reading sale group data
	  * @return A new data processor
	  */
	def forSaleGroups[M <: KeyMapping[SaleGroupData]](filePath: Path, mapping: M) =
		DataProcessor[SaleGroupData, M](filePath, mapping, 100) { (sales, connection) =>
			implicit val c: Connection = connection
			sales.foreach { sale =>
				sale.priceModifier.foreach { priceMod =>
					DbSaleGroup.inShopWithId(sale.shopId).withIdentifier(sale.groupIdentifier).amount_=(priceMod)
				}
			}
		}
	
	/**
	  * Creates a new data processor that targets product price documents
	  * @param filePath Document path
	  * @param mapping Mapping for reading product data from the documents
	  * @param priceType The expected price content type
	  * @param isFullyInclusiveDocument Whether the specified document is ordered by product electric id and contains
	  *                                 all of targeted shop's products in the specified electric id range
	  *                                 (default = true)
	  * @return A new data processor
	  */
	def forPrices[M <: KeyMapping[ShopProductData]](filePath: Path, mapping: M, priceType: PriceType,
													isFullyInclusiveDocument: Boolean = true) =
		DataProcessor[ShopProductData, M](filePath, mapping, if (isFullyInclusiveDocument) 10000 else 100) { (prices, connection) =>
			implicit val c: Connection = connection
			DbProducts.insertData(prices, priceType, isFullyInclusiveDocument)
		}
}

/**
 * Constains instructions and logic for parsing input data
 * @author Mikko Hilpinen
 * @since 4.8.2020, v1.2
 * @param filePath Path where the data is read
 * @param mapping Mapping used for interpreting column data and producing output
  * @param parse A function called for each processed item
 */
case class DataProcessor[A, +M <: KeyMapping[A]](filePath: Path, mapping: M, suggestedInputSize: Int)
												(parse: (Vector[A], Connection) => Unit)
{
	private implicit val csvEncoding: Codec = Codec.UTF8
	
	/**
	  * Processes the following read model
	  * @param models Models to parse and process
	  * @param connection DB Connection
	  * @return Success on model parse success. Failure otherwise.
	  */
	def apply(models: Vector[Model[Property]])(implicit connection: Connection) =
		models.tryMap { mapping(_) }.map { parse(_, connection) }
	
	/**
	  * Reads targeted file and processes its contents
	  * @param connection Database connection
	  * @return Success on file read success. Failure otherwise.
	  */
	def apply()(implicit connection: Connection): Try[Unit] =
	{
		// Reads file contents using either csv parsing or excel row processing
		filePath.fileType.toLowerCase match
		{
			case "csv" =>
				// TODO: Handle csv line separator (now expects ;)
				CsvReader.iterateLinesIn(filePath) { processModels(_) }.flatten
			case _ =>
				ReadExcel.fromSheetAtIndex(filePath, 0, mapping.requiredKeys.size)
					.flatMap { models => processModels(models.iterator) }
		}
	}
	
	private def processModels(iterator: Iterator[Model[Constant]])(implicit connection: Connection) =
	{
		var failure: Option[Throwable] = None
		while (iterator.hasNext && failure.isEmpty)
		{
			failure = apply(iterator.takeNext(suggestedInputSize)).failure
		}
		failure match
		{
			case Some(error) => Failure(error)
			case None => Success(())
		}
	}
}
