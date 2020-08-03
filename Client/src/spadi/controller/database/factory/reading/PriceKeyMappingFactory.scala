package spadi.controller.database.factory.reading

import spadi.controller.database.Tables
import spadi.model.enumeration.PriceType
import spadi.model.partial.reading.PriceKeyMappingData
import spadi.model.stored.reading.PriceKeyMapping
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{FromRowFactoryWithTimestamps, FromRowModelFactory}

/**
  * Used for reading price key mappings from the database
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
object PriceKeyMappingFactory extends FromRowModelFactory[PriceKeyMapping]
	with FromRowFactoryWithTimestamps[PriceKeyMapping]
{
	override def creationTimePropertyName = "created"
	
	override def table = Tables.priceKeyMap
	
	override def apply(model: Model[Property]) = table.requirementDeclaration.validate(model).toTry.flatMap { valid =>
		PriceType.forId(valid("typeIdentifier")).map { priceType =>
			PriceKeyMapping(valid("id"), PriceKeyMappingData(priceType, valid("shopId"), valid("electricIdKey"),
				valid("productNameKey"), valid("productNameKeyAlternative"), valid("priceKey"), valid("saleUnitKey"),
				valid("saleCountKey"), valid("saleGroupKey")))
		}
	}
}
