package dev.jitesh.downloadmanager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Manages all active downloads using a fixed thread pool.
 */
public class DownloadManager {
	
	private final ExecutorService pool;
	
	public DownloadManager(int maxConcurrentDownloads) {
		pool = Executors.newFixedThreadPool(maxConcurrentDownloads);
	}
	
	/**
	 * Submits a new download task.
	 * 
	 * @param url: String representing download URL
	 * @param out: String representing output location
	 * @param status: DownloadStatus object
	 * @param token: PauseToken object
	 * @return Future representing download completion.
	 */
	public Future<Boolean> download(String url, String out, DownloadStatus status, PauseToken token) {
		return pool.submit(new DownloadTask(url, out, status, token));
	}
	
	/**
	 * Shuts down ExecuterService pool.
	 * 
	 */
	public void shutdown() {
		pool.shutdown();
	}
}
