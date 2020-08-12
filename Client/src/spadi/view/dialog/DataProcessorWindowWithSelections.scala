package spadi.view.dialog

import java.nio.file.Path

import spadi.controller.read.DataProcessor
import spadi.model.cached.read.{KeyMapping, KeyMappingFactory}
import spadi.model.stored.pricing.Shop
import spadi.view.component.Fields
import spadi.view.util.Setup._
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.reflection.component.swing.input.SearchFrom

/**
 * This data source dialog variation uses selection (drop down) fields when requesting for values
 * @author Mikko Hilpinen
 * @since 7.6.2020, v1.1
 */
class DataProcessorWindowWithSelections[A, +M <: KeyMapping[A]](path: Path, shop: Shop, mappingFactory: KeyMappingFactory[A, M],
																headerRow: Vector[String])
										   (makeProcessor: (Path, M) => DataProcessor[A, M])
	extends DataProcessorWindowLike[A, M, SearchFrom[String, _]](path, shop, mappingFactory)(makeProcessor)
{
	// ATTRIBUTES   -----------------------------------
	
	private implicit val languageCode: String = "fi"
	
	
	// IMPLEMENTED  -----------------------------------
	
	override protected def keyField(isRequired: Boolean) =
	{
		inputContext.forGrayFields.use { implicit c =>
			val field = Fields.searchFrom[String](fieldWidth, "Kolumnia nimellä '%s' ei löydetty",
				if (isRequired) "Hae" else "Hae (vapaaehtoinen)")
			field.content = headerRow
			field
		}
	}
	
	override protected def valueOfField(field: SearchFrom[String, _]) = field.value
	
	override protected def setFieldValue(field: SearchFrom[String, _], value: Value) = field.value = value.string
}
