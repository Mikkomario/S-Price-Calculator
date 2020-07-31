package spadi.model.stored.pricing

import spadi.model.cached.pricing.Price

/**
  * Represents a stored product's net price in a shop
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class NetPrice(id: Int, productId: Int, shopId: Int, price: Price)
