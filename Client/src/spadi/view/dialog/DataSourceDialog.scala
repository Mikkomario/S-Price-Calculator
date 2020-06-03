package spadi.view.dialog

import java.nio.file.Path

import spadi.view.util.Setup._
import spadi.model.{DataSource, KeyMappingFactory, Shop}
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
import utopia.reflection.localization.LocalString
import utopia.reflection.localization.LocalString._
import utopia.reflection.shape.Alignment.BottomLeft
import utopia.reflection.util.Screen
import utopia.reflection.shape.LengthExtensions._

import scala.util.{Failure, Success}

/**
 * Used for creating new data sources. Returns either Right: a data source or Left: whether previous dialog
 * should be shown instead
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
class DataSourceDialog[+A](path: Path, shop: Shop, mappingFactory: KeyMappingFactory[A])
	extends InputDialog[Either[Boolean, DataSource[A]]]
{
	// ATTRIBUTES   ------------------------
	
	private implicit val languageCode: String = "fi"
	
	private val fieldWidth = standardFieldWidth.any.expanding
	
	private val headerRowFieldName: LocalString = "Otsikkorivi"
	private val firstDataRowFieldName: LocalString = "Ensimmäinen tuoterivi"
	
	private val (headerRowField, firstDataRowField) = inputContext.forGrayFields.use { implicit context =>
		TextField.contextualForPositiveInts(fieldWidth, prompt = Some("Ensimmäinen rivi on 1")) ->
			TextField.contextualForPositiveInts(fieldWidth, prompt = Some("Yleensä otsikkorivi +1"))
	}
	
	private val (inputComponents, inputRows) = inputContext.forGrayFields.use { implicit context =>
		mappingFactory.fieldNames.splitMap { case (fieldName, isRequired) =>
			val field = TextField.contextual(fieldWidth, prompt = if (isRequired) None else Some("Vapaaehtoinen"))
			(fieldName, field, isRequired) -> new InputRowBlueprint(fieldName, field)
		}
	}
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Current field input, each value tied to assiciated field name (based on mapping factory)
	 */
	def input = Model(fieldsWithNames.map { case (name, field) => name -> field.value })
	/**
	 * Updates current input field content
	 * @param newInput New input field values. Values are read from properties matching field names (determined by
	 *                 the used mapping factory).
	 */
	def input_=(newInput: template.Model[Property]) = fieldsWithNames.foreach { case (name, field) =>
		field.text = newInput(name).string }
	
	private def fieldsWithNames = inputComponents.map { case (name, field, _) =>
		name.string -> field } :+ (headerRowFieldName.string -> headerRowField) :+
		(firstDataRowFieldName.string -> firstDataRowField)
	
	
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
						Fields.openFileButton(path, visibleDialogs.headOption.map { _.component })
					}
				}
			}
		})
	}
	
	override protected def fields = Vector(
		RowGroups.separateGroups(inputRows),
		RowGroups.singleGroup(
			new InputRowBlueprint(headerRowFieldName, headerRowField),
			new InputRowBlueprint(firstDataRowFieldName, firstDataRowField))
	)
	
	override protected def additionalButtons = Vector(new DialogButtonBlueprint[Either[Boolean, DataSource[A]]](
		"Edellinen", Some(Icons.previous), location = BottomLeft)({ () => Some(Left(true)) -> true }))
	
	override protected def produceResult =
	{
		// Both header row index and first data row index are required
		headerRowField.intValue.filter { _ > 0 } match
		{
			case Some(headerRow) =>
				firstDataRowField.intValue.filter { _ > 0 } match
				{
					case Some(firstDataRow) =>
						// Makes sure all required values are defined
						val valuesAndFields = inputComponents.map { case (fieldName, field, isRequired) =>
							(fieldName, field, isRequired, field.value) }
						valuesAndFields.findMap { case (_, field, isRequired, value) =>
							if (isRequired && value.isEmpty) Some(field) else None } match
						{
							case Some(missingField) => Left(missingField, "Tämä tieto on pakollinen")
							case None =>
								// Parses the key mapping based on fields
								mappingFactory(Model(valuesAndFields.map { case (fieldName, _, _, value) =>
									fieldName.string -> (value: Value)
								})) match
								{
									case Success(parsed) =>
										// Creates a data source based on the provided data
										Right(Right(DataSource(path, parsed, headerRow - 1, firstDataRow - 1)))
									case Failure(error) =>
										Left(inputComponents.head._2,
											"Odottamaton virhe: %s".localized.interpolated(Vector(error.getLocalizedMessage)))
								}
						}
					case None => Left(firstDataRowField, "Tähän tarvitaan numero joka on suurempi kuin 0")
				}
			case None => Left(headerRowField, "Tähän tarvitaan numero joka on suurempi kuin 0")
		}
	}
	
	override protected def defaultResult = Left(false)
	
	override protected def title = "Tiedoston lukeminen"
}
