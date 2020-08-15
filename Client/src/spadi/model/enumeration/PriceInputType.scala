package spadi.model.enumeration

import utopia.reflection.localization.LocalString

/**
 * An enumeration for different types of input files
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
sealed trait PriceInputType
{
	/**
	 * @return Name of this input type
	 */
	def name: LocalString
	
	override def toString = name.string
}

object PriceInputType
{
	private implicit val languageCode: String = "en"
	
	/**
	 * All values of this enumeration
	 */
	val values = Vector[PriceInputType](SalePrice, BasePrice, SaleGroup)
	
	/**
	 * Input that produces prices with sales
	 */
	case object SalePrice extends PriceInputType
	{
		override def name = "Nettohinta"
	}
	
	/**
	 * Input that produces prices without sales
	 */
	case object BasePrice extends PriceInputType
	{
		override def name = "Perushinta"
	}
	
	/**
	 * Input that produces sales only
	 */
	case object SaleGroup extends PriceInputType
	{
		override def name = "Pelkkä Alennus"
	}
}
