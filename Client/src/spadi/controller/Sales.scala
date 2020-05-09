package spadi.controller

import spadi.model.SalesGroup

/**
 * Used for storing & accessing product sales groups
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
object Sales extends LocalModelsContainer[SalesGroup]
{
	override protected def factory = SalesGroup
	
	override protected val fileName = "sales.json"
}
