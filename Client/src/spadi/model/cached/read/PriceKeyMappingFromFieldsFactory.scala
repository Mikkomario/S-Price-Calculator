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
	
	private val (priceFieldName, priceDescription) = priceType match
	{
		case Net => "Nettohinta" -> ("Tuotteen myyntihinta alennuksineen. Jos dokumentissa on useita hintatietoja, " +
			"on tämä yleensä niistä matalin.\n\nJos tuotteita myydään esim. 10 kappaleen erissä, " +
			"on tämä silloin 10 kappaleen hinta.")
		case Base => "Perushinta" -> ("Tuotteen myyntihinta ennen kuin siihen lisätään alennus. " +
			"Jos dokumentissa on useita hintoja, on tämä yleensä niistä korkein.\n\nJos tuotteita myydään " +
			"esim. 10 kappaleen erissä, on tämä silloin 10 kappaleen hinta.")
	}
	
	override def fields =
	{
		val base = Vector(
			// Electric id must be 4-11 characters long and must contain at least a single digit
			InputField.withValidation("Sähkönumero",
				"Sähkönumero on noin 7 merkkiä pitkä numerosarja jota käytetään tuotteen tunnistamiseen " +
					"tukkujen välillä. Joissain tapauksissa sähkönumero voi sisältää myös kirjaimia.",
				isRequired = true) { s =>
				val str = s.getString
				str.length > 3 && str.length < 12 && str.exists { _.isDigit }
			},
			InputField.freeForm("Nimi",
				"Tuotteen tekninen nimi. Nimen tulisi olla uniikki yksittäisen tukun tuotteiden välillä.",
				isRequired = true),
			InputField.freeForm("Lisänimi",
				"Mahdollinen toinen kuvaus tuotteelle. Esimerkiksi tuotekategorian nimi. " +
					"Tätä nimeä ei näytetä ohjelmassa, mutta sitä voi käyttää etsiessä tuotetietoja."),
			// Price must be parseable to a positive double
			InputField.withValidation(priceFieldName, priceDescription, isRequired = true) { _.double.exists { _ >= 0.0 } },
			InputField.freeForm("Yksikkö", "Yleensä esim. kpl, m, pss tai pkt", isRequired = true),
			// Purchase count must be a positive integer
			InputField.withValidation("Ostomäärä", "Jos tuotteita myydään esim. 10 tai 100 kappaleen " +
				"tai metrin erissä, on tämä silloin 10 tai 100 tms.\n\nKaikissa dokumenteissa tätä tietoa ei anneta, " +
				"jolloin hintojen oletetaan olevan kappalehintoja.") { _.int.exists { _ > 0 } }
		)
		priceType match
		{
			case Net => base
			// Sale group id must be between 3 and 11 characters
			case Base => base :+ InputField.withValidation("Alennusryhmä",
				"Alennusryhmän tunnistekoodi. Yleensä noin neljä merkkiä pitkä numerojen ja/tai " +
					"kirjaimien sarja. Samantyyppinen sarja pitäisi löytyä myös alennusdokumentista, " +
					"sillä tämän avulla tiedetään, mitä alennusryhmää käyttää millekin tuotteelle.", isRequired = true) { s =>
				val str = s.getString
				str.length > 2 && str.length < 12
			}
		}
	}
	
	override protected def fromValidatedModel(model: Model[Constant]) = PriceKeyMappingData(priceType, shopId,
		model("Sähkönumero"), model("Nimi"), model("Lisänimi"), model(priceFieldName),
		model("Yksikkö"), model("Ostomäärä"), model("Alennusryhmä"))
}
