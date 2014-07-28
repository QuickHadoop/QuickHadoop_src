package distrib.hadoop.ui;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import distrib.hadoop.resource.Messages;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainApp extends Application {
	
	private Stage mainStage;
	private MainController mainContr;
	private WizardController wizardContr;
	private LoginController loginContr;
	private ScanController scanContr;
	private ConfigController configContr;
	private AddHostController addContr;
	private AddHostsController addsContr;
	private InfoDlgController infoContr;
    
	private Image tray = new Image(MainApp.class.getResourceAsStream("images/tray.png"));
	
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(MainApp.class, (java.lang.String[])null);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            try {  
                /* 将打印重定向到文件中 */
                PrintStream ps = new PrintStream("log.txt");  
                System.setOut(ps);
                System.setErr(ps);
            } catch (FileNotFoundException e) {  
                e.printStackTrace();  
            } 
            
        	String locale = System.getProperty("user.language");
    		if(Locale.CHINESE.toString().equals(locale)){
    			Messages.setLocale(Locale.CHINESE);
    		} else {
    			Messages.setLocale(Locale.ENGLISH);
    		}
        	
        	mainStage = primaryStage;
        	mainStage.setTitle(Messages.getString("MainApp.title")); //$NON-NLS-1$
        	mainStage.initStyle(StageStyle.UNDECORATED);
            showMainPane();
            mainStage.getIcons().add(tray);
            mainStage.show();
            
            /* 扫描本地主机 */
            if(mainContr != null) {
            	mainContr.scanLocalHosts();
            }
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 显示主界面
     * 
     * @param stage
     */
    public void showMainPane() {
        try {
        	mainContr = (MainController) getController(mainStage, "Main.fxml"); //$NON-NLS-1$
            mainContr.setApp(this);
            mainContr.setStage(mainStage);
            mainContr.setInputMainView();
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 显示安装向导框
     * 
     * @param stage
     */
    public void showInstallWizard() {
        try {
        	Stage wizardStage = new Stage(StageStyle.TRANSPARENT);
        	wizardStage.initModality(Modality.APPLICATION_MODAL);
        	wizardStage.initOwner(mainStage);
        	wizardContr = (WizardController) getController(wizardStage, "Wizard.fxml"); //$NON-NLS-1$
        	wizardContr.setApp(this);
        	wizardContr.setStage(wizardStage);
        	wizardStage.show();
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 显示登录对话框
     * 
     * @param stage
     */
    public void showLoginDialog() {
        try {
        	Stage loginDlg = new Stage(StageStyle.TRANSPARENT);
        	loginDlg.initModality(Modality.APPLICATION_MODAL);
        	loginDlg.initOwner(mainStage);
        	loginContr = (LoginController) getController(loginDlg, "Login.fxml"); //$NON-NLS-1$
        	loginContr.setApp(this);
        	loginContr.setStage(loginDlg);
        	loginDlg.show();
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 显示扫描对话框
     * 
     * @param stage
     */
    public void showScanDialog() {
        try {
        	Stage scanDlg = new Stage(StageStyle.TRANSPARENT);
        	scanDlg.initModality(Modality.APPLICATION_MODAL);
        	scanDlg.initOwner(mainStage);
        	scanContr = (ScanController) getController(scanDlg, "Scan.fxml"); //$NON-NLS-1$
        	scanContr.setApp(this);
        	scanContr.setStage(scanDlg);
        	scanDlg.show();
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 显示添加主机网段对话框
     * 
     * @param stage
     */
    public void showAddHostsDialog() {
        try {
        	Stage addDlg = new Stage(StageStyle.TRANSPARENT);
        	addDlg.initModality(Modality.APPLICATION_MODAL);
        	addDlg.initOwner(mainStage);
        	addsContr = (AddHostsController) getController(addDlg, "AddHosts.fxml"); //$NON-NLS-1$
        	addsContr.setApp(this);
        	addsContr.setStage(addDlg);
        	addDlg.show();
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 显示提示对话框
     * 
     * @param stage
     */
    public int showInfoDialog(String info) {
        try {
        	Stage infoDlg = new Stage(StageStyle.TRANSPARENT);
        	infoDlg.initModality(Modality.APPLICATION_MODAL);
        	infoDlg.initOwner(mainStage);
        	infoContr = (InfoDlgController) getController(infoDlg, "InfoDlg.fxml"); //$NON-NLS-1$
        	infoContr.setStage(infoDlg);
        	infoContr.setInfo(info);
        	infoDlg.show();
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return infoContr.getRet();
    }
    
    /**
     * 显示添加主机对话框
     * 
     * @param stage
     */
    public void showAddHostDialog(String hostType) {
        try {
        	Stage addDlg = new Stage(StageStyle.TRANSPARENT);
        	addDlg.initModality(Modality.APPLICATION_MODAL);
        	addDlg.initOwner(mainStage);
        	addContr = (AddHostController) getController(addDlg, "AddHost.fxml"); //$NON-NLS-1$
        	addContr.setApp(this);
        	addContr.setNewHostType(hostType);
        	addContr.setStage(addDlg);
        	addDlg.show();
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 显示配置文件对话框
     * 
     * @param stage
     */
    public void showConfigDialog() {
        try {
        	Stage configDlg = new Stage(StageStyle.TRANSPARENT);
        	configDlg.initModality(Modality.APPLICATION_MODAL);
        	configDlg.initOwner(mainStage);
        	configContr = (ConfigController) getController(configDlg, "Config.fxml"); //$NON-NLS-1$
        	configContr.setStage(configDlg);
        	configDlg.show();
        } catch (Exception ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 获取fxml文件对应的controller
     * 
     * @param stage
     * @param fxml
     * @return
     * @throws Exception
     */
    private Initializable getController(Stage stage, String fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        InputStream in = MainApp.class.getResourceAsStream(fxml);
        if(in == null) {
        	throw new NullPointerException("fxml not found!");
        }
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        loader.setLocation(MainApp.class.getResource(fxml));
        AnchorPane page;
        try {
            page = (AnchorPane) loader.load(in);
        } finally {
            in.close();
        } 
        Scene scene = new Scene(page);
        stage.setScene(scene);
        stage.centerOnScreen();
        return (Initializable) loader.getController();
    }
    
	/**
	 * @return the mainContr
	 */
	public MainController getMainContr() {
		return mainContr;
	}

	/**
	 * @param mainContr the mainContr to set
	 */
	public void setMainContr(MainController mainContr) {
		this.mainContr = mainContr;
	}

	/**
	 * @return the wizardContr
	 */
	public WizardController getWizardContr() {
		return wizardContr;
	}

	/**
	 * @param wizardContr the wizardContr to set
	 */
	public void setWizardContr(WizardController wizardContr) {
		this.wizardContr = wizardContr;
	}

	/**
	 * @return the loginContr
	 */
	public LoginController getLoginContr() {
		return loginContr;
	}

	/**
	 * @param loginContr the loginContr to set
	 */
	public void setLoginContr(LoginController loginContr) {
		this.loginContr = loginContr;
	}

	public InfoDlgController getInfoContr() {
		return infoContr;
	}

	public void setInfoContr(InfoDlgController infoContr) {
		this.infoContr = infoContr;
	}
}
