package spadi.controller

import spadi.model.SalesGroupKeyMapping

/**
 * Contains mappings for reading sales percent documents
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object SalesKeyMappings extends LocalModelsContainer[SalesGroupKeyMapping]
{
	// INITIAL CODE ------------------------------
	
	// Will always contain at least the default mapping
	if (current.isEmpty)
		current :+= SalesGroupKeyMapping.default
	
	
	// IMPLEMENTED  ------------------------------
	
	override protected def factory = SalesGroupKeyMapping
	
	override protected val fileName = "sales-mappings.json"
}
