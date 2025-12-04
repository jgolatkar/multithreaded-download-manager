package dev.jitesh.downloadmanager;


/**
 * Controls pause/resume behavior for download tasks.
 * Uses wait/notifyAll to block threads cooperatively.
 */
public class PauseToken {
	private boolean paused = false;
	
	public synchronized void pause() {
		paused = true;
	}
	
	public synchronized void resume() {
		paused = false;
		notifyAll();
	}
	
	
	public synchronized boolean isPaused() {
		return paused;
	}
	
	
	public synchronized void checkPaused() throws InterruptedException {
		while(paused) {
			wait();
		}
	}
}
