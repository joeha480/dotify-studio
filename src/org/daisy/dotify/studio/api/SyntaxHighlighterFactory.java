package org.daisy.dotify.studio.api;

import java.util.List;
import java.util.Optional;

/**
 * Provides a syntax highlighting factory.
 * @author Joel Håkansson
 */
public interface SyntaxHighlighterFactory {

	/**
	 * Lists supported syntaxes.
	 * @return returns a list of supported syntaxes.
	 */
	public List<String> listSupportedSyntaxes();
	
	/**
	 * Gets a highlighter for the specified syntax
	 * @param syntax the syntax
	 * @return returns the syntax
	 */
	public Optional<SyntaxHighlighter> getSyntaxHighlighter(String syntax);

}
