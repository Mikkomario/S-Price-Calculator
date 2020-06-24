package spadi.controller

import spadi.model.SalesGroupKeyMapping

/**
 * Contains mappings for reading sales percent documents
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
object SalesKeyMappings extends LocalModelsContainer[SalesGroupKeyMapping]("sales-mappings.json",
	SalesGroupKeyMapping)