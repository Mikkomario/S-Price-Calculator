package spadi.controller.database.model.pricing

import java.time.Instant

import spadi.controller.database.factory.pricing.ProductNameFactory
import spadi.model.partial.pricing.ProductNameData
import spadi.model.stored.pricing.ProductName
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

@deprecated("Replaced with ShopProductModel", "v1.2")
object ProductNameModel
{
	// COMPUTED	--------------------------
	
	/**
	  * @return A model that has just been marked as deprecated
	  */
	def nowDeprecated = apply(deprecatedAfter = Some(Instant.now()))
	
	
	// OTHER	--------------------------
	
	/**
	  * @param productId Id of described product
	  * @return A model with only product id set
	  */
	def withProductId(productId: Int) = apply(productId = Some(productId))
	
	/**
	  * @param shopId Id of the shop that uses this name
	  * @return A model with only shop id set
	  */
	def withShopId(shopId: Int) = apply(shopId = Some(shopId))
	
	/**
	  * @param productName A product name
	  * @return A model with only product name set
	  */
	def withName(productName: String) = apply(name = Some(productName))
	
	/**
	  * @param productId Id of described product
	  * @param shopId Id of the shop that uses this name
	  * @param data Name data
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted name
	  */
	def insert(productId: Int, shopId: Int, data: ProductNameData)(implicit connection: Connection) =
	{
		val id = apply(None, Some(productId), Some(shopId), Some(data.name), data.alternativeName).insert().getInt
		ProductName(id, productId, shopId, data)
	}
}

/**
  * Used for interacting with product names in the DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
@deprecated("Replaced with ShopProductModel", "v1.2")
case class ProductNameModel(id: Option[Int] = None, productId: Option[Int] = None, shopId: Option[Int] = None,
							name: Option[String] = None, alternativeName: Option[String] = None,
							deprecatedAfter: Option[Instant] = None) extends StorableWithFactory[ProductName]
{
	// IMPLEMENTED	-------------------------------
	
	override def factory = ProductNameFactory
	
	override def valueProperties = Vector("id" -> id, "productId" -> productId, "shopId" -> shopId,
		"namePrimary" -> name, "nameAlternative" -> alternativeName, "deprecatedAfter" -> deprecatedAfter)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param name An alternative name for this product (may be none)
	  * @return A copy of this model with alternative name set (if specified)
	  */
	def withAlternativeName(name: Option[String]) = copy(alternativeName = name)
}
