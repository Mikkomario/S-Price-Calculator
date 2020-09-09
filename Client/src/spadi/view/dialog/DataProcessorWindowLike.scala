package spadi.view.dialog

import java.nio.file.Path

import spadi.controller.read.DataProcessor
import spadi.model.cached.read.{InputField, KeyMapping, KeyMappingFactory}
import spadi.model.stored.pricing.Shop
import spadi.view.component.Fields
import spadi.view.util.Icons
import spadi.view.util.Setup._
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.genesis.util.Screen
import utopia.reflection.component.swing.display.MultiLineTextView
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.template.Focusable
import utopia.reflection.container.stack.StackLayout.Leading
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.window.interaction.{DialogButtonBlueprint, InputRowBlueprint, RowGroups}
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.Alignment.BottomLeft
import utopia.reflection.shape.LengthExtensions._

import scala.util.{Failure, Success}

/**
 * A common trait for dialogs used for creating new data processors.
 * Returns either Right: a data source or Left: whether previous dialog should be shown instead
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
  * @tparam A Type of processed input
  * @tparam KF Key field type
 */
abstract class DataProcessorWindowLike[A, +M <: KeyMapping[A], KF <: AwtStackable with Focusable]
(path: Path, shop: Shop, mappingFactory: KeyMappingFactory[A, M])(makeProcessor: (Path, M) => DataProcessor[A, M])
	extends InputWindow[Either[Boolean, DataProcessor[A, M]]]
{
	// ATTRIBUTES   ------------------------
	
	private implicit val languageCode: String = "fi"
	
	/**
	 * Default input field width
	 */
	protected val fieldWidth = standardFieldWidth.any.expanding
	
	private lazy val (inputComponents, inputRows) = inputContext.forGrayFields.use { implicit context =>
		mappingFactory.fields.splitMap { fieldSpec =>
			val field = keyField(fieldSpec)
			(fieldSpec.name, field, fieldSpec.isRequired) -> new InputRowBlueprint(fieldSpec.name, field)
		}
	}
	
	
	// ABSTRACT ----------------------------
	
	protected def keyField(specification: InputField): KF
	
	protected def valueOfField(field: KF): Value
	
	protected def setFieldValue(field: KF, value: Value): Unit
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Current field input, each value tied to assiciated field name (based on mapping factory)
	 */
	def input = Model(keyFieldsWithNames.map { case (name, field) => name -> valueOfField(field) })
	
	/**
	 * Updates current input field content
	 * @param newInput New input field values. Values are read from properties matching field names (determined by
	 *                 the used mapping factory).
	 */
	def input_=(newInput: template.Model[Property]) =
		keyFieldsWithNames.foreach { case (name, field) => setFieldValue(field, newInput(name)) }
	
	private def keyFieldsWithNames = inputComponents.map { case (name, field, _) =>
		name.string -> field }
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def header =
	{
		// TODO: Possibly add a progress bar in the header
		
		val titleLabel = backgroundContext.forTextComponents().expandingToRight.mapFont { _ * 1.2 }.use { implicit titleC =>
			TextLabel.contextual("${shop}-tiedoston (${fileName}) lukeminen".localized.interpolated(
				Map("shop" -> shop.name, "fileName" -> path.fileName)))
		}
		Some(backgroundContext.forTextComponents().use { implicit context =>
			Stack.buildColumnWithContext(isRelated = true) { s =>
				s += titleLabel
				s += Stack.buildRowWithContext(layout = Leading) { row =>
					row += MultiLineTextView.contextual(
						"Ohje: Kirjoita alle minkä nimisestä kolumnista kukin tieto haetaan", Screen.width / 3,
						useLowPriorityForScalingSides = true, isHint = true)
					row += context.forPrimaryColorButtons.use { implicit btnC =>
						Fields.openFileButton(path, visibleDialogs.headOption.map { _.component })
					}
				}
			}
		})
	}
	
	override protected def fields = Vector(RowGroups.separateGroups(inputRows))
	
	override protected def additionalButtons = Vector(new DialogButtonBlueprint[Either[Boolean, DataProcessor[A, M]]](
		"Edellinen", Some(Icons.previous), location = BottomLeft)({ () => Some(Left(true)) -> true }))
	
	override protected def produceResult =
	{
		// Makes sure all required values are defined
		val valuesAndFields = inputComponents.map { case (fieldName, field, isRequired) =>
			(fieldName, field, isRequired, valueOfField(field)) }
		valuesAndFields.findMap { case (_, field, isRequired, value) =>
			if (isRequired && value.isEmpty) Some(field) else None } match
		{
			case Some(missingField) => Left(missingField, "Tämä tieto on pakollinen")
			case None =>
				// Parses the key mapping based on fields
				mappingFactory(Model(valuesAndFields.map { case (fieldName, _, _, value) => fieldName.string -> value })) match
				{
					case Success(parsed) =>
						// Creates a data source based on the provided data
						Right(Right(makeProcessor(path, parsed)))
					case Failure(error) =>
						Left(inputComponents.head._2,
							"Odottamaton virhe: %s".localized.interpolated(Vector(error.getLocalizedMessage)))
				}
		}
	}
	
	override protected def defaultResult = Left(false)
	
	override protected def title = "Tiedoston lukeminen"
}
