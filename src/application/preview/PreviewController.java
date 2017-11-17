package application.preview;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.daisy.braille.utils.pef.PEFBook;
import org.daisy.dotify.api.tasks.AnnotatedFile;
import org.daisy.dotify.api.tasks.CompiledTaskSystem;
import org.daisy.dotify.api.tasks.TaskSystem;
import org.daisy.dotify.consumer.identity.IdentityProvider;
import org.daisy.dotify.consumer.tasks.TaskSystemFactoryMaker;
import org.daisy.dotify.tasks.runner.RunnerResult;
import org.daisy.dotify.tasks.runner.TaskRunner;

import com.googlecode.e2u.BookReader;
import com.googlecode.e2u.Start;

import application.l10n.Messages;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import shared.BuildInfo;
import shared.Settings;
import shared.Settings.Keys;

/**
 * Provides a preview controller.
 * @author Joel Håkansson
 *
 */
public class PreviewController extends BorderPane implements Preview {
	private static final Logger logger = Logger.getLogger(PreviewController.class.getCanonicalName());
	@FXML WebView browser;
	private OptionsController options;
	private String url;
	private Start start;
	private ExecutorService exeService;
	private boolean closing;
	private EmbossView embossView;

	/**
	 * Creates a new preview controller.
	 */
	public PreviewController() {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Preview.fxml"), Messages.getBundle());
			fxmlLoader.setRoot(this);
			fxmlLoader.setController(this);
			fxmlLoader.load();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to load view", e);
		}

