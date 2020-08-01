package spadi.controller.database.model.pricing

import java.time.Instant

import spadi.controller.database.factory.pricing.NetPriceFactory
import spadi.model.cached.pricing.Price
import spadi.model.stored.pricing.NetPrice
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object NetPriceModel
{
	// COMPUTED	--------------------------
	
	/**
	  * @return A model that has just been marked deprecated
	  */
	def nowDeprecated = apply(deprecatedAfter = Some(Instant.now()))
	
	
	// OTHER	--------------------------
	
	/**
	  * @param shopId Id of the shop that gives this price
	  * @return A model with only shop id set
	  */
	def withShopId(shopId: Int) = apply(shopId = Some(shopId))
	
	/**
	  * Inserts a new net price to the DB
	  * @param productId Id of the described product
	  * @param shopId Id of the shop that gives this price
	  * @param price Price given to this product in that shop
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted price
	  */
	def insert(productId: Int, shopId: Int, price: Price)(implicit connection: Connection) =
	{
		val id = apply(None, Some(productId), Some(shopId), Some(price)).insert().getInt
		NetPrice(id, productId, shopId, price)
	}
}

/**
  * Used for interacting with product net prices in DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
case class NetPriceModel(id: Option[Int] = None, productId: Option[Int] = None, shopId: Option[Int] = None,
						 price: Option[Price] = None, deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[NetPrice]
{
	override def factory = NetPriceFactory
	
	override def valueProperties = Vector("id" -> id, "productId" -> productId, "shopId" -> shopId,
		"netPrice" -> price.map { _.amount }, "saleUnit" -> price.map { _.unit },
		"saleCount" -> price.map { _.unitsSold }, "deprecatedAfter" -> deprecatedAfter)
}
