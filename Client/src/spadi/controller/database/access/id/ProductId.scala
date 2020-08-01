package spadi.controller.database.access.id

import spadi.controller.database.model.pricing.ProductModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.SingleIntIdAccess

/**
  * Used for accessing individual product ids
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object ProductId extends SingleIntIdAccess
{
	// IMPLEMENTED	--------------------
	
	override def target = table
	
	override def table = model.table
	
	override def globalCondition = None
	
	
	// COMPUTED	------------------------
	
	private def model = ProductModel
	
	
	// OTHER	------------------------
	
	/**
	  * @param identifier An electric identifier for this product
	  * @param connection Database connection (implicit)
	  * @return Product id matching that identifier. If no such existed yet, one is created.
	  */
	def forElectricIdentifier(identifier: String)(implicit connection: Connection) =
	{
		// Inserts a product row if one can't be found
		read(Some(mergeCondition(model.withElectricId(identifier).toCondition))).getOrElse {
			model.insertEmptyProduct(identifier)
		}
	}
}
