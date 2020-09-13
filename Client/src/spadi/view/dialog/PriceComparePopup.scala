package spadi.view.dialog

import java.awt.event.KeyEvent

import spadi.controller.Log
import spadi.model.stored.pricing.{Product, Shop}
import spadi.view.component.Fields
import spadi.view.controller.PriceCompareRowVC
import spadi.view.util.{Browser, Icons}
import utopia.reflection.component.swing.template.AwtComponentRelated
import spadi.view.util.Setup._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.WaitUtils
import utopia.flow.util.TimeExtensions._
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.shape.shape2D.Point
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.{HandlerType, Mortal}
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.swing.button.{ImageAndTextButton, ImageButton}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.template.{ComponentLike, Focusable}
import utopia.reflection.container.stack.StackLayout.{Leading, Trailing}
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.{Popup, Window}
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.{WhenClickedOutside, WhenFocusLost}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.Alignment

/**
  * This object allows one to display a price comparison pop-up for a product
  * @author Mikko Hilpinen
  * @since 22.8.2020, v1.2.1
  */
object PriceComparePopup
{
	// ATTRIBUTES	----------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val focusDelay = 0.1.seconds
	
	
	// OTHER	--------------------------------
	
	/**
	  * Displays a price comparison pop-up over a component
	  * @param component Component this pop-up is displayed over
	  * @param product Product displayed on this view
	  * @param shops Known shops
	  * @param gainFocus Whether the new pop-up should gain focus
	  */
	def displayOver(component: ComponentLike with AwtComponentRelated, product: Product, shops: Iterable[Shop],
					gainFocus: Boolean = true) =
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
		val googleButton =
		{
			val searchWords = sortedPrices.headOption match
			{
				case Some(price) => (shops.find { _.id == price.shopId }.map { _.name }.toVector :+ price.name)
					.map { _.trim }.filterNot { _.isEmpty }
				case None => Vector()
			}
			if (Browser.isEnabled && searchWords.nonEmpty)
				Some(baseContext.inContextWithBackground(backgroundColor).forTextComponents().forPrimaryColorButtons
					.use { implicit c =>
						ImageAndTextButton.contextual(Icons.google.inButton, "Googlaa tuote") {
							Browser.google(product.electricId +: searchWords).failure.foreach { error =>
								Log(error, "Failed to open browser")
								Fields.errorDialog("Googlaus epäonnistui.\nVirheilmoitus: %s".autoLocalized
									.interpolated(Vector(error.getLocalizedMessage))).display(productsStack.parentWindow)
							}
						}
					})
			else
				None
		}
		val (popup, closeButton) = baseContext.inContextWithBackground(backgroundColor).forTextComponents().use { implicit context =>
			val closeButton = ImageButton.contextualWithoutAction(Icons.close.asIndividualButton)
			val mainStack = Stack.buildRowWithContext(layout = Leading, isRelated = true) { mainRow =>
				mainRow += productsStack
				mainRow += closeButton
			}
			val content = (googleButton match
			{
				case Some(button) =>
					Stack.buildColumnWithContext(isRelated = true) { s =>
						s += mainStack
						s += button
					}
				case None => mainStack
			}).framed(margins.small.any, backgroundColor)
			
			val popup = Popup(component, content, actorHandler, if (gainFocus) WhenFocusLost else WhenClickedOutside,
				Alignment.Left) { (cSize, pSize) => Point(cSize.width + margins.medium, (cSize.height - pSize.height) / 2.0 ) }
			closeButton.registerAction(popup.close)
			
			popup -> closeButton
		}
		// Closes the pop-up if any key is pressed (except button triggering keys)
		GlobalKeyboardEventHandler.registerKeyStateListener(new HidePopupOnKeyListener(popup, gainFocus))
		
		popup.display(gainFocus)
		// Makes the Google button the first focused component in the window
		if (gainFocus)
			googleButton.foreach { button => WaitUtils.delayed(focusDelay) { button.requestFocusInWindow() } }
		// Also, if this view didn't gain focus, will transfer focus on tab-key
		else
			component.addKeyStateListener(new MoveFocusToPopupListener(popup, googleButton.getOrElse(closeButton)))
	}
	
	
	// NESTED	----------------------------
	
	private class HidePopupOnKeyListener(window: Window[_], isFiltered: Boolean)
		extends KeyStateListener with Mortal with Handleable
	{
		override val keyStateEventFilter = KeyStateEvent.notKeysFilter(
			if (isFiltered) Vector(KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_TAB) else Vector(KeyEvent.VK_TAB))
		
		override def onKeyState(event: KeyStateEvent) = window.close()
		
		override def isDead = window.isClosed
	}
	
	private class MoveFocusToPopupListener(window: Window[_], firstFocusComponent: Focusable)
		extends KeyStateListener with Mortal
	{
		override val keyStateEventFilter = KeyStateEvent.keyFilter(KeyEvent.VK_TAB)
		
		override def allowsHandlingFrom(handlerType: HandlerType) = window.isVisible
		
		override def onKeyState(event: KeyStateEvent) =
		{
			window.requestFocus()
			WaitUtils.delayed(focusDelay) { firstFocusComponent.requestFocusInWindow() }
		}
		
		override def isDead = window.isClosed
	}
}
