package distrib.hadoop.ui;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import distrib.hadoop.cluster.Cluster;
import distrib.hadoop.cluster.HBase;
import distrib.hadoop.cluster.Hadoop;
import distrib.hadoop.cluster.Zookeeper;
import distrib.hadoop.host.Host;
import distrib.hadoop.resource.Messages;
import distrib.hadoop.shell.RemoteShell;
import distrib.hadoop.util.Util;

public class LoginController extends AnchorPane implements Initializable {

	private MainApp mainApp;
	
	private Stage stage;
	
	@FXML
	private Label titleLabel;

	@FXML
	private Label ipLabel;
	
	@FXML
	private Label userLabel;
	
	@FXML
	private Label passwdLabel;
	
	@FXML
	private TextField ipText;
	
	@FXML
	private TextField userNameText;
	
	@FXML
	private TextField passwordText;
	
	
	@FXML
	private Button cancelBtn;
	
	@FXML
	private Button okBtn;

	
    private double mouseDragOffsetX = 0;
    private double mouseDragOffsetY = 0;
    
    /** 标题提示信息 */
    private static final String TITLE_INFO = Messages.getString("LoginController.title"); //$NON-NLS-1$
    
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
		ipLabel.setText(Messages.getString("NameNode.ip.input"));
		userLabel.setText(Messages.getString("UserName.input"));
		passwdLabel.setText(Messages.getString("Passwd.input"));
		okBtn.setText(Messages.getString("OkBtn"));
		cancelBtn.setText(Messages.getString("CancelBtn"));
		
		String localIp = Util.getLoaclIp();
		if(Util.ipIsValid(localIp)) {
			ipText.setText(localIp.substring(0, localIp.lastIndexOf(".") + 1)); //$NON-NLS-1$
		}
		
		ipText.textProperty().addListener(showInfo);
		userNameText.textProperty().addListener(showInfo);
		passwordText.textProperty().addListener(showInfo);

		cancelBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				stage.close();
			}
		});
		
		okBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				String ip = ipText.getText().trim();
				String usrName = userNameText.getText().trim();
				String passwd = passwordText.getText().trim();
				
				if(!Util.ping(ip, 2000)) {
					showErr(Messages.getString("LoginController.unreachable")); //$NON-NLS-1$
					return;
				}
				
				if(usrName.isEmpty()
						|| passwd.isEmpty()) {
					showErr(Messages.getString("LoginController.passwd.empty")); //$NON-NLS-1$
					return;
				}
					
				Host host = new Host(null, ip, usrName, passwd);
				Set<String> ips = new LinkedHashSet<String>();
				try {
					host.login();
					String ver = host.getHadoopVer();
					
					Hadoop hadoop = Hadoop.get(ver);
					if(hadoop == null) {
						showErr(Messages.getString("LoginController.no.hadoop")); //$NON-NLS-1$
						return;
					}
					
					String hadoopBin = host.getShell().getCmdOutPut("which hadoop"); //$NON-NLS-1$
					if(hadoopBin != null && !hadoopBin.isEmpty()) {
						int index = hadoopBin.indexOf("/bin/hadoop"); //$NON-NLS-1$
						hadoop.setHome(hadoopBin.substring(0, index));
						hadoop.setCfgPath();
					}
					
					ips.add(ip);
					ips.addAll(host.getMasters(hadoop));
					ips.addAll(host.getSlaves(hadoop));
					host.logout();
					createCluster(usrName, passwd, ips);
					Cluster.getInstance().setHadoop(hadoop);
					mainApp.getMainContr().refreshClusterUI(true);
				} catch (Exception e) {
					e.printStackTrace();
					showErr(Messages.getString("LoginController.connect.failed")); //$NON-NLS-1$
					return;
				}

				stage.close();
			}
		});
		
		okBtn.requestFocus();
	}
	
	private void createCluster(String usrName, String passwd, Set<String> ips) {
		Cluster cluster = Cluster.getInstance();
		List<Host> hostList = cluster.getHostList();
		hostList.clear();
		
		boolean zkInstalled = false;
		boolean hBaseInstalled = false;
		
		for(String h : ips) {
			try {		
				if(!Util.ping(h, 2000)) {
					return;
				}
				Host host = new Host(null, h, usrName, passwd);
				host.login();
				RemoteShell sh = host.getShell();
				String hostName = sh.getCmdOutPut("hostname"); //$NON-NLS-1$
				zkInstalled |= sh.fileExists(Zookeeper.getInstance().getHome());
				hBaseInstalled |= sh.fileExists(HBase.getInstance().getHome());
				host.logout();
				if(hostName == null) {
					return;
				}
				host.setHostName(hostName);
				host.isZookeeperProperty().setValue(zkInstalled);
				host.isHBaseProperty().setValue(hBaseInstalled);
				hostList.add(host);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		cluster.setSupportZookeeper(zkInstalled);
		cluster.setSupportHbase(hBaseInstalled);
		cluster.prepareWork();
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
