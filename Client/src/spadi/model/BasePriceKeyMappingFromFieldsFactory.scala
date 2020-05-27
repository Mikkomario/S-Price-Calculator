package spadi.model

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.reflection.localization.LocalString._

/**
 * A factory that creates new base price key mappings based on input data
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
object BasePriceKeyMappingFromFieldsFactory extends KeyMappingFactory[ProductBasePrice]
{
	private implicit val languageCode: String = "fi"
	
	override val fieldNames = Vector("Sähkönumero".local -> true, "Alennusryhmä".local -> true, "Nimi".local -> true,
		"Lisänimet".local -> false, "Perushinta".local -> true, "Yksikkö".local -> true, "Ostomäärä".local -> true)
	
	override protected def fromValidatedModel(model: Model[Constant]) = ProductBasePriceKeyMapping(
		model("Sähkönumero"), model("Alennusryhmä"),
		model("Nimi").getString +: model("Lisänimet").getString.split(',').map { _.trim }.toVector, model("Perushinta"),
		model("Ostomäärä"), model("Yksikkö"))
}
