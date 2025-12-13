package dev.jitesh.downloadmanager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads a single file.
 * Supports pause/resume and progress updates via DownloadStatus.
 * Returns true on success, false otherwise.
 */
public class DownloadTask implements Callable<Boolean> {
	
	private static final Logger logger = Logger.getLogger(DownloadTask.class.getName());
	
	private static final int CONNECT_TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 10000;

	private final String fileUrl;
	private final String outputFile;
	private final DownloadStatus status;
	private final PauseToken pauseToken;
	
	
	public DownloadTask(String fileUrl, String outputFile, 
			DownloadStatus status, PauseToken pauseToken) {
		
		this.fileUrl = fileUrl;
		this.outputFile = outputFile;
		this.status = status;
		this.pauseToken = pauseToken;
	}
	
	@Override
	public Boolean call() {
		
		try {		
			status.setState(DownloadState.DOWNLOADING);
			
		    URL url = URI.create(fileUrl).toURL();
		    
		    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		    conn.setConnectTimeout(CONNECT_TIMEOUT);
		    conn.setReadTimeout(READ_TIMEOUT);
		    conn.setRequestMethod("GET");
		    
		    long totalBytes = conn.getContentLengthLong();
		    status.setTotalBytes(totalBytes);
			
			try(InputStream in = new BufferedInputStream(conn.getInputStream());
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile), 8192)) {
				
				
				byte[] buffer = new byte[4096];
				int bytesRead;
				long downloaded = 0;
				
				while((bytesRead = in.read(buffer)) != -1) {
					
					// check if download is cancelled
					if (Thread.currentThread().isInterrupted()) {
						status.setState(DownloadState.CANCELLED);
						return false;
					}
					
					pauseToken.checkPaused();
					
					out.write(buffer, 0, bytesRead);
					downloaded += bytesRead;
					status.setDownloadedBytes(downloaded);
				}
				
				out.flush();
	 		}
			
			status.setState(DownloadState.COMPLETED);
			return true;
			
		} catch (InterruptedException ie) {
            // Thread was interrupted while paused or sleeping
            Thread.currentThread().interrupt(); // restore interrupt
            status.setState(DownloadState.CANCELLED);
            status.setErrorMessage("Interrupted");
            logger.log(Level.INFO, "Download interrupted: " + fileUrl);
        } catch (Exception e) {
 			StringWriter sw = new StringWriter();
 			e.printStackTrace(new PrintWriter(sw));
 			
        	status.setState(DownloadState.FAILED);
 			status.setErrorMessage(sw.toString());
 			
 			logger.log(Level.SEVERE, "Download failed: " + fileUrl, e);
 		}
		
		return false;

	}

}
