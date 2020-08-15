package spadi.controller.database.access.id

import spadi.controller.database.factory.pricing.{ProductFactory, ShopProductFactory}
import spadi.controller.database.model.pricing.ShopProductModel
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyIntIdAccess
import utopia.vault.sql.{Select, Where}
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple shop product ids at a time
  * @author Mikko Hilpinen
  * @since 9.8.2020, v1.2
  */
object ShopProductIds extends ManyIntIdAccess
{
	// IMPLEMENTED	--------------------------
	
	override def target = table
	
	override def table = factory.table
	
	override def globalCondition = None
	
	
	// COMPUTED	------------------------------
	
	private def factory = ShopProductFactory
	
	private def column = table.primaryColumn.get
	
	private def model = ShopProductModel
	
	
	// OTHER	------------------------------
	
	/**
	  * Finds shop product ids for a certain electric id range in a shop
	  * @param shopId Id of targeted shop
	  * @param minElectricId First included electric id
	  * @param maxElectricId Last included electric id
	  * @param connection DB Connection (implicit)
	  * @return A map that has electric ids as keys and shop product ids as values
	  */
	def forElectricIdsBetween(shopId: Int, minElectricId: String, maxElectricId: String)(implicit connection: Connection) =
	{
		// Needs to join to the products table
		val electricIdColumn = ProductFactory.electricIdColumn
		connection(Select(table join ProductFactory.table, Vector(column, electricIdColumn)) +
			Where(model.withShopId(shopId).toCondition && electricIdColumn.isBetween(minElectricId, maxElectricId)))
			.rows.map { row => row(electricIdColumn).getString -> row(column).getInt }.toMap
	}
	
	/**
	  * @param shopId Id of targeted shop
	  * @param min First included shop product id
	  * @param max Last included shop product id
	  * @param connection DB Connection (implicit)
	  * @return Electric id to shop product id map for targeted shop product id range
	  */
	def electricIdMapForShopProductIdsBetween(shopId: Int, min: Int, max: Int)(implicit connection: Connection) =
	{
		val electricIdColumn = ProductFactory.electricIdColumn
		connection(Select(table join ProductFactory.table, Vector(column, electricIdColumn)) +
			Where(model.withShopId(shopId).toCondition && column.isBetween(min, max)))
			.rows.map { row => row(electricIdColumn).getString -> row(column).getInt }.toMap
	}
}
