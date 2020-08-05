package spadi.view.dialog

import java.nio.file.Path

import spadi.controller.read.DataProcessor
import spadi.model.cached.read.{KeyMapping, KeyMappingFactory2}
import spadi.model.stored.pricing.Shop
import spadi.view.util.Setup._
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.reflection.component.swing.input.TextField

/**
 * This data source dialog variation uses text fields for requesting data
 * @author Mikko Hilpinen
 * @since 27.5.2020, v1.1
 */
class DataProcessorWindowWithTextFields[A, +M <: KeyMapping[A]](path: Path, shop: Shop, mappingFactory: KeyMappingFactory2[A, M])
										   (makeProcessor: (Path, M) => DataProcessor[A, M])
	extends DataProcessorWindowLike[A, M, TextField](path, shop, mappingFactory)(makeProcessor)
{
	// ATTRIBUTES   ------------------------
	
	private implicit val languageCode: String = "fi"
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def keyField(isRequired: Boolean) = inputContext.forGrayFields.use { implicit context =>
		TextField.contextual(fieldWidth, prompt = if (isRequired) None else Some("Vapaaehtoinen"))
	}
	
	override protected def valueOfField(field: TextField) = field.value
	
	override protected def setFieldValue(field: TextField, value: Value) = field.text = value.string
}
