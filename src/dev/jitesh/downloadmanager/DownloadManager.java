package dev.jitesh.downloadmanager;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Manages downloads: start / pause / resume / cancel.
 * Uses a fixed thread pool for downloads.
 */
public class DownloadManager {
    private static final Logger logger = Logger.getLogger(DownloadManager.class.getName());
	
	private final ExecutorService pool;
	
	private final AtomicInteger idGenerator = new AtomicInteger(1);
	private final Map<Integer, DownloadStatus> statusMap = new ConcurrentHashMap<>();
	private final Map<Integer, Future<Boolean>> futureMap = new ConcurrentHashMap<>();
	private final Map<Integer, String> urlMap = new ConcurrentHashMap<>();
	private final Map<Integer, String> pathMap = new ConcurrentHashMap<>();
	private final Map<Integer, PauseToken> tokenMap = new ConcurrentHashMap<>();

	
	public DownloadManager(int maxConcurrentDownloads) {
		this.pool = Executors.newFixedThreadPool(maxConcurrentDownloads);
	}
	
	/**
	 * Starts a new download and return it's id
	 * 
	 * @param url	the file URL to download
	 * @param saveAs	local file path where download will be saved
	 * @return unique download id
	 */
	public int startDowload(String url, String saveAs) {
		int id = idGenerator.getAndIncrement();
		DownloadStatus status = new DownloadStatus();
		PauseToken token = new PauseToken();
		
		status.setState(DownloadState.QUEUED);
		statusMap.put(id, status);
		tokenMap.put(id, token);
		urlMap.put(id, url);
		pathMap.put(id, saveAs);
		
		DownloadTask task = new DownloadTask(url, saveAs, status, token);
		Future<Boolean> future = pool.submit(task);
		futureMap.put(id, future);
		
		return id;
	}
	
	/**
	 * Shuts down ExecuterService pool.
	 * 
	 */
	public void shutdown() {
		pool.shutdown();
	}

	public boolean pause(int id) {
		PauseToken t = tokenMap.get(id);
		DownloadStatus s = statusMap.get(id);
		if (t == null || s == null) return false;
		t.pause();
		s.setState(DownloadState.PAUSED);
		return true;
	}

	public boolean resume(int id) {
		PauseToken t = tokenMap.get(id);
		DownloadStatus s = statusMap.get(id);
		if (t == null || s == null) return false;
		t.resume();
		s.setState(DownloadState.DOWNLOADING);
		return true;
		
	}

	public boolean cancel(int id) {
		Future<Boolean> f = futureMap.get(id);
		DownloadStatus s = statusMap.get(id);
		if (f == null || s == null) return false;
		
		boolean cancelled = f.cancel(true); // interrupt if running
		
		if (cancelled) {
			s.setState(DownloadState.CANCELLED);
		} else {
			// If cancel returned false, it may have already completed
			if (s.getState() != DownloadState.COMPLETED) {
				s.setState(DownloadState.CANCELLED);
			}
		}
		
		tokenMap.remove(id);
		futureMap.remove(id);
		urlMap.remove(id);
		pathMap.remove(id);
		
		return cancelled;
	}


	public DownloadStatus getStatus(int id) {
		return statusMap.get(id);
	}
	
    public Map<Integer, DownloadStatus> getAllStatuses() {
        return Collections.unmodifiableMap(statusMap);
    }
}
