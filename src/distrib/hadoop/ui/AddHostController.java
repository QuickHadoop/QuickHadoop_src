package distrib.hadoop.ui;

import java.net.URL;
import java.util.ResourceBundle;

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
import distrib.hadoop.resource.Messages;
import distrib.hadoop.util.Util;

public class AddHostController extends AnchorPane implements Initializable {

	private MainApp mainApp;
	
	private Stage stage;
	
	@FXML
	private Label titleLabel;

	@FXML
	private Label ipLabel;
	
	@FXML
	private TextField ip;
	
	@FXML
	private Button cancelBtn;
	
	@FXML
	private Button okBtn;

	/** 新添加主机的类型 */
	private String newHostType;
	
    private double mouseDragOffsetX = 0;
    private double mouseDragOffsetY = 0;
    
    /** 标题提示信息 */
    private static final String TITLE_INFO = Messages.getString("AddHostController.title"); //$NON-NLS-1$
    
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
		ipLabel.setText(Messages.getString("Host.ip"));
		okBtn.setText(Messages.getString("OkBtn"));
		cancelBtn.setText(Messages.getString("CancelBtn"));
		
		ip.textProperty().addListener(showInfo);
		
		String localIp = Util.getLoaclIp();
		if(Util.ipIsValid(localIp)) {
			String net = localIp.substring(0, localIp.lastIndexOf(".") + 1); //$NON-NLS-1$
			ip.setText(net);
		}
		
		cancelBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				stage.close();
			}
		});
		
		okBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				String hostIp = ip.getText().trim();
				
				if(!Util.ipIsValid(hostIp)) {
					showErr(Messages.getString("AddHostController.ip.invalid")); //$NON-NLS-1$
					return;
				}
				
				if(!Util.ping(hostIp, 2000)) {
					showErr(Messages.getString("AddHostController.connect.failed")); //$NON-NLS-1$
					return;
				}
				
				MainController main = mainApp.getMainContr();
				if(Cluster.NN_NAME.equals(newHostType)) {
					main.addNameNode(hostIp);
				} else if(Cluster.SN_NAME.equals(newHostType)) {
					main.addSecNameNode(hostIp);
				} else if(Cluster.DN_NAME.equals(newHostType)) {
					main.addDataNode(hostIp);
				}
				stage.close();
			}
		});
		okBtn.requestFocus();
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

	public String getNewHostType() {
		return newHostType;
	}

	public void setNewHostType(String newHostType) {
		this.newHostType = newHostType;
	}
}
