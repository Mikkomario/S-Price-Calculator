package spadi.view.dialog

import java.nio.file.Path

import spadi.model.{KeyMappingFactory, Shop}
import spadi.view.component.Fields
import spadi.view.util.Setup._
import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.generic.ValueConversions._
import utopia.reflection.component.swing.input.{DropDown, SearchFrom}
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.localization.LocalString._

/**
 * This data source dialog variation uses selection (drop down) fields when requesting for values
 * @author Mikko Hilpinen
 * @since 7.6.2020, v1.1
 */
class DataSourceWindowWithSelections[+A](path: Path, shop: Shop, mappingFactory: KeyMappingFactory[A],
                                         firstDocumentRows: Vector[Vector[Value]])
	extends DataSourceWindowLike[A, DropDown[Int, _], SearchFrom[String, _]](path, shop, mappingFactory)
{
	// ATTRIBUTES   -----------------------------------
	
	private implicit val languageCode: String = "fi"
	
	// Will only consider rows that have a chance of mapping to all required keys
	private val targetRows =
	{
		val requiredColumnCount = mappingFactory.fieldNames.count { _._2 }
		// Shifts indices to start from 1
		firstDocumentRows.zipWithIndex.filter { case (row, _) => row.count { _.isDefined } >= requiredColumnCount }
			.map { case (row, index) => index + 1 -> row }.toMap
	}
	
	private val headerRowPointer = new PointerWithEvents[Option[Int]](None)
	private val keyPoolPointer = new PointerWithEvents[Vector[String]](Vector())
	
	private val rowDisplayFunction = DisplayFunction.noLocalization[Int] { rowIndex =>
		s"$rowIndex: ${ targetRows.getOrElse(rowIndex, Vector()).flatMap { _.string }.take(2).mkString(", ") }, ..."
	}
	
	override protected val headerRowField = inputContext.forGrayFields.use { implicit c =>
		Fields.dropDown[Int]("Yksikään luettu rivi ei sovellu otsikkoriviksi!", "Valitse otsikkorivi",
			displayFunction = rowDisplayFunction, valuePointer = headerRowPointer)
	}
	
	override protected val firstDataRowField = inputContext.forGrayFields.use { implicit c =>
		Fields.dropDown[Int]("Yksikään rivi ei sisällä tarvittavia arvoja!",
			"Valitse ensimmäinen tuoterivi", displayFunction = rowDisplayFunction)
	}
	
	
	// INITIAL CODE -----------------------------------
	
	// Whenever header selection changes, updates other options
	headerRowPointer.addListener { e =>
		e.newValue match
		{
			case Some(headerRowIndex) =>
				// Data row is the next valid row after header row, unless otherwise selected
				val dataRowOptions = targetRows.keySet.toVector.filter { _ > headerRowIndex }.sorted
				firstDataRowField.content = dataRowOptions
				if (firstDataRowField.value.forall { _ <= headerRowIndex })
					firstDataRowField.selectFirst()
				
				// Selectable keys are based on the header row
				keyPoolPointer.value = targetRows.getOrElse(headerRowIndex, Vector()).flatMap { _.string }
			case None =>
				keyPoolPointer.value = Vector()
				firstDataRowField.content = targetRows.keySet.toVector.sorted
		}
	}
	headerRowField.content = targetRows.keySet.toVector.sorted
	
	
	// IMPLEMENTED  -----------------------------------
	
	override protected def keyField(isRequired: Boolean) =
	{
		inputContext.forGrayFields.use { implicit c =>
			Fields.searchFrom(fieldWidth, "Kolumnia nimellä '%s' ei löydetty",
				if (isRequired) "Hae" else "Hae (vapaaehtoinen)", contentPointer = keyPoolPointer)
		}
	}
	
	override protected def valueOfField(field: Either[DropDown[Int, _], SearchFrom[String, _]]) = field match
	{
		case Left(select) => select.value
		case Right(search) => search.value
	}
	
	override protected def setFieldValue(field: Either[DropDown[Int, _], SearchFrom[String, _]], value: Value) = field match
	{
		case Left(select) => select.value = value.int
		case Right(search) => search.value = value.string
	}
}
