package pers.cmeu.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import pers.cmeu.common.DBUtil;
import pers.cmeu.common.StrUtil;
import pers.cmeu.models.AttributeCVF;
import pers.cmeu.models.ColumnItem;
import pers.cmeu.models.SuperAttribute;
import pers.cmeu.view.AlertUtil;

public class AddGrandAttributeController extends BaseController {

	// 存储信息table里面的所有属性
	ObservableList<AttributeCVF> attributeCVF;

	@FXML
	private CheckBox chkUnlineCamel;
	@FXML
	private CheckBox chkSerializable;
	@FXML
	private CheckBox chkCreateJDBCtype;
	@FXML
	private CheckBox chkGetAndSet;
	@FXML
	private CheckBox chkConstruct;
	@FXML
	private CheckBox chkConstructAll;

	@FXML
	private CheckBox chkCreateEntity;
	@FXML
	private CheckBox chkCreateDao;
	@FXML
	private CheckBox chkCreateMap;
	@FXML
	private CheckBox chkCreateService;
	@FXML
	private CheckBox chkCreateServiceImpl;
	@FXML
	private CheckBox chkCreateAll;

	@FXML
	private TextField txtPrimaryKey;
	@FXML
	private TextField txtCustomType;
	@FXML
	private TextField txtCustomName;
	@FXML
	private TextField txtTableName;
	@FXML
	private TextField txtClassName;
	@FXML
	private TextField txtDaoName;
	@FXML
	private TextField txtMapperName;
	@FXML
	private TextField txtServiceName;
	@FXML
	private TextField txtServiceImplName;
	@FXML
	private TextField txtJoinTableName;
	@FXML
	private TextField txtJoinColumnName;

	@FXML
	private Button btnSuccess;
	@FXML
	private Button btnCancel;
	@FXML
	private Button btnAddToTableView;
	@FXML
	private TableView<AttributeCVF> tblEntityProperty;
	@FXML
	private TableColumn<AttributeCVF, Boolean> tdCheck;
	@FXML
	private TableColumn<AttributeCVF, String> tdColumn;
	@FXML
	private TableColumn<AttributeCVF, String> tdJDBCType;
	@FXML
	private TableColumn<AttributeCVF, String> tdJAVAType;
	@FXML
	private TableColumn<AttributeCVF, String> tdPropertyName;

	// 主键策略
	@FXML
	private TextArea txtaSelectKey;
	@FXML
	private Label lblSelectKey;
	@FXML
	private CheckBox chkSelectKey;

	@FXML
	private ListView<Label> lvTableList;

