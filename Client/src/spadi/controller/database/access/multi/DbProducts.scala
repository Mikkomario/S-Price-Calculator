package spadi.controller.database.access.multi

import spadi.controller.database.access.id.ProductId
import spadi.controller.database.factory.pricing.ProductFactory
import spadi.model.partial.pricing.ProductData
import spadi.model.stored.pricing.Product
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess

/**
  * Used for accessing multiple products at once
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object DbProducts extends ManyModelAccess[Product]
{
	// IMPLEMENTED	--------------------------
	
	override def factory = ProductFactory
	
	override def globalCondition = None
	
	
	// OTHER	------------------------------
	
	def insertData(data: ProductData)(implicit connection: Connection) =
	{
		// First makes sure the product row exists
		val productId = ProductId.forElectricIdentifier(data.electricId)
		
		// Next inserts name, base price and net price data for each shop where applicable
		data.shopData.foreach { case (shopId, shopData) =>
			// Checks previous product name. Deprecates and replaces it if necessary
			
		}
	}
}
