package application.ui.preview;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.daisy.dotify.common.xml.XmlEncodingDetectionException;
import org.junit.Test;

public class EditorControllerTest {
	
	@Test
	public void testLoadData_01() throws IOException, XmlEncodingDetectionException {
		FileInfo.Builder builder = new FileInfo.Builder((File)null);
		// Byte order mark not added automatically
		byte[] data = "\uFEFFabc".getBytes("utf-8");
		assertEquals("abc", EditorController.loadData(data, builder, false));
	}
	
	@Test
	public void testLoadData_02() throws IOException, XmlEncodingDetectionException {
		FileInfo.Builder builder = new FileInfo.Builder((File)null);
		// No byte order mark
		byte[] data = "abcd".getBytes("utf-8");
		assertEquals("abcd", EditorController.loadData(data, builder, false));
	}
	
	@Test
	public void testLoadData_03() throws IOException, XmlEncodingDetectionException {
		FileInfo.Builder builder = new FileInfo.Builder((File)null);
		// Byte order mark added automatically!
		byte[] data = "abc".getBytes("utf-16");
		assertEquals("abc", EditorController.loadData(data, builder, false));
	}

	@Test
	public void testLoadData_04() throws IOException, XmlEncodingDetectionException {
		FileInfo.Builder builder = new FileInfo.Builder((File)null);
		// Byte order mark not added automatically
		byte[] data = "\uFEFFabc".getBytes("utf-16BE");
		assertEquals("abc", EditorController.loadData(data, builder, false));
	}
	
	@Test
	public void testLoadData_05() throws IOException, XmlEncodingDetectionException {
		FileInfo.Builder builder = new FileInfo.Builder((File)null);
		// Byte order mark not added automatically
		byte[] data = "\uFEFFabc".getBytes("utf-16LE");
		assertEquals("abc", EditorController.loadData(data, builder, false));
	}
	
	@Test
	public void testLoadData_06() throws IOException, XmlEncodingDetectionException {
		FileInfo.Builder builder = new FileInfo.Builder((File)null);
		// Byte order mark not added automatically
		byte[] data = "\uFEFFabc".getBytes("utf-32");
		assertEquals("abc", EditorController.loadData(data, builder, false));
	}
	
	@Test
	public void testLoadData_07() throws IOException, XmlEncodingDetectionException {
		FileInfo.Builder builder = new FileInfo.Builder((File)null);
		// Byte order mark not added automatically
		byte[] data = "\uFEFFabc".getBytes("utf-32LE");
		assertEquals("abc", EditorController.loadData(data, builder, false));
	}
	
	@Test
	public void testLoadData_08() throws IOException, XmlEncodingDetectionException {
		FileInfo.Builder builder = new FileInfo.Builder((File)null);
		// Byte order mark not added automatically
		byte[] data = "\uFEFFabc".getBytes("utf-32BE");
		assertEquals("abc", EditorController.loadData(data, builder, false));
	}

}
