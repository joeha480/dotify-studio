package org.daisy.dotify.studio.api;

import java.util.Collection;

import org.fxmisc.richtext.model.StyleSpans;

public interface SyntaxHighlighter {

	/**
	 * Computes the highlights for the specified text.
	 * @param text the text
	 * @return returns style information
	 */
	public StyleSpans<Collection<String>> computeHighlighting(String text);

}
