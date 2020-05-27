package spadi.view.dialog

import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.reflection.component.swing.TextField
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.container.swing.window.dialog.interaction.{ButtonColor, InputDialog, InputRowBlueprint, RowGroups}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._

/**
 * Used for editing and creating shops
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
class EditShopDialog(initialName: String = "") extends InputDialog[Option[String]]
{
	// ATTRIBUTES   -------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val backgroundContext = baseContext.inContextWithBackground(primaryColors.light)
	private val inputContext = baseContext.inContextWithBackground(primaryColors).forTextComponents()
	
	private val nameField = inputContext.forGrayFields.use { implicit c =>
		TextField.contextual(standardFieldWidth.any.expanding, initialText = initialName) }
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def okButtonText = "OK"
	
	override protected def cancelButtonText = "Peruuta"
	
	override protected def okButtonIcon = Some(Icons.checkCircle)
	
	override protected def closeIcon = Icons.close
	
	override protected def fieldLabelContext = inputContext.withTextAlignment(Alignment.Right)
	
	override protected def popupContext = baseContext.inContextWithBackground(colorScheme.error).forTextComponents()
	
	override protected def executionContext = exc
	
	override protected def fields = Vector(RowGroups.singleRow(new InputRowBlueprint("Tukun Nimi", nameField)))
	
	override protected def additionalButtons = Vector()
	
	override protected def buildLayout(inputRows: Vector[RowGroups[AwtStackable]]) =
	{
		// Places each group in a single stack (ignores stacks when there is only a single group / item in a group)
		val groupComponents = inputContext.use { implicit context =>
			inputRows.map { group =>
				val relatedItemStacks = group.groups.map { relatedRows =>
					if (relatedRows.isSingleRow)
						relatedRows.rows.head
					else
						Stack.buildColumnWithContext(isRelated = true) { s => relatedRows.rows.foreach { s += _ } }
				}
				val groupComponent =
				{
					if (relatedItemStacks.size == 1)
						relatedItemStacks.head
					else
						Stack.buildColumnWithContext() { s => relatedItemStacks.foreach { s += _ } }
				}
				groupComponent.framed(margins.small.any, context.containerBackground)
			}
		}
		if (groupComponents.size == 1)
			groupComponents.head
		else
			backgroundContext.use { implicit c => Stack.buildColumnWithContext() { s =>
				groupComponents.foreach { s += _ } } }
	}
	
	override protected def produceResult = nameField.value match
	{
		case Some(shopName) => Right(Some(shopName))
		case None => Left(nameField, "Annathan vielä tukun nimen?")
	}
	
	override protected def standardContext = backgroundContext
	
	override protected def buttonContext(buttonColor: ButtonColor, hasIcon: Boolean) =
		backgroundContext.forTextComponents().forButtons(buttonColor)
	
	override protected def defaultResult = None
	
	override protected def title = if (initialName.isEmpty) "Lisää tukku" else "Vaihda tukun nimi"
}
