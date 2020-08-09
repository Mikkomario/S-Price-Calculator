package spadi.controller.database.access.single

import spadi.controller.database.factory.pricing.ShopProductFactory
import spadi.controller.database.model.pricing.ShopProductModel
import spadi.model.stored.pricing.ShopProductInfo
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleRowModelAccess, UniqueAccess}
import utopia.vault.sql.{Limit, Select, Where}

/**
  * Used for accessing individual shop product descriptions
  * @author Mikko Hilpinen
  * @since 9.8.2020, v1.2
  */
object DbShopProduct extends SingleRowModelAccess[ShopProductInfo]
{
	// IMPLEMENTED	------------------------------------
	
	override def factory = ShopProductFactory
	
	override val globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	----------------------------------------
	
	private def model = ShopProductModel
	
	
	// OTHER	----------------------------------------
	
	/**
	  * @param shopProductId A shop product description's id
	  * @return An access point to that product description's data
	  */
	def apply(shopProductId: Int) = DbSingleShopProduct(shopProductId)
	
	/**
	  * @param shopId A shop id
	  * @return An access point to that shop's individual product descriptions
	  */
	def inShopWithId(shopId: Int) = DbSpecifiedShopProduct(shopId)
	
	
	// NESTED	----------------------------------------
	
	case class DbSingleShopProduct(shopProductId: Int) extends SingleRowModelAccess[ShopProductInfo]
		with UniqueAccess[ShopProductInfo]
	{
		// IMPLEMENTED	--------------------------------
		
		override def condition = DbShopProduct.mergeCondition(model.withId(shopProductId).toCondition)
		
		override def factory = DbShopProduct.factory
		
		
		// COMPUTED	-----------------------------------
		
		/**
		  * @param connection DB Connection (implicit)
		  * @return This shop product's shop's id
		  */
		def shopId(implicit connection: Connection) = connection(
			Select(table, table(factory.shopIdAttName)) + Where(condition) + Limit(1)).firstValue.int
		
		/**
		  * @return An access point to this product's base price
		  */
		def basePrice = DbProductBasePrice.forShopProductWithId(shopProductId)
		
		/**
		  * @return An access point to this product's net price
		  */
		def netPrice = DbProductNetPrice.forShopProductWithId(shopProductId)
	}
	
	case class DbSpecifiedShopProduct(shopId: Int) extends SingleRowModelAccess[ShopProductInfo]
	{
		// IMPLEMENTED	--------------------------------
		
		override def factory = DbShopProduct.factory
		
		override def globalCondition = Some(DbShopProduct.mergeCondition(model.withShopId(shopId)))
		
		
		// OTHER	-----------------------------------
		
		/**
		  * @param productId Id of the described product
		  * @return An access point to that product's description in this shop
		  */
		def forProductWithId(productId: Int) = DbSingleSpecifiedShopProduct(productId)
		
		
		// NESTED	-----------------------------------
		
		case class DbSingleSpecifiedShopProduct(productId: Int) extends SingleRowModelAccess[ShopProductInfo]
			with UniqueAccess[ShopProductInfo]
		{
			// IMPLEMENTED	---------------------------
			
			override def condition = DbSpecifiedShopProduct.this.mergeCondition(model.withProductId(productId))
			
			override def factory = DbSpecifiedShopProduct.this.factory
		}
	}
}
