package spadi.model.partial.pricing

import spadi.model.cached.pricing.Price

object ShopProductData
{
	/**
	  * Creates a new product data with net price included
	  * @param electricId A product's electric id
	  * @param shopId Id of the shop this description is for
	  * @param name Product name
	  * @param alternativeName Alternative product name (optional)
	  * @param price Product net price
	  * @return A new product data
	  */
	def netPrice(electricId: String, shopId: Int, name: String, alternativeName: Option[String], price: Price) =
		apply(electricId, shopId, name, alternativeName, netPrice = Some(price))
	
	/**
	  * Creates a new product data with base price included
	  * @param electricId A product's electric id
	  * @param shopId Id of the shop this description is for
	  * @param name Product name information
	  * @param alternativeName Alternative product name (optional)
	  * @param price Product base price info
	  * @return A new product data
	  */
	def basePrice(electricId: String, shopId: Int, name: String, alternativeName: Option[String], price: BasePriceData) =
		apply(electricId, shopId, name, alternativeName, Some(price))
}

/**
  * Contains basic shop-specific information about a product
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class ShopProductData(electricId: String, shopId: Int, name: String, alternativeName: Option[String] = None,
						   basePrice: Option[BasePriceData] = None, netPrice: Option[Price] = None)
