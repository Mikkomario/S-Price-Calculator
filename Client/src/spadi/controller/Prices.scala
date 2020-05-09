package spadi.controller

import spadi.model.ProductPrice

/**
 * Contains a list of product prices. Saves data in local file system
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
object Prices extends LocalModelsContainer[ProductPrice]
{
	// IMPLEMENTED	-------------------------------
	
	override protected def factory = ProductPrice
	
	override protected val fileName = "prices.json"
}
