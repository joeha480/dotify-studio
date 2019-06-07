package application.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.daisy.braille.utils.pef.TextHandler;
import org.daisy.dotify.api.table.TableCatalog;
import org.daisy.dotify.common.xml.XMLTools;
import org.daisy.streamline.api.media.AnnotatedFile;
import org.daisy.streamline.api.media.DefaultAnnotatedFile;
import org.daisy.streamline.api.option.UserOption;
import org.daisy.streamline.api.option.UserOptionValue;
import org.daisy.streamline.api.tasks.InternalTaskException;
import org.daisy.streamline.api.tasks.ReadWriteTask;

class Brf2PefTask extends ReadWriteTask {
	private final String rootLang;
	private final Map<String, String> params;
	private static List<UserOption> options = null;

	Brf2PefTask(String name, String rootLang, Map<String, Object> params) {
		super(name);
		this.rootLang = rootLang;
		this.params = new HashMap<>();
		for (Map.Entry<String, Object> v : params.entrySet()) {
			switch (v.getKey()) {
				case TextHandler.KEY_AUTHOR:
				case TextHandler.KEY_DATE:
				case TextHandler.KEY_DUPLEX:
				case TextHandler.KEY_IDENTIFIER:
				case TextHandler.KEY_LANGUAGE:
				case TextHandler.KEY_MODE:
				case TextHandler.KEY_TITLE:
					this.params.put(v.getKey(), v.getValue().toString());
					break;
				default:
					break;
			}
		}
	}

	@Override
	public AnnotatedFile execute(AnnotatedFile input, File output) throws InternalTaskException {
		try {
			TextHandler.with(input.getPath().toFile(), output, TableCatalog.newInstance())
					.options(params)
					.parse();
		} catch (IOException e) {
			throw new InternalTaskException(e);
		}
		return new DefaultAnnotatedFile.Builder(output.toPath()).extension("pef").mediaType("application/x-pef+xml").build();
	}

	@Override
	@Deprecated
	public void execute(File input, File output) throws InternalTaskException {
		execute(new DefaultAnnotatedFile.Builder(input).build(), output);
	}

	@Override
	public List<UserOption> getOptions() {
		if (options==null) {
			options = new ArrayList<>();
			//TODO: add display names
			options.add(new UserOption.Builder(TextHandler.KEY_AUTHOR).build());
			options.add(new UserOption.Builder(TextHandler.KEY_DATE).build());
			options.add(new UserOption.Builder(TextHandler.KEY_DUPLEX).build());
			options.add(new UserOption.Builder(TextHandler.KEY_IDENTIFIER).build());
			options.add(new UserOption.Builder(TextHandler.KEY_LANGUAGE).build());
			UserOption.Builder mode = new UserOption.Builder(TextHandler.KEY_MODE)
					.displayName("Table");
			TableCatalog.newInstance().list().stream()
					.sorted((v1, v2)->v1.getDisplayName().compareTo(v2.getDisplayName()))
					.forEach(v->
					mode.addValue(
							new UserOptionValue.Builder(v.getIdentifier())
									.displayName(v.getDisplayName())
									.description(v.getDescription())
									.build()
					)
			);
			options.add(mode.build());
			options.add(new UserOption.Builder(TextHandler.KEY_TITLE).build());
		}
		return options;
	}

}
