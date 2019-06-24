package application.ui.preview;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import org.daisy.braille.utils.pef.PEFBook;

import com.googlecode.ajui.Context;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class AboutBookController {
	private static final Logger logger = Logger.getLogger(AboutBookController.class.getCanonicalName());
	@FXML public WebView browser;

	public void loadBook(PEFBook book) {
		AboutBookHtml content = new AboutBookHtml(book);
        WebEngine webEngine = browser.getEngine();
        webEngine.setUserStyleSheetLocation(this.getClass().getResource("base.css").toString());
		webEngine.loadContent(content.getResult(new Context() {
			@Override
			public Map<String, String> getArgs() {
				return Collections.emptyMap();
			}

			@Override
			public String getTarget() {
				return "";
			}

			@Override
			public void log(String msg) {}

			@Override
			public void close() {
			}}));
	}
	
}
