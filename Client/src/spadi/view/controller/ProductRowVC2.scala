package spadi.view.controller

import spadi.controller.ProfitsPercentage
import spadi.model.stored.pricing.{Product, Shop}
import spadi.view.util.Setup._
import utopia.reflection.component.context.{ColorContext, TextContext}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.LengthExtensions._

object ProductRowVC2
{
	/**
	  * @param group Segmented group used to lay out this row
	  * @param product First displayed product
	  * @param shops Known shops
	  * @param context Component creation context (implicit)
	  * @return New product row
	  */
	def apply(group: SegmentGroup, product: Product, shops: Iterable[Shop])(implicit context: ColorContext) =
		new ProductRowVC2(group, product, shops)(context)
}

/**
 * Displays product's information on a row
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class ProductRowVC2(segmentGroup: SegmentGroup, initialProduct: Product, shops: Iterable[Shop])
				   (parentContext: ColorContext)
	extends StackableAwtComponentWrapperWrapper with Refreshable[Product]
{
	// ATTRIBUTES   -------------------------------
	
	private var _content = initialProduct
	
	private implicit val context: TextContext = parentContext.forTextComponents()
	
	private val idLabel = TextLabel.contextual()
	private val nameLabel = TextLabel.contextual()(context.mapFont { _ * 0.8 })
	private val priceLabel = TextLabel.contextual()
	private val profitLabel = TextLabel.contextual()
	private val finalPriceLabel = TextLabel.contextual()
	
	private val row = Stack.rowWithItems(segmentGroup.wrap(Vector(idLabel, nameLabel, priceLabel,
		profitLabel, finalPriceLabel)), margins.medium.any)
	
	
	// INITIAL CODE -------------------------------
	
	updateLabels()
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def wrapped = row
	
	override def content_=(newContent: Product) =
	{
		_content = newContent
		updateLabels()
	}
	
	override def content = _content
	
	
	// OTHER    -----------------------------------
	
	private def percentString(percentage: Double) =
	{
		val rounded = math.round(percentage).toInt
		s"$rounded%"
	}
	
	private def updateLabels() =
	{
		idLabel.text = content.electricId.noLanguageLocalizationSkipped
		nameLabel.text = displayNameFor(content)
		priceLabel.text = content.cheapestPrice.map { _.toString }.getOrElse("? €/kpl").noLanguageLocalizationSkipped
		val profitsPercentage = content.cheapestPrice.map { p => ProfitsPercentage.forPrice(p.amount) }
		profitLabel.text = profitsPercentage.map(percentString).getOrElse("?%").noLanguageLocalizationSkipped
		finalPriceLabel.text = content.cheapestPrice.flatMap { original => profitsPercentage.map { profit =>
			(original * (1 + profit / 100.0)).toString } }.getOrElse("? €/kpl").noLanguageLocalizationSkipped
	}
	
	private def displayNameFor(product: Product) =
	{
		val shopName = product.cheapestShopId.flatMap { id => shops.find { _.id == id } }.map { _.name }
			.getOrElse("Tuntematon Tukku")
		s"${product.name} ($shopName)".noLanguageLocalizationSkipped
	}
}
