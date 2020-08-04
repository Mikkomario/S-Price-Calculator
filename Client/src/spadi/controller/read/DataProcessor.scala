package spadi.controller.read

import java.nio.file.Path

import spadi.controller.database.access.multi.DbProducts
import spadi.controller.database.access.single.DbSaleGroup
import spadi.model.cached.read.KeyMapping
import spadi.model.partial.pricing.{ProductData, SaleGroupData}
import spadi.model.stored.reading.SaleKeyMapping
import utopia.vault.database.Connection

object DataProcessor
{
	/**
	  * Creates a new data processor that targets sale group documents
	  * @param filePath Document path
	  * @param mapping Mapping for reading sale group data
	  * @return A new data processor
	  */
	def forSaleGroups(filePath: Path, mapping: SaleKeyMapping) =
		DataProcessor[SaleGroupData](filePath, mapping) { (sale, connection) =>
			implicit val c: Connection = connection
			sale.priceModifier.foreach { priceMod =>
				DbSaleGroup.inShopWithId(sale.shopId).withIdentifier(sale.groupIdentifier).amount_=(priceMod)
			}
		}
	
	def forPrices(filePath: Path, mapping: KeyMapping[ProductData]) =
		DataProcessor[ProductData](filePath, mapping) { (price, connection) =>
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
case class DataProcessor[A](filePath: Path, mapping: KeyMapping[A])(parse: (A, Connection) => Unit)
{
	/**
	  * Processes the following read item
	  * @param item Item to process
	  * @param connection DB Connection
	  */
	def apply(item: A)(implicit connection: Connection) = parse(item, connection)
}
