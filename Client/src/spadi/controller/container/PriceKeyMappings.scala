package spadi.controller.container

import spadi.model.cached.read.ProductBasePriceKeyMapping

/**
  * Contains mappings for reading product price documents
  * @author Mikko Hilpinen
  * @since 8.5.2020, v2
  */
object PriceKeyMappings extends LocalModelsContainer[ProductBasePriceKeyMapping]("price-mappings.json",
	ProductBasePriceKeyMapping)
