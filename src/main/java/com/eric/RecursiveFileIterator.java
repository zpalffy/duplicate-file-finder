package com.eric;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecursiveFileIterator implements Iterable<File> {

	private List<File> directories = new ArrayList<>();

	private List<File> files = new ArrayList<>();

	public RecursiveFileIterator(File dir) {
		fillCollections(dir);
	}

	private void fillCollections(File f) {
		File[] list = f.listFiles();
		if (list != null) {
			for (File file : list) {
				if (file.isDirectory()) {
					directories.add(file);
				} else if (file.isFile()) {
					files.add(file);
				}
			}
		}
	}

	private void cycle() {
		if (files.isEmpty()) {
			while (!directories.isEmpty() && files.isEmpty()) {
				fillCollections(directories.remove(0));
			}
		}
	}

	@Override
	public Iterator<File> iterator() {
		return new Iterator<File>() {

			@Override
			public boolean hasNext() {
				cycle();
				return !files.isEmpty();
			}

			@Override
			public File next() {
				return files.remove(0);
			}
		};
	}
}
