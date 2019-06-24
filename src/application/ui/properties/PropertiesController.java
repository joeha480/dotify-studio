package application.ui.properties;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.daisy.braille.utils.pef.PEFBook;

import com.googlecode.ajui.Context;

import application.l10n.Messages;
import application.ui.preview.AboutBookHtml;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

/**
 * Provides a controller for the validation view.
 * @author Joel Håkansson
 *
 */
public class PropertiesController extends BorderPane {
	private static final Logger LOGGER = Logger.getLogger(PropertiesController.class.getCanonicalName());
	@FXML WebView browser;

	/**
	 * Creates a new validation view controller.
	 */
	public PropertiesController() {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Properties.fxml"), Messages.getBundle());
			fxmlLoader.setRoot(this);
			fxmlLoader.setController(this);
			fxmlLoader.load();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to load view", e);
		}
	}
	
	@FXML void initialize() {
		//browser.getEngine().setUserStyleSheetLocation(this.getClass().getResource("base.css").toString());
	}
	
	/**
	 * Sets the data.
	 * @param report the validation report
	 * @param action an action to take when an item is selected
	 */
	public void setModel(PEFBook book) {
		System.out.println("HERE");
		AboutBookHtml content = new AboutBookHtml(book);
		browser.getEngine().loadContent(content.getResult(new Context() {
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
	
	/**
	 * Removes all items.
	 */
	public void clear() {
	}


}
