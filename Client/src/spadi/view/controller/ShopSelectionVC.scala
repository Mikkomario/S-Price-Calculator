package spadi.view.controller

import spadi.controller.ShopData
import spadi.model.Shop
import spadi.view.component.Fields
import spadi.view.dialog.EditShopDialog
import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.reflection.component.Focusable
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.input.Input
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.swing.button.ImageButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.Stack
import utopia.reflection.localization.DisplayFunction

/**
 * A drop down field used for selecting shops
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
class ShopSelectionVC(implicit context: ButtonContextLike) extends StackableAwtComponentWrapperWrapper
	with Input[Option[Shop]] with Focusable
{
	// ATTRIBUTES   ---------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val noResultView = TextLabel.contextual("Yhtään tukkua ei ole vielä rekisteröity")
	private val dd = Fields.dropDown[Shop]("Yhtään tukkua ei ole vielä rekisteröity",
		"Valitse Tukku", DisplayFunction.noLocalization[Shop] { _.name },
		sameInstanceCheck = _.id == _.id, contentIsStateless = false)
	// Edit button is used for renaming the shop
	private val editButton = ImageButton.contextual(Icons.edit.asIndividualButtonWithColor(primaryColors)) {
		dd.parentWindow.foreach { window =>
			dd.selected.foreach { shopToEdit =>
				new EditShopDialog(shopToEdit.name).display(window)
					.foreach { _.filterNot { _ == shopToEdit.name }.foreach { newName =>
						ShopData.renameShopWithId(shopToEdit.id, newName)
					} }
			}
		}
	}
	private val addButton = ImageButton.contextual(Icons.plus.asIndividualButtonWithColor(primaryColors)) {
		dd.parentWindow.foreach { window =>
			new EditShopDialog().display(window).foreach { _.foreach { newShopName =>
				val newShop = ShopData.addShop(newShopName)
				dd.selectOne(newShop)
			} }
		}
	}
	
	private val view = Stack.buildRowWithContext(isRelated = true) { s =>
		s += dd
		s += editButton
		s += addButton
	}
	
	
	// INITIAL CODE ----------------------------
	
	noResultView.background = context.buttonColor
	
	// Updates drop down options whenever container content changes
	ShopData.shopsPointer.addListener(ContentUpdator, Some(Vector()))
	
	
	// IMPLEMENTED  ----------------------------
	
	override def requestFocusInWindow() = dd.requestFocusInWindow()
	
	override def value = dd.value
	
	override protected def wrapped = view
	
	
	// OTHER    --------------------------------
	
	/**
	 * Finalizes this VC for removal, removing any listeners & dependencies
	 */
	def end() = ShopData.shopsPointer.removeListener(ContentUpdator)
	
	
	// NESTED   --------------------------------
	
	private object ContentUpdator extends ChangeListener[Vector[Shop]]
	{
		override def onChangeEvent(event: ChangeEvent[Vector[Shop]]) = dd.content = event.newValue
	}
}
