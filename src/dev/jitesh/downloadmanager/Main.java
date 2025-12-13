package dev.jitesh.downloadmanager;

import javax.swing.SwingUtilities;

public class Main {
	
	private static final int MAX_CONCURRENT_DOWNLOADS = 20;
	
	public static void main(String[] args) {
		
		DownloadManager manager = new DownloadManager(MAX_CONCURRENT_DOWNLOADS);
		
		SwingUtilities.invokeLater(() -> {
			DownloadGUI gui = new DownloadGUI(manager);
			gui.display();
		});
		
		// clean up thread pool when app exists
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			manager.shutdown();
		}));
	}
}
