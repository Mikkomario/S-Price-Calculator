package spadi.view.controller

import spadi.model.stored.pricing.{Shop, ShopProduct}
import spadi.view.util.Setup._
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.LengthExtensions._

/**
  * Used for displaying an individual product price when comparing prices between shops
  * @author Mikko Hilpinen
  * @since 22.8.2020, v1.2
  */
class PriceCompareRowVC(segmentGroup: SegmentGroup, initialContent: ShopProduct, shops: Iterable[Shop])
					   (implicit context: TextContextLike)
	extends StackableAwtComponentWrapperWrapper with Refreshable[ShopProduct]
{
	// ATTRIBUTES	------------------------------
	
	private implicit val languageCode: String = "fi"
	
	private var product = initialContent
	
	private val shopNameLabel = TextLabel.contextual(shops.find { _.id == initialContent.shopId }
		.map { _.name.noLanguageLocalizationSkipped }.getOrElse("Tuntematon tukku"))
	private val priceLabel = TextLabel.contextual(initialContent.price.map { _.toString }.getOrElse("? €/kpl")
		.noLanguageLocalizationSkipped)
	
	private val view = Stack.rowWithItems(segmentGroup.wrap(Vector(shopNameLabel, priceLabel)), margins.small.any)
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def wrapped = view
	
	override def content_=(newContent: ShopProduct) =
	{
		product = newContent
		shopNameLabel.text = shops.find { _.id == newContent.shopId }.map { _.name.noLanguageLocalizationSkipped }
			.getOrElse("Tuntematon tukku")
		priceLabel.text = newContent.price.map { _.toString }.getOrElse("? €/kpl").noLanguageLocalizationSkipped
	}
	
	override def content = product
}