        WebEngine webEngine = browser.getEngine();
        browser.setOnDragOver(event->event.consume());
        webEngine.setCreatePopupHandler(p-> {
                Stage stage = new Stage(StageStyle.UTILITY);
                WebView wv2 = new WebView();
                stage.setScene(new Scene(wv2));
                stage.show();
                return wv2.getEngine();
            }
        );
		exeService = Executors.newWorkStealingPool();
		closing = false;
	}
	
	/**
	 * Converts and opens a file.
	 * @param selected the file
	 * @param options the options
	 */
	public void convertAndOpen(File selected, Map<String, Object> options) {
		try {
			File out = File.createTempFile("dotify-studio", ".pef");
			String tag = Settings.getSettings().getString(Keys.locale, Locale.getDefault().toLanguageTag());
			DotifyTask dt = new DotifyTask(selected, out, tag, options);
			dt.setOnSucceeded(ev -> {
				Thread pefWatcher = open(new String[]{"-open", out.getAbsolutePath()});
				updateOptions(dt.getValue(), options);
	    		Thread th = new Thread(new SourceDocumentWatcher(selected, out, tag, pefWatcher));
	    		th.setDaemon(true);
	    		th.start();
			});
			dt.setOnFailed(ev->{
				logger.log(Level.WARNING, "Import failed.", dt.getException());
	    		Alert alert = new Alert(AlertType.ERROR, dt.getException().toString(), ButtonType.OK);
	    		alert.showAndWait();
			});
			exeService.submit(dt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void updateOptions(DotifyResult dr, Map<String, Object> opts) {
		if (options==null) {
			options = new OptionsController();
			setLeft(options);
		}
		options.setOptions(dr.getTaskSystem(), dr.getResults(), opts);

	}
	
    class SourceDocumentWatcher extends DocumentWatcher {
    	private final File output;
    	private final String locale;
    	private final Thread pefWatcher;

    	SourceDocumentWatcher(File input, File output, String locale, Thread pefWatcher) {
    		super(input);
    		this.output = output;
    		this.locale = locale;
    		this.pefWatcher = pefWatcher;
    	}

		@Override
		boolean shouldMonitor() {
			return super.shouldMonitor() && !closing;
		}

		@Override
		boolean shouldPerformAction() {
			return (super.shouldPerformAction() && options.isWatching()) || options.refreshRequested();
		}

		@Override
		void performAction() {
			try {
				Map<String, Object> opts = options.getParams();
	    		DotifyTask dt = new DotifyTask(file, output, locale, opts);
	    		dt.setOnFailed(ev->{
	    			logger.log(Level.WARNING, "Update failed.", dt.getException());
		    		Alert alert = new Alert(AlertType.ERROR, dt.getException().toString(), ButtonType.OK);
		    		alert.showAndWait();
	    		});
	    		dt.setOnSucceeded(ev -> {
	    			Platform.runLater(() -> {
	    				if (pefWatcher!=null) {
	    					pefWatcher.interrupt();
	    				}
	    				updateOptions(dt.getValue(), opts);
	    			});
	    		});
	    		exeService.submit(dt);
			} catch (Exception e) { 
				logger.log(Level.SEVERE, "A severe error occurred.", e);
			}
		}
    }
    
    class PefDocumentWatcher extends DocumentWatcher {
    	PefDocumentWatcher(File pef) {
    		super(pef);
    	}

		@Override
		boolean shouldMonitor() {
			return super.shouldMonitor() && !closing;
		}

		@Override
		void performAction() {
			Platform.runLater(()->reload());
		}

    }
    
    private static class DotifyResult {
    	private final CompiledTaskSystem taskSystem;
    	private final List<RunnerResult> results;
    	
    	private DotifyResult(CompiledTaskSystem taskSystem, List<RunnerResult> results) {
    		this.taskSystem = taskSystem;
    		this.results = results;
    	}

		private CompiledTaskSystem getTaskSystem() {
			return taskSystem;
		}

		private List<RunnerResult> getResults() {
			return results;
		}
    	
    }
	
    class DotifyTask extends Task<DotifyResult> {
    	private final File inputFile;
    	private final File outputFile;
    	private final String locale;
    	private final Map<String, Object> params;
    	
    	DotifyTask(File inputFile, File outputFile, String locale, Map<String, Object> params) {
    		this.inputFile = inputFile;
    		this.outputFile = outputFile;
    		this.locale = locale;
    		this.params = new HashMap<>(params);
    		this.params.put("systemName", BuildInfo.NAME);
    		this.params.put("systemBuild", BuildInfo.BUILD);
    		this.params.put("systemRelease", BuildInfo.VERSION);
    		this.params.put("conversionDate", new Date().toString());
    	}
    	
    	@Override
    	protected DotifyResult call() throws Exception {
    		AnnotatedFile ai = IdentityProvider.newInstance().identify(inputFile);
    		String inputFormat = getFormatString(ai);
    		TaskSystem ts;
			ts = TaskSystemFactoryMaker.newInstance().newTaskSystem(inputFormat, "pef", locale);
			logger.info("About to run with parameters " + params);
			
			logger.info("Thread: " + Thread.currentThread().getThreadGroup());
			CompiledTaskSystem tl = ts.compile(params);
			TaskRunner.Builder builder = TaskRunner.withName(ts.getName());
			return new DotifyResult(tl, builder.build().runTasks(ai, outputFile, tl));
    	}

    	//FIXME: Duplicated from Dotify CLI. If this function is needed to run Dotify, find a home for it
    	private String getFormatString(AnnotatedFile f) {
    		if (f.getFormatName()!=null) {
    			return f.getFormatName();
    		} else if (f.getExtension()!=null) {
    			return f.getExtension();
    		} else if (f.getMediaType()!=null) {
    			return f.getMediaType();
    		} else {
    			return null;
    		}
    	}

    }

	/**
	 * Starts a new preview server.
	 * @param args the arguments
	 * @return returns a thread that watches for changes in the pef file
	 */
	public Thread open(String[] args) {
		Thread pefWatcherThread = null;
		if (args.length==2) {
			File file = new File(args[1]);
			PefDocumentWatcher pefWatcher = new PefDocumentWatcher(file);
    		pefWatcherThread = new Thread(pefWatcher);
    		pefWatcherThread.setDaemon(true);
    		pefWatcherThread.start();
		}
		Task<String> startServer = new Task<String>() {

			@Override
			protected String call() throws Exception {
		        try {
		        	start = new Start();
		        	return start.start(args, false, false);
				} catch (Exception e1) {
					Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE, "Failed to load server.", e1);;
				}  
		        return null;
			}
		};
		startServer.setOnSucceeded(ev -> {
				this.url = startServer.getValue();
				if (url!=null) {
					browser.getEngine().load(url);
				} else {
					browser.getEngine().load(getClass().getResource("resource-files/fail.html").toString());
				}
			}
		);
		Thread th = new Thread(startServer);
		th.setDaemon(true);
		th.start();
		return pefWatcherThread;
	}
	
	/**
	 * Reloads the web view.
	 */
	public void reload() {
		browser.getEngine().reload();
	}
	
	/**
	 * Gets the url for the book in the view.
	 * @return returns the url
	 */
	public String getURL() {
		return url;
	}
	
	/**
	 * Informs the controller that it should close.
	 */
	public void closing() {
		closing = true;
	}
	
	/**
	 * Gets the uri for the book
	 * @return returns the uri
	 */
	public Optional<URI> getBookURI() {
		if (start!=null) {
			return start.getMainPage().getBookURI();
		} else {
			return Optional.<URI>empty();
		}
	}
	
	/**
	 * Shows the emboss dialog.
	 */
	public void showEmbossDialog() {
		if (start!=null) {
			Optional<BookReader.BookReaderResult> reader = start.getMainPage().getBookReaderResult();
			if (reader.isPresent() && reader.get().isValid()) {
				PEFBook book = reader.get().getBook();
				if (embossView==null) {
					embossView = new EmbossView(book);
					embossView.initOwner(this.getScene().getWindow());
					embossView.initModality(Modality.APPLICATION_MODAL); 
				} else {
					embossView.setBook(book);
				}
				embossView.showAndWait();
			} else {
				Alert alert = new Alert(AlertType.ERROR, Messages.ERROR_CANNOT_EMBOSS_INVALID_FILE.localize(), ButtonType.OK);
	    		alert.showAndWait();
			}
		}
	}

	@Override
	public boolean canSave() {
		return false;
	}

	@Override
	public void save() {
		throw new UnsupportedOperationException();
	}

}
