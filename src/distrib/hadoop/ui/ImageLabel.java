package distrib.hadoop.ui;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * 带图标的文字标签
 */
public class ImageLabel extends Label {

	private boolean movePlace;
	private boolean needRemove;
	private boolean needHide;
	
	/** 全局成员，当前界面正在拖动的图片 */
	private static ImageLabel dragingImg;
	
	/** 字体颜色 */
	private Color textColor = new Color(0.3, 0.3, 0.5, 1);
	
	public ImageLabel(String label, ImageView img) {
		super(label, img);
		setContentDisplay(ContentDisplay.TOP);
		setTextFill(textColor);
	}

	/**
	 * 添加拖拽事件
	 */
	public void addDragEvent() {
		setOnDragDetected(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent event) {
				if(getOpacity() < 1) {
					dragingImg = null;
					return;
				}
				dragingImg = ImageLabel.this;
				setMovePlace(false);
				Dragboard db = startDragAndDrop(TransferMode.ANY);
				ClipboardContent content = new ClipboardContent();
				content.putString(getText());
				db.setContent(content);
				setOpacity(0.2);
				event.consume();
			}
		});
        
		setOnDragDone(new EventHandler<DragEvent>() {
			public void handle(DragEvent event) {
				if(!movePlace) {
					setOpacity(1);
				} else {
					if(needHide) {
						setOpacity(0);
						setDisable(true);						
					} else {
						setText("");
					}
					
					if(needRemove) {
						removeSelf();
					}
				}
				
				dragingImg = null;
			}
		});
	}
	
	/**
	 * 从父面板中删除
	 */
	private void removeSelf() {
		((Pane)getParent()).getChildren().remove(this);
	}
	
	/**
	 * 获取拖动目标位置
	 * 
	 * @param pane
	 * @return
	 */
	public static ImageLabel getTarget(Pane pane) {
		if(pane == null || dragingImg == null) {
			return null;
		}
		
		return getTarget(pane, dragingImg.getText());
	}
	
	/**
	 * 获取拖动目标位置
	 * 
	 * @param pane
	 * @return
	 */
	public static ImageLabel getTarget(Pane pane, String ip) {
		if(pane == null || ip == null) {
			return null;
		}
		
		for (Node child : pane.getChildren()) {
			if(!(child instanceof ImageLabel)) {
				continue;
			}
			
			ImageLabel childImg = (ImageLabel)child;
			if(ip.equals(childImg.getText())) {
				return childImg;
			}
		}
		
		return null;
	}
	
	/**
	 * 拖到到某个面板
	 * 
	 * @param pane
	 */
	public static void droppedTo(Pane pane) {
		if(dragingImg == null) {
			return;
		}
		
		ImageLabel target = getTarget(pane);
		if(target == null || dragingImg.equals(target)) {
			return;
		}
		
		if(target.getOpacity() < 1) {
			dragingImg.setMovePlace(true);
			target.setDisable(false);
			target.setOpacity(1);
		}
	}
	
	/**
	 * 拖拽经过面板区域
	 * 
	 * @param pane
	 */
	public static void dragEnter(Pane pane) {
		if(dragingImg == null) {
			return;
		}
		
		ImageLabel target = getTarget(pane);
		if(target == null || dragingImg.equals(target)) {
			return;
		}
		
		if(target.getOpacity() < 1) {
			target.setDisable(false);
			target.setOpacity(0.2);
		}
	}
	
	/**
	 * 拖拽不经过面板区域
	 * 
	 * @param pane
	 */
	public static void dragExited(Pane pane, double opacity) {
		if(dragingImg == null) {
			return;
		}
		
		ImageLabel target = getTarget(pane);
		if(target == null || dragingImg.equals(target)) {
			return;
		}
		
		if(target.getOpacity() < 1) {
			target.setDisable(true);
			target.setOpacity(opacity);
		}
	}
	
	public boolean isMovePlace() {
		return movePlace;
	}
	
	public boolean isNeedRemove() {
		return needRemove;
	}

	public void setNeedRemove(boolean needRemove) {
		this.needRemove = needRemove;
	}
	
	public boolean isNeedHide() {
		return needHide;
	}

	public void setNeedHide(boolean needHide) {
		this.needHide = needHide;
	}

	public void setMovePlace(boolean movePlace) {
		this.movePlace = movePlace;
	}

	public static ImageLabel getDragingImg() {
		return dragingImg;
	}

	public static void setDragingImg(ImageLabel dragingImg) {
		ImageLabel.dragingImg = dragingImg;
	}
}
