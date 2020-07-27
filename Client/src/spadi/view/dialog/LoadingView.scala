package spadi.view.dialog

import java.time.Instant

import spadi.model.ProgressState
import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.event.Changing
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.WaitUtils
import utopia.flow.async.AsyncExtensions._
import utopia.genesis.animation.Animation
import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape2D.Direction2D.Up
import utopia.reflection.component.swing.display.ProgressBar
import utopia.reflection.component.swing.label.{AnimationLabel, TextLabel}
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.container.swing.window.{Dialog, Frame}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment.BottomLeft
import utopia.reflection.shape.{Alignment, StackLength, StackLengthModifier}
import utopia.reflection.shape.LengthExtensions._

import scala.concurrent.Future

/**
 * Used for displaying a loading window during an ongoing background process
 * @author Mikko Hilpinen
 * @since 8.7.2020, v1.1.2
 */
class LoadingView(progressPointer: Changing[ProgressState])
{
	// ATTRIBUTES   --------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val context = baseContext.inContextWithBackground(primaryColors).forTextComponents()
	
	private lazy val progressBar = context.use { implicit c =>
		ProgressBar.contextual(standardFieldWidth.any.expanding x margins.medium.downTo(margins.small),
			progressPointer.map { _.progress })
	}
	
	// The view consists of an animated label on the left, followed by a description and a progress bar combination
	private lazy val view =
	{
		val loadingLabel = context.use { implicit c =>
			val loadingIcon = Icons.large.loading
			AnimationLabel.contextualWithRotatingImage(loadingIcon, loadingIcon.size.toPoint / 2,
				Animation { p => Rotation.ofCircles(p) }.over(1.seconds))
		}
		val progressStack = context.expandingToRight.withTextAlignment(BottomLeft).expandingTo(Up).use { implicit c =>
			val statusLabel = TextLabel.contextual(progressPointer.value.description)
			statusLabel.addWidthConstraint(new NoShrinkingConstraint(standardFieldWidth.any))
			// Status label updates automatically
			progressPointer.addListener { e => statusLabel.text = e.newValue.description }
			
			Stack.buildColumnWithContext(isRelated = true) { s =>
				s += statusLabel
				s += progressBar
			}
		}
		context.use { implicit c =>
			Stack.buildRowWithContext(layout = Center) { s =>
				s += loadingLabel
				s += progressStack
			}.framed(margins.medium.upscaling.square, c.containerBackground)
		}
	}
	
	
	// OTHER    ------------------------------
	
	/**
	 * Displays this loading view
	 * @param parentWindow Window over which this view is displayed. Optional.
	 * @return A future of the background progress completion and closing of this view.
	 */
	def display(parentWindow: Option[java.awt.Window] = None) =
	{
		// Presents the window only if there is some loading still to be done
		if (progressPointer.value.progress < 1)
		{
			val loadingStarted = Instant.now()
			
			val title: LocalizedString = "Käsitellään..."
			val window = parentWindow match
			{
				case Some(parent) => new Dialog(parent, view, title, Program, Alignment.Left)
				case None => Frame.windowed(view, title, Program, Alignment.Left)
			}
			
			// Delays the window display a little, in case the loading progress was very short
			WaitUtils.delayed((loadingStarted + 0.25.seconds) - Instant.now()) {
				if (progressPointer.value.progress < 1)
				{
					// Displays the window
					window.startEventGenerators(actorHandler)
					window.display()
					
					// Closes the window once background processing has completed
					progressBar.completionFuture.waitFor()
					window.close()
				}
				else
					window.close()
			}
		}
		else
			Future.successful(())
	}
	
	/**
	 * Displays this loading view
	 * @param parentWindow Window over which this view is displayed.
	 * @return A future of the background progress completion and closing of this view.
	 */
	def displayOver(parentWindow: java.awt.Window) = display(Some(parentWindow))
	
	
	// NESTED   ------------------------------
	
	private class NoShrinkingConstraint(startLength: StackLength) extends StackLengthModifier
	{
		// ATTRIBUTES   ----------------------
		
		private var currentLength = startLength.expanding
		
		
		// IMPLEMENTED  ----------------------
		
		override def apply(length: StackLength) =
		{
			if (length.optimal > currentLength.optimal)
				currentLength = length.expanding
			
			currentLength
		}
	}
}
