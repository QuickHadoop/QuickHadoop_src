package distrib.hadoop.ui;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import distrib.hadoop.cluster.Cluster;
import distrib.hadoop.cluster.Hadoop;
import distrib.hadoop.cluster.HadoopV1;
import distrib.hadoop.host.Host;
import distrib.hadoop.resource.Messages;

public class WizardController extends AnchorPane implements Initializable {

	private MainApp mainApp;
	
	private Stage stage;
	
	@FXML
	private Label passwdTile;
	
	@FXML
	private Label selectFileTile;

	@FXML
	private Label installTile;
	
	@FXML
	private StackPane wizardStack;
	
	@FXML
	private GridPane loginPage;	
	
	@FXML
	private GridPane versionPage;	
	
	@FXML
	private GridPane zookeeperPage;
	
	@FXML
	private GridPane finishPage;
	
	@FXML
	private Label userLabel;
	
	@FXML
	private Label passwdLabel;
	
	@FXML
	private TextField userNameText;
	
	@FXML
	private TextField passwordText;

	@FXML
	private Label fileLabel;
	
	@FXML
	private TextField fileText;
	
	@FXML
	private Button browseBtn;
	
	@FXML
	private ImageView loginOk;

	@FXML
	private ImageView hostNameOk;
	
	@FXML
	private ImageView rsaOk;
	
	@FXML
	private ImageView setupOk;
	
	@FXML
	private ImageView configOk;
	
	@FXML
	private ImageView formatOk;
	
	@FXML
	private Button previousBtn;
	
	@FXML
	private Button nextBtn;
	
	@FXML
	private Button cancelBtn;
	
	@FXML
	private Button finishBtn;
	
	@FXML
	private Label loginLabel;

	@FXML
	private Label hostNameLabel;
	
	@FXML
	private Label rsaLabel;
	
	@FXML
	private Label setupLabel;
	
	@FXML
	private Label configLabel;
	
	@FXML
	private Label formatLabel;
	
	@FXML
	private Label successLabel;
	
	@FXML
	private ProgressBar progressBar;
	
	@FXML
	private TableView<Host> rolesTable;

	@FXML
	private Label haLabel;
	
	@FXML
	private Label hbaseLabel;
	
	@FXML
	private Label zookLabel;
	
	@FXML
	private CheckBox haCheck;
	
	@FXML
	private CheckBox hbaseCheck;
	
	@FXML
	private CheckBox zookeeperCheck;
	
    private double mouseDragOffsetX = 0;
    private double mouseDragOffsetY = 0;
	
    private List<GridPane> pages = new ArrayList<GridPane>();
    private DoubleProperty progress = new SimpleDoubleProperty(0);
    private Hadoop hadoop;
    
