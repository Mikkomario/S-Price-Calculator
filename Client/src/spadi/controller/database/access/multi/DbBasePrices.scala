package spadi.controller.database.access.multi

import spadi.controller.database.factory.pricing.{BasePriceFactory, ProductFactory, ShopProductFactory}
import spadi.controller.database.model.pricing.{BasePriceModel, ShopProductModel}
import spadi.model.cached.pricing.Price
import spadi.model.stored.pricing.BasePrice
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyRowModelAccess
import utopia.vault.sql.{Delete, Where}
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple base prices at a time
  * @author Mikko Hilpinen
  * @since 12.8.2020, v1.2
  */
object DbBasePrices extends ManyRowModelAccess[BasePrice]
{
	// IMPLEMENTED	-------------------------
	
	override def factory = BasePriceFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	-----------------------------
	
	private def model = BasePriceModel
	
	
	// OTHER	-----------------------------
	
	/**
	  * Inserts a new base price to the database. Previous price version should be deprecated before this method call.
	  * @param shopProductId Id of targeted shop product description
	  * @param price New product base price
	  * @param saleGroupId Id of associated sale group. None if no sale group should be associated.
	  * @param connection DB Connection (implicit)
	  * @return Index of the newly inserted base price
	  */
	def insert(shopProductId: Int, price: Price, saleGroupId: Option[Int] = None)(implicit connection: Connection) =
		model.insert(shopProductId, price, saleGroupId)
	
	/**
	  * Deprecates all base prices of a certain shop in an product electric id range
	  * @param shopId Id of the targeted shop
	  * @param firstElectricId First included product electric id
	  * @param lastElectricId Last included electric id
	  * @param connection DB Connection (implicit)
	  * @return Number of deprecated prices
	  */
	def deprecateElectricIdRange(shopId: Int, firstElectricId: String, lastElectricId: String)
								(implicit connection: Connection) =
	{
		// Joins into shop product and product tables
		// Uses a different non-deprecated condition
		connection(model.nowDeprecated.toUpdateStatement(
			Some(table join ShopProductFactory.table join ProductFactory.table)) +
			Where(factory.deprecationColumn.isNull && ShopProductModel.withShopId(shopId).toCondition &&
				ProductFactory.electricIdColumn.isBetween(firstElectricId, lastElectricId))).updatedRowCount
	}
	
	/**
	  * Deprecates base prices of targeted shop product ids
	  * @param shopProductIds Targeted shop product ids
	  * @param connection DB Connection (implicit)
	  * @return Number of deprecated prices
	  */
	def deprecatePricesForShopProductIds(shopProductIds: Set[Int])(implicit connection: Connection) =
	{
		model.nowDeprecated.updateWhere(factory.deprecationColumn.isNull &&
			factory.shopProductIdColumn.in(shopProductIds))
	}
	
	/**
	  * Deletes all deprecated base price data
	  * @param connection DB Connection (implicit)
	  */
	def deleteDeprecatedData()(implicit connection: Connection): Unit =
		connection(Delete(table) + Where(factory.deprecationColumn.isNotNull))
}
