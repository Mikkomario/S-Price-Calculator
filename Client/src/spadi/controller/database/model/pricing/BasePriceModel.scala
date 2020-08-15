package spadi.controller.database.model.pricing

import java.time.Instant

import spadi.controller.database.access.single.DbSaleGroup
import spadi.controller.database.factory.pricing.BasePriceFactory
import spadi.model.cached.pricing.Price
import spadi.model.partial.pricing.BasePriceData
import spadi.model.stored.pricing.{BasePrice, SaleGroup}
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.sql.Insert

object BasePriceModel
{
	// COMPUTED	-------------------------
	
	/**
	  * @return A model that has just been marked as deprecated
	  */
	def nowDeprecated = apply(deprecatedAfter = Some(Instant.now()))
	
	def table = BasePriceFactory.table
	
	
	// OTHER	-------------------------
	
	/**
	  * @param priceId A base price id
	  * @return A model with only id set
	  */
	def withId(priceId: Int) = apply(Some(priceId))
	
	/**
	  * @param shopProductId Id of a shop's product description
	  * @return A model with only shop product id set
	  */
	def withShopProductId(shopProductId: Int) = apply(shopProductId = Some(shopProductId))
	
	/**
	  * Inserts a number of new base prices to DB
	  * @param items base prices to insert. Consist of shop product id, base price and sale group id (optional)
	  * @param connection DB Connection
	  */
	def insertMany(items: Vector[(Int, Price, Option[Int])])(implicit connection: Connection): Unit =
	{
		Insert(table, items.map { case (shopProductId, price, saleGroupId) =>
			apply(None, Some(shopProductId), Some(price), saleGroupId).toModel })
	}
	
	/**
	  * Inserts a new base price to the database
	  * @param shopProductId Id of targeted shop product description
	  * @param basePrice New base price for the product
	  * @param saleGroupId Id of the associated sale group. None if no sale group should be associated.
	  * @param connection DB Connection (implicit)
	  * @return Index of the newly inserted base price.
	  */
	def insert(shopProductId: Int, basePrice: Price, saleGroupId: Option[Int] = None)(implicit connection: Connection) =
		apply(None, Some(shopProductId), Some(basePrice), saleGroupId).insert().getInt
	
	/**
	  * Inserts a new base price to the DB and connects it with a sale group if one can be found
	  * @param shopId Id of the shop that gives this price
	  * @param shopProductId Id of that shop's product description
	  * @param data Base price data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted base price, including affecting sale if found
	  */
	def insert(shopId: Int, shopProductId: Int, data: BasePriceData)(implicit connection: Connection): BasePrice =
	{
		// Checks whether targeted sale group already exists, creates one if not
		val saleGroup = data.saleGroupIdentifier.map { identifier =>
			DbSaleGroup.inShopWithId(shopId).withIdentifier(identifier).getOrInsert }
		insert(shopProductId, data.price, saleGroup)
	}
	
	/**
	  * Inserts a new base price to the DB
	  * @param shopProductId Id of the shop specific product description this price is attached to
	  * @param price Price given to this product by default
	  * @param sale Sale group to connect to this price
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted base price
	  */
	def insert(shopProductId: Int, price: Price, sale: Option[SaleGroup])(implicit connection: Connection) =
	{
		// Inserts the base price
		val id = apply(None, Some(shopProductId), Some(price), sale.map { _.id }).insert().getInt
		BasePrice(id, shopProductId, price, sale)
	}
}

/**
  * Used for interacting with product base prices in DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
case class BasePriceModel(id: Option[Int] = None, shopProductId: Option[Int] = None, price: Option[Price] = None,
						  saleGroupId: Option[Int] = None, deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[BasePrice]
{
	override def factory = BasePriceFactory
	
	override def valueProperties = Vector("id" -> id, "shopProductId" -> shopProductId,
		"basePrice" -> price.map { _.amount }, "saleUnit" -> price.map { _.unit },
		"saleCount" -> price.map { _.unitsSold }, "saleGroupId" -> saleGroupId,
		"deprecatedAfter" -> deprecatedAfter)
}
