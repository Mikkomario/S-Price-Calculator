package spadi.controller

/**
 * Used for logging errors
 * @author Mikko Hilpinen
 * @since 8.5.2020, v1
 */
object Log
{
	def apply(error: Throwable, message: String) =
	{
		println(message)
		error.printStackTrace()
	}
}