	@FXML
	private ToggleGroup joinType;
	@FXML
	private RadioButton radioInner;
	@FXML
	private RadioButton radioLeft;
	@FXML
	private RadioButton radioRight;
	@FXML
	private RadioButton radioWhere;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		tblEntityProperty.setEditable(true);
		tblEntityProperty.setStyle("-fx-font-size:14px");
		tblEntityProperty.setPlaceholder(new Label("双击左边表名数据加载..."));
		initListItem();
		radioInner.setUserData("inner");
		radioLeft.setUserData("left");
		radioRight.setUserData("right");
		radioWhere.setUserData("where");
	}

	/**
	 * 加载左边list所有表名
	 */
	public void initListItem() {
		try {
			List<String> tableNames = DBUtil.getTableNames(
					((IndexController) StageManager.CONTROLLER.get("index")).getSelectedDatabaseConfig());

			for (String str : tableNames) {
				ImageView imageView = new ImageView("pers/resource/image/table.png");
				imageView.setFitHeight(18);
				imageView.setFitWidth(18);
				Label label = new Label(str);
				label.setGraphic(imageView);
				label.setUserData(str);
				label.setPrefWidth(lvTableList.getPrefWidth());
				label.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
					if (event.getClickCount() == 2) {
						// 双击显示表名与主键,同时加载到tableview
						try {
							String tableName = label.getUserData().toString();
							txtTableName.setText(tableName);
							txtClassName.setText(StrUtil.unlineToPascal(tableName));
							txtDaoName.setText(StrUtil.unlineToPascal(tableName) + "Dao");
							txtMapperName.setText(StrUtil.unlineToPascal(tableName) + "Mapper");
							txtServiceName.setText(StrUtil.unlineToPascal(tableName) + "Service");
							txtServiceImplName.setText(StrUtil.unlineToPascal(tableName) + "ServiceImpl");
							String primaryKey = DBUtil
									.getTablePrimaryKey(((IndexController) StageManager.CONTROLLER.get("index"))
											.getSelectedDatabaseConfig(), tableName);
							if (primaryKey == null || "".equals(primaryKey)) {
								txtPrimaryKey.setText(null);
								txtPrimaryKey.setPromptText("注意:该表没有主键!");
							} else {
								txtPrimaryKey.setText(primaryKey);
								String tmpTb=((AddSonAttributeController) StageManager.CONTROLLER
										.get("addPropertyBySon")).getTableName();
								String tmpKey = ((AddSonAttributeController) StageManager.CONTROLLER
										.get("addPropertyBySon")).getPrimaryKey();
								if (tmpTb!=null) {
									txtJoinTableName.setText(tmpTb);
								}
								if (tmpKey != null) {
									txtJoinColumnName.setText(tmpKey);
								}
							}
							if (tableName != null) {
								// 初始化表属性
								initTable();
							}

						} catch (Exception e) {
							AlertUtil.showErrorAlert("加载失败!失败原因:\r\n" + e.getMessage());
						}

					}
				});
				lvTableList.getItems().add(label);
			}

		} catch (Exception e) {
			AlertUtil.showErrorAlert("获得子表失败!原因:" + e.getMessage());
		}
	}

	/**
	 * 初始化右边的表
	 */
	public void initTable() {
		// 获得工厂数据
		attributeCVF = getAttributeCVFs();
		tdCheck.setCellFactory(CheckBoxTableCell.forTableColumn(tdCheck));
		tdCheck.setCellValueFactory(new PropertyValueFactory<>("check"));

		tdColumn.setCellValueFactory(new PropertyValueFactory<>("conlumn"));
		tdJDBCType.setCellValueFactory(new PropertyValueFactory<>("jdbcType"));

		tdJAVAType.setCellValueFactory(new PropertyValueFactory<>("javaType"));

		tdPropertyName.setCellValueFactory(new PropertyValueFactory<>("propertyName"));
		tdPropertyName.setCellFactory(TextFieldTableCell.forTableColumn());
		tdPropertyName.setOnEditCommit(event -> {
			event.getTableView().getItems().get(event.getTablePosition().getRow()).setPropertyName(event.getNewValue());
		});

		// 是否将字符驼峰命名;
		if (chkUnlineCamel.isSelected()) {
			toCamel();
		} else {
			notCamel();
		}
	}

	/**
	 * 获得数据库列并初始化
	 * 
	 * @return
	 */
	public ObservableList<AttributeCVF> getAttributeCVFs() {
		ObservableList<AttributeCVF> result = null;
		try {
			List<AttributeCVF> attributeCVFs = DBUtil.getTableColumns(
					((IndexController) StageManager.CONTROLLER.get("index")).getSelectedDatabaseConfig(),
					txtTableName.getText());
			result = FXCollections.observableList(attributeCVFs);
		} catch (Exception e) {
			AlertUtil.showErrorAlert("加载属性列失败!失败原因:\r\n" + e.getMessage());
		}

		return result;
	}

	/**
	 * 是否将java属性设置为驼峰命名
	 * 
	 * @param event
	 */
	public void unlineCamel(ActionEvent event) {
		if (chkUnlineCamel.isSelected()) {
			toCamel();
		} else {
			notCamel();
		}
	}

	/**
	 * 设置属性为帕斯卡
	 */
	public void toCamel() {
		tblEntityProperty.getItems().clear();
		for (AttributeCVF attr : attributeCVF) {
			attr.setPropertyName(StrUtil.unlineToCamel(attr.getPropertyName()));
			tblEntityProperty.getItems().add(attr);
		}
	}

	/**
	 * 设置属性名跟列名相同
	 */
	public void notCamel() {
		ObservableList<AttributeCVF> item = attributeCVF;
		tblEntityProperty.getItems().clear();
		for (AttributeCVF attr : item) {
			if (attr.getConlumn() == null || "".equals(attr.getConlumn())) {
				attr.setPropertyName(StrUtil.fristToLoCase(attr.getPropertyName()));
			} else {
				attr.setPropertyName(StrUtil.fristToLoCase(attr.getConlumn()));
			}
			tblEntityProperty.getItems().add(attr);
		}

	}
	

	/**
	 * 将属性添加到属性表
	 */
	public void addToTable(ActionEvent event) {
		AttributeCVF attribute = new AttributeCVF();
		attribute.setJavaType(txtCustomType.getText());
		attribute.setPropertyName(txtCustomName.getText());
		this.attributeCVF.add(attribute);
		tblEntityProperty.getItems().add(attribute);
	}

	/**
	 * 是否全部创建
	 * @param event
	 */
	public void anyCreateAll(ActionEvent event) {
		if (!chkCreateEntity.isSelected()||!chkCreateDao.isSelected()||!chkCreateMap.isSelected()||!chkCreateService.isSelected()||!chkCreateServiceImpl.isSelected()) {
			chkCreateAll.setSelected(true);
		}
		boolean value=chkCreateAll.isSelected();
		chkCreateEntity.setSelected(value);
		chkCreateDao.setSelected(value);
		chkCreateMap.setSelected(value);
		chkCreateService.setSelected(value);
		chkCreateServiceImpl.setSelected(value);
	}
	/**
	 * 生成主键策略
	 * 
	 * @param event
	 */
	public void selectKey(ActionEvent event) {
		if (txtPrimaryKey.getText() == null || "".equals(txtPrimaryKey.getText())) {
			AlertUtil.showWarnAlert("你尚未选择表或者你所选择的表没有主键");
			chkSelectKey.selectedProperty().set(false);
			return;
		}
		String keyType = "";
		for (AttributeCVF attr : tblEntityProperty.getItems()) {
			if (attr.getConlumn().equals(txtPrimaryKey.getText())) {
				keyType = attr.getJavaTypeValue();
				break;
			}
		}
		String dbType = ((IndexController) StageManager.CONTROLLER.get("index")).getSelectedDatabaseConfig()
				.getDbType();
		StringBuffer res = new StringBuffer();
		res.append("        <selectKey keyProperty=\""+txtPrimaryKey.getText()+"\" resultType=\""+keyType+"\" ");
		if ("MySQL".equals(dbType)) {
			res.append("order=\"AFTER\">\r\n            SELECT LAST_INSERT_ID() AS "+txtPrimaryKey.getText());
		}else if ("SqlServer".equals(dbType)) {
			res.append("order=\"AFTER\">\r\n            SELECT SCOPE_IDENTITY() AS "+txtPrimaryKey.getText());
		} else if ("PostgreSQL".equals(dbType)) {
			res.append("order=\"BEFORE\">\r\n            SELECT nextval() AS "+txtPrimaryKey.getText());
		}else {
			res.append("order=\"BEFORE\">\r\n            SELECT .Nextval FROM dual");
		}
		res.append("\r\n        </selectKey>");
		txtaSelectKey.setText(res.toString());
		lblSelectKey.setVisible(chkSelectKey.isSelected());
		txtaSelectKey.setVisible(chkSelectKey.isSelected());
	}

	/**
	 * 取消
	 * 
	 * @param event
	 */
	public void cancel(ActionEvent event) {
		StageManager.STAGE.get("addPropertyByGrand").close();
		StageManager.STAGE.remove("addPropertyByGrand");
	}

	/**
	 * 确定
	 * 
	 * @param event
	 */
	public void success(ActionEvent event) {
		if (txtTableName != null && !("".equals(txtTableName.getText()))) {
			//将属性添加到上一级
			AddSonAttributeController addSon=(AddSonAttributeController)StageManager.CONTROLLER.get("addPropertyBySon");
			if (chkCreateEntity.isSelected()) {
				//将信息存进首页等待创建
				IndexController index = (IndexController) StageManager.CONTROLLER.get("index");
				SuperAttribute attr = new SuperAttribute();
				attr.setClassName(txtClassName.getText());
				if (chkCreateDao.isSelected()) {
					attr.setDaoName(txtDaoName.getText());
				}
				if (chkCreateMap.isSelected()) {
					attr.setMapperName(txtMapperName.getText());
				}
				if (chkCreateService.isSelected()) {
					attr.setServiceName(txtServiceName.getText());
				}
				if (chkCreateServiceImpl.isSelected()) {
					attr.setServiceImplName(txtServiceImplName.getText());
				}
				if (chkSelectKey.isSelected()) {
					attr.setSelectKey(txtaSelectKey.getText());
				}
				attr.setJoinType(joinType.getSelectedToggle().getUserData().toString());
				attr.setJoinColumn(txtJoinColumnName.getText());
				attr.setTableName(txtTableName.getText());
				attr.setPrimaryKey(txtPrimaryKey.getText());
				attr.setCamel(chkUnlineCamel.isSelected());
				attr.setSerializable(chkSerializable.isSelected());
				attr.setCreateJDBCType(chkCreateJDBCtype.isSelected());
				attr.setCreateGetSet(chkGetAndSet.isSelected());
				attr.setConstruct(chkConstruct.isSelected());
				attr.setConstructAll(chkConstructAll.isSelected());
				attr.setAttributes(tblEntityProperty.getItems());
				index.addSuperAttributes(attr);
			}
			AttributeCVF attribute = new AttributeCVF();
			String tmpType=txtClassName.getText();
			if (!addSon.isAnyOpenPro()) {
				tmpType="java.util.List<"+tmpType+">";
			}
			attribute.setJavaType(tmpType);
			attribute.setPropertyName(StrUtil.fristToLoCase(txtClassName.getText()));
			//添加列集给mapper
			ColumnItem item=new ColumnItem();
			item.setAttributeCVFs(this.tblEntityProperty.getItems());
			item.setClassName(txtClassName.getText());
			item.setInPropertyName(StrUtil.fristToLoCase(txtClassName.getText()));
			item.setAnyAssociation(addSon.isAnyOpenPro());
			item.setTableName(txtTableName.getText());
			item.setPrimaryKey(txtPrimaryKey.getText());
			item.setJoinTableName(txtJoinTableName.getText());
			item.setJoinColumn(txtJoinColumnName.getText());
			item.setJoinType(joinType.getSelectedToggle().getUserData().toString());
			attribute.setColumnItem(item);
			addSon.attributeCVF.add(attribute);
			addSon.tblEntityProperty.getItems().add(attribute);
		}
		StageManager.STAGE.get("addPropertyByGrand").close();
		StageManager.STAGE.remove("addPropertyByGrand");
	}

}
