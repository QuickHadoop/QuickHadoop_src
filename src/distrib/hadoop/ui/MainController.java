package distrib.hadoop.ui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import distrib.hadoop.cluster.Cluster;
import distrib.hadoop.exception.AuthException;
import distrib.hadoop.exception.InstallException;
import distrib.hadoop.host.Host;
import distrib.hadoop.resource.Messages;
import distrib.hadoop.thread.ThreadPool;
import distrib.hadoop.util.Util;

/**
 * �������������
 * 
 * @author guolin
 *
 */
public class MainController extends AnchorPane implements Initializable {
    
    @FXML
    private Button scanButton;

    @FXML
    private Button createButton;

    @FXML
    private Button maintainButton;
    
    @FXML
    private Button configButton;
     
    @FXML
    private Button deleteButton;
    
    @FXML
    private Button startButton;
    
    @FXML
    private Button stopButton;

    @FXML
    private TilePane hostsPane;

    @FXML
    private VBox nameNodePane;
    
    @FXML
    private VBox secNameNodePane;
    
    @FXML
    private VBox dataNodesPane;
    
    @FXML
    private TilePane slavesPane;
    
    @FXML
    private AnchorPane rightPane;

	/** 主程序对象 */
    private MainApp mainApp;

    /** 所属对话框 */
    private Stage stage;
    
	/** 是否最大化 */
    private boolean maximized = false;
    
    /** 背景 */
    private Rectangle2D backupWindowBounds = null;
    
    /** 拖动位移 */
    private double mouseDragOffsetX = 0;
    private double mouseDragOffsetY = 0;
    
    private Image hostImg = new Image(MainController.class.getResourceAsStream("images/host.png")); //$NON-NLS-1$
    
    private ImageLabel nameNodeImg;
    private ImageLabel secNameNodeImg;
    
    /** 右键菜单 */
    private ContextMenu menu;
    
    
    /** 扫描到的主机列表 */
    private ObservableList<String> hostList = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	configButton.setDisable(true);
    	deleteButton.setDisable(true);
    	startButton.setDisable(true);
    	stopButton.setDisable(true);
    	
