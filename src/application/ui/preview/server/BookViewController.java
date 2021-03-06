package application.ui.preview.server;

import java.io.File;
import java.net.URI;
import java.util.Objects;

import org.daisy.braille.utils.pef.PEFBook;

import application.common.Settings;
import application.ui.preview.server.preview.stax.BookReader;
import application.ui.preview.server.preview.stax.BookReaderResult;
import application.ui.preview.server.preview.stax.StaxPreviewController;

public class BookViewController {
	private static final Settings settings = Settings.getSettings();
	private BookReader bookReader;
	private StaxPreviewController controller;

	public BookViewController(File f) {
		bookReader = new BookReader(Objects.requireNonNull(f));
		controller = null;
	}

	/**
	 * Gets the book uri.
	 * @return the uri, never null
	 */
	public URI getBookURI() {
		return bookReader.getFile().toURI();
	}

	public PEFBook getBook() {
		return bookReader.getResult().getBook();
	}

	public BookReaderResult getBookReaderResult() {
		return bookReader.getResult();
	}

	public boolean bookIsValid() {
		return bookReader.getResult().isValid();
	}

	public AboutBookView getAboutBookView() {
		return new AboutBookView(bookReader.getResult().getBook(), bookReader.getResult().getValidationMessages());
	}

	public StaxPreviewController getPreviewView() {
		if (controller==null) {
			controller = new StaxPreviewController(bookReader, settings);
		}
		return controller;
	}

	public void close() {
		bookReader.cancel();
	}

}
