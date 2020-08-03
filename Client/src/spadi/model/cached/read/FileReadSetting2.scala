package spadi.model.cached.read

import java.nio.file.Path

import spadi.model.enumeration.PriceInputType
import spadi.model.stored.pricing.Shop

/**
  * A model that represents a user-specified setting for reading of a certain file.
  * @author Mikko Hilpinen
  * @since 26.5.2020, v1.1
  */
case class FileReadSetting2(path: Path, shop: Shop, inputType: PriceInputType)
