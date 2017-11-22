package org.daisy.dotify.studio.api;

import java.util.Collection;
import java.util.Optional;

public interface SyntaxHighlighterFactoryMakerService {
	
	public Optional<SyntaxHighlighterFactory> get(String syntax);
	public Optional<SyntaxHighlighter> newSyntaxHighlighter(String syntax);
	public Collection<String> list();
}
