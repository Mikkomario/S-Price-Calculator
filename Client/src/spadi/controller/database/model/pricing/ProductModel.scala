package spadi.controller.database.model.pricing

import spadi.controller.database.factory.pricing.ProductFactory
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.sql.Insert

object ProductModel
{
	// COMPUTED	-----------------------
	
	/**
	  * @return Factory used by this model
	  */
	def factory = ProductFactory
	
	/**
	  * @return Table used by this model
	  */
	def table = factory.table
	
	
	// OTHER	-----------------------
	
	/**
	  * @param productId A product id
	  * @return A model with only id set
	  */
	def withId(productId: Int) = apply(Some(productId))
	
	/**
	  * @param electricId Identifier unique to this product
	  * @return A model with only electric identifier set
	  */
	def withElectricId(electricId: String) = apply(electricId = Some(electricId))
	
	/**
	  * Only inserts a product row, no other product data is included
	  * @param electricId Identifier unique to this product
	  * @param connection Database connection (implicit)
	  * @return Newly inserted product's id
	  */
	def insertEmptyProduct(electricId: String)(implicit connection: Connection) =
		apply(None, Some(electricId)).insert().getInt
	
	/**
	  * Inserts multiple products to the database
	  * @param electricIds Electric ids to insert
	  * @param connection DB Connection (implicit)
	  * @return Range of generated product ids. The order of indices might not match that of specified electric ids.
	  */
	def insertMany(electricIds: Vector[String])(implicit connection: Connection) =
	{
		if (electricIds.nonEmpty)
		{
			val ids = Insert(table, electricIds.map { eId => apply(None, Some(eId)).toModel }).generatedIntKeys
			ids.min to ids.max
		}
		else
			0 until 0
	}
}

/**
  * Used for interacting with products in DB
  * @author Mikko Hilpinen
  * @since 1.8.2020, v1.2
  */
case class ProductModel(id: Option[Int] = None, electricId: Option[String] = None) extends Storable
{
	override def table = ProductModel.table
	
	override def valueProperties = Vector("id" -> id, "electricId" -> electricId)
}
