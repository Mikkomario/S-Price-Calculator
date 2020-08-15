package spadi.view.component

import spadi.view.util.Icons
import utopia.reflection.component.context.{ButtonContext, ColorContext}
import utopia.reflection.component.swing.button.{ButtonLike, ImageAndTextButton, ImageButton}
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.localization.LocalizedString
import spadi.view.util.Setup._
import utopia.genesis.shape.shape2D.{Direction2D, Point}
import utopia.genesis.util.Screen
import utopia.reflection.component.swing.display.MultiLineTextView
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Popup
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._

object Warning
{
	/**
	  * Creates a new warning button with an action
	  * @param text Text displayed on warning pop-up
	  * @param isCritical Whether this warning should be marked as critical (default = false)
	  * @param makeButton A function for making the action button
	  * @param context Context for this button
	  * @return A warning button
	  */
	def actionable(text: LocalizedString, isCritical: Boolean = false)
				  (makeButton: ButtonContext => AwtStackable with ButtonLike)(implicit context: ColorContext) =
		new Warning(text, Some(makeButton), isCritical)(context)
	
	/**
	  * Creates a new warning button with message only
	  * @param text Text displayed on warning pop-up
	  * @param isCritical Whether this warning should be marked as critical (default = false)
	  * @param context Context for this button
	  * @return A warning button
	  */
	def nonActionable(text: LocalizedString, isCritical: Boolean = false)(implicit context: ColorContext) =
		new Warning(text, None, isCritical)(context)
}

/**
  * Used for displaying a warning to the user. More details & actions are shown on click
  * @author Mikko Hilpinen
  * @since 15.8.2020, v1.2
  */
class Warning(text: LocalizedString, makeButton: Option[ButtonContext => AwtStackable with ButtonLike] = None,
			  isCritical: Boolean = false)(context: ColorContext)
	extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES	-------------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val color = if (isCritical) colorScheme.error else warningColors.forBackground(context.containerBackground)
	private val button = context.use { implicit c =>
		ImageButton.contextual(Icons.warning.asIndividualButtonWithColor(color)) { displayPopup() } }
	
	
	// IMPLEMENTED	-------------------------------
	
	override protected def wrapped = button
	
	
	// OTHER	----------------------------------
	
	private def displayPopup(): Unit =
	{
		val textContext = baseContext.inContextWithBackground(color).forTextComponents()
		val textView = textContext.use { implicit c => MultiLineTextView.contextual(text, Screen.width / 3,
			useLowPriorityForScalingSides = true) }
		val (stack, closeButton) = makeButton match
		{
			case Some(makeButton) =>
				// If specific action button is defined, displays it with the close button at the bottom right corner
				textContext.forPrimaryColorButtons.use { implicit c =>
					val closeButton = ImageAndTextButton.contextualWithoutAction(Icons.close.inButton, "OK")
					val stack = Stack.buildColumnWithContext() { mainStack =>
						mainStack += textView
						mainStack += Stack.buildRowWithContext() { buttonRow =>
							buttonRow += makeButton(c)
							buttonRow += closeButton
						}.alignedToSide(Direction2D.Right)
					}
					stack -> closeButton
				}
			case None =>
				// If no action button is defined, uses an iconified close button at the right side of the pop-up
				textContext.use { implicit c =>
					val closeButton = ImageButton.contextualWithoutAction(Icons.close.asIndividualButton)
					val stack = Stack.buildRowWithContext(layout = Center) { s =>
						s += textView
						s += closeButton
					}
					stack -> closeButton
				}
		}
		val popup = Popup(button, stack.framed(margins.small.any, color), actorHandler,
			resizeAlignment = Alignment.Left) { (btnSize, windowSize) =>
				Point(btnSize.width + margins.medium, (btnSize.height - windowSize.height) / 2) }
		closeButton.registerAction(popup.close)
		
		popup.display()
	}
}
