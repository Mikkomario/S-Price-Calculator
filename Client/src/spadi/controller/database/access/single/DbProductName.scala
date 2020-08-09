package spadi.controller.database.access.single

import spadi.controller.database.factory.pricing.ProductNameFactory
import spadi.controller.database.model.pricing.ProductNameModel
import spadi.model.partial.pricing.ProductNameData
import spadi.model.stored.pricing.ProductName
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleRowModelAccess, UniqueAccess}

/**
  * Used for accessing individual product names in DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
@deprecated("Replaced with DbShopProduct", "v1.2")
object DbProductName extends SingleRowModelAccess[ProductName]
{
	// IMPLEMENTED	-------------------------
	
	override def factory = ProductNameFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	-----------------------------
	
	private def model = ProductNameModel
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param productId Product id
	  * @return An access point to that product's individual names
	  */
	def forProductWithId(productId: Int) = DbSingleProductName(productId)
	
	
	// NESTED	-----------------------------
	
	case class DbSingleProductName(productId: Int) extends SingleRowModelAccess[ProductName]
	{
		// IMPLEMENTED	---------------------
		
		override def factory = DbProductName.factory
		
		override val globalCondition = Some(DbProductName.mergeCondition(model.withProductId(productId).toCondition))
		
		
		// OTHER	------------------------
		
		/**
		  * @param shopId Id of the targeted shop
		  * @return An access point to this product's name in that shop
		  */
		def inShopWithId(shopId: Int) = DbSingleShopProductName(shopId)
		
		
		// NESTED	------------------------
		
		case class DbSingleShopProductName(shopId: Int) extends SingleRowModelAccess[ProductName]
			with UniqueAccess[ProductName]
		{
			// IMPLEMENTED	---------------------
			
			override val condition = DbSingleProductName.this.mergeCondition(model.withShopId(shopId).toCondition)
			
			override def factory = DbSingleProductName.this.factory
			
			
			// OTHER	-------------------------
			
			/**
			  * Changes the name of this product
			  * @param newName New name assigned for this product
			  * @param newAlternativeName New alternative name for this product (optional)
			  * @param connection DB Connection (implicit)
			  * @return New name version
			  */
			def set(newName: String, newAlternativeName: Option[String] = None)(implicit connection: Connection) =
			{
				// May use previous name if it's identical
				if (isDefined)
				{
					find(model.withName(newName).withAlternativeName(newAlternativeName).toCondition).getOrElse {
						// If previous version wasn't identical, deprecates it
						model.nowDeprecated.updateWhere(condition)
						// Then replaces it
						model.insert(productId, shopId, ProductNameData(newName, newAlternativeName))
					}
				}
				else
					model.insert(productId, shopId, ProductNameData(newName, newAlternativeName))
			}
		}
	}
}
