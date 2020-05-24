package spadi.controller

import spadi.model.ProductBasePriceKeyMapping

/**
 * Contains mappings for reading product price documents
 * @author Mikko Hilpinen
 * @since 8.5.2020, v2
 */
object PriceKeyMappings extends LocalModelsContainer[ProductBasePriceKeyMapping]("price-mappings.json", ProductBasePriceKeyMapping)
{
	// INITIAL CODE	------------------------------

	// Will always contain at least the default mapping
	if (current.isEmpty)
		current :+= ProductBasePriceKeyMapping.default
}
