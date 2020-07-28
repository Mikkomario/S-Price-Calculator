package spadi.controller.container

import spadi.model.cached.pricing.product.ProductBasePrice

/**
  * Contains a list of product prices. Saves data in local file system
  * @author Mikko Hilpinen
  * @since 8.5.2020, v1
  */
object Prices extends LocalModelsContainer[ProductBasePrice]("prices.json", ProductBasePrice)
