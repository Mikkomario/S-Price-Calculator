package spadi.view.dialog

import spadi.model.stored.pricing.{Product, Shop}
import spadi.view.controller.PriceCompareRowVC
import spadi.view.util.Icons
import utopia.reflection.component.swing.template.AwtComponentRelated
import spadi.view.util.Setup._
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.swing.button.ImageButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.template.ComponentLike
import utopia.reflection.container.stack.StackLayout.{Leading, Trailing}
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Popup
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.LengthExtensions._

/**
  * This object allows one to display a price comparison pop-up for a product
  * @author Mikko Hilpinen
  * @since 22.8.2020, v1.2.1
  */
object PriceComparePopup
{
	private implicit val languageCode: String = "fi"
	
	/**
	  * Displays a price comparison pop-up over a component
	  * @param component Component this pop-up is displayed over
	  * @param product Product displayed on this view
	  * @param shops Known shops
	  */
	def displayOver(component: ComponentLike with AwtComponentRelated, product: Product, shops: Iterable[Shop]) =
	{
		val backgroundColor = primaryColors.bestAgainst(Vector(grayColors.dark, grayColors.default))
		val segmentGroup = new SegmentGroup(layouts = Vector(Trailing, Leading))
		
		// Creates the price comparison list
		val headerRow = baseContext.inContextWithBackground(grayColors.dark).forTextComponents().use { implicit c =>
			val headerRow = Stack.rowWithItems(segmentGroup.wrap(Vector[LocalizedString]("Tukku", "Hinta")
				.map { TextLabel.contextual(_) }), margins.small.any)
			headerRow.background = c.containerBackground
			headerRow
		}
		val productsStack = baseContext.inContextWithBackground(grayColors.default).forTextComponents().use { implicit c =>
			val stack = Stack.buildColumnWithContext(isRelated = true) { s =>
				s += headerRow
				product.shopData.toVector.sortBy { _.price.map { _.pricePerUnit }.getOrElse(100000.0) }
					.foreach { shopProduct =>
						s += new PriceCompareRowVC(segmentGroup, shopProduct, shops)
					}
			}
			stack.background = c.containerBackground
			stack
		}
		
		// Adds a close button and wraps in a popup
		val popup = baseContext.inContextWithBackground(backgroundColor).forTextComponents().use { implicit context =>
			val closeButton = ImageButton.contextualWithoutAction(Icons.close.asIndividualButton)
			val content = Stack.buildRowWithContext(layout = Leading, isRelated = true) { mainRow =>
				mainRow += productsStack
				mainRow += closeButton
			}.framed(margins.small.any, backgroundColor)
			
			val popup = Popup(component, content, actorHandler) { (cSize, pSize) =>
				Point(cSize.width + margins.medium, (cSize.height - pSize.height) / 2.0 ) }
			closeButton.registerAction(popup.close)
			
			popup
		}
		
		popup.display()
	}
}
