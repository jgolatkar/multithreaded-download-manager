package dev.jitesh.downloadmanager;

import java.awt.GridLayout;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * A minimal Swing-based GUI for downloading a single file.
 * Uses SwingWorker to check progress safely on EDT.
 */
public class DownloadGUI {
	
	private static final Logger logger = Logger.getLogger(DownloadGUI.class.getName());
	
	public void display() {
		
		JFrame frame = new JFrame("Download Manager");
		frame.setSize(550, 350);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTextField urlField = new JTextField();
		JTextField outField = new JTextField();
		JButton dwnldBtn = new JButton("Download");
		
		JProgressBar pgrsBar = new JProgressBar();
		pgrsBar.setStringPainted(true);
		
		frame.setLayout(new GridLayout(5, 1));
		frame.add(new JLabel("File URL"));
		frame.add(urlField);
		frame.add(new JLabel("Save As"));
		frame.add(outField);
		frame.add(dwnldBtn);
		frame.add(pgrsBar);
		
		dwnldBtn.addActionListener(event -> {
			
			String url = urlField.getText();
			String saveAs = outField.getText();
			
			logger.info("Downloading for URL: " +  url);
			logger.info("Saved at: " +  saveAs);
			
			DownloadManager manager = new DownloadManager(3);
			DownloadStatus status = new DownloadStatus();
			PauseToken token = new PauseToken();
			
			manager.download(url, saveAs, status, token);
			
			new SwingWorker<Void, Void>() {

				@Override
				protected Void doInBackground() throws Exception {
					
					while(status.getState() != DownloadState.COMPLETED
							&& status.getState() != DownloadState.FAILED
							&& status.getState() != DownloadState.CANCELLED) {
						
						long totalBytes = status.getTotalBytes();
						long dwldBytes = status.getDownloadedBytes();
						
						if (totalBytes > 0) {
							pgrsBar.setValue((int)(dwldBytes * 100 / totalBytes));
							Thread.sleep(200);
						}
					}
					
					return null;
				}

				@Override
				protected void done() {
					if(status.getState() == DownloadState.COMPLETED) {
						pgrsBar.setValue(100);
					}
				}
				
				
			
			}.execute();
		});
		
		frame.setVisible(true);
	}
}
