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
import distrib.hadoop.resource.Messages;
import distrib.hadoop.util.Util;

public class ScanController extends AnchorPane implements Initializable {

	private MainApp mainApp;
	
	private Stage stage;
	
	@FXML
	private Label titleLabel;

	@FXML
	private Label startLabel;

	@FXML
	private Label endLabel;
	
	@FXML
	private TextField startIp;
	
	@FXML
	private TextField endIp;
	
	@FXML
	private Button cancelBtn;
	
	@FXML
	private Button okBtn;

	
    private double mouseDragOffsetX = 0;
    private double mouseDragOffsetY = 0;
    
    /** 标题提示信息 */
    private static final String TITLE_INFO = Messages.getString("ScanController.title"); //$NON-NLS-1$
    
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
		startLabel.setText(Messages.getString("StartIp.label"));
		endLabel.setText(Messages.getString("EndIp.label"));
		okBtn.setText(Messages.getString("OkBtn"));
		cancelBtn.setText(Messages.getString("CancelBtn"));
		
		startIp.textProperty().addListener(showInfo);
		endIp.textProperty().addListener(showInfo);
		
		String localIp = Util.getLoaclIp();
		if(Util.ipIsValid(localIp)) {
			String net = localIp.substring(0, localIp.lastIndexOf(".") + 1); //$NON-NLS-1$
			String start = net + "1"; //$NON-NLS-1$
			String end = net + "254"; //$NON-NLS-1$
			startIp.setText(start);
			endIp.setText(end);
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
				String ip1 = startIp.getText().trim();
				String ip2 = endIp.getText().trim();
				
				if(!Util.ipIsValid(ip1) 
						|| !Util.ipIsValid(ip2)) {
					showErr(Messages.getString("ScanController.ip.invalid")); //$NON-NLS-1$
					return;
				}
				
				String net1 = Util.getIpNet(ip1);
				String net2 = Util.getIpNet(ip2);
				if(!net1.equals(net2)) {
					showErr(Messages.getString("ScanController.net.notsame")); //$NON-NLS-1$
					return;
				}
				
				int start = Util.getIpHost(ip1);
				int end = Util.getIpHost(ip2);
				if(start > end) {
					showErr(Messages.getString("ScanController.ip.err")); //$NON-NLS-1$
					return;
				}
				
				mainApp.getMainContr().scanHosts(net1, start, end);
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
}
