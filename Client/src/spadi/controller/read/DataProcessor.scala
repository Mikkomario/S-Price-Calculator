package spadi.controller.read

import java.nio.file.Path

import spadi.controller.database.access.multi.DbProducts
import spadi.controller.database.access.single.DbSaleGroup
import spadi.model.cached.read.KeyMapping
import spadi.model.partial.pricing.{ProductData, SaleGroupData}
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.parse.CsvReader
import utopia.flow.util.FileExtensions._
import utopia.vault.database.Connection

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
		DataProcessor[SaleGroupData, M](filePath, mapping) { (sale, connection) =>
			implicit val c: Connection = connection
			sale.priceModifier.foreach { priceMod =>
				DbSaleGroup.inShopWithId(sale.shopId).withIdentifier(sale.groupIdentifier).amount_=(priceMod)
			}
		}
	
	/**
	  * Creates a new data processor that targets product price documents
	  * @param filePath Document path
	  * @param mapping Mapping for reading product data from the documents
	  * @return A new data processor
	  */
	def forPrices[M <: KeyMapping[ProductData]](filePath: Path, mapping: M) =
		DataProcessor[ProductData, M](filePath, mapping) { (price, connection) =>
			implicit val c: Connection = connection
			DbProducts.insertData(price)
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
case class DataProcessor[A, +M <: KeyMapping[A]](filePath: Path, mapping: M)(parse: (A, Connection) => Unit)
{
	/**
	  * Processes the following read model
	  * @param model Model to parse and process
	  * @param connection DB Connection
	  * @return Success on model parse success. Failure otherwise.
	  */
	def apply(model: Model[Property])(implicit connection: Connection) =
		mapping(model).map { parse(_, connection) }
	
	/**
	  * Reads targeted file and processes its contents
	  * @param connection Database connection
	  * @return Success on file read success. Failure otherwise.
	  */
	def apply()(implicit connection: Connection): Try[Unit] =
	{
		// Collects some data from parsing results
		var firstParseFailure: Option[Throwable] = None
		var hadSuccess = false
		def processModel(model: Model[Property]) = apply(model) match
		{
			case Success(_) => hadSuccess = true
			case Failure(error) =>
				error.printStackTrace() // TODO: Remove test prints
				if (firstParseFailure.isEmpty) firstParseFailure = Some(error)
		}
		
		// TODO: Handle csv line separator (now expects ;)
		// Reads file contents using either csv parsing or excel row processing
		(filePath.fileType.toLowerCase match
		{
			case "csv" => CsvReader.foreachLine(filePath)(processModel)
			case _ => ReadExcel.foreachRowInSheetAtIndex(filePath, 0, mapping.requiredKeys.size)(processModel)
		}).flatMap { _ =>
			// Checks whether all model parsing failed. Returns failure if so.
			if (hadSuccess)
				Success(())
			else
				firstParseFailure match
				{
					case Some(error) => Failure(error)
					case None => Success(())
				}
		}
	}
}
