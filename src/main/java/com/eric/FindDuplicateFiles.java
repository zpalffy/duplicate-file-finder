package com.eric;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
			dirs.add(System.getProperty("user.dir"));
		}

		// look at every file and cache size:
		long start = System.currentTimeMillis();
		int count = 0;
		for (String dir : dirs) {
			File d = new File(dir);
			verbose("Checking directory: " + d.getAbsolutePath());

			for (File file : FileUtils.listFiles(d, null, true)) {
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

		verbose("Looking at the size of " + count + " files took " + (System.currentTimeMillis() - start) + " ms.");

		// iterate over all and look at those with same size:
		start = System.currentTimeMillis();
		for (List<File> filesOfSameSize : files.values()) {
			if (filesOfSameSize.size() > 1) {
				checkFilesOfSize(filesOfSameSize);
			}
		}

		verbose("---------------------");
		verbose("Checking CRC's took " + (System.currentTimeMillis() - start) + "ms.");
		verbose("Duplicate files: " + duplicates);
		verbose("Size of duplicate files: " + FileUtils.byteCountToDisplaySize(size));
	}

	private String displayFileSize(long s) {
		return displayBytes ? Long.toString(s) : FileUtils.byteCountToDisplaySize(s);
	}

	private String filename(File f) throws IOException {
		return displayChecksum ? f.getAbsolutePath() + " (" + FileUtils.checksumCRC32(f) + ")" : f.getAbsolutePath();
	}

	private void displayDuplicates(List<File> files) throws IOException {
		duplicates += files.size();
		long s = files.get(0).length();

		out("---------------------");
		out(filename(files.get(0)));
		for (int i = 1; i < files.size(); i++) {
			size += s;
			out(" ||       size: " + displayFileSize(s));
			out(filename(files.get(i)));
		}
	}

	private void checkFilesOfSize(List<File> files) throws IOException {
		if (skipChecksum) {
			displayDuplicates(files);
		} else {
			Map<Long, File> crcs = new HashMap<Long, File>();
			for (File file : files) {
				long check = FileUtils.checksumCRC32(file);

				File other = crcs.get(check);
				if (other == null) {
					crcs.put(check, file);
				} else {
					duplicates++;
					size += file.length();
					out("---------------------");
					out(filename(other));
					out(" ||       size: " + displayFileSize(file.length()));
					out(filename(file));
				}
			}
		}
	}

	public static void main(String[] args) {
		Command.main(new FindDuplicateFiles(), args);
	}
}