import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javafx.concurrent.Task;

public class DownloadTask extends Task<Long> {

	private final int BUFFER_SIZE = 32 * 1024;
	private URL url;
	private File outfile;
	private long start;
	private long size;
	private long length = 0;

	public DownloadTask(URL url, File outfile, long start, long size) throws MalformedURLException {
		this.url = url;
		this.outfile = outfile;
		this.start = start;
		this.size = size;
	}

	@Override
	protected Long call() throws IOException {
		download();
		return null;
	}

	private void download() {
		try {
			String range = "";
			URLConnection connection = url.openConnection();
			if (size > 0) {
				range = String.format("bytes=%d-%d", start, start + size - 1);
			} else {
				range = String.format("bytes=%d-", start);
			}
			connection.setRequestProperty("Range", range);
			InputStream instream = connection.getInputStream();
			RandomAccessFile writer = new RandomAccessFile(outfile, "rwd");
			writer.seek(start);
			updateProgress(0, size);
			byte[] buffer = new byte[BUFFER_SIZE];
			do {
				if (isCancelled()) {
					break;
				}
				int n = instream.read(buffer);
				if (n < 0)
					break;
				writer.write(buffer, 0, n);
				length += n;
				updateMessage(length + "");
				updateProgress(length, size);
			} while (length < size);
			writer.close();
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	}
}
