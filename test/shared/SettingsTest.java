package shared;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class SettingsTest {

	@Test
	public void testRecent() throws IOException {
		File recentFile = File.createTempFile("test", ".tmp");
		recentFile.deleteOnExit();
		File t1 = File.createTempFile("test", ".tmp");
		t1.deleteOnExit();
		File t2 = File.createTempFile("test", ".tmp");
		t2.deleteOnExit();
		File t3 = File.createTempFile("test", ".tmp");
		t3.deleteOnExit();
		File t4 = File.createTempFile("test", ".tmp");
		t4.deleteOnExit();
		
		Settings.addToRecent(recentFile, t1.getAbsolutePath());
		Settings.addToRecent(recentFile, t2.getAbsolutePath());
		Settings.addToRecent(recentFile, t3.getAbsolutePath());
		Settings.addToRecent(recentFile, t1.getAbsolutePath());
		List<File> recent = Settings.getRecent(recentFile);
		assertEquals(3, recent.size());
		assertEquals(t1, recent.get(0));
		assertEquals(t3, recent.get(1));
		assertEquals(t2, recent.get(2));
		
		Settings.addToRecent(recentFile, t4.getAbsolutePath());
		recent = Settings.getRecent(recentFile);
		assertEquals(4, recent.size());
		assertEquals(t4, recent.get(0));
		assertEquals(t1, recent.get(1));
		assertEquals(t3, recent.get(2));
		assertEquals(t2, recent.get(3));
	}

}
