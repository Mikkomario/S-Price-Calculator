package spadi.view.controller

import spadi.controller.ProfitsPercentage
import spadi.model.stored.pricing.{Product, Shop}
import spadi.view.dialog.PriceComparePopup
import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.util.StringExtensions.ExtendedString
import utopia.reflection.component.context.{ColorContext, TextContext}
import utopia.reflection.component.swing.button.ImageButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.LengthExtensions._

object ProductRowVC
{
	/**
	  * @param group Segmented group used to lay out this row
	  * @param product First displayed product
	  * @param shops Known shops
	  * @param context Component creation context (implicit)
	  * @return New product row
	  */
	def apply(group: SegmentGroup, product: Product, shops: Iterable[Shop])(implicit context: ColorContext) =
		new ProductRowVC(group, product, shops)(context)
}

/**
 * Displays product's information on a row
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1
 */
class ProductRowVC(segmentGroup: SegmentGroup, initialProduct: Product, shops: Iterable[Shop])
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
	private val savingsLabel = TextLabel.contextual()
	private val moreButton = ImageButton.contextualWithoutAction(Icons.more.asIndividualButtonWithColor(primaryColors))
	
	private val row = Stack.rowWithItems(segmentGroup.wrap(Vector(idLabel, nameLabel, priceLabel,
		profitLabel, finalPriceLabel, savingsLabel, moreButton)), margins.medium.any)
	
	
	// INITIAL CODE -------------------------------
	
	updateLabels()
	moreButton.registerAction(showDetails)
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def wrapped = row
	
	override def content_=(newContent: Product) =
	{
		_content = newContent
		updateLabels()
	}
	
	override def content = _content
	
	
	// OTHER    -----------------------------------
	
	/**
	  * Displays more details about this product's price options
	  */
	def showDetails() = PriceComparePopup.displayOver(moreButton, _content, shops)
	
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
		// Updates savings label
		val savingsText = content.cheapestPrice match
		{
			case Some(cheapest) =>
				val alternativePrices = content.alternativePrices
				if (alternativePrices.isEmpty)
					"---"
				else
				{
					val sameUnitPrices = alternativePrices.filter { _.unit ~== cheapest.unit }
					if (sameUnitPrices.isEmpty)
						s"? €/${cheapest.unit}"
					else
					{
						// Rounds savings to 1 decimal
						val savings = sameUnitPrices.map { _.pricePerUnit - cheapest.pricePerUnit }
							.map { amount => math.round(amount * 10) / 10.0 }.filter { _ > 0 }
						if (savings.size > 1 && savings.last > savings.head)
							s"${savings.head}-${savings.last} €/${cheapest.unit}"
						else if (savings.nonEmpty)
							s"${savings.head} €/${cheapest.unit}"
						else
							s"0 €/${cheapest.unit}"
					}
				}
			case None => "? €/kpl"
		}
		savingsLabel.text = savingsText.noLanguageLocalizationSkipped
	}
	
	private def displayNameFor(product: Product) =
	{
		val shopName = product.cheapestShopId.flatMap { id => shops.find { _.id == id } }.map { _.name }
			.getOrElse("Tuntematon Tukku")
		s"${product.name} ($shopName)".noLanguageLocalizationSkipped
	}
}
