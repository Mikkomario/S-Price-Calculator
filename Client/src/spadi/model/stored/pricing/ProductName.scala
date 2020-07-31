package spadi.model.stored.pricing

import spadi.model.partial.pricing.ProductNameData
import spadi.model.stored.Stored

/**
  * Represents a stored product name in a shop
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class ProductName(id: Int, productId: Int, shopId: Int, data: ProductNameData) extends Stored[ProductNameData]
