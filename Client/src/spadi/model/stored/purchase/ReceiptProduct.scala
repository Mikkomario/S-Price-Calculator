package spadi.model.stored.purchase

import spadi.model.partial.purchase.ReceiptProductData
import spadi.model.stored.Stored

/**
  * Represents a chosen product
  * @author Mikko Hilpinen
  * @since 16.8.2020, v1.2.1
  */
case class ReceiptProduct(id: Int, receiptEditId: Int, data: ReceiptProductData) extends Stored[ReceiptProductData]
