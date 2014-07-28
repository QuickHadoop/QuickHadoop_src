package distrib.hadoop.ui;

import java.net.URL;
import java.util.ResourceBundle;

import distrib.hadoop.resource.Messages;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class InfoDlgController extends AnchorPane implements Initializable {

	private Stage stage;
	
	@FXML
	private Label infoLabel;
	
	@FXML
	private Button okBtn;
	
    private double mouseDragOffsetX = 0;
    private double mouseDragOffsetY = 0;

    /** 对话框返回值 */
	private int ret = CANCEL;
	
	public static int OK = 1;
	public static int CANCEL = 0;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		okBtn.setText(Messages.getString("OkBtn"));
		
		okBtn.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				stage.close();
				ret = OK;
			}
		});

		okBtn.requestFocus();
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
	public void setInfo(String info) {
		infoLabel.setText(info);
	}
	
	public void close() {
		stage.close();
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

	public int getRet() {
		return ret;
	}
}
