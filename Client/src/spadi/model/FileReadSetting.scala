package spadi.model

import java.nio.file.Path

/**
 * A model that represents a user-specified setting for reading of a certain file.
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
case class FileReadSetting(path: Path, shop: Shop, inputType: PriceInputType)
