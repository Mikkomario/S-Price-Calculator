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
case class SaleKeyMappingFromFieldsFactory(shopId: Int) extends KeyMappingFactory[SaleGroupData, SaleKeyMappingData]
{
	// ATTRIBUTES   -----------------------------
	
	private implicit val languageCode: String = "fi"
	
	
	// IMPLEMENTED  -----------------------------
	
	override val fields = Vector(
		InputField.withValidation("ID", isRequired = true) { s =>
			val str = s.getString
			str.length > 2 && str.length < 12
		},
		InputField.withValidation("Alennusprosentti", isRequired = true) { _.int.exists { i =>
			i >= -100 && i <= 100 } }
	)
	
	override protected def fromValidatedModel(model: Model[Constant]) = SaleKeyMappingData(shopId, model("ID"),
		model("Alennusprosentti"))
}
