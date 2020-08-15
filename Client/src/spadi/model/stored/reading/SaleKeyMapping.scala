package spadi.model.stored.reading

import spadi.model.partial.reading.SaleKeyMappingData
import spadi.model.stored.Stored

/**
  * Used for mapping sale group fields with document headers
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class SaleKeyMapping(id: Int, data: SaleKeyMappingData) extends Stored[SaleKeyMappingData]
