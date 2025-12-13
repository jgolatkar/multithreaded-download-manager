package dev.jitesh.downloadmanager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * A minimal Swing-based GUI for downloading a single file.
 * Uses SwingWorker to check progress safely on EDT.
 */
public class DownloadGUI {
	
	private static final Logger logger = Logger.getLogger(DownloadGUI.class.getName());

	
	private final DownloadManager manager;
	
	// UI Components
	private final JFrame frame; 
	private final JTextField urlField;
	private final JTextField outField;
	private final JButton dwnldBtn;
	private final JPanel listPanel;
	
	public DownloadGUI(DownloadManager manager) {
		this.manager = manager;
		frame = new JFrame("Download Manager");
		urlField = new JTextField();
		outField = new JTextField();
		dwnldBtn = new JButton("Download");
		listPanel = new JPanel();
	}
	
	public void display() {
		
		frame.setSize(700, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout(8, 8));
		
		// Top Input Panel
		JPanel topPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
		topPanel.add(new JLabel("URL: "), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		topPanel.add(urlField, gbc);
		
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
		topPanel.add(new JLabel("Save As: "), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		topPanel.add(outField, gbc);
		
		gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 2; gbc.weightx = 0.0;
		topPanel.add(dwnldBtn, gbc);
		frame.add(topPanel, BorderLayout.NORTH);
		
		// Center: scrollable list of downloads
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		JScrollPane scroll = new JScrollPane(listPanel);
		frame.add(scroll, BorderLayout.CENTER);
		
		// Action for download button
		dwnldBtn.addActionListener(e -> addDownload());
		
		// Start SwingWorker to refresh UI periodically
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				while (!isCancelled()) {
					SwingUtilities.invokeLater(() -> refreshAllRows());
					Thread.sleep(200);
				}
				return null;
			}
			
		}.execute();
		
		frame.setVisible(true);
	}

	private void addDownload() {
		String url = urlField.getText().trim();
		String saveAs = outField.getText().trim();
		
		if (url.isEmpty() || saveAs.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Provide both URL and Save As path", "Input Required", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		int id = manager.startDowload(url, saveAs);
		DownloadStatus status = manager.getStatus(id);
		
		DownloadRow row = new DownloadRow(id, url, saveAs, status, manager);
        listPanel.add(row.getPanel());
        listPanel.revalidate();

        // clear url/out fields for convenience
        urlField.setText("");
        outField.setText("");
	}
	
	private void refreshAllRows() {
        // iterate through components and refresh each row UI
        Component[] comps = listPanel.getComponents();
        for (Component c : comps) {
            if (c instanceof JPanel) {
                // cast to our row panel convention
                JPanel panel = (JPanel) c;
                Object tag = panel.getClientProperty("downloadRow");
                if (tag instanceof DownloadRow) {
                    ((DownloadRow) tag).refresh();
                }
            }
        }
    }
	
	
	private static class DownloadRow {
		private final int id;
		private final String url;
		private final String path;
		private final DownloadStatus status;
		private final DownloadManager manager;
		
		private final JPanel panel = new JPanel(new GridBagLayout());
		private final JLabel lblName = new JLabel();
		private final JProgressBar progressBar = new JProgressBar(0, 100);
		private final JLabel lblState = new JLabel();
		private final JButton btnPause = new JButton("Pause");
		private final JButton btnResume = new JButton("Resume");
		private final JButton btnCancel = new JButton("Cancel");
		
		DownloadRow(int id, String url, String path, DownloadStatus status, DownloadManager manager) {
			this.id = id;
			this.url = url;
			this.path = path;
			this.status = status;
			this.manager = manager;
			
			buildUI();
			panel.putClientProperty("downloadRow", this);
		}
		
		private void buildUI() {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(4, 4, 4, 4);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			lblName.setText("[" + id + "] " + path + " (" + shortUrl(url) + ")");
            progressBar.setStringPainted(true);

            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
            panel.add(lblName, gbc);

            gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.0;
            panel.add(lblState, gbc);

            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
            panel.add(progressBar, gbc);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            btnPanel.add(btnPause);
            btnPanel.add(btnResume);
            btnPanel.add(btnCancel);

            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
            panel.add(btnPanel, gbc);

            // initial visibility
            btnResume.setEnabled(false);
            
            // actions
            btnPause.addActionListener(e -> {
                manager.pause(id);
                btnPause.setEnabled(false);
                btnResume.setEnabled(true);
            });

            btnResume.addActionListener(e -> {
                manager.resume(id);
                btnPause.setEnabled(true);
                btnResume.setEnabled(false);
            });

            btnCancel.addActionListener(e -> {
                manager.cancel(id);
                btnPause.setEnabled(false);
                btnResume.setEnabled(false);
                btnCancel.setEnabled(false);
            });

            panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		}

		JPanel getPanel() {
			return panel;
		}
		
		void refresh() {
            DownloadState st = status.getState();
            lblState.setText(st.toString());

            int percent = status.getProgressPercent();
            if (percent >= 0) {
                progressBar.setValue(percent);
                progressBar.setIndeterminate(false);
                progressBar.setString(percent + "%");
            } else {
                // unknown total -> indeterminate progress
                progressBar.setIndeterminate(true);
                progressBar.setString("Downloading...");
            }

            if (st == DownloadState.COMPLETED) {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                progressBar.setString("100%");
                btnPause.setEnabled(false);
                btnResume.setEnabled(false);
                btnCancel.setEnabled(false);
            } else if (st == DownloadState.FAILED) {
                progressBar.setIndeterminate(false);
                progressBar.setString("Failed");
                btnPause.setEnabled(false);
                btnResume.setEnabled(false);
                btnCancel.setEnabled(false);

                String err = status.getErrorMessage();
                if (err != null && !err.isEmpty()) {
                    // small non-blocking info
                    lblState.setText("FAILED");
                    // optionally show a tooltip with error
                    panel.setToolTipText(err.length() > 200 ? err.substring(0, 200) + "..." : err);
                }
            } else if (st == DownloadState.CANCELLED) {
                progressBar.setIndeterminate(false);
                progressBar.setString("Cancelled");
                btnPause.setEnabled(false);
                btnResume.setEnabled(false);
                btnCancel.setEnabled(false);
            }
        }

        private String shortUrl(String u) {
            if (u == null) return "";
            if (u.length() < 60) return u;
            return u.substring(0, 30) + "..." + u.substring(u.length() - 20);
        }
		
	}
}
