package spadi.controller.database.model.pricing

import java.time.Instant

import spadi.controller.database.access.single.DbSaleGroup
import spadi.controller.database.factory.pricing.BasePriceFactory
import spadi.model.cached.pricing.Price
import spadi.model.partial.pricing.BasePriceData
import spadi.model.stored.pricing.BasePrice
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object BasePriceModel
{
	// COMPUTED	-------------------------
	
	/**
	  * @return A model that has just been marked as deprecated
	  */
	def nowDeprecated = apply(deprecatedAfter = Some(Instant.now()))
	
	
	// OTHER	-------------------------
	
	/**
	  * @param shopId Id of associated shop
	  * @return A model with only shop id set
	  */
	def withShopId(shopId: Int) = apply(shopId = Some(shopId))
	
	/**
	  * Inserts a new base price to the DB. Won't connect this price with a sale group.
	  * @param productId Id of the described product
	  * @param shopId Id of the shop that gives this price
	  * @param data Base price data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted base price, including affecting sale if found
	  */
	def insertWithoutSale(productId: Int, shopId: Int, data: BasePriceData)(implicit connection: Connection) =
	{
		val id = apply(None, Some(productId), Some(shopId), Some(data.price), data.saleGroupIdentifier).insert().getInt
		BasePrice(id, productId, shopId, data)
	}
	
	/**
	  * Inserts a new base price to the DB and connects it with a sale group if one can be found
	  * @param productId Id of the described product
	  * @param shopId Id of the shop that gives this price
	  * @param data Base price data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted base price, including affecting sale if found
	  */
	def insert(productId: Int, shopId: Int, data: BasePriceData)(implicit connection: Connection) =
	{
		// Inserts the base price, then connects it with a sale group if one is found
		val id = apply(None, Some(productId), Some(shopId), Some(data.price), data.saleGroupIdentifier).insert().getInt
		val saleGroup = data.saleGroupIdentifier.flatMap { identifier =>
			DbSaleGroup.inShopWithId(shopId).withIdentifier(identifier).pull }
		saleGroup.foreach { sale => BasePriceSaleGroupLinkModel.insert(id, sale.id) }
		
		BasePrice(id, productId, shopId, data, saleGroup)
	}
}

/**
  * Used for interacting with product base prices in DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
case class BasePriceModel(id: Option[Int] = None, productId: Option[Int] = None, shopId: Option[Int] = None,
						  price: Option[Price] = None, saleGroupIdentifier: Option[String] = None,
						  deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[BasePrice]
{
	override def factory = BasePriceFactory
	
	override def valueProperties = Vector("id" -> id, "productId" -> productId, "shopId" -> shopId,
		"basePrice" -> price.map { _.amount }, "saleUnit" -> price.map { _.unit },
		"saleCount" -> price.map { _.unitsSold }, "saleGroupIdentifier" -> saleGroupIdentifier,
		"deprecatedAfter" -> deprecatedAfter)
}
