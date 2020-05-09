package spadi.controller

import spadi.model.ProductPriceKeyMapping

/**
 * Contains mappings for reading product price documents
 * @author Mikko Hilpinen
 * @since 8.5.2020, v2
 */
object PriceKeyMappings extends LocalModelsContainer[ProductPriceKeyMapping]
{
	// INITIAL CODE	------------------------------

	// Will always contain at least the default mapping
	if (current.isEmpty)
		current :+= ProductPriceKeyMapping.default
	
	
	// IMPLEMENTED	------------------------------

	override protected def factory = ProductPriceKeyMapping

	override protected val fileName = "price-mappings.json"
}
