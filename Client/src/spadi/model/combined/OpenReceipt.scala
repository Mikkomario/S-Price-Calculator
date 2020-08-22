package spadi.model.combined

import spadi.model.Extender
import spadi.model.stored.purchase.Receipt

/**
  * Combines receipt with associated product information
  * @author Mikko Hilpinen
  * @since 16.8.2020, v1.2.1
  */
case class OpenReceipt(wrapped: Receipt, products: Vector[CompleteReceiptProduct]) extends Extender[Receipt]
