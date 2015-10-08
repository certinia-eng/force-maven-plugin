package com.financialforce.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
	public static byte[] zipRoot(File rootDir) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(bos);
		
		File[] tops = rootDir.listFiles();
		if ((tops == null) || (tops.length == 0)) {
			throw new IOException("No files found in " + rootDir);
		}
		zipFiles("", tops, zos);
		zos.close();
		return bos.toByteArray();
	}
	
	public static void zipFiles(String relPath, File[] files, ZipOutputStream os) throws IOException {
		for (File file : files) {
			zipFile(relPath, file, os);
		}
	}
	
	public static void zipFile(String relPath, File file, ZipOutputStream os) throws IOException {
		String filePath = relPath + file.getName();
		
		if ((file.isDirectory()) && (!file.getName().startsWith("."))) {
			filePath = filePath + '/';
			ZipEntry dir = new ZipEntry(filePath);
			dir.setTime(file.lastModified());
			os.putNextEntry(dir);
			os.closeEntry();
			
			zipFiles(filePath, file.listFiles(), os);
		}
		else if ((!file.getName().startsWith(".")) && (!file.getName().endsWith("~"))) {
			addFile(filePath, file, os);
		}
	}
	
	private static ZipEntry addFile(String filename, File file, ZipOutputStream os) throws IOException {
		ZipEntry entry = new ZipEntry(filename);
		entry.setTime(file.lastModified());
		entry.setSize(file.length());
		os.putNextEntry(entry);
		FileInputStream is = new FileInputStream(file);
		
		try {
			FileChannel src = is.getChannel();
			WritableByteChannel dest = Channels.newChannel(os);
			copy(src, dest);
			os.closeEntry();
			return entry;
		}
		finally {
			is.close();
		}
	}
			  
	private static void copy(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(8092);
		while (src.read(buffer) != -1) {
			buffer.flip();
			while (buffer.hasRemaining()) {
				dest.write(buffer);
			}
			buffer.clear();
		}
	}	
}
