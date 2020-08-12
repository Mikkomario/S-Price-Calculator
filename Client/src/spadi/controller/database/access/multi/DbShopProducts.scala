package spadi.controller.database.access.multi

import spadi.controller.database.factory.pricing.ShopProductFactory
import spadi.controller.database.model.pricing.ShopProductModel
import spadi.model.stored.pricing.ShopProduct
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyRowModelAccess
import utopia.vault.sql.OrderBy

/**
  * Used for accessing multiple shop products at a time
  * @author Mikko Hilpinen
  * @since 12.8.2020, v1.2
  */
object DbShopProducts extends ManyRowModelAccess[ShopProduct]
{
	// IMPLEMENTED	-------------------------------
	
	override def factory = ShopProductFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	-----------------------------------
	
	private def model = ShopProductModel
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param shopId Id of targeted shop
	  * @return An access point to that shop's products
	  */
	def forShopWithId(shopId: Int) = DbSpecificShopProducts(shopId)
	
	
	// NESTED	-----------------------------------
	
	case class DbSpecificShopProducts(shopId: Int) extends ManyRowModelAccess[ShopProduct]
	{
		// IMPLEMENTED	---------------------------
		
		override def factory = DbShopProducts.factory
		
		override def globalCondition = Some(DbShopProducts.mergeCondition(model.withShopId(shopId)))
		
		
		// OTHER	-------------------------------
		
		/**
		  * Reads n products from this access point
		  * @param numberOfProducts Number of products to read
		  * @param connection DB Connection (implicit)
		  * @return Products
		  */
		def take(numberOfProducts: Int)(implicit connection: Connection) =
			factory.take(numberOfProducts, OrderBy.descending(table.primaryColumn.get), globalCondition)
	}
}
