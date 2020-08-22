package spadi.model.cached.read

import spadi.model.enumeration.PriceType
import spadi.model.enumeration.PriceType.{Base, Net}
import spadi.model.partial.pricing.ShopProductData
import spadi.model.partial.reading.PriceKeyMappingData
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.reflection.localization.LocalString._

object PriceKeyMappingFromFieldsFactory
{
	/**
	  * @param shopId Id of targeted shop
	  * @return A mapping factory that reads net price documents for that shop
	  */
	def forNetPricesInShopWithId(shopId: Int) = apply(Net, shopId)
	
	/**
	  * @param shopId Id of targeted shop
	  * @return A mapping factory that reads base price documents for that shop
	  */
	def forBasePricesInShopWithId(shopId: Int) = apply(Base, shopId)
}

/**
  * A factory that creates new base price key mappings based on input data
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.2
  */
case class PriceKeyMappingFromFieldsFactory(priceType: PriceType, shopId: Int)
	extends KeyMappingFactory[ShopProductData, PriceKeyMappingData]
{
	private implicit val languageCode: String = "fi"
	
	private val priceFieldName = priceType match
	{
		case Net => "Nettohinta"
		case Base => "Perushinta"
	}
	
	override val fieldNames =
	{
		val base = Vector("Sähkönumero".local -> true, "Nimi".local -> true,
			"Lisänimi".local -> false, priceFieldName.local -> true, "Yksikkö".local -> true, "Ostomäärä".local -> false)
		priceType match
		{
			case Net => base
			case Base => base :+ ("Alennusryhmä".local -> true)
		}
	}
	
	override protected def fromValidatedModel(model: Model[Constant]) = PriceKeyMappingData(priceType, shopId,
		model("Sähkönumero"), model("Nimi"), model("Lisänimi"), model(priceFieldName),
		model("Yksikkö"), model("Ostomäärä"), model("Alennusryhmä"))
}
