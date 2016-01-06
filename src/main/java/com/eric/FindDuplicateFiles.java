package com.eric;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.beust.jcommander.Parameter;

public class FindDuplicateFiles extends Command {

	@Parameter(description = "directories")
	private List<String> dirs = new ArrayList<>();

	@Parameter(names = { "-m", "--min-file-size" }, description = "Files smaller than this size in bytes will be ignored.")
	private long minSize;

	@Parameter(names = "--skip-checksum", description = "Skips the checksum comparison between files that match in size.")
	private boolean skipChecksum;

	@Parameter(names = "--display-bytes", description = "Displays file sizes in bytes")
	private boolean displayBytes;

	@Parameter(names = "--display-checksum", description = "Displays the file checksum next to the file name.")
	private boolean displayChecksum;

	private int duplicates;

	private long size;

	@Override
	protected String getProgramName() {
		return "duplicate-files";
	}

	@Override
	protected void run() throws Exception {
		Map<Long, List<File>> files = new HashMap<Long, List<File>>();
		if (dirs.isEmpty()) {
			dirs.add(getWorkingDirectory());
		}

		// look at every file and cache size:
		StopWatch watch = StopWatch.start();
		int count = 0;
		for (String dir : dirs) {
			File d = new File(dir);
			verbose("Checking directory: %s", d.getAbsolutePath());

			for (File file : new RecursiveFileIterator(d)) {
				count++;
				long length = file.length();

				if (length >= minSize) {
					List<File> existing = files.get(length);
					if (existing == null) {
						existing = new ArrayList<File>(1);
						files.put(length, existing);
					}

					existing.add(file);
				}
			}
		}

		verbose("Looking at the size of %,d files took %s", count, watch.stop());

		/*
		 * Iterate and remove everything of size 1. This is to reduce memory as
		 * quickly as possible before creating the (potentially also big)
		 * checksum map.
		 */
		Iterator<List<File>> it = files.values().iterator();
		while (it.hasNext()) {
			if (it.next().size() <= 1) {
				it.remove();
			}
		}

		// iterate over all and look at those with same size:
		watch.restart();

		// use iterator so we can remove each when done:
		it = files.values().iterator();
		while (it.hasNext()) {
			checkFilesOfSize(it.next());
			it.remove();
		}

		verbose("---------------------");
		verbose("Checking CRC's took %s", watch.stop());
		verbose("Duplicate files: %,d", duplicates);
		verbose("Size of duplicate files: %s", FileUtils.byteCountToDisplaySize(size));
	}

	private String displayFileSize(long s) {
		return displayBytes ? Long.toString(s) : FileUtils.byteCountToDisplaySize(s);
	}

	private String filename(File f) throws IOException {
		return displayChecksum ? f.getAbsolutePath() + " (" + FileUtils.checksumCRC32(f) + ")" : f.getAbsolutePath();
	}

	private void displayDuplicates(File... files) throws IOException {
		duplicates += files.length - 1;
		long s = files[0].length();

		out("---------------------");
		out(filename(files[0]));
		for (int i = 1; i < files.length; i++) {
			size += s;
			out(" ||       size: %s", displayFileSize(s));
			out(filename(files[i]));
		}
	}

	private void checkFilesOfSize(List<File> files) throws IOException {
		if (skipChecksum) {
			displayDuplicates(files.toArray(new File[files.size()]));
		} else {
			Map<Long, File> crcs = new HashMap<Long, File>();
			for (File file : files) {
				long check = FileUtils.checksumCRC32(file);

				File other = crcs.get(check);
				if (other == null) {
					crcs.put(check, file);
				} else {
					displayDuplicates(other, file);
				}
			}
		}
	}

	public static void main(String[] args) {
		Command.main(new FindDuplicateFiles(), args);
	}
}