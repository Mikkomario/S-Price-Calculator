package spadi.view.dialog

import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.genesis.shape.shape2D.Direction2D
import utopia.reflection.container.swing.window.dialog.interaction
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.container.swing.window.dialog.interaction.{ButtonColor, RowGroups}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._

/**
 * Used for editing and creating shops
 * @author Mikko Hilpinen
 * @since 26.5.2020, v1.1
 */
trait InputDialog[+A] extends interaction.InputDialog[A]
{
	// ATTRIBUTES   -------------------------
	
	private implicit val languageCode: String = "fi"
	
	protected val backgroundContext = baseContext.inContextWithBackground(primaryColors.dark)
	protected val inputContext = baseContext.inContextWithBackground(primaryColors).forTextComponents()
	
	
	// ABSTRACT -----------------------------
	
	/**
	 * @return Header component. None if no header should be present
	 */
	protected def header: Option[AwtStackable]
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def okButtonText = "OK"
	
	override protected def cancelButtonText = "Peruuta"
	
	override protected def okButtonIcon = Some(Icons.checkCircle)
	
	override protected def closeIcon = Icons.close
	
	override protected def fieldLabelContext = inputContext.withTextAlignment(Alignment.Right).expandingTo(Direction2D.Left)
	
	override protected def popupContext = baseContext.inContextWithBackground(colorScheme.error).forTextComponents()
	
	override protected def executionContext = exc
	
	override protected def buildLayout(rowGroups: Vector[RowGroups[AwtStackable]]) =
	{
		// Places each group in a single stack (ignores stacks when there is only a single group / item in a group)
		// Possible header is treated as an individual group
		val groupComponents = header.toVector ++ inputContext.use { implicit context =>
			rowGroups.map { group=>
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
	
	override protected def standardContext = backgroundContext
	
	override protected def buttonContext(buttonColor: ButtonColor, hasIcon: Boolean) =
		backgroundContext.forTextComponents().forButtons(buttonColor)
}
