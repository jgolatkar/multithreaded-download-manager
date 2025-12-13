package dev.jitesh.downloadmanager;

/**
 * Represents the states of a download task.
 */
public enum DownloadState {
	PENDING,
	QUEUED,
	DOWNLOADING,
	PAUSED,
	COMPLETED,
	CANCELLED,
	FAILED
}
