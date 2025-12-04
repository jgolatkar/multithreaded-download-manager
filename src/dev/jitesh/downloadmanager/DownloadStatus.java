package dev.jitesh.downloadmanager;


/**
 * Holds the progress and state information of a file download.
 * All methods are synchronized to ensure thread-safe updates.
 */
public class DownloadStatus {

	private long downloadedBytes;
	private long totalBytes;
	private String errorMessage;
	private DownloadState state;
	
	public DownloadStatus() {
        this.state = DownloadState.QUEUED;
        this.totalBytes = 0;
        this.downloadedBytes = 0;
        this.errorMessage = null;
    }

	public synchronized long getDownloadedBytes() {
		return downloadedBytes;
	}

	public synchronized void setDownloadedBytes(long downloadedBytes) {
		this.downloadedBytes = downloadedBytes;
	}

	public synchronized long getTotalBytes() {
		return totalBytes;
	}

	public synchronized void setTotalBytes(long totalBytes) {
		this.totalBytes = totalBytes;
	}

	public synchronized DownloadState getState() {
		return state;
	}

	public synchronized void setState(DownloadState state) {
		this.state = state;
	}
	
    public synchronized void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }

    public synchronized String getErrorMessage() {
        return errorMessage;
    }
	
	
}
