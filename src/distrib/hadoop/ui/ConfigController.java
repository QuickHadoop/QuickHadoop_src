package distrib.hadoop.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import distrib.hadoop.cluster.Cluster;
import distrib.hadoop.cluster.Hadoop;
import distrib.hadoop.exception.AuthException;
import distrib.hadoop.host.Host;
import distrib.hadoop.resource.Messages;

public class ConfigController extends AnchorPane implements Initializable {
	
	private Stage stage;
	
	@FXML
	private Label titleLabel;

	@FXML
	private Label fileLabel;

	@FXML
	private Label uploadLabel;
	
	@FXML
	private TextField fileText;
	
	@FXML
	private TextField pathText;
	
	@FXML
	private Button browseBtn;
	
	@FXML
	private Button cancelBtn;
	
	@FXML
	private Button okBtn;

	@FXML
	private ProgressBar progressbar;
	
	private DoubleProperty progress = new SimpleDoubleProperty(0);
	
    private double mouseDragOffsetX = 0;
    private double mouseDragOffsetY = 0;
    
    private File file;
    private boolean uploadFinished;
    
    /** 标题提示信息 */
    private static final String TITLE_INFO = Messages.getString("ConfigController.title"); //$NON-NLS-1$
    
    /**
     * 监听文本框显示提示信息
     */
    private ChangeListener<String> showInfo = new ChangeListener<String>() {
		@Override
		public void changed(ObservableValue<? extends String> arg0,
				String arg1, String arg2) {
			titleLabel.setText(TITLE_INFO);
			titleLabel.getStyleClass().remove("label-err"); //$NON-NLS-1$
			titleLabel.getStyleClass().add("label"); //$NON-NLS-1$
		}
	};
    
	/**
	 * 显示错误信息
	 * 
	 * @param err
	 */
	private void showErr(String err) {
		titleLabel.setText(err);
		titleLabel.getStyleClass().remove("label"); //$NON-NLS-1$
		titleLabel.getStyleClass().add("label-err"); //$NON-NLS-1$
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		titleLabel.setText(TITLE_INFO);
		fileLabel.setText(Messages.getString("Upload.file.label"));
		uploadLabel.setText(Messages.getString("Upload.label"));
		browseBtn.setText(Messages.getString("BrowseBtn"));
		okBtn.setText(Messages.getString("OkBtn"));
		cancelBtn.setText(Messages.getString("CancelBtn"));
		
		setDefultUploadPath();
		
		fileText.textProperty().addListener(showInfo);
		pathText.textProperty().addListener(showInfo);
		
		browseBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle(Messages.getString("ConfigController.select.file")); //$NON-NLS-1$
				file = fileChooser.showOpenDialog(stage);
				if(file != null) {
					fileText.setText(file.getAbsolutePath());
				}
			}
		});
		
		cancelBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				stage.close();
			}
		});
		
		okBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				if(uploadFinished) {
					stage.close();
					return;
				}
				
				final Cluster cluster = Cluster.getInstance();
				
				final String localPath = fileText.getText().trim();
				final String remotePath = pathText.getText().trim();
				final String fileName = localPath.substring(localPath.lastIndexOf(File.separator) + 1);
				final String remoteFile = remotePath + "/" + fileName; //$NON-NLS-1$
				
				if(localPath.isEmpty()
						|| remotePath.isEmpty()) {
					showErr(Messages.getString("ConfigController.file.empty")); //$NON-NLS-1$
					return;
				}
				
				File test = new File(localPath);
				if(!test.exists()) {
					showErr(Messages.getString("ConfigController.file.nonexist")); //$NON-NLS-1$
					return;
				}
				
				if(!test.isFile()) {
					showErr(Messages.getString("ConfigController.file.invalid")); //$NON-NLS-1$
					return;
				}
				
				progressbar.setVisible(true);
				fileText.setDisable(true);
				pathText.setDisable(true);
				browseBtn.setDisable(true);
				
				try {
					cluster.login();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
		    	new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							cluster.putFile(localPath, fileName, remotePath);
							progress.set(1);
							cluster.logout();
							uploadFinished = true;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
		    	
		    	final long totalSize = file.length() * cluster.getHostList().size();

		    	new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							while(!uploadFinished) {
								Thread.sleep(1000);
								long size = 0;
								for(Host h : cluster.getHostList()) {
									size += h.getFileSize(remoteFile);
								}

								double r = (double)size / (double)totalSize;
								if(r > progress.get()) {
									progress.set(r);									
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		});
		
		okBtn.requestFocus();
		
		progress.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				if(progressbar.getProgress() < progress.doubleValue()) {
					progressbar.setProgress(progress.doubleValue());
				}
				
				Platform.runLater(new Runnable() {
				    @Override
				    public void run() {
						if(progress.doubleValue() >= 1) {
							okBtn.setText(Messages.getString("ConfigController.close")); //$NON-NLS-1$
						}
				    }
				});
			}
		});
	}

	/**
	 * 设置默认的上传目录
	 */
	private void setDefultUploadPath() {
		Hadoop hadoop = Cluster.getInstance().getHadoop();
		Host nameNode = Cluster.getInstance().getNameNode();
		if(hadoop != null && nameNode != null) {
			try {
				nameNode.login();
				nameNode.getHostEnv();
				nameNode.logout();
				String defText = hadoop.getCfgPath();
				if(!defText.startsWith(nameNode.getUserHome())) {
					defText = nameNode.getUserHome() + "/" + defText; //$NON-NLS-1$
				}
				pathText.setText(defText);
			} catch (AuthException | IOException e) {
				e.printStackTrace();
			}			
		}
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
    /**
     * 关闭向导
     * 
     * @param event
     */
    @FXML
    public void windowClosePressed(ActionEvent event) {
    	stage.close();
    }
    
    /**
     * 点击向导框
     * 
     * @param event
     */
    @FXML
    public void windowPressed(MouseEvent event) {
        mouseDragOffsetX = event.getSceneX();
        mouseDragOffsetY = event.getSceneY();
    }
    
    /**
     * 拖拽向导框
     * 
     * @param event
     */
    @FXML
    public void windowDragged(MouseEvent event) {
        stage.setX(event.getScreenX()-mouseDragOffsetX);
        stage.setY(event.getScreenY()-mouseDragOffsetY);
    }
}
