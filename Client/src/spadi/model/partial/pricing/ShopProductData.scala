package spadi.model.partial.pricing

import spadi.model.cached.pricing.Price

object ShopProductData
{
	/**
	  * Creates a new product data with net price included
	  * @param name Product name information
	  * @param price Product net price
	  * @return A new product data
	  */
	def netPrice(name: ProductNameData, price: Price) = apply(name, netPrice = Some(price))
	
	/**
	  * Creates a new product data with base price included
	  * @param name Product name information
	  * @param price Product base price info
	  * @return A new product data
	  */
	def basePrice(name: ProductNameData, price: BasePriceData) = apply(name, Some(price))
}

/**
  * Contains basic shop-specific information about a product
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class ShopProductData(name: ProductNameData, basePrice: Option[BasePriceData] = None,
						   netPrice: Option[Price] = None)
