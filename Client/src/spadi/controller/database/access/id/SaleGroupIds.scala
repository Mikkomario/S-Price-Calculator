package spadi.controller.database.access.id

import spadi.controller.database.factory.pricing.SaleGroupFactory
import spadi.controller.database.model.pricing.SaleGroupModel
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyIntIdAccess
import utopia.vault.sql.Extensions._
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing multiple sale group ids at a time
  * @author Mikko Hilpinen
  * @since 11.8.2020, v1.2
  */
object SaleGroupIds extends ManyIntIdAccess
{
	// IMPLEMENTED	------------------------
	
	override def target = table
	
	override def table = factory.table
	
	override def globalCondition = None
	
	
	// COMPUTED	---------------------------
	
	private def factory = SaleGroupFactory
	
	private def model = SaleGroupModel
	
	private def column = table.primaryColumn.get
	
	
	// OTHER	--------------------------
	
	/**
	  * @param shopId Id of targeted shop
	  * @return An access point to sale group ids in that shop
	  */
	def forShopWithId(shopId: Int) = ShopSaleGroupIds(shopId)
	
	
	// NESTED	--------------------------
	
	case class ShopSaleGroupIds(shopId: Int) extends ManyIntIdAccess
	{
		// IMPLEMENTED	------------------
		
		override def target = SaleGroupIds.target
		
		override def table = SaleGroupIds.table
		
		override def globalCondition = Some(SaleGroupIds.mergeCondition(model.withShopId(shopId)))
		
		
		// OTHER	---------------------
		
		/**
		  * Finds sale group ids matching a range of sale group identifiers
		  * @param minIdentifier First included group identifier
		  * @param maxIdentifier Last included group identifier
		  * @param connection DB Connection (implicit)
		  * @return Sale group identifier -> id pairs as a map
		  */
		def forIdentifiersBetween(minIdentifier: String, maxIdentifier: String)(implicit connection: Connection) =
		{
			// find(factory.groupIdentifierColumn.isBetween(minIdentifier, maxIdentifier))
			connection(Select(table, Vector(column, factory.groupIdentifierColumn)) +
				Where(mergeCondition(factory.groupIdentifierColumn.isBetween(minIdentifier, maxIdentifier))))
				.rows.map { row => row(factory.groupIdentifierColumn).getString -> row(column).getInt }.toMap
		}
	}
}
