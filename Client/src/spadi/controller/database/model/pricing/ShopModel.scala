package spadi.controller.database.model.pricing

import spadi.controller.database.factory.pricing.ShopFactory
import spadi.model.stored.pricing.Shop
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object ShopModel
{
	/**
	  * Inserts a new shop to the database
	  * @param shopName Name of the shop
	  * @param connection DB Connection
	  * @return Newly inserted shop
	  */
	def insert(shopName: String)(implicit connection: Connection) =
	{
		val id = apply(None, Some(shopName)).insert().getInt
		Shop(id, shopName)
	}
}

/**
  * Used for interacting with shop data in DB
  * @author Mikko Hilpinen
  * @since 2.8.2020, v1.2
  */
case class ShopModel(id: Option[Int] = None, name: Option[String] = None) extends StorableWithFactory[Shop]
{
	override def factory = ShopFactory
	
	override def valueProperties = Vector("id" -> id, "name" -> name)
}
