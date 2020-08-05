package spadi.controller.database.access.multi

import spadi.controller.database.factory.reading.SaleKeyMappingFactory
import spadi.controller.database.model.reading.SaleKeyMappingModel
import spadi.model.partial.reading.SaleKeyMappingData
import spadi.model.stored.reading.SaleKeyMapping
import utopia.vault.database.Connection
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
	
	/**
	  * Inserts a new mapping to DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted mapping
	  */
	def insert(data: SaleKeyMappingData)(implicit connection: Connection) = model.insert(data)
	
	
	// NESTED	---------------------------------
	
	case class DbShopSaleKeyMappings(shopId: Int) extends ManyRowModelAccess[SaleKeyMapping]
	{
		// IMPLEMENTED	-------------------------
		
		override def factory = DbSaleKeyMappings.factory
		
		override def globalCondition = Some(DbSaleKeyMappings.mergeCondition(model.withShopId(shopId)))
	}
}
