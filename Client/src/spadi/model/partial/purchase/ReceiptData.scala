package spadi.model.partial.purchase

/**
  * Contains information about a combined products list
  * @author Mikko Hilpinen
  * @since 16.8.2020, v1.2.1
  * @param name Name / summary of this receipt
  * @param products List of chosen products
  */
case class ReceiptData(name: String, products: Set[ReceiptProductData])