    /** 选择文件标题提示信息 */
    private static final String SEL_FILE_TITLE = Messages.getString("WizardController.select.file.title"); //$NON-NLS-1$
    
    
	/**
	 * 显示错误信息
	 * 
	 * @param err
	 */
	private void showErr(Label label, String err) {
		label.setText(err);
		label.getStyleClass().remove("label"); //$NON-NLS-1$
		label.getStyleClass().add("label-err"); //$NON-NLS-1$
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		passwdTile.setText(Messages.getString("WizardController.passwd.title"));
		selectFileTile.setText(SEL_FILE_TITLE);
		installTile.setText(Messages.getString("Install.hadoop"));
		
		userLabel.setText(Messages.getString("UserName.input"));
		passwdLabel.setText(Messages.getString("Passwd.input"));
		fileLabel.setText(Messages.getString("File.input"));
		loginLabel.setText(Messages.getString("LoginLabel"));
		hostNameLabel.setText(Messages.getString("CfgHostNameLabel"));
		rsaLabel.setText(Messages.getString("RsaLabel"));
		setupLabel.setText(Messages.getString("SetupLabel"));
		configLabel.setText(Messages.getString("ConfigLabel"));
		formatLabel.setText(Messages.getString("FormatLabel"));
		successLabel.setText(Messages.getString("SuccessLabel"));
		haLabel.setText(Messages.getString("HALabel"));
		hbaseLabel.setText(Messages.getString("HBaseLabel"));
		zookLabel.setText(Messages.getString("ZookeeperLabel"));
		
		previousBtn.setText(Messages.getString("PreviousBtn"));
		nextBtn.setText(Messages.getString("NextBtn"));
		cancelBtn.setText(Messages.getString("CancelBtn"));
		finishBtn.setText(Messages.getString("FinishBtn"));
		browseBtn.setText(Messages.getString("BrowseBtn"));
		
		haCheck.setText(Messages.getString("HACheck"));
		hbaseCheck.setText(Messages.getString("HBaseCheck"));
		zookeeperCheck.setText(Messages.getString("ZookeeperCheck"));
		
		pages.add(loginPage);
		pages.add(versionPage);
		pages.add(zookeeperPage);
		pages.add(finishPage);
		
		fileText.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0,
					String arg1, String arg2) {
				selectFileTile.setText(SEL_FILE_TITLE);
				selectFileTile.getStyleClass().remove("label-err"); //$NON-NLS-1$
				selectFileTile.getStyleClass().add("label"); //$NON-NLS-1$
			}
		});
		
		browseBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle(Messages.getString("WizardController.select.file")); //$NON-NLS-1$
				File file = fileChooser.showOpenDialog(stage);
				if(file != null) {
					String path = file.getAbsolutePath();
					fileText.setText(path);
				}
			}
		});
		
		nextBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				int curIndex = pages.indexOf(getCurrentPage());
				if(curIndex >= pages.size() - 1) {
					return;
				}
				
				if(getCurrentPage().equals(loginPage) 
						&& !checkLogin()) {
					return;
				}
				
				if(getCurrentPage().equals(versionPage)) {
					if(!checkFilePass(fileText.getText())) {
						return;
					}
					prepareInstall();
				}
				
				GridPane nextPage = pages.get(++curIndex);
				if(nextPage.equals(zookeeperPage)) {
					TableFactory.getInstance(rolesTable).fill();
				}
				
				showPage(nextPage);
				
				previousBtn.setDisable(false);
				
				if(curIndex >= pages.size() - 1) {
					previousBtn.setDisable(true);
					nextBtn.setDisable(true);
					cancelBtn.setDisable(true);
					setUpHadoop();
				}
			}
		});
		
		previousBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				int curIndex = pages.indexOf(getCurrentPage());
				if(curIndex < 1) {
					return;
				}
				
				showPage(pages.get(--curIndex));
				if(curIndex < 1) {
					previousBtn.setDisable(true);
				}
				
				cancelBtn.setDisable(false);
				finishBtn.setDisable(true);
				nextBtn.setDisable(false);
			}
		});
		
		cancelBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				stage.close();
			}
		});
		
		finishBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				stage.close();
			}
		});
		
		progress.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				double v = newValue.doubleValue();
				loginLabel.setVisible(true);
				if(v >= 0.1) {
					loginOk.setVisible(true);
					hostNameLabel.setVisible(true);
				}
				if(v >= 0.2) {
					hostNameOk.setVisible(true);
					rsaLabel.setVisible(true);
				}
				if(v >= 0.4) {
					rsaOk.setVisible(true);
					setupLabel.setVisible(true);
				}
				if(v >= 0.6) {
					setupOk.setVisible(true);
					configLabel.setVisible(true);
				}
				if(v >= 0.7) {
					configOk.setVisible(true);
					formatLabel.setVisible(true);
				}
				if(v >= 0.8) {
					formatOk.setVisible(true);
				}
				if(v >= 1) {
					configOk.setVisible(true);
					successLabel.setVisible(true);
					finishBtn.setDisable(false);
				}
				progressBar.setProgress(v);
			}
		});
		
		ChangeListener<Boolean> checkListener = new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov,
					Boolean old_val, Boolean new_val) {
				if(new_val) {
					zookeeperCheck.setSelected(true);
				}
				if(!haCheck.isSelected() && !hbaseCheck.isSelected()) {
					zookeeperCheck.setSelected(false);
				}
			}
		};
		
		zookeeperCheck.setDisable(true);
		haCheck.selectedProperty().addListener(checkListener);
		hbaseCheck.selectedProperty().addListener(checkListener);
	}
	
	/**
	 * 检查用户名密码输入
	 * 
	 * @param path
	 * @return
	 */
	private boolean checkLogin() {
		String name = userNameText.getText();
		String passwd = passwordText.getText();
		if(name == null || name.isEmpty() 
				|| passwd == null || passwd.isEmpty()) {
			return false;
		}
		
		mainApp.getMainContr().setUpClustre(name, passwd);
		return true;
	}
	
	/**
	 * 检查输入的文件
	 * 
	 * @param path
	 * @return
	 */
	private boolean checkFilePass(String path) {
		if(!path.endsWith(".tar.gz")) { //$NON-NLS-1$
			showErr(selectFileTile, Messages.getString("WizardController.file.tar")); //$NON-NLS-1$
			return false;
		}
		
		hadoop = Hadoop.getFromFile(path);
		if(hadoop == null) {
			showErr(selectFileTile, Messages.getString("WizardController.file.invalid")); //$NON-NLS-1$
			return false;
		}
		
		if((hadoop instanceof HadoopV1) && haCheck.isSelected()) {
			showErr(selectFileTile, Messages.getString("WizardController.ha.support")); //$NON-NLS-1$
			return false;
		}
		
		return true;
	}
	
    /**
     * 设置集群安装特性
     */
    private void prepareInstall() {
    	Cluster cluster = Cluster.getInstance();
    	cluster.setHadoop(hadoop);
    	cluster.setHaAutoRecover(haCheck.isSelected());
    	cluster.setSupportHbase(hbaseCheck.isSelected());
    	cluster.setSupportZookeeper(zookeeperCheck.isSelected());
    }
	
    /**
     * 安装Hadoop
     */
    private void setUpHadoop() {
    	new Thread(new Runnable() {
			
			@Override
			public void run() {
				deployHadoop();
			}
		}).start();
    }
    
    /**
     * 部署Hadoop
     */
    private void deployHadoop() {
		Cluster clusters = Cluster.getInstance();
		
		try {
			clusters.login();
			progress.set(0.1);
			clusters.prepareWork();
			clusters.stopAll();
			progress.set(0.2);
			clusters.cfgHostName();
			clusters.cfgDNS();
			progress.set(0.3);
			clusters.shareRsa();
			progress.set(0.4);
			clusters.getHostEnv();
			progress.set(0.5);
			clusters.install();
			progress.set(0.6);
			clusters.closeFireWall();
			progress.set(0.7);
			clusters.initialize();
			progress.set(0.8);
			clusters.logout();
			progress.set(1.0);
			refreshUI();
		} catch (Exception e) {
			e.printStackTrace();
			showInstallErr(e.getMessage());
			clusters.logout();
		}
    }
    
    
    /**
     * 显示安装错误信息
     */
    private void showInstallErr(final String errInfo) {
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
		    	double prog = progress.get();
		    	if(prog < 0.8) {
		    		successLabel.setVisible(true);
		    		showErr(successLabel, errInfo);
		    		cancelBtn.setDisable(false);
		    	}
		    }
		});
    }
    
    /**
     * 安装成功，刷新UI
     */
    private void refreshUI() {
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
		    	mainApp.getMainContr().refreshClusterUI(true);
		    }
		});
    }
    
	/**
	 * 获取当前页
	 * @return
	 */
	private GridPane getCurrentPage() {
		for(GridPane p : pages) {
			if(p.isVisible()) {
				return p;
			}
		}
		return loginPage;
	}

	/**
	 * 显示page页
	 * @param page
	 */
	private void showPage(GridPane page) {
		for(Node c : wizardStack.getChildren()){
			c.setVisible(false);
		}
		
		page.setVisible(true);
	}
	
	public void setApp(MainApp mainApp) {
		this.mainApp = mainApp;
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