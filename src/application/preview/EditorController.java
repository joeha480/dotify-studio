package application.preview;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.daisy.dotify.common.xml.XMLTools;
import org.daisy.dotify.common.xml.XmlEncodingDetectionException;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import application.l10n.Messages;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Provides an editor controller.
 * @author Joel Håkansson
 *
 */
public class EditorController extends BorderPane implements Preview {
	private static final Logger logger = Logger.getLogger(EditorController.class.getCanonicalName());
	private static final char BYTE_ORDER_MARK = '\uFEFF';

	@FXML CheckBox wordWrap;
	@FXML CheckBox lineNumbers;
	@FXML Label encodingLabel;
	@FXML Label bomLabel;
	private CodeArea codeArea;
	private VirtualizedScrollPane<CodeArea> scrollPane;
	private FileInfo fileInfo = new FileInfo.Builder(null).build();
	private ExecutorService executor;
	private final ReadOnlyBooleanProperty canEmbossProperty;
	private final ReadOnlyBooleanProperty canExportProperty;
	private final BooleanProperty canSaveProperty;
	private final ReadOnlyStringProperty urlProperty;
	//private String hash;

	/**
	 * Creates a new preview controller.
	 */
	public EditorController() {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Editor.fxml"), Messages.getBundle());
			fxmlLoader.setRoot(this);
			fxmlLoader.setController(this);
			fxmlLoader.load();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to load view", e);
		}
		executor = Executors.newWorkStealingPool();
		canEmbossProperty = BooleanProperty.readOnlyBooleanProperty(new SimpleBooleanProperty(false));
		canExportProperty = BooleanProperty.readOnlyBooleanProperty(new SimpleBooleanProperty(false));
		canSaveProperty = new SimpleBooleanProperty(false);
		urlProperty = new SimpleStringProperty();
	}
	
	@FXML void initialize() {
		codeArea = new CodeArea();
		codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
		/*
		codeArea.textProperty().addListener((obs, oldText, newText)-> {
			codeArea.setStyleSpans(0, XMLStyleHelper.computeHighlighting(newText));
		});*/
		codeArea.setWrapText(true);
		scrollPane = new VirtualizedScrollPane<>(codeArea);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		setCenter(scrollPane);
	}
	
    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return fileInfo.isXml()?XMLStyleHelper.computeHighlighting(text):XMLStyleHelper.noStyles(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        codeArea.setStyleSpans(0, highlighting);
    }

	/**
	 * Converts and opens a file.
	 * @param f the file
	 */
	public void load(File f, boolean xmlMarkup) {
		codeArea.clear();
		codeArea.richChanges()
			.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
			.successionEnds(Duration.ofMillis(500))
			.supplyTask(this::computeHighlightingAsync)
			.awaitLatest(codeArea.richChanges())
			.filterMap(t -> {
				if(t.isSuccess()) {
					return Optional.of(t.get());
				} else {
					t.getFailure().printStackTrace();
					return Optional.empty();
				}
			})
			.subscribe(this::applyHighlighting);
		FileInfo.Builder builder = new FileInfo.Builder(f);
		try {
			String text = loadFile(f, builder, xmlMarkup);
			codeArea.replaceText(0, 0, text);
			canSaveProperty.set(true);
		} catch (IOException | XmlEncodingDetectionException e) {
			logger.warning("Failed to read: " + f);
			canSaveProperty.set(false);
		} finally {
			this.fileInfo = builder.build();
			encodingLabel.setText(fileInfo.getCharset().name());
			bomLabel.setText(fileInfo.hasBom()?"BOM":"-");
		}
	}
	
	static String loadFile(File f, FileInfo.Builder builder, boolean xmlMarkup) throws IOException, XmlEncodingDetectionException {
		builder.xml(xmlMarkup);
		byte[] data = Files.readAllBytes(f.toPath());
		Charset encoding;
		if (xmlMarkup) {
			//TODO: Ask if there is an encoding mismatch
			encoding = Charset.forName(XMLTools.detectXmlEncoding(data));
		} else {
			encoding = XMLTools.detectBomEncoding(data).orElse(StandardCharsets.UTF_8);
		}
		builder.charset(encoding);
		String text = new String(data, encoding);
		if (text.charAt(0)==BYTE_ORDER_MARK) {
			builder.bom(true);
			text = text.substring(1);
		} else {
			builder.bom(false);
		}
		return text;
	}

	@FXML void toggleWordWrap() {
		scrollPane.setHbarPolicy(wordWrap.isSelected()?ScrollBarPolicy.NEVER:ScrollBarPolicy.AS_NEEDED);
		codeArea.setWrapText(wordWrap.isSelected());
	}

	@FXML void toggleLineNumbers() {
		if (lineNumbers.isSelected()) {
			codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
		} else {
			codeArea.setParagraphGraphicFactory(null);
		}
	}

	private String makeHash(String data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return DatatypeConverter.printHexBinary(md.digest(data.getBytes())).toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			logger.warning("Failed to create checksum");
		}
		return null;
	}

	@Override
	public void save() {
		try {
			saveToFile(fileInfo.getFile(), fileInfo, codeArea.getText());
		} catch (IOException e) {
			logger.warning("Failed to write: " + fileInfo.getFile());
		}
	}

	@Override
	public void saveAs(File f) throws IOException {
		saveToFile(f, fileInfo, codeArea.getText());
	}
	
	static void saveToFile(File f, FileInfo fileInfo, String text) throws IOException {
		Charset charset = StandardCharsets.UTF_8;
		Optional<String> _encoding;
		if (fileInfo.isXml() && (_encoding = XMLTools.getDeclaredEncoding(text)).isPresent()) {
			String encoding = _encoding.get();
			try {
				charset = Charset.forName(encoding);
			} catch (Exception e) {
				Platform.runLater(()-> {
					Alert alert = new Alert(AlertType.ERROR, Messages.ERROR_UNSUPPORTED_XML_ENCODING.localize(encoding), ButtonType.OK);
					alert.showAndWait();
				});
				return;
			}
			if (StandardCharsets.UTF_16.equals(charset) || "UTF-32".equalsIgnoreCase(encoding) || StandardCharsets.UTF_8.equals(charset) && fileInfo.hasBom()) {
				// UTF-16 and UTF-32 declared without endian suffix (e.g. UTF-16LE) require a BOM
				// UTF-8 should have it if it had it when the file was read (or if it were changed in the editor) 
				text = BYTE_ORDER_MARK + text;
			}
		} else {
			// Text file, or an XML-file without a declaration, in which case it was read as UTF-8
			charset = fileInfo.getCharset();
			// For text files, all unicode encodings require a BOM (unless it's utf-8)
			if (!StandardCharsets.UTF_8.equals(charset) || fileInfo.hasBom()) {
				text = BYTE_ORDER_MARK + text;
			}
		}
		Files.write(f.toPath(), text.getBytes(charset));
	}

	@Override
	public void export(File f) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void closing() {
		executor.shutdown();
	}

	@Override
	public List<ExtensionFilter> getSaveAsFilters() {
		if (fileInfo.getFile()!=null) {
			String name = fileInfo.getFile().getName();
			int dot = name.lastIndexOf('.');			
			if (dot>=0 && dot<name.length()) {
				String ext = name.substring(dot+1, name.length());
				return Arrays.asList(new ExtensionFilter(ext + "-files", "*." + ext));
			}
		}
		return Collections.emptyList();
	}

	@Override
	public void reload() {
		//
	}

	@Override
	public void showEmbossDialog() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ReadOnlyBooleanProperty canEmbossProperty() {
		return canEmbossProperty;
	}

	@Override
	public ReadOnlyBooleanProperty canExportProperty() {
		return canExportProperty;
	}

	@Override
	public ReadOnlyStringProperty urlProperty() {
		return urlProperty;
	}

	@Override
	public ReadOnlyBooleanProperty canSaveProperty() {
		return canSaveProperty;
	}

	@Override
	public Map<String, Object> getOptions() {
		return null;
	}

}