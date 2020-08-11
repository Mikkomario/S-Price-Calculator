package spadi.controller.database.access.multi

import spadi.controller.database.factory.pricing.{NetPriceFactory, ProductFactory, ShopProductFactory}
import spadi.controller.database.model.pricing.{NetPriceModel, ShopProductModel}
import spadi.model.cached.pricing.Price
import spadi.model.stored.pricing.NetPrice
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyRowModelAccess
import utopia.vault.sql.Where
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple net prices at a time
  * @author Mikko Hilpinen
  * @since 11.8.2020, v1.2
  */
object DbNetPrices extends ManyRowModelAccess[NetPrice]
{
	// IMPLEMENTED	-------------------------------
	
	override def factory = NetPriceFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	-----------------------------------
	
	private def model = NetPriceModel
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Inserts a new net price for a product. The previous price should be deprecated before this point.
	  * @param shopProductId Id of the targeted shop product description.
	  * @param newPrice New price for the product in that shop.
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted price model.
	  */
	def insert(shopProductId: Int, newPrice: Price)(implicit connection: Connection) =
		model.insert(shopProductId, newPrice)
	
	/**
	  * Deprecates a range of product net prices for a single shop
	  * @param shopId Id of the targeted shop
	  * @param firstElectricId Target electric id range start
	  * @param lastElectricId Target electric id range end
	  * @param connection DB Connection (implicit)
	  * @return Number of rows / prices affected
	  */
	def deprecateElectricIdRange(shopId: Int, firstElectricId: String, lastElectricId: String)
								(implicit connection: Connection) =
	{
		// Needs to join into shop product and product tables
		connection(model.nowDeprecated.toUpdateStatement(Some(table join ShopProductFactory.table join ProductFactory.table)) +
			Where(mergeCondition(ShopProductModel.withShopId(shopId).toCondition &&
				ProductFactory.electricIdColumn.isBetween(firstElectricId, lastElectricId)))).updatedRowCount
	}
}
