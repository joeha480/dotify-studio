package application.preview;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.daisy.dotify.api.tasks.AnnotatedFile;
import org.daisy.dotify.studio.api.Editor;

import application.l10n.Messages;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Provides a preview controller.
 * @author Joel Håkansson
 *
 */
public class SourcePreviewController extends BorderPane implements Editor {
	private static final Logger logger = Logger.getLogger(SourcePreviewController.class.getCanonicalName());
	static final Pattern XML_PATTERN = Pattern.compile("\\Qapplication/\\E([\\w-]+\\+)?\\Qxml\\E");
	static final Pattern TEXT_PATTERN = Pattern.compile("\\Qtext/\\E.+");
	@FXML TabPane tabs;
	@FXML Tab preview;
	@FXML Tab source;
	private final BooleanProperty canEmbossProperty;
	private final BooleanProperty canExportProperty;
	private final BooleanProperty canSaveProperty;
	private final BooleanProperty modifiedProperty;
	private final StringProperty urlProperty;

	/**
	 * Creates a new preview controller.
	 */
	public SourcePreviewController() {
		canEmbossProperty = new SimpleBooleanProperty();
		canExportProperty = new SimpleBooleanProperty();
		canSaveProperty = new SimpleBooleanProperty();
		modifiedProperty = new SimpleBooleanProperty();
		urlProperty = new SimpleStringProperty();
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SourcePreview.fxml"), Messages.getBundle());
			fxmlLoader.setRoot(this);
			fxmlLoader.setController(this);
			fxmlLoader.load();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to load view", e);
		}
	}

	@FXML void initialize() {
	}

	public static boolean supportsFormat(AnnotatedFile af) {
		// TODO: also support application/epub+zip
		return isText(af) || isHTML(af) || isXML(af);
	}

	public static boolean isXML(AnnotatedFile af) {
		return af.getMediaType()!=null && XML_PATTERN.matcher(af.getMediaType()).matches();
	}

	public static boolean isHTML(AnnotatedFile af) { 
		return af.getMediaType()!=null && "text/html".equals(af.getMediaType());
	}

	public static boolean isText(AnnotatedFile af) {
		return af.getMediaType()!=null && TEXT_PATTERN.matcher(af.getMediaType()).matches()
				|| af.getMediaType()==null && af.getExtension()!=null && "txt".equals(af.getExtension());
	}

	/**
	 * Converts and opens a file.
	 * @param selected the file
	 * @param options the options
	 */
	public void open(AnnotatedFile selected, Map<String, Object> options) {
		if (options==null) {
			open(selected);
		} else {
			PreviewController prv = new PreviewController();
			setupOpen(prv, selected);
			prv.open(selected, options);
		}
	}

	public void open(AnnotatedFile selected) {
		PreviewController prv = new PreviewController();
		setupOpen(prv, selected);
		prv.open(selected, null);
	}

	private void setupOpen(PreviewController prv, AnnotatedFile selected) {
		preview.setContent(prv);
		source.setText(Messages.LABEL_SOURCE.localize(selected.getFile().getName()));
		EditorController editor = new EditorController();
		editor.load(selected.getFile(), isXML(selected));
		source.setContent(editor);
		canEmbossProperty.bind(
				tabs.getSelectionModel().selectedItemProperty().isEqualTo(preview).and(prv.canEmbossProperty())
			.or(
				tabs.getSelectionModel().selectedItemProperty().isEqualTo(source).and(editor.canEmbossProperty())
			)
		);
		canExportProperty.bind(
				tabs.getSelectionModel().selectedItemProperty().isEqualTo(preview).and(prv.canExportProperty())
			.or(
				tabs.getSelectionModel().selectedItemProperty().isEqualTo(source).and(editor.canExportProperty())
			)
		);
		canSaveProperty.bind(
				tabs.getSelectionModel().selectedItemProperty().isEqualTo(preview).and(prv.canSaveProperty())
			.or(
				tabs.getSelectionModel().selectedItemProperty().isEqualTo(source).and(editor.canSaveProperty())
			)
		);
		modifiedProperty.bind(editor.modifiedProperty());
		urlProperty.bind(
				new When(tabs.getSelectionModel().selectedItemProperty().isEqualTo(preview))
					.then(prv.urlProperty())
					.otherwise(
						new When(tabs.getSelectionModel().selectedItemProperty().isEqualTo(source))
							.then(editor.urlProperty())
							.otherwise(new SimpleStringProperty())
					)
				);
	}

	@Override
	public void reload() {
		getSelectedView().ifPresent(v->v.reload());
	}

	@Override
	public Optional<String> getURL() {
		return getSelectedView().map(v->v.getURL()).orElse(Optional.empty());
	}

	@Override
	public void showEmbossDialog() {
		((Editor)preview.getContent()).showEmbossDialog();
	}

	@Override
	public void closing() {
		((Editor)source.getContent()).closing();
		((Editor)preview.getContent()).closing();
	}

	private Optional<Editor> getSelectedView() {
		return Optional.ofNullable(tabs.getSelectionModel())
				.map(v->v.getSelectedItem().getContent())
				.filter(v->v instanceof Editor)
				.map(v->(Editor)v);
	}

	@Override
	public void save() {
		SingleSelectionModel<Tab> m = tabs.getSelectionModel(); 
		if (m!=null && m.getSelectedItem() == source) {
			((EditorController)source.getContent()).save();
		}
	}

	@Override
	public void saveAs(File f) throws IOException {
		Optional<Editor> view = getSelectedView();
		if (view.isPresent()) {
			view.get().saveAs(f);
		}
	}

	@Override
	public void export(File f) throws IOException {
		((Editor)preview.getContent()).export(f);
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
	public List<ExtensionFilter> getSaveAsFilters() {
		return getSelectedView().map(v->v.getSaveAsFilters()).orElse(Collections.emptyList());
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
	public ReadOnlyBooleanProperty modifiedProperty() {
		return modifiedProperty;
	}

	@Override
	public ReadOnlyBooleanProperty toggleViewProperty() {
		return new SimpleBooleanProperty(true);
	}

	@Override
	public void toggleView() {		
		SingleSelectionModel<Tab> select = tabs.getSelectionModel();
		if (select.isSelected(0)) {
			select.selectLast();
		} else {
			select.selectFirst();
		}
	}

	@Override
	public Map<String, Object> getOptions() {
		SingleSelectionModel<Tab> m = tabs.getSelectionModel();
		if (m!=null) {
			Tab t = m.getSelectedItem();
			if (t!=null && t.getContent() instanceof Editor) {
				Editor p = (Editor)t.getContent();
				if (t == source) {
					if (p.getURL().orElse("").endsWith(".pef")) {
						return null;
					} else {
						return ((Editor)preview.getContent()).getOptions();
					}
				} else if (t == preview) {
					return null;
				}
			}
		}
		throw new RuntimeException();
	}

	@Override
	public void activate() {
		getSelectedView().ifPresent(v->v.activate());
	}
}