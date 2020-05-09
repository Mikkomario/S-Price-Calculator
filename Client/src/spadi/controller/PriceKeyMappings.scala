package spadi.controller

import spadi.model.ProductPriceKeyMapping

/**
 * Contains mappings for reading product price documents
 * @author Mikko Hilpinen
 * @since 8.5.2020, v2
 */
object PriceKeyMappings extends LocalModelsContainer[ProductPriceKeyMapping]("price-mappings.json", ProductPriceKeyMapping)
{
	// INITIAL CODE	------------------------------

	// Will always contain at least the default mapping
	if (current.isEmpty)
		current :+= ProductPriceKeyMapping.default
}
