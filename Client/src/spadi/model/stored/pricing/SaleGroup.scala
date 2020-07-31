package spadi.model.stored.pricing

/**
  * Represents a recorded shop's sale group
  * @author Mikko Hilpinen
  * @since 31.7.2020, v1.2
  */
case class SaleGroup(id: Int, shopId: Int, groupIdentifier: String, amount: SaleAmount)
