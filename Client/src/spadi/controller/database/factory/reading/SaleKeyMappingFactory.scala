package spadi.controller.database.factory.reading

import spadi.controller.database.Tables
import spadi.model.partial.reading.SaleKeyMappingData
import spadi.model.stored.reading.SaleKeyMapping
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{FromRowFactoryWithTimestamps, FromValidatedRowModelFactory}

/**
  * Used for reading sale key mappings from DB
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
object SaleKeyMappingFactory extends FromValidatedRowModelFactory[SaleKeyMapping]
	with FromRowFactoryWithTimestamps[SaleKeyMapping]
{
	override def creationTimePropertyName = "created"
	
	override protected def fromValidatedModel(model: Model[Constant]) = SaleKeyMapping(model("id"),
		SaleKeyMappingData(model("shopId"), model("groupIdKey"), model("salePercentKey")))
	
	override def table = Tables.saleGroupKeyMap
}
