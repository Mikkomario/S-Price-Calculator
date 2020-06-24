package spadi.model

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.reflection.localization.LocalString._

/**
 * A factory that creates sales group key mappings from input data
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
object SalesGroupKeyMappingFromFieldsFactory extends KeyMappingFactory[SalesGroup]
{
	// ATTRIBUTES   -----------------------------
	
	private implicit val languageCode: String = "fi"
	
	
	// IMPLEMENTED  -----------------------------
	
	override val fieldNames = Vector("ID".local -> true, "Nimi".local -> true, "Alennusprosentti".local -> true,
		"Valmistaja".local -> true)
	
	override protected def fromValidatedModel(model: Model[Constant]) = SalesGroupKeyMapping(model("ID"), model("Nimi"),
		model("Alennusprosentti"), model("Valmistaja"))
}