    	menu = new ContextMenu();
    }
    
    /**
     * 刷新集群视图
     * 
     * @param created	是否已经创建集群
     */
    public void refreshClusterUI(boolean created) {
    	Cluster cluster = Cluster.getInstance();
    	
    	nameNodeImg.setOpacity(0.1);
    	nameNodeImg.setText(""); //$NON-NLS-1$
    	secNameNodeImg.setOpacity(0.1);
    	secNameNodeImg.setText(""); //$NON-NLS-1$
    	
    	if(cluster.getNameNode() != null) {
			nameNodeImg.setText(cluster.getNameNode().getIp());
			nameNodeImg.setOpacity(1);
    	}

    	if(cluster.getSecNameNode() != null) {
    		secNameNodeImg.setText(cluster.getSecNameNode().getIp());
    		secNameNodeImg.setOpacity(1);
    	}
    	
    	slavesPane.getChildren().clear();
    	if(cluster.getDataNodeList() != null) {
    		for(Host h : cluster.getDataNodeList()) {
    			ImageLabel imgLabel = new ImageLabel(h.getIp(), new ImageView(hostImg));
    			imgLabel.addDragEvent();
    			slavesPane.getChildren().add(imgLabel);
    		}    		
    	}
    	
		if(created) {
    		createButton.setDisable(true);
    		configButton.setDisable(false);
    		deleteButton.setDisable(false);
    		startButton.setDisable(false);
    		stopButton.setDisable(false);
    	} else {
    		createButton.setDisable(false);
    		configButton.setDisable(true);
    		deleteButton.setDisable(true);
    		startButton.setDisable(true);
    		stopButton.setDisable(true);
    	}
    }
    
    public void setInputMainView(){
    	nameNodeImg = new ImageLabel("", new ImageView(hostImg)); //$NON-NLS-1$
    	nameNodeImg.setOpacity(0.1);
    	nameNodeImg.addDragEvent();
    	nameNodePane.getChildren().add(nameNodeImg);
    	
    	secNameNodeImg = new ImageLabel("", new ImageView(hostImg)); //$NON-NLS-1$
    	secNameNodeImg.setOpacity(0.1);
    	secNameNodeImg.addDragEvent();
    	secNameNodePane.getChildren().add(secNameNodeImg);
    	
    	nameNodePane.setOnMousePressed(new EventHandler<MouseEvent>() {
    		@Override
    		public void handle(MouseEvent e) {
    			if(!canEditCluster()) {
    				return;
    			}
    			if(e.isSecondaryButtonDown()) {
    				menu.getItems().clear();
    		    	MenuItem item1 = new MenuItem(Messages.getString("MainController.add")); //$NON-NLS-1$
    		    	item1.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							if(nameNodeImg.getOpacity() < 1) {
								mainApp.showAddHostDialog(Cluster.NN_NAME);
							}
						}
					});
    		    	
    		    	MenuItem item2 = new MenuItem(Messages.getString("MainController.del")); //$NON-NLS-1$
    		    	item2.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							if(nameNodeImg.getOpacity() == 1) {
								ImageLabel.setDragingImg(nameNodeImg);
								ImageLabel.droppedTo(hostsPane);
						    	nameNodeImg.setOpacity(0.1);
						    	nameNodeImg.setText(""); //$NON-NLS-1$
							}
						}
					});
    		    	if(nameNodeImg.getOpacity() < 1) {
    		    		menu.getItems().add(item1);    		    		
    		    	} else {
    		    		menu.getItems().add(item2);    		    		
    		    	}
    				menu.show(nameNodePane, e.getScreenX(), e.getScreenY());
    			} else {
    				menu.hide();
    			}
    		}
    	});
    	secNameNodePane.setOnMousePressed(new EventHandler<MouseEvent>() {
    		@Override
    		public void handle(MouseEvent e) {
    			if(!canEditCluster()) {
    				return;
    			}
    			if(e.isSecondaryButtonDown()) {
    				menu.getItems().clear();
    		    	MenuItem item1 = new MenuItem(Messages.getString("MainController.add.option")); //$NON-NLS-1$
    		    	item1.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							if(secNameNodeImg.getOpacity() < 1) {
								mainApp.showAddHostDialog(Cluster.SN_NAME);
							}
						}
					});
    		    	MenuItem item2 = new MenuItem(Messages.getString("MainController.del")); //$NON-NLS-1$
    		    	item2.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							if(secNameNodeImg.getOpacity() == 1) {
								ImageLabel.setDragingImg(secNameNodeImg);
								ImageLabel.droppedTo(hostsPane);
								secNameNodeImg.setOpacity(0.1);
								secNameNodeImg.setText(""); //$NON-NLS-1$
							}
						}
					});
    		    	if(secNameNodeImg.getOpacity() < 1) {
    		    		menu.getItems().add(item1);    		    		
    		    	} else {
    		    		menu.getItems().add(item2);    		    		
    		    	}
    				menu.show(nameNodePane, e.getScreenX(), e.getScreenY());
    			} else {
    				menu.hide();
    			}
    		}
    	});
    	dataNodesPane.setOnMousePressed(new EventHandler<MouseEvent>() {
    		@Override
    		public void handle(MouseEvent e) {
    			if(!canEditCluster()) {
    				return;
    			}
    			if(e.isSecondaryButtonDown()) {
    				menu.getItems().clear();
    		    	MenuItem item1 = new MenuItem(Messages.getString("MainController.add")); //$NON-NLS-1$
    		    	item1.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							mainApp.showAddHostDialog(Cluster.DN_NAME);
						}
					});
    		    	MenuItem item2 = new MenuItem(Messages.getString("MainController.add.net")); //$NON-NLS-1$
    		    	item2.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							mainApp.showAddHostsDialog();
						}
					});
    		    	MenuItem item3 = new MenuItem(Messages.getString("MainController.del.all")); //$NON-NLS-1$
    		    	item3.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							for(Node child : slavesPane.getChildren()) {
								if(child instanceof ImageLabel) {
									ImageLabel.setDragingImg((ImageLabel) child);
									ImageLabel.droppedTo(hostsPane);
								}
							}
							slavesPane.getChildren().clear();
						}
					});
    		    	menu.getItems().add(item1);
    		    	menu.getItems().add(item2);
    		    	menu.getItems().add(item3);
    				menu.show(nameNodePane, e.getScreenX(), e.getScreenY());
    			} else {
    				menu.hide();
    			}
    		}
    	});
    	
    	nameNodePane.setOnDragEntered(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(ImageLabel.getDragingImg() != null && nameNodeImg.getOpacity() < 1) {
					nameNodeImg.setText(ImageLabel.getDragingImg().getText());
				}
				ImageLabel.dragEnter(nameNodePane);
			}
		});
        
    	nameNodePane.setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(!nameNodeImg.equals(ImageLabel.getDragingImg()) && nameNodeImg.getOpacity() < 1) {
					nameNodeImg.setText(""); //$NON-NLS-1$
				}
				ImageLabel.dragExited(nameNodePane, 0.1);
			}
		});

    	nameNodePane.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(ImageLabel.getDragingImg() != null && nameNodeImg.getOpacity() < 1) {
					nameNodeImg.setText(ImageLabel.getDragingImg().getText());
				}
				ImageLabel.droppedTo(nameNodePane);
				event.consume();
			}
		});
        
    	nameNodePane.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(canEditCluster()) {
					event.acceptTransferModes(TransferMode.MOVE);					
				}
			}
		});
        
    	secNameNodePane.setOnDragEntered(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(ImageLabel.getDragingImg() != null && secNameNodeImg.getOpacity() < 1) {
					secNameNodeImg.setText(ImageLabel.getDragingImg().getText());
				}
				ImageLabel.dragEnter(secNameNodePane);
			}
		});
        
    	secNameNodePane.setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(!secNameNodeImg.equals(ImageLabel.getDragingImg()) && secNameNodeImg.getOpacity() < 1) {
					secNameNodeImg.setText(""); //$NON-NLS-1$
				}
				ImageLabel.dragExited(secNameNodePane, 0.1);
			}
		});
        
    	secNameNodePane.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(ImageLabel.getDragingImg() != null && secNameNodeImg.getOpacity() < 1) {
					secNameNodeImg.setText(ImageLabel.getDragingImg().getText());
				}
				ImageLabel.droppedTo(secNameNodePane);
				event.consume();
			}
		});
    	
    	secNameNodePane.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(canEditCluster()) {
					event.acceptTransferModes(TransferMode.MOVE);					
				}
			}
		});
    	
        hostsPane.setOnDragEntered(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				ImageLabel.dragEnter(hostsPane);
			}
		});
        
        hostsPane.setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				ImageLabel.dragExited(hostsPane, 0);
			}
		});
        
        hostsPane.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				ImageLabel.droppedTo(hostsPane);
				event.consume();
			}
		});
        
        hostsPane.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(canEditCluster()) {
					event.acceptTransferModes(TransferMode.MOVE);					
				}
			}
		});
        
        rightPane.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if(canEditCluster()) {
					event.acceptTransferModes(TransferMode.MOVE);					
				}
			}
		});
        
        slavesPane.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				ImageLabel dragImg = ImageLabel.getDragingImg();
				ImageLabel target = ImageLabel.getTarget(slavesPane);
				if(target == null && dragImg != null) {
					newDataNodeImg(dragImg.getText());
		        	dragImg.setMovePlace(true);
				} 
			}
		});
        
        dataNodesPane.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				dataNodesPane.setStyle("-fx-background-color: #808080"); //$NON-NLS-1$
			}
		});
        
        dataNodesPane.setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				dataNodesPane.setStyle("-fx-background-color: #dcdcdc"); //$NON-NLS-1$
			}
		});
        
        scanButton.setText(Messages.getString("Action.scan.Hosts"));
        createButton.setText(Messages.getString("Action.create.cluster"));
        deleteButton.setText(Messages.getString("Action.delete.cluster"));
        maintainButton.setText(Messages.getString("Action.login.cluster"));
        configButton.setText(Messages.getString("Action.upload.file"));
        startButton.setText(Messages.getString("Action.start.all"));
        stopButton.setText(Messages.getString("Action.stop.all"));
        
    	scanButton.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				mainApp.showScanDialog();
			}
		});
    	
    	createButton.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				if(nameNodeImg.getOpacity() < 1) {
					mainApp.showInfoDialog(Messages.getString("MainController.drag.first")); //$NON-NLS-1$
				} else {
					mainApp.showInstallWizard();					
				}
			}
		});
    	
    	deleteButton.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				final Cluster cluster = Cluster.getInstance();
				mainApp.showInfoDialog(Messages.getString("MainController.wait.delete")); //$NON-NLS-1$
				
		    	new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							cluster.login();
							cluster.restore();
							cluster.logout();
							
					    	for(Host h : cluster.getHostList()) {
					    		String ip = h.getIp();
					    		if(ip != null && !hostList.contains(ip)) {
					    			hostList.add(ip);
					    		}
					    	}
					    	
					    	cluster.getHostList().clear();
							Platform.runLater(new Runnable() {
							    @Override
							    public void run() {
							    	refreshClusterUI(false);
							    	mainApp.getInfoContr().close();
							    }
							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		});
    	
    	maintainButton.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				mainApp.showLoginDialog();
			}
    	});
    	
    	configButton.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				mainApp.showConfigDialog();
			}
    	});
    	
    	startButton.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				mainApp.showInfoDialog(Messages.getString("MainController.start.all"));
		    	new Thread(new Runnable() {
					@Override
					public void run() {
						startCluster();
						closeInfoDlg();
					}

				}).start();
			}
		});
    	
    	stopButton.setOnMouseClicked(new EventHandler<Event>() {
    		@Override
    		public void handle(Event arg0) {
				mainApp.showInfoDialog(Messages.getString("MainController.stop.all"));
		    	new Thread(new Runnable() {
					@Override
					public void run() {
						stopCluster();
						closeInfoDlg();
					}

				}).start();
    		}
    	});
    	
    	hostList.addListener(hostChange);
    }

	/**
	 * 关闭消息对话框
	 */
	private void closeInfoDlg() {
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
		    	mainApp.getInfoContr().close();
		    }
		});
	}
	
	/**
	 * 启动集群所有进程
	 * 
	 * @throws InstallException
	 * @throws AuthException
	 * @throws IOException
	 */
	private synchronized void startCluster() {
		try {
			Cluster cluster = Cluster.getInstance();
			cluster.login();
			cluster.startAll();
			cluster.logout();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * 停止集群所有进程
	 * 
	 * @throws InstallException
	 * @throws AuthException
	 * @throws IOException
	 */
	private synchronized void stopCluster() {
		try {
			Cluster cluster = Cluster.getInstance();
			cluster.login();
			cluster.stopAll();
			cluster.logout();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 是否可添加、删除主机到集群中
	 * 
	 * @return
	 */
	private boolean canEditCluster() {
		return Cluster.getInstance().getHostList().size() == 0;
	}
	
    /**
     * 扫描主机监听事件
     */
    ListChangeListener<String> hostChange = new ListChangeListener<String>() {
		@Override
		public void onChanged(Change<? extends String> c) {
			while(c.next()) {
				if(c.wasAdded()) {
					synchronized (hostList) {
						for(String h : c.getAddedSubList()) {
							newHost(h);
						}						
					}
				} else if(c.wasRemoved()) {
					synchronized (hostList) {
						for(String h : c.getRemoved()) {
							hostsPane.getChildren().remove(getHostImg(h));
						}						
					}
				}
			}
		}
	};
    
	/**
	 * 扫描局域网内的主机设备
	 */
	public void scanLocalHosts() {
		String localIp = Util.getLoaclIp();
		if(Util.ipIsValid(localIp)) {
			String net = Util.getIpNet(localIp);
			scanHosts(net, 1, 254);
		}
	}
	
	/**
	 * 扫描网段内的主机设备
	 * 
	 * @param net
	 * @param start
	 * @param end
	 */
	public void scanHosts(String net, int start, int end) {
		hostList.clear();
		
		List<Runnable> runList = new ArrayList<Runnable>();
		for(int i = start; i <= end; i++) {
			final String ip = net + i;
			runList.add(new Runnable() {
				@Override
				public void run() {
					if(Util.ping(ip, 2000)) {
						synchronized (hostList) {
							hostList.add(ip);							
						}
					}
				}
			});
		}
		
		ThreadPool.Execute(runList, false);
	}
    /**
     * 根据IP地址得到主机图片
     * 
     * @param ip
     * @return
     */
    private ImageLabel getHostImg(String ip) {
    	if(ip == null || ip.isEmpty()) {
    		return null;
    	}
    	
    	ImageLabel result = null;
		for(Node node : hostsPane.getChildren()) {
			if(!(node instanceof ImageLabel)) {
				continue;
			}
			
			result = (ImageLabel)node; 
			if(ip.equals(result.getText())) {
				return result;
			}
		}
		
		return null;
    }
    
    /**
     * 集群主机列表
     */
    public void setUpClustre(String usrName, String passwd) {
    	Cluster cluster = Cluster.getInstance();
		List<Host> list = cluster.getHostList();
		list.clear();
		
    	String nnIp = nameNodeImg.getText();
    	if(Util.ipIsValid(nnIp)) {
    		list.add(new Host(Cluster.NN_NAME, nnIp, usrName, passwd));    		 //$NON-NLS-1$
    	}
    	
    	String snIp = secNameNodeImg.getText();
    	if(Util.ipIsValid(snIp)) {
    		list.add(new Host(Cluster.SN_NAME, snIp, usrName, passwd));    		 //$NON-NLS-1$
    	}
    	
    	String dnIp;
    	int index = 1;
		for(Node child : slavesPane.getChildren()) {
			if(child instanceof ImageLabel) {
				ImageLabel slaveLabel = (ImageLabel)child;
				dnIp = slaveLabel.getText();
				if(Util.ipIsValid(dnIp)) {
					list.add(new Host(Cluster.DN_NAME + index, dnIp, usrName, passwd)); //$NON-NLS-1$
					index++;
				}
			}
		}
		
		cluster.prepareWork();
    }
    
	/**
	 * 在主机视图中创建一个Host节点
	 * 
	 * @param ip
	 */
	private void newHost(final String ip) {
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
		    	ImageLabel imgLabel = new ImageLabel(ip, new ImageView(hostImg));
		    	imgLabel.addDragEvent();	
		    	imgLabel.setNeedHide(true);
		    	hostsPane.getChildren().add(imgLabel);
		    }
		});
	}
	
	@FXML
    public void mainTreeClicked(MouseEvent event){

    }
    /**
     * ����رհ�ť
     * 
     * @param event
     */
    @FXML
    public void windowClosePressed(ActionEvent event) {
    	System.exit(0);
    }
    
    
    
    /**
     * �����С����ť
     * 
     * @param event
     */
    @FXML
    public void windowMinPressed(ActionEvent event) {
    	stage.setIconified(true);
    }
    
    /**
     * �����󻯰�ť
     * 
     * @param event
     */
    @FXML
    public void windowMaxPressed(ActionEvent event) {
    	toogleMaximized();
    }
    
    /**
     * ˫��top����¼�
     * 
     * @param event
     */
    @FXML
    public void windowClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            toogleMaximized();
        }
    }
    
    /**
     * ���top����¼�
     * 
     * @param event
     */
    @FXML
    public void windowPressed(MouseEvent event) {
        mouseDragOffsetX = event.getSceneX();
        mouseDragOffsetY = event.getSceneY();
    }
    
    /**
     * ��קtop����¼�
     * 
     * @param event
     */
    @FXML
    public void windowDragged(MouseEvent event) {
        if(!maximized) {
            stage.setX(event.getScreenX()-mouseDragOffsetX);
            stage.setY(event.getScreenY()-mouseDragOffsetY);
        }
    }
    
    /**
     * ������󻯻�ָ����ڴ�С
     */
    public void toogleMaximized() {
        final Screen screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1).get(0);
        if (maximized) {
            maximized = false;
            if (backupWindowBounds != null) {
                stage.setX(backupWindowBounds.getMinX());
                stage.setY(backupWindowBounds.getMinY());
                stage.setWidth(backupWindowBounds.getWidth());
                stage.setHeight(backupWindowBounds.getHeight());
            }
        } else {
            maximized = true;
            backupWindowBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            stage.setX(screen.getVisualBounds().getMinX());
            stage.setY(screen.getVisualBounds().getMinY());
            stage.setWidth(screen.getVisualBounds().getWidth());
            stage.setHeight(screen.getVisualBounds().getHeight());
		}
    }
    
    /**
     * 添加DataNode节点
     */
    public void addDataNode(String ip) {
    	if(ip == null || ip.isEmpty()) {
    		return;
    	}
    	
    	ImageLabel target = ImageLabel.getTarget(slavesPane, ip);
    	if(target == null && !ip.equals(nameNodeImg.getText())
    			&& !ip.equals(secNameNodeImg.getText())) {
    		newDataNodeImg(ip);
    	} 
    	
    	ImageLabel host = getHostImg(ip);
    	if(host != null) {
    		host.setOpacity(0);
    		host.setDisable(true);
    	}
    }

	/**
	 * 添加一个DataNode主机到slavesPane
	 * 
	 * @param ip
	 */
	private void newDataNodeImg(String ip) {
		final ImageLabel imgLabel = new ImageLabel(ip, new ImageView(hostImg));
		imgLabel.setNeedRemove(true);
		imgLabel.addDragEvent();
		imgLabel.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if(e.isSecondaryButtonDown()) {
					menu.getItems().clear();
			    	MenuItem item = new MenuItem(Messages.getString("MainController.del")); //$NON-NLS-1$
			    	item.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							ImageLabel.setDragingImg(imgLabel);
							ImageLabel.droppedTo(hostsPane);
							slavesPane.getChildren().remove(imgLabel);
						}
					});
			    	menu.getItems().add(item);
			    	menu.show(imgLabel, e.getScreenX(), e.getScreenY());
			    	e.consume();
				}
			}
		});
		slavesPane.getChildren().add(imgLabel);
	}
    
    /**
     * 添加SecNameNode节点
     */
    public void addSecNameNode(String ip) {
    	if(ip == null || ip.isEmpty()) {
    		return;
    	}
    	
    	secNameNodeImg.setText(ip);
    	secNameNodeImg.setOpacity(1);
    	
    	ImageLabel host = getHostImg(ip);
    	if(host != null) {	
        	host.setOpacity(0);
        	host.setDisable(true);	
    	}
    }
    
    /**
     * 添加NameNode节点
     */
    public void addNameNode(String ip) {
    	if(ip == null || ip.isEmpty()) {
    		return;
    	}
    	
    	nameNodeImg.setText(ip);
    	nameNodeImg.setOpacity(1);
    	
    	ImageLabel host = getHostImg(ip);
    	if(host != null) {
        	host.setOpacity(0);
        	host.setDisable(true);	
    	}
    }
    
    /**
     * 设置主程序
     * 
     * @param stage
     */
    public void setApp(MainApp mainApp){
        this.mainApp = mainApp;
    }
    
    public void setStage(Stage stage) {
    	this.stage = stage;
    }

	/**
	 * @return the hostList
	 */
	public ObservableList<String> getHostList() {
		return hostList;
	}
}
