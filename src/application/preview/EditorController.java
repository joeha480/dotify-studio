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
	private FileInfo fileInfo = new FileInfo.Builder((File)null).build();
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
			String text = loadData(Files.readAllBytes(f.toPath()), builder, xmlMarkup);
			codeArea.replaceText(0, 0, text);
			canSaveProperty.set(true);
		} catch (IOException | XmlEncodingDetectionException e) {
			logger.warning("Failed to read: " + f);
			canSaveProperty.set(false);
		} finally {
			this.fileInfo = builder.build();
			updateFileInfo(this.fileInfo);
		}
	}
	
	static String loadData(byte[] data, FileInfo.Builder builder, boolean xml) throws IOException, XmlEncodingDetectionException {
		builder.xml(xml);
		Charset encoding;
		if (xml) {
			//TODO: Ask if there is an encoding mismatch
			encoding = Charset.forName(XMLTools.detectXmlEncoding(data));
		} else {
			encoding = XMLTools.detectBomEncoding(data).orElse(StandardCharsets.UTF_8);
		}
		builder.charset(encoding);
		String text = new String(data, encoding);
		if (!text.isEmpty() && text.charAt(0)==BYTE_ORDER_MARK) {
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
			updateFileInfo(saveToFile(fileInfo.getFile(), fileInfo, codeArea.getText()));
		} catch (IOException e) {
			logger.warning("Failed to write: " + fileInfo.getFile());
		}
	}

	@Override
	public void saveAs(File f) throws IOException {
		updateFileInfo(saveToFile(f, fileInfo, codeArea.getText()));
	}
	
	private void updateFileInfo(FileInfo fileInfo) {
		this.fileInfo = fileInfo;
		encodingLabel.setText(fileInfo.getCharset().name());
		bomLabel.setText(fileInfo.hasBom()?"BOM":"");
	}
	
	static FileInfo saveToFile(File f, FileInfo fileInfo, String text) throws IOException {
		FileInfo.Builder builder = FileInfo.with(fileInfo);
		builder.file(f);
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
				return null;
			}
			if (StandardCharsets.UTF_16.equals(charset)) {
				// UTF-16 will append a BOM by itself
				builder.bom(true);
			} else if (fileInfo.hasBom() && (isStandardUnicodeCharset(charset) || isUtf32Charset(encoding))) {
				// Add BOM if the original file had it and the new encoding is a unicode charset
				text = BYTE_ORDER_MARK + text;
				builder.bom(true);
			} else {
				builder.bom(false);
			}
		} else {
			// Text file, or an XML-file without a declaration
			charset = fileInfo.getCharset();
			if (StandardCharsets.UTF_16.equals(charset)) {
				// UTF-16 will append a BOM by itself
				builder.bom(true);
			} else if (	(StandardCharsets.UTF_8.equals(charset) && fileInfo.hasBom()) ||
						(!StandardCharsets.UTF_8.equals(charset) && isStandardUnicodeCharset(charset) || isUtf32Charset(charset.name())) ) {
				// For text files, all unicode encodings require a BOM (unless it's utf-8)
				text = BYTE_ORDER_MARK + text;
				builder.bom(true);
			} else {
				builder.bom(false);
			}
		}
		builder.charset(charset);
		Files.write(f.toPath(), text.getBytes(charset));
		return builder.build();
	}
	
	private static boolean isStandardUnicodeCharset(Charset charset) {
		return StandardCharsets.UTF_8.equals(charset) || StandardCharsets.UTF_16.equals(charset) || StandardCharsets.UTF_16LE.equals(charset)
				|| StandardCharsets.UTF_16BE.equals(charset);
	}
	
	private static boolean isUtf32Charset(String encoding) {
		return encoding.toLowerCase().startsWith("utf-32");
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