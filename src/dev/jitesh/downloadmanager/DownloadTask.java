package dev.jitesh.downloadmanager;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * A Callable task responsible for downloading a single file.
 * Supports pause/resume and progress updates via DownloadStatus.
 */
public class DownloadTask implements Callable<Boolean> {

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
		
		status.setState(DownloadState.DOWNLOADING);
		
		try {
		    URL url = URI.create(fileUrl).toURL();
			
			try(InputStream in = url.openStream();
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
				
				long totalBytes = url.openConnection().getContentLengthLong();
				status.setTotalBytes(totalBytes);
				
				byte[] buffer = new byte[4096];
				int bytesRead;
				long downloaded = 0;
				
				while((bytesRead = in.read(buffer)) != -1) {
					pauseToken.checkPaused();
					
					out.write(buffer, 0, bytesRead);
					downloaded += bytesRead;
					status.setDownloadedBytes(downloaded);
				}
				
				status.setState(DownloadState.COMPLETED);
				return true;
	 		} 
		} catch (InterruptedException ie) {
            // Thread was interrupted while paused or sleeping
            Thread.currentThread().interrupt(); // restore interrupt
            status.setState(DownloadState.CANCELLED);
        } catch (Exception e) {
 			status.setState(DownloadState.FAILED);
 			status.setErrorMessage(e.getMessage());
 		}
		
		return false;

	}

}
