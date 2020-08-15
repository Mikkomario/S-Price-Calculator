package spadi.controller.database.access.single

import spadi.controller.database.factory.pricing.ShopFactory
import spadi.controller.database.model.pricing.ShopModel
import spadi.model.stored.pricing.Shop
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleRowModelAccess, UniqueAccess}
import utopia.vault.sql.{Delete, Where}

/**
  * Used for accessing individual shops
  * @author Mikko Hilpinen
  * @since 15.8.2020, v1.2
  */
object DbShop extends SingleRowModelAccess[Shop]
{
	// IMPLEMENTED	-------------------------------
	
	override def factory = ShopFactory
	
	override def globalCondition = None
	
	
	// COMPUTED	-----------------------------------
	
	private def model = ShopModel
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param shopId A shop id
	  * @return An access point to that shop's data
	  */
	def apply(shopId: Int) = SingleDbShop(shopId)
	
	
	// NESTED	-----------------------------------
	
	case class SingleDbShop(shopId: Int) extends SingleRowModelAccess[Shop] with UniqueAccess[Shop]
	{
		// IMPLEMENTED	---------------------------
		
		override def condition = DbShop.mergeCondition(model.withId(shopId))
		
		override def factory = DbShop.factory
		
		
		// OTHER	-------------------------------
		
		/**
		  * Deletes this shop and all linked data
		  * @param connection DB Connection (implicit)
		  */
		def delete()(implicit connection: Connection): Unit = connection(Delete(table) + Where(condition))
	}
}
