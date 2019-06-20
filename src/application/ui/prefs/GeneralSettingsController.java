package application.ui.prefs;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.daisy.dotify.api.table.TableCatalog;
import org.daisy.streamline.api.details.FormatDetailsProvider;
import org.daisy.streamline.api.details.FormatDetailsProviderService;
import org.daisy.streamline.api.media.FileDetails;
import org.daisy.streamline.api.media.FormatIdentifier;

import application.common.FactoryPropertiesAdapter;
import application.common.FeatureSwitch;
import application.common.LocaleEntry;
import application.common.NiceName;
import application.common.Settings;
import application.common.Settings.Keys;
import application.common.SupportedLocales;
import application.ui.preview.FileDetailsCatalog;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Provides a controller for the general settings view.
 * @author Joel Håkansson
 *
 */
public class GeneralSettingsController {
	@FXML private Label previewTranslation;
	@FXML private Label previewDescription;
	@FXML private ComboBox<FactoryPropertiesAdapter> selectTable;
	@FXML private CheckBox showTemplateDialogCheckbox;
	@FXML private CheckBox lineNumbersCheckbox;
	@FXML private CheckBox wordWrapCheckbox;
	@FXML private CheckBox autosaveCheckbox;
	@FXML private VBox rootVBox;
	@FXML private HBox hboxOutputFormat;
	@FXML private HBox hboxAutosave;
	@FXML private ComboBox<NiceName> selectOutputFormat;
	@FXML private ComboBox<LocaleEntry> selectLocale;

	@FXML void initialize() {
		if (FeatureSwitch.SELECT_OUTPUT_FORMAT.isOn()) {
			// TODO: This list should be created dynamically from available outputs.
			// Note that the issue with this isn't to generate the list of outputs, this can be done easily.
			// However, currently there isn't a way to filter the list of inputs based on the selected output
			// (in the import dialog). Html is allowed in the list, because it has a reasonably broad support.
			FormatDetailsProviderService detailsProvider = FormatDetailsProvider.newInstance();
			selectOutputFormat.getItems().addAll(
					Arrays.asList(FileDetailsCatalog.PEF_FORMAT, FileDetailsCatalog.XHTML_FORMAT, FileDetailsCatalog.FORMATTED_TEXT_FORMAT).stream()
					.map(v->new NiceName(v.getMediaType(), 
							detailsProvider.getDetails(FormatIdentifier.with(v.getFormatName())).map(v2->v2.getDisplayName()).orElse(v.getFormatName())
							))
					.collect(Collectors.toList())
			);
			FileDetails current = FileDetailsCatalog.forMediaType(Settings.getSettings().getConvertTargetFormat());
			selectOutputFormat.getSelectionModel().select(new NiceName(current.getMediaType(), detailsProvider.getDetails(FormatIdentifier.with(current.getFormatName())).map(v2->v2.getDisplayName()).orElse(current.getFormatName())));
			selectOutputFormat.valueProperty().addListener((ov, t0, t1)->Settings.getSettings().setConvertTargetFormat(t1.getKey()));
		} else {
			rootVBox.getChildren().remove(hboxOutputFormat);
		}
		wordWrapCheckbox.setSelected(Settings.getSettings().shouldWrapLines());
		wordWrapCheckbox.selectedProperty().addListener((o, ov, nv)->{
			Settings.getSettings().setWordWrap(nv.booleanValue());
		});
		
		lineNumbersCheckbox.setSelected(Settings.getSettings().shouldShowLineNumbers());
		lineNumbersCheckbox.selectedProperty().addListener((o, ov, nv)->{
			Settings.getSettings().setLineNumbers(nv.booleanValue());
		});
		
		if (FeatureSwitch.AUTOSAVE.isOn()) {
			autosaveCheckbox.setSelected(Settings.getSettings().shouldAutoSave());
			autosaveCheckbox.selectedProperty().addListener((o, ov, nv)->{
				Settings.getSettings().setAutoSave(nv.booleanValue());
			});
		} else {
			rootVBox.getChildren().remove(hboxAutosave);
		}
		
		showTemplateDialogCheckbox.setSelected(Settings.getSettings().getShowTemplateDialogOnImport());
		showTemplateDialogCheckbox.selectedProperty().addListener((o, ov, nv)->{
			Settings.getSettings().setShowTemplateDialogOnImport(nv.booleanValue());
		});

		previewDescription.setText("");
		FactoryPropertiesScanner tableScanner = new FactoryPropertiesScanner(()->TableCatalog.newInstance().list(), Keys.charset);
		tableScanner.setOnSucceeded(ev -> {
			selectTable.getItems().addAll(tableScanner.getValue());
			if (tableScanner.getCurrentValue()!=null) {
				selectTable.setValue(tableScanner.getCurrentValue());
				previewDescription.setText(tableScanner.getCurrentValue().getProperties().getDescription());
			}
			selectTable.valueProperty().addListener((ov, t0, t1)-> { 
				Settings.getSettings().put(Keys.charset, t1.getProperties().getIdentifier());
				previewDescription.setText(t1.getProperties().getDescription());
			});
		});
		Thread th1 = new Thread(tableScanner);
		th1.setDaemon(true);
		th1.start();
		
		List<LocaleEntry> locales = SupportedLocales.list()
			.stream()
			.map(LocaleEntry::new)
			.sorted()
			.collect(Collectors.toList());
		selectLocale.getItems().addAll(locales);
		String tag = Settings.getSettings().getString(Keys.locale, Locale.getDefault().toLanguageTag());
		locales.stream()
			.filter(v->v.getKey().equals(tag))
			.findFirst()
			.ifPresent(v->selectLocale.getSelectionModel().select(v));
		selectLocale.valueProperty().addListener((ov, t0, t1)->Settings.getSettings().put(Keys.locale, t1.getKey()));
	}
}
