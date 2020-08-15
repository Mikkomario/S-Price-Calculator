package spadi.controller.database.model.pricing

import java.time.Instant

import spadi.controller.database.factory.pricing.NetPriceFactory
import spadi.model.cached.pricing.Price
import spadi.model.stored.pricing.NetPrice
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.sql.Insert

object NetPriceModel
{
	// COMPUTED	--------------------------
	
	/**
	  * @return A model that has just been marked deprecated
	  */
	def nowDeprecated = apply(deprecatedAfter = Some(Instant.now()))
	
	def table = NetPriceFactory.table
	
	
	// OTHER	--------------------------
	
	/**
	  * @param netPriceId A net price id
	  * @return A model with only id set
	  */
	def withId(netPriceId: Int) = apply(Some(netPriceId))
	
	/**
	  * @param shopProductId Id of a shop's product description
	  * @return A model with only product id set
	  */
	def withShopProductId(shopProductId: Int) = apply(shopProductId = Some(shopProductId))
	
	/**
	  * Inserts a number of prices to the database
	  * @param prices Prices to insert. Each pair contains a shop product id and associated net price.
	  * @param connection DB Connection (implicit)
	  */
	def insertMany(prices: Vector[(Int, Price)])(implicit connection: Connection): Unit =
	{
		Insert(table, prices.map { case (shopProductId, price) => apply(None, Some(shopProductId), Some(price)).toModel })
	}
	
	/**
	  * Inserts a new net price to the DB
	  * @param shopProductId Id of the shop product description this price is attached to
	  * @param price Price given to this product in that shop
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted price
	  */
	def insert(shopProductId: Int, price: Price)(implicit connection: Connection) =
	{
		val id = apply(None, Some(shopProductId), Some(price)).insert().getInt
		NetPrice(id, shopProductId, price)
	}
}

/**
  * Used for interacting with product net prices in DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
case class NetPriceModel(id: Option[Int] = None, shopProductId: Option[Int] = None,
						 price: Option[Price] = None, deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[NetPrice]
{
	override def factory = NetPriceFactory
	
	override def valueProperties = Vector("id" -> id, "shopProductId" -> shopProductId,
		"netPrice" -> price.map { _.amount }, "saleUnit" -> price.map { _.unit },
		"saleCount" -> price.map { _.unitsSold }, "deprecatedAfter" -> deprecatedAfter)
}
