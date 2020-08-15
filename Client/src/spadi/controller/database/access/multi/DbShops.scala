package spadi.controller.database.access.multi

import spadi.controller.database.factory.pricing.ShopFactory
import spadi.controller.database.model.pricing.ShopModel
import spadi.model.stored.pricing.Shop
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyRowModelAccess

/**
  * Used for accessing multiple shops at once
  * @author Mikko Hilpinen
  * @since 2.8.2020, v1.2
  */
object DbShops extends ManyRowModelAccess[Shop]
{
	// IMPLEMENTED	----------------------------
	
	override def factory = ShopFactory
	
	override def globalCondition = None
	
	
	// COMPUTED	--------------------------------
	
	private def model = ShopModel
	
	
	// OTHER	--------------------------------
	
	/**
	  * Inserts a new shop to the database
	  * @param shopName Name of the new shop
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted shop
	  */
	def insert(shopName: String)(implicit connection: Connection) = model.insert(shopName)
}
