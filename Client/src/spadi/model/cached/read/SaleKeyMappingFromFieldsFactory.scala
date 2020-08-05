package spadi.model.cached.read

import spadi.model.partial.pricing.SaleGroupData
import spadi.model.partial.reading.SaleKeyMappingData
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.reflection.localization.LocalString._

/**
  * A factory that creates sales group key mappings from input data
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class SaleKeyMappingFromFieldsFactory(shopId: Int) extends KeyMappingFactory2[SaleGroupData, SaleKeyMappingData]
{
	// ATTRIBUTES   -----------------------------
	
	private implicit val languageCode: String = "fi"
	
	
	// IMPLEMENTED  -----------------------------
	
	override val fieldNames = Vector("ID".local -> true, "Alennusprosentti".local -> true)
	
	override protected def fromValidatedModel(model: Model[Constant]) = SaleKeyMappingData(shopId, model("ID"),
		model("Alennusprosentti"))
}
