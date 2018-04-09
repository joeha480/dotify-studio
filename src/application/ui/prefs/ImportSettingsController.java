package application.ui.prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.daisy.streamline.api.media.FileDetails;

import application.common.FeatureSwitch;
import application.common.NiceName;
import application.common.Settings;
import application.common.Settings.Keys;
import application.ui.preview.FileDetailsCatalog;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Provides a controller for the general settings view.
 * @author Joel Håkansson
 *
 */
public class ImportSettingsController {
	@FXML private VBox rootVBox;
	@FXML private HBox hboxOutputFormat;
	@FXML private ComboBox<NiceName> selectOutputFormat;
	@FXML private ComboBox<String> selectLocale;

	@FXML void initialize() {
		if (FeatureSwitch.SELECT_OUTPUT_FORMAT.isOn()) {
			// TODO: This list should be created dynamically from available outputs.
			// Note that the issue with this isn't to generate the list of outputs, this can be done easily.
			// However, currently there isn't a way to filter the list of inputs based on the selected output
			// (in the import dialog). Html is allowed in the list, because it has a reasonably broad support.
			selectOutputFormat.getItems().addAll(
					Arrays.asList(FileDetailsCatalog.PEF_FORMAT, FileDetailsCatalog.HTML_FORMAT).stream()
					.map(v->new NiceName(v.getMediaType(), v.getFormatName()))
					.collect(Collectors.toList())
			);
			FileDetails current = FileDetailsCatalog.forMediaType(Settings.getSettings().getImportOutputFormat());
			selectOutputFormat.getSelectionModel().select(new NiceName(current.getMediaType(), current.getFormatName()));
			selectOutputFormat.valueProperty().addListener((ov, t0, t1)->Settings.getSettings().setImportOutputFormat(t1.getKey()));
		} else {
			rootVBox.getChildren().remove(hboxOutputFormat);
		}
		selectLocale.getItems().addAll(toString(Locale.getAvailableLocales()));
		String tag = Settings.getSettings().getString(Keys.locale, Locale.getDefault().toLanguageTag());
		selectLocale.getSelectionModel().select(tag);
		selectLocale.valueProperty().addListener((ov, t0, t1)->Settings.getSettings().put(Keys.locale, t1));
	}
	
	private List<String> toString(Locale[] locales) {
		List<String> ret = new ArrayList<>();
		for (Locale l : locales) {
			ret.add(l.toLanguageTag());
		}
		Collections.sort(ret);
		return ret;
	}
	
}
