package spadi.model.stored.pricing

import spadi.model.cached.pricing.Price

/**
  * Represents a recorded product's standard price in a shop
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class BasePrice(id: Int, productId: Int, shopId: Int, price: Price, sale: Option[SaleGroup] = None)
