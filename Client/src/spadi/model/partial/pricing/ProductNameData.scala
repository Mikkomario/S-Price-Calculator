package spadi.model.partial.pricing

/**
  * Contains basic information about a product's name in a shop
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class ProductNameData(name: String, alternativeName: Option[String] = None)
