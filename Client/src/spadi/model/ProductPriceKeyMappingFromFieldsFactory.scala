package spadi.model
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.reflection.localization.LocalString._

/**
 * Used for creating new product price key mappings from input data
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
object ProductPriceKeyMappingFromFieldsFactory extends KeyMappingFactory[ProductPrice]
{
	// ATTRIBUTES   -----------------------------
	
	private implicit val languageCode: String = "fi"
	
	
	// IMPLEMENTED  -----------------------------
	
	override val fieldNames = Vector("Sähkönumero".local -> true, "Nimi".local -> true,
		"Lisänimet".local -> false, "Hinta".local -> true, "Yksikkö".local -> true, "Ostomäärä".local -> true)
	
	override protected def fromValidatedModel(model: Model[Constant]) = ProductPriceKeyMapping(
		model("Sähkönumero"), model("Nimi").getString +:
			model("Lisänimet").getString.split(',').map { _.trim }.filterNot { _.isEmpty }.toVector,
		model("Hinta"), model("Ostomäärä"), model("Yksikkö"))
}
