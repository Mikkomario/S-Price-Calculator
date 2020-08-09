package spadi.model.partial.pricing

import spadi.model.cached.pricing.Price

object ShopProductData
{
	/**
	  * Creates a new product data with net price included
	  * @param name Product name
	  * @param alternativeName Alternative product name (optional)
	  * @param price Product net price
	  * @return A new product data
	  */
	def netPrice(name: String, alternativeName: Option[String], price: Price) =
		apply(name, alternativeName, netPrice = Some(price))
	
	/**
	  * Creates a new product data with base price included
	  * @param name Product name information
	  * @param alternativeName Alternative product name (optional)
	  * @param price Product base price info
	  * @return A new product data
	  */
	def basePrice(name: String, alternativeName: Option[String], price: BasePriceData) =
		apply(name, alternativeName, Some(price))
}

/**
  * Contains basic shop-specific information about a product
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class ShopProductData(name: String, alternativeName: Option[String] = None, basePrice: Option[BasePriceData] = None,
						   netPrice: Option[Price] = None)
