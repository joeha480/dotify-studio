package application.prefs;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.daisy.braille.api.factory.FactoryProperties;
import org.daisy.braille.api.table.BrailleConstants;
import org.daisy.braille.consumer.table.TableCatalog;

import com.googlecode.e2u.Settings;
import com.googlecode.e2u.Settings.Keys;

import application.l10n.Messages;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;

public class PreferencesController {
	@FXML private Tab previewTab;
	@FXML private Tab embossTab;
	@FXML private Label previewTranslation;
	@FXML private Label brailleFont;
	@FXML private Label textFont;
	@FXML private Label previewDescription;
	@FXML private ComboBox<FactoryPropertiesAdapter> selectTable;
	@FXML private ComboBox<FontEntry> selectBrailleFont;
	@FXML private ComboBox<FontEntry> selectTextFont;

	@FXML
	public void initialize() {
		previewTab.setText(Messages.TAB_PREVIEW.localize());
		previewDescription.setText("");
		previewTranslation.setText(Messages.LABEL_TRANSLATION.localize());
		TableScanner tableScanner = new TableScanner();
		tableScanner.setOnSucceeded(ev -> {
			selectTable.getItems().addAll(tableScanner.getValue());
			if (tableScanner.currentValue!=null) {
				selectTable.setValue(tableScanner.currentValue);
				previewDescription.setText(tableScanner.currentValue.p.getDescription());
			}
			selectTable.valueProperty().addListener((ov, t0, t1)-> { 
				Settings.getSettings().put(Keys.charset, t1.p.getIdentifier());
				previewDescription.setText(t1.p.getDescription());
			});
		});
		Thread th1 = new Thread(tableScanner);
		th1.setDaemon(true);
		th1.start();
		
		brailleFont.setText(Messages.LABEL_BRAILLE_FONT.localize());
		textFont.setText(Messages.LABEL_TEXT_FONT.localize());
		FontEntry defaultBrailleFont = new FontEntry("", Messages.VALUE_USE_DEFAULT.localize(), true);
		selectBrailleFont.getItems().add(defaultBrailleFont);
		selectBrailleFont.setValue(defaultBrailleFont);
		FontEntry defaultTextFont = new FontEntry("", Messages.VALUE_USE_DEFAULT.localize(), false);
		selectTextFont.getItems().add(defaultTextFont);
		selectTextFont.setValue(defaultTextFont);
		FontScanner fontScanner = new FontScanner();
		fontScanner.setOnSucceeded(t -> {
			selectBrailleFont.valueProperty().addListener((ov, t0, t1)-> Settings.getSettings().put(Keys.brailleFont, t1.key));
			selectTextFont.valueProperty().addListener((ov, t0, t1) -> Settings.getSettings().put(Keys.textFont, t1.key));
		});
		Thread th = new Thread(fontScanner);
		th.setDaemon(true);
		th.start();
	}
	
	private class TableScanner extends Task<List<FactoryPropertiesAdapter>> {
		private final String currentTable = Settings.getSettings().getString(Keys.charset, "");
		private FactoryPropertiesAdapter currentValue;

		@Override
		protected List<FactoryPropertiesAdapter> call() throws Exception {
			List<FactoryPropertiesAdapter> tc = new ArrayList<>();
			for (FactoryProperties p : TableCatalog.newInstance().list()) {
				FactoryPropertiesAdapter ap = new FactoryPropertiesAdapter(p);
				tc.add(ap);
				if (p.getIdentifier().equals(currentTable)) {
					currentValue = ap; 
				}
			}
			Collections.sort(tc);
			return tc;
		}
		
	}
	
	private class FontScanner extends Task<Void> {
		private final String currentBrailleFont = Settings.getSettings().getString(Keys.brailleFont, "");
		private final String currentTextFont = Settings.getSettings().getString(Keys.textFont, "");

		@Override
		protected Void call() throws Exception {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			for (String f : ge.getAvailableFontFamilyNames()) {
				if (isCancelled()) {
					break;
				}
				Font font = Font.decode(f);
				int inx = font.canDisplayUpTo(BrailleConstants.BRAILLE_PATTERNS_256);
				if (inx==-1) {
					process(new FontEntry(f, f, true));
				} else if (inx>=64) {
					process(new FontEntry(f, f + " ("+Messages.MESSAGE_SIX_DOT_ONLY.localize()+")", true));
				} else {
					process(new FontEntry(f, f, false));
				}
			}
			return null;
		}

		private void process(FontEntry f) {
			if (f.brailleFont) {
				Platform.runLater(()-> selectBrailleFont.getItems().add(f));
				if (f.key.equals(currentBrailleFont)) {
					Platform.runLater(()->selectBrailleFont.setValue(f));					
				}
			}
			Platform.runLater(()-> selectTextFont.getItems().add(f));
			if (f.key.equals(currentTextFont)) {
				Platform.runLater(()->selectTextFont.setValue(f));
			}
		}
		
	}
	
	private static class FactoryPropertiesAdapter implements Comparable<FactoryPropertiesAdapter> {
		private final FactoryProperties p;
		private FactoryPropertiesAdapter(FactoryProperties p) {
			this.p = p;
		}
		
		@Override
		public int compareTo(FactoryPropertiesAdapter o) {
			return p.getDisplayName().compareTo(o.p.getDisplayName());
		}
		
		@Override
		public String toString() {
			return p.getDisplayName();
		}

	}

	private static class FontEntry {
		private final String key;
		private final String value;
		private final boolean brailleFont;
		
		private FontEntry(String key, String value, boolean brailleFont) {
			this.key = key;
			this.value = value;
			this.brailleFont = brailleFont;
		}
		
		@Override
		public String toString() {
			return value;
		}
	}
}
