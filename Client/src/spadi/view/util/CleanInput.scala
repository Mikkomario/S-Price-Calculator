package spadi.view.util

import utopia.reflection.text.Regex

/**
  * Used for cleaning input string from exceptional characters
  * @author Mikko Hilpinen
  * @since 22.8.2020, v1.2.1
  */
object CleanInput
{
	private val regex = Regex.alphaNumeric || Regex.anyOf(".,/: +-?!@€£$%&()[]{}*")
	
	/**
	  * @param input Input string
	  * @return A clean version of the input string
	  */
	def apply(input: String) =
	{
		regex.filter(input.trim.replace('ä', 'a').replace('ö', 'o')
			.replace('Ä', 'A').replace('Ö', 'O'))
	}
}
