package spadi.controller.database.access.single

import spadi.controller.database.factory.pricing.ProductFactory
import spadi.controller.database.model.pricing.ProductModel
import spadi.model.stored.pricing.Product
import utopia.vault.nosql.access.{SingleModelAccess, UniqueAccess}

/**
  * Used for accessing individual product's information in the DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
object DbProduct extends SingleModelAccess[Product]
{
	// IMPLEMENTED	------------------------
	
	override def factory = ProductFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	----------------------------
	
	private def model = ProductModel
	
	
	// OTHER	----------------------------
	
	/**
	  * @param id A product id
	  * @return An access point to an individual product with that id
	  */
	def apply(id: Int) = DbSingleProduct(id)
	
	
	// NESTED	----------------------------
	
	case class DbSingleProduct(productId: Int) extends SingleModelAccess[Product] with UniqueAccess[Product]
	{
		// IMPLEMENTED	--------------------
		
		override lazy val condition = DbProduct.mergeCondition(model.withId(productId).toCondition)
		
		override def factory = DbProduct.factory
		
		
		// COMPUTED	------------------------
		
		/**
		  * @return An access point to this product's individual names
		  */
		def name = DbProductName.forProductWithId(productId)
		
		/**
		  * @return An access point to this product's net prices in various shops
		  */
		def netPrice = DbProductNetPrice.forProductWithId(productId)
		
		/**
		  * @return An access point to this product's base prices in various shops
		  */
		def basePrice = DbProductBasePrice.forProductWithId(productId)
	}
}
