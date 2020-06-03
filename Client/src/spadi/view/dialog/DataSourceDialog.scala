package spadi.view.dialog

import java.awt.Window
import java.nio.file.Path

import spadi.view.util.Setup._
import spadi.model.{KeyMapping, KeyMappingFactory, Shop}
import spadi.view.component.Fields
import spadi.view.util.Icons
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.util.FileExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.generic.ValueConversions._
import utopia.reflection.component.swing.{MultiLineTextView, TextField}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.window.dialog.interaction.{DialogButtonBlueprint, InputRowBlueprint, RowGroups}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.Alignment.BottomLeft
import utopia.reflection.util.Screen
import utopia.reflection.shape.LengthExtensions._

import scala.util.{Failure, Success}

/**
 * Used for creating new data sources. Returns either Right: a key mapping or Left: whether previous dialog
 * should be shown instead
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
// TODO: Handle header row index and first data row index too
class DataSourceDialog[+A](parentWindow: Window, path: Path, shop: Shop, mappingFactory: KeyMappingFactory[A])
	extends InputDialog[Either[Boolean, KeyMapping[A]]]
{
	// ATTRIBUTES   ------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val (inputComponents, inputRows) = inputContext.forGrayFields.use { implicit context =>
		val fieldWidth = standardFieldWidth.any.expanding
		mappingFactory.fieldNames.splitMap { case (fieldName, isRequired) =>
			val field = TextField.contextual(fieldWidth, prompt = if (isRequired) None else Some("Vapaaehtoinen"))
			(fieldName, field, isRequired) -> new InputRowBlueprint(fieldName, field)
		}
	}
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Current field input, each value tied to assiciated field name (based on mapping factory)
	 */
	def input = Model(inputComponents.map { case (name, field, _) =>
		name.string -> field.value
	})
	/**
	 * Updates current input field content
	 * @param newInput New input field values. Values are read from properties matching field names (determined by
	 *                 the used mapping factory).
	 */
	def input_=(newInput: template.Model[Property]) = inputComponents.foreach { case (name, field, _) =>
		field.text = newInput(name.string).string
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def header =
	{
		// TODO: Possibly add a progress bar in the header
		
		val titleLabel = backgroundContext.forTextComponents().expandingToRight.mapFont { _ * 1.5 }.use { implicit titleC =>
			TextLabel.contextual("${shop}-tiedoston (${fileName}) lukeminen".localized.interpolated(
				Map("shop" -> shop.name, "fileName" -> path.fileName)))
		}
		Some(backgroundContext.forTextComponents().use { implicit context =>
			Stack.buildColumnWithContext(isRelated = true) { s =>
				s += titleLabel
				s += Stack.buildRowWithContext() { row =>
					row += MultiLineTextView.contextual(
						"Ohje: Kirjoita alle minkä nimisestä kolumnista kukin tieto haetaan", Screen.width / 3,
						useLowPriorityForScalingSides = true, isHint = true)
					row += context.forPrimaryColorButtons.use { implicit btnC =>
						Fields.openFileButton(path, Some(parentWindow))
					}
				}
			}
		})
	}
	
	override protected def fields = Vector(RowGroups.separateGroups(inputRows))
	
	override protected def additionalButtons = Vector(new DialogButtonBlueprint[Either[Boolean, KeyMapping[A]]](
		"Edellinen", Some(Icons.previous), location = BottomLeft)({ () => Some(Left(true)) -> true }))
	
	override protected def produceResult =
	{
		// Makes sure all required values are defined
		val valuesAndFields = inputComponents.map { case (fieldName, field, isRequired) =>
			(fieldName, field, isRequired, field.value) }
		valuesAndFields.findMap { case (_, field, isRequired, value) =>
			if (isRequired && value.isEmpty) Some(field) else None } match
		{
			case Some(missingField) => Left(missingField, "Tämä tieto on pakollinen")
			case None =>
				mappingFactory(Model(valuesAndFields.map { case (fieldName, _, _, value) =>
					fieldName.string -> (value: Value)
				})) match
				{
					case Success(parsed) => Right(Right(parsed))
					case Failure(error) =>
						Left(inputComponents.head._2,
							"Odottamaton virhe: %s".localized.interpolated(Vector(error.getLocalizedMessage)))
				}
		}
	}
	
	override protected def defaultResult = Left(false)
	
	override protected def title = "Tiedoston lukeminen"
}
