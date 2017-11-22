package org.daisy.dotify.studio.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class SyntaxHighlighterFactoryMaker implements SyntaxHighlighterFactoryMakerService {
	private final List<SyntaxHighlighterFactory> providers;
	private final Map<String, SyntaxHighlighterFactory> map;
	private final Logger logger;

	/**
	 * Creates a new empty instance. To create a populated instance, use {@link #newInstance()}.
	 */
	public SyntaxHighlighterFactoryMaker() {
		logger = Logger.getLogger(this.getClass().getCanonicalName());
		providers = new CopyOnWriteArrayList<>();
		map = Collections.synchronizedMap(new HashMap<String, SyntaxHighlighterFactory>());
	}
	
	/**
	 * <p>
	 * Creates a new EmbosserCatalog and populates it using SPI
	 * (java service provider interface).
	 * </p>
	 *
	 * @return returns a new {@link #SyntaxHighlightingFactoryMakerService}
	 */
	public static SyntaxHighlighterFactoryMakerService newInstance() {
		SyntaxHighlighterFactoryMaker ret = new SyntaxHighlighterFactoryMaker();
		Iterator<SyntaxHighlighterFactory> i = ServiceLoader.load(SyntaxHighlighterFactory.class).iterator();
		while (i.hasNext()) {
			SyntaxHighlighterFactory ep = i.next();
			ret.addFactory(ep);
		}
		return ret;
	}
	
	/**
	 * Adds a factory
	 * @param factory the factory to add
	 */
	public void addFactory(SyntaxHighlighterFactory factory) {
		logger.finer("Adding factory: " + factory);
		providers.add(factory);
	}
	
	@Override
	public Optional<SyntaxHighlighterFactory> get(String syntax) {
		if (syntax==null) {
			return null;
		}
		SyntaxHighlighterFactory template = map.get(syntax);
		if (template==null) {
			// this is to avoid adding items to the cache that were removed
			// while iterating
			synchronized (map) {
				for (SyntaxHighlighterFactory p : providers) {
					for (String fp : p.listSupportedSyntaxes()) {
						if (fp.equals(syntax)) {
							logger.fine("Found a factory for " + syntax + " (" + p.getClass() + ")");
							map.put(fp, p);
							template = p;
							break;
						}
					}
				}
			}
		}
		if (template!=null) {
			return Optional.of(template);
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<SyntaxHighlighter> newSyntaxHighlighter(String syntax) {
		return get(syntax).flatMap(v->v.getSyntaxHighlighter(syntax));
	}
	
	@Override
	public Collection<String> list() {
		Collection<String> ret = new ArrayList<>();
		for (SyntaxHighlighterFactory p : providers) {
			ret.addAll(p.listSupportedSyntaxes());
		}
		return ret;
	}
}
