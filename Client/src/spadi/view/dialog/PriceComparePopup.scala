package spadi.view.dialog

import spadi.model.stored.pricing.{Product, Shop}
import spadi.view.controller.PriceCompareRowVC
import spadi.view.util.Icons
import utopia.reflection.component.swing.template.AwtComponentRelated
import spadi.view.util.Setup._
import utopia.genesis.handling.KeyStateListener
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
import utopia.reflection.shape.{StackInsets, StackLength}

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
		
		// Creates the price comparison list (header + a row for each price option)
		val rowCap = margins.small.downscaling
		val headerRow = baseContext.inContextWithBackground(grayColors.dark).forTextComponents().use { implicit c =>
			Stack.rowWithItems(segmentGroup.wrap(Vector[LocalizedString]("Tukku", "Hinta")
				.map { TextLabel.contextual(_) }), c.relatedItemsStackMargin)
				.framed(StackInsets.horizontal(rowCap), c.containerBackground)
		}
		val sortedPrices = product.shopData.toVector.sortBy { _.price.map { _.pricePerUnit }.getOrElse(100000.0) }
		// The cheapest price is displayed at the top and highlighted
		val firstRow = sortedPrices.headOption.map { price =>
			val highlightColor = secondaryColors.bestAgainst(Vector(grayColors.dark, grayColors.default))
			baseContext.inContextWithBackground(highlightColor).forTextComponents().use { implicit c =>
				val row = new PriceCompareRowVC(segmentGroup, price, shops)
				row.background = c.containerBackground
				row
			}
		}
		// Builds the products row list, which consists of 0-n elements
		val productsStack = baseContext.inContextWithBackground(grayColors.default).forTextComponents().use { implicit c =>
			Stack.buildColumn(StackLength.fixedZero) { stack =>
				stack += headerRow
				val moreRows = sortedPrices.drop(1).map { price => new PriceCompareRowVC(segmentGroup, price, shops) }
				stack +=
				{
					// Case: Multiple rows
					if (moreRows.nonEmpty)
						Stack.columnWithItems(firstRow.toVector ++ moreRows, c.relatedItemsStackMargin)
							.framed(rowCap, c.containerBackground)
					else
						firstRow match
						{
							// Case: Single row
							case Some(row) => row.framed(rowCap, c.containerBackground)
							// Case: No rows at all
							case None =>
								val label = TextLabel.contextual("Ei hintatietoja saatavilla", isHint = true)
								label.background = c.containerBackground
								label
						}
				}
			}
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
		// Closes the pop-up if any key is pressed
		popup.addKeyStateListener(KeyStateListener.onAnyKeyPressed { _ => popup.close() })
		
		popup.display()
	}
}
