package spadi.model.stored.reading

import spadi.model.partial.reading.PriceKeyMappingData
import spadi.model.stored.Stored

/**
  * Used for mapping document headers with products
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class PriceKeyMapping(id: Int, data: PriceKeyMappingData) extends Stored[PriceKeyMappingData]
