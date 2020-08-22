package spadi.model.partial.purchase

import spadi.model.cached.pricing.Price

/**
  * Contains information about a product purchase choice
  * @author Mikko Hilpinen
  * @since 16.8.2020, v1.2.1
  * @param shopProductId Id of the chosen product & shop
  * @param price Product sale price
  * @param unitsBought Number of units bought
  * @param profitModifier Modifier used for calculating profit. Final profit is price * unitsBought * profitModifier
  */
case class ReceiptProductData(shopProductId: Int, price: Price, unitsBought: Int, profitModifier: Double)
