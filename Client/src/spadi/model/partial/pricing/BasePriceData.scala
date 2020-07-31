package spadi.model.partial.pricing

import spadi.model.cached.pricing.Price

/**
  * Contains basic data about a product's standard price
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class BasePriceData(price: Price, saleGroupIdentifier: Option[String] = None)
