package spadi.view.component

import spadi.controller.{Globals, Log}
import spadi.controller.database.access.multi.DbShopProducts
import spadi.view.util.Icons
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.reflection.component.context.ColorContext
import spadi.view.util.Setup._
import utopia.genesis.shape.Axis.X
import utopia.reflection.color.ColorRole.{Error, Warning}
import utopia.reflection.component.swing.button.ImageAndTextButton
import utopia.reflection.component.swing.label.{ImageLabel, TextLabel}
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.stack.StackLayout.{Center, Leading, Trailing}
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.interaction.ButtonColor
import utopia.reflection.shape.LengthExtensions.LengthNumber
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.stack.StackLength

import scala.util.{Failure, Success}

object Overview
{
	/**
	  * Creates a new overview panel
	  * @param context Color context in parent component (implicit)
	  * @return A new view
	  */
	def apply()(implicit context: ColorContext) = new Overview(context)
}

/**
  * Displays an overview about current component status
  * @author Mikko Hilpinen
  * @since 26.8.2020, v1.2
  */
class Overview(parentContext: ColorContext) extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES	---------------------------
	
	// Loads the data first
	private val data = connectionPool.tryWith { implicit connection =>
		DbShopProducts.count
	}
	
	private implicit val languageCode: String = "fi"
	private val context = parentContext.withLightGrayBackground
	
	// Converts read result to a view
	private val view = data match
	{
		case Success(data) =>
			val foundContent = data.exists { _._2 > 0 }
			val openDirectoryButton = context.forTextComponents()
				.forButtons(if (foundContent) ButtonColor.primary else ButtonColor.secondary)
				.use { implicit c =>
					ImageAndTextButton.contextual(Icons.folder.inButton, "Avaa luettava kansio") {
						Globals.fileInputDirectory.openInDesktop().failure.foreach { error =>
							Fields.errorDialog(s"Kansion avaaminen epäonnistui.\nVirheilmoitus: %s".autoLocalized
								.interpolated(Vector(error.getLocalizedMessage))).display(parentWindow)
						}
					}
				}
			
			// Case: No product data found => Displays a warning
			if (foundContent)
			{
				val segmentGroup = new SegmentGroup(X, Vector(Trailing, Leading))
				val colors = colorScheme.gray.values.filterNot { _ == context.containerBackground }
				val headerRow = context.inContextWithBackground(colors.last).forTextComponents().use { implicit c =>
					val row = Stack.rowWithItems(segmentGroup.wrap(
						Vector("Tukku", "Tuotteita").map { TextLabel.contextual(_) }), c.relatedItemsStackMargin)
					row.background = c.containerBackground
					row
				}
				val contentStack = context.inContextWithBackground(colors.head).forTextComponents().use { implicit c =>
					val stack = Stack.buildColumnWithContext(isRelated = true) { s =>
						data.toVector.sortBy { -_._2 }.foreach { case (shop, count) =>
							s += Stack.rowWithItems(segmentGroup.wrap(Vector(shop.name, count.toString)
								.map { TextLabel.contextual(_) }), c.relatedItemsStackMargin)
						}
					}
					stack.background = c.containerBackground
					stack
				}
				context.use { implicit c =>
					Stack.buildColumnWithContext() { s =>
						s += Stack.columnWithItems(Vector(headerRow, contentStack), StackLength.fixedZero)
						s += openDirectoryButton
					}.framed(margins.medium.any, c.containerBackground)
				}
			}
			// Case: Product data found. Lists how many products exist for each of the shops
			else
			{
				val warning = context.forChildComponentWithRole(Warning).forTextComponents().expandingToRight
					.use { implicit c =>
						Stack.buildRowWithContext(layout = Center, isRelated = true) { s =>
							s += ImageLabel.contextual(Icons.warning.singleColorImage)
							s += TextLabel.contextual("Tuotetietoja ei ole vielä luettu")
						}.framed(margins.small.any, c.containerBackground)
					}
				context.forTextComponents().mapFont { _ * 0.8 }.use { implicit c =>
					Stack.buildColumnWithContext(isRelated = true) { s =>
						s += warning
						s += TextLabel.contextual(
							"Sinun tulee lisätä .csv, .xlsx tai .xls -tiedostoja luettavaan kansioon")
						s += openDirectoryButton
					}.framed(margins.medium.any, c.containerBackground)
				}
			}
			
		// Case: Read failed => Displays an error
		case Failure(error) =>
			Log(error, "Failed to read shop products count")
			parentContext.forChildComponentWithRole(Error).forTextComponents().use { implicit c =>
				Stack.buildRowWithContext(layout = Center, isRelated = true) { s =>
					s += ImageLabel.contextual(Icons.warning.singleColorImage)
					s += TextLabel.contextual("Tuotetietojen lukeminen epäonnistui")
				}.framed(margins.small.any, c.containerBackground)
			}
	}
	
	override protected def wrapped = view
}
