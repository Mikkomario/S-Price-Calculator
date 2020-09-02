package spadi.view.util

import java.awt.Desktop
import java.net.URI

import utopia.flow.util.StringExtensions._

import scala.util.{Failure, Try}

/**
  * Used for browsing urls
  * @author Mikko Hilpinen
  * @since 26.8.2020, v1.2.2
  */
object Browser
{
	// ATTRIBUTES	-------------------------
	
	/**
	  * Whether browsing is currently enabled
	  */
	lazy val isEnabled = Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)
	
	/**
	  * Opens a website address
	  * @param url url address to open
	  * @return Success or failure
	  */
	def open(url: String) =
	{
		if (isEnabled)
			Try { Desktop.getDesktop.browse(new URI(url)) }
		else
			Failure(new BrowserNotSupportedException("Browsing feature is not enabled"))
	}
	
	/**
	  * Performs a google search
	  * @param words Words to google
	  * @return Success or failure
	  */
	def google(words: Seq[String]) =
	{
		val query = words.flatMap { _.words }.mkString("+")
		open(s"https://www.google.com/search?q=$query")
	}
	
	
	// NESTED	-----------------------------
	
	private class BrowserNotSupportedException(message: String) extends Exception(message)
}
