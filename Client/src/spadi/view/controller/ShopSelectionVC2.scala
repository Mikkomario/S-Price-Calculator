package spadi.view.controller

import spadi.controller.Log
import spadi.controller.database.access.multi.DbShops
import spadi.model.stored.pricing.Shop
import spadi.view.component.Fields
import spadi.view.dialog.EditShopWindow
import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.swing.button.ImageButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.template.Focusable
import utopia.reflection.component.template.input.Interaction
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.localization.LocalString._

import scala.util.{Failure, Success}

/**
 * A drop down field used for selecting shops
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
class ShopSelectionVC2(shopsPointer: PointerWithEvents[Vector[Shop]])(implicit context: ButtonContextLike)
	extends StackableAwtComponentWrapperWrapper with Interaction[Option[Shop]] with Focusable
{
	// ATTRIBUTES   ---------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val noResultView = TextLabel.contextual("Yhtään tukkua ei ole vielä rekisteröity")
	private val dd = Fields.dropDown[Shop]("Yhtään tukkua ei ole vielä rekisteröity",
		"Valitse Tukku", DisplayFunction.noLocalization[Shop] { _.name },
		sameInstanceCheck = _.id == _.id, contentIsStateless = false)
	// Edit button is used for renaming the shop (temporarily disabled)
	/*
	private val editButton = ImageButton.contextual(Icons.edit.asIndividualButtonWithColor(primaryColors)) {
		dd.parentWindow.foreach { window =>
			dd.selected.foreach { shopToEdit =>
				new EditShopWindow(shopToEdit.name).displayOver(window)
					.foreach { _.filterNot { _ == shopToEdit.name }.foreach { newName =>
						ShopData.renameShopWithId(shopToEdit.id, newName)
					} }
			}
		}
	}*/
	private val addButton = ImageButton.contextual(Icons.plus.asIndividualButtonWithColor(primaryColors)) {
		dd.parentWindow.foreach { window =>
			// Requests shop name in a separate dialog
			new EditShopWindow().displayOver(window).foreach { _.foreach { newShopName =>
				// Adds the new shop to the DB
				connectionPool.tryWith { implicit connection => DbShops.insert(newShopName) } match
				{
					case Success(shop) =>
						// Adds the shop to the list of selectable shops and selects it
						println(s"Adding shop $shop")
						shopsPointer.value :+= shop
						dd.selectOne(shop)
					case Failure(error) =>
						// Displays error message
						Log(error, "Failed to insert a new shop to DB")
						Fields.errorDialog("Tukun lisääminen epäonnistui.\nVirheilmoitus: %s"
							.autoLocalized.interpolated(Vector(error.getLocalizedMessage))).displayOver(window)
				}
			} }
		}
	}
	
	private val view = Stack.buildRowWithContext(isRelated = true) { s =>
		s += dd
		// s += editButton
		s += addButton
	}
	
	
	// INITIAL CODE ----------------------------
	
	noResultView.background = context.buttonColor
	
	// Updates drop down options whenever container content changes
	shopsPointer.addListener(ContentUpdator, Some(Vector()))
	
	
	// IMPLEMENTED  ----------------------------
	
	override def requestFocusInWindow() = dd.requestFocusInWindow()
	
	override def value = dd.value
	
	override def value_=(newValue: Option[Shop]) = dd.value = newValue
	
	override protected def wrapped = view
	
	
	// OTHER    --------------------------------
	
	/**
	 * Finalizes this VC for removal, removing any listeners & dependencies
	 */
	def end() = shopsPointer.removeListener(ContentUpdator)
	
	
	// NESTED   --------------------------------
	
	private object ContentUpdator extends ChangeListener[Vector[Shop]]
	{
		override def onChangeEvent(event: ChangeEvent[Vector[Shop]]) = dd.content = event.newValue
	}
}
