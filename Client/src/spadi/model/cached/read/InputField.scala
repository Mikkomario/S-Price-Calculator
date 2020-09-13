package spadi.model.cached.read

import utopia.flow.datastructure.immutable.Value
import utopia.reflection.localization.LocalString

object InputField
{
	/**
	  * Creates a field that only supports certain types of values
	  * @param name Field name
	  * @param helpText A helpful description of the field role (optional)
	  * @param isRequired Whether field must be defined (default = false)
	  * @param validator A function that checks whether a value would be accepted in this field
	  * @return A new field
	  */
	def withValidation(name: LocalString, helpText: LocalString = LocalString.empty, isRequired: Boolean = false)(
		validator: Value => Boolean) = InputField(name, Some(validator), helpText, isRequired)
	
	/**
	  * Creates a field that doesn't perform any checking and supports all types of values
	  * @param name Field name
	  * @param helpText A helpful description of the field role (optional)
	  * @param isRequired Whether field must be defined (default = false)
	  * @return A new field
	  */
	def freeForm(name: LocalString, helpText: LocalString = LocalString.empty, isRequired: Boolean = false) =
		InputField(name, None, helpText, isRequired)
}

/**
  * Represents a single input field required when reading product or sales data (Eg. Required int id field)
  * @author Mikko Hilpinen
  * @since 5.9.2020, v1.2.3
  */
case class InputField(name: LocalString, validator: Option[Value => Boolean] = None,
					  helpText: LocalString = LocalString.empty, isRequired: Boolean = false)
{
	// COMPUTED	---------------------------
	
	/**
	  * @return Whether this field doesn't necessarily need to be defined
	  */
	def isOptional = !isRequired
	
	
	// OTHER	---------------------------
	
	/**
	  * Checks whether this field would accept all of the specified example values
	  * @param values A set of example values
	  * @return Whether this field's validator accepts all of those values
	  */
	def allowsValues(values: Iterable[Value]) = validator.forall(values.forall)
}
