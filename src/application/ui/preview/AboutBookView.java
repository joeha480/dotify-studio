package application.ui.preview;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.daisy.braille.utils.pef.PEFBook;

import application.l10n.Messages;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AboutBookView extends Stage {
	private static final Logger logger = Logger.getLogger(AboutBookView.class.getCanonicalName());
	private FXMLLoader loader;

	public AboutBookView() {
		try {
			loader = new FXMLLoader(this.getClass().getResource("AboutBook.fxml"), Messages.getBundle());
			Parent root = loader.load();
			setScene(new Scene(root));
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to load view", e);
		}
		//setTitle(Messages.ABOUT_BOOK_WINDOW_TITLE.localize());
	}
	
	public void setBook(PEFBook book) {
		loader.<AboutBookController>getController().loadBook(book);
	}
}
