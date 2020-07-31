package spadi.model.partial.pricing

import spadi.model.cached.pricing.Price

/**
  * Contains basic shop-specific information about a product
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class ShopProductData(name: ProductNameData, basePrice: Option[BasePriceData] = None,
						   netPrice: Option[Price] = None)
