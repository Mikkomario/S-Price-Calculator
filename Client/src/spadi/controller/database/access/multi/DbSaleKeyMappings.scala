package spadi.controller.database.access.multi

import spadi.controller.database.factory.reading.SaleKeyMappingFactory
import spadi.controller.database.model.reading.SaleKeyMappingModel
import spadi.model.stored.reading.SaleKeyMapping
import utopia.vault.nosql.access.ManyRowModelAccess

/**
  * Used for accessing multiple sale key mappings at a time
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
object DbSaleKeyMappings extends ManyRowModelAccess[SaleKeyMapping]
{
	// IMPLEMENTED	-----------------------------
	
	override def factory = SaleKeyMappingFactory
	
	override def globalCondition = None
	
	
	// COMPUTED	---------------------------------
	
	private def model = SaleKeyMappingModel
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param shopId Id of targeted shop
	  * @return An access point to mappings describing that shop's documents
	  */
	def forShopWithId(shopId: Int) = DbShopSaleKeyMappings(shopId)
	
	
	// NESTED	---------------------------------
	
	case class DbShopSaleKeyMappings(shopId: Int) extends ManyRowModelAccess[SaleKeyMapping]
	{
		// IMPLEMENTED	-------------------------
		
		override def factory = DbSaleKeyMappings.factory
		
		override def globalCondition = Some(DbSaleKeyMappings.mergeCondition(model.withShopId(shopId)))
	}
}
