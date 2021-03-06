package spadi.controller.database.model.pricing

import java.time.Instant

import spadi.controller.database.factory.pricing.ShopProductFactory
import spadi.model.partial.pricing.ShopProductData
import spadi.model.stored.pricing.ShopProduct
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.sql.Insert

object ShopProductModel
{
	def table = ShopProductFactory.table
	
	/**
	  * @param shopProductId A shop product description's id
	  * @return A model with only id set
	  */
	def withId(shopProductId: Int) = apply(Some(shopProductId))
	
	/**
	  * @param shopId A shop's id
	  * @return A model with shop id set
	  */
	def withShopId(shopId: Int) = apply(shopId = Some(shopId))
	
	/**
	  * @param productId A product's id
	  * @return A model with product id set
	  */
	def withProductId(productId: Int) = apply(productId = Some(productId))
	
	/**
	  * @param name Product name
	  * @return A model with name set
	  */
	def withName(name: String) = apply(name = Some(name))
	
	/**
	  * Inserts a number of shop product rows to the database
	  * @param items Shop products. Each containing product id, shop id, name and alternative name
	  * @param connection DB Connection (implicit)
	  * @return Range of generated shop product ids. The order of the ids might not match that of the listed items.
	  */
	def insertMany(items: Vector[(Int, Int, String, Option[String])])(implicit connection: Connection) =
	{
		if (items.nonEmpty)
		{
			val insertTime = Instant.now()
			val ids = Insert(table, items.map { case (productId, shopId, name, altName) =>
				apply(None, Some(productId), Some(shopId), Some(name), altName, Some(insertTime)).toModel }).generatedIntKeys
			ids.min to ids.max
		}
		else
			0 until 0
	}
	
	/**
	  * Inserts a new shop product row to the DB
	  * @param productId Id of the described product
	  * @param shopId Id of the shop that owns this description
	  * @param name Product's name in this shop
	  * @param alternativeName Product's alternative name (optional)
	  * @param updateTime Update time recorded on the new row (default = current time)
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted product info
	  */
	def insert(productId: Int, shopId: Int, name: String, alternativeName: Option[String] = None,
			   updateTime: Instant = Instant.now())(implicit connection: Connection) =
	{
		val id = apply(None, Some(productId), Some(shopId), Some(name), alternativeName, Some(updateTime)).insert().getInt
		ShopProduct(id, productId, shopId, name, alternativeName)
	}
	
	/**
	  * Inserts a new shop product to the DB, including possible price data
	  * @param productId Id of the described product
	  * @param data Product data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted product info
	  */
	// TODO: Possibly remove this method
	def insert(productId: Int, data: ShopProductData)(implicit connection: Connection): ShopProduct =
	{
		// Inserts the base row first, then price rows
		val base = insert(productId, data.shopId, data.name, data.alternativeName)
		val netPrice = data.netPrice.map { p => NetPriceModel.insert(base.id, p) }
		val basePrice = data.basePrice.map { p => BasePriceModel.insert(data.shopId, base.id, p) }
		
		base.copy(basePrice = basePrice, netPrice = netPrice)
	}
}

/**
  * Used for interacting with shop product data
  * @author Mikko Hilpinen
  * @since 9.8.2020, v1.2
  */
case class ShopProductModel(id: Option[Int] = None, productId: Option[Int] = None, shopId: Option[Int] = None,
							name: Option[String] = None, alternativeName: Option[String] = None,
							updated: Option[Instant] = None)
	extends StorableWithFactory[ShopProduct]
{
	// IMPLEMENTED	---------------------------
	
	override def factory = ShopProductFactory
	
	override def valueProperties = Vector("id" -> id, "productId" -> productId, "shopId" -> shopId, "name" -> name,
		"nameAlternative" -> alternativeName, "updated" -> updated)
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return A copy of this model that has just been marked as updated
	  */
	def nowUpdated = copy(updated = Some(Instant.now()))
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param alternativeName An alternative product name
	  * @return A copy of this model with specified name
	  */
	def withAlternativeName(alternativeName: Option[String]) = copy(alternativeName = alternativeName)
}
