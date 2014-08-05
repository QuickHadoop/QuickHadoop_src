package distrib.hadoop.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import distrib.hadoop.cluster.Cluster;
import distrib.hadoop.cluster.Hadoop;
import distrib.hadoop.cluster.HadoopV2;
import distrib.hadoop.host.Host;

/**
 * 表格工具类
 */
@SuppressWarnings("rawtypes")
public class TableFactory {
	/** 表格对象 */
	private TableView tableView; 
	
	private static TableFactory instance;
	
	private TableFactory() {
	}
	
	public static TableFactory getInstance(TableView tableView) {
		if(instance == null) {
			instance = new TableFactory();
		}
		instance.setTableView(tableView);
		return instance;
	}
	
	@SuppressWarnings({ "unchecked" })
	public void fill() {
		Cluster cluster = Cluster.getInstance();
		Hadoop hadoop = cluster.getHadoop();
		List<Host> hosts = Cluster.getInstance().getHostList();
		for(Host h : hosts) {
			boolean isDataNode = h.getHostName().startsWith(Cluster.DN_NAME);
			h.isDataNodeProperty().setValue(isDataNode);
			h.isNameNodeProperty().setValue(!isDataNode);
			h.isJournalNodeProperty().setValue(hadoop instanceof HadoopV2);
			h.isZookeeperProperty().setValue(cluster.isSupportZookeeper());
			h.isHBaseProperty().setValue(cluster.isSupportHbase());
		}
		
		ObservableList<Host> data = FXCollections.observableArrayList(hosts);

		tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		tableView.getColumns().clear();
		
		TableColumn hostCol = new TableColumn();
		hostCol.setText("Host");
		hostCol.setPrefWidth(55);
		hostCol.setCellValueFactory(new PropertyValueFactory("ip"));
		hostCol.setSortable(false);

		List<TableColumn> cols = new ArrayList<TableColumn>();
		cols.add(hostCol);
		cols.add(createCheckBoxCell("NameNode", "isNameNode", 40));
		cols.add(createCheckBoxCell("DataNode", "isDataNode", 40));
		if(hadoop instanceof HadoopV2) {
			cols.add(createCheckBoxCell("JournalNode", "isJournalNode", 50));			
		}
		if(cluster.isSupportZookeeper()) {
			cols.add(createCheckBoxCell("Zookeeper", "isZookeeper", 40));			
		}
		if(cluster.isSupportHbase()) {
			cols.add(createCheckBoxCell("HBase", "isHBase", 10));			
		}

		tableView.setItems(data);
		tableView.getColumns().addAll(cols);
	}

	@SuppressWarnings({ "unchecked" })
	private TableColumn<Host, Boolean> createCheckBoxCell(String txt,
			String prop, int prefWidth) {
		TableColumn<Host, Boolean> col = new TableColumn<Host, Boolean>();
		col.setText(txt);
		col.setPrefWidth(prefWidth);
		col.setCellValueFactory(new PropertyValueFactory(prop));
		col.setSortable(false);
		
		final boolean disable = txt.equals("NameNode") || txt.equals("DataNode");
		col.setCellFactory(new Callback<TableColumn<Host, Boolean>, 
				TableCell<Host, Boolean>>() {
			public TableCell<Host, Boolean> call(TableColumn<Host, Boolean> p) {
				
				return new CheckBoxTableCell<Host, Boolean>(disable);
			}
		});
		return col;
	}

	public static class CheckBoxTableCell<S, T> extends TableCell<S, T> {
		private final CheckBox checkBox;
		private ObservableValue<T> ov;

		public CheckBoxTableCell(boolean disable) {
			this.checkBox = new CheckBox();
			this.checkBox.setAlignment(Pos.CENTER);
			this.checkBox.setDisable(disable);

			setAlignment(Pos.CENTER);
			setGraphic(checkBox);
		}

		@Override
		public void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				setGraphic(checkBox);
				if (ov instanceof BooleanProperty) {
					checkBox.selectedProperty().unbindBidirectional(
							(BooleanProperty) ov);
				}
				ov = getTableColumn().getCellObservableValue(getIndex());
				if (ov instanceof BooleanProperty) {
					checkBox.selectedProperty().bindBidirectional(
							(BooleanProperty) ov);
				}
			}
		}
	}

	public TableView getTableView() {
		return tableView;
	}

	public void setTableView(TableView tableView) {
		this.tableView = tableView;
	}
}
