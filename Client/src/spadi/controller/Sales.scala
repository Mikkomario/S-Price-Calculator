package spadi.controller

import spadi.model.SalesGroup

/**
 * Used for storing & accessing product sales groups
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
object Sales extends LocalModelsContainer[SalesGroup]("sales.json", SalesGroup)
