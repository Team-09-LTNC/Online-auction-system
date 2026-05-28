package com.auction.client.controller.admin;

import com.auction.client.manager.AdminManager;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Bộ điều khiển cho màn hình quản trị người đặt giá.
 */
public class BiddersViewController implements Initializable {

  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_LOCKED = "LOCKED";
  private static final String ALL_STATUS = "Tat ca";
  private static final List<String> FILTER_OPTIONS = List.of(
      ALL_STATUS,
      STATUS_ACTIVE,
      STATUS_LOCKED
  );
  private static final DateTimeFormatter LOCK_TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

  @FXML
  private StackPane contentPane;
  @FXML
  private TableView<Bidder> bidderTable;
  @FXML
  private TableColumn<Bidder, String> colUsername;
  @FXML
  private TableColumn<Bidder, String> colFullname;
  @FXML
  private TableColumn<Bidder, String> colStatus;
  @FXML
  private TableColumn<Bidder, String> colLockUntil;
  @FXML
  private Label lblBidderCount;
  @FXML
  private TextField tfSearch;
  @FXML
  private ComboBox<String> cbStatusFilter;
  @FXML
  private Button btnToggleLock;

  private final ObservableList<Bidder> masterList = FXCollections.observableArrayList();
  private FilteredList<Bidder> filteredList;

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    setupColumns();
    setupStatusFilter();
    setupTable();
    loadData();
  }

  private void setupColumns() {
    colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
    colFullname.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullname()));

    colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    colStatus.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(String status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
          setText(null);
          setStyle("");
          return;
        }
        boolean locked = STATUS_LOCKED.equals(status);
        setText(locked ? "Bi khoa" : "Hoat dong");
        setStyle(locked
            ? "-fx-text-fill: #C0392B; -fx-font-weight: bold; -fx-font-size: 12px;"
            : "-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-font-size: 12px;");
      }
    });
    colLockUntil.setCellValueFactory(c -> new SimpleStringProperty(formatLockUntil(c.getValue())));
  }

  private void setupStatusFilter() {
    cbStatusFilter.setItems(FXCollections.observableArrayList(FILTER_OPTIONS));
    cbStatusFilter.getSelectionModel().selectFirst();
  }

  private void setupTable() {
    filteredList = new FilteredList<>(masterList, p -> true);
    bidderTable.setItems(filteredList);
    bidderTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
  }

  private void loadData() {
    AdminManager.getInstance().getBidders(
        users -> Platform.runLater(() -> {
          masterList.clear();
          users.forEach(u -> masterList.add(
              new Bidder(u.getUsername(), u.getFullname(), u.getStatus(), u.getLockUntil())));
          updateCountLabel();
        }),
        error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait())
    );
  }

  @FXML
  private void handleTableClick(MouseEvent event) {
    Bidder selected = bidderTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      btnToggleLock.setDisable(true);
      return;
    }
    btnToggleLock.setDisable(false);
    refreshLockButtonText(selected);
  }

  @FXML
  private void handleSearch() {
    applyFilter();
  }

  @FXML
  private void handleFilter() {
    applyFilter();
  }

  @FXML
  private void handleToggleLock() {
    Bidder selected = bidderTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }

    String newStatus = STATUS_LOCKED.equals(selected.getStatus()) ? STATUS_ACTIVE : STATUS_LOCKED;
    AdminManager.getInstance().toggleAccountLock(
        selected.getUsername(),
        newStatus,
        response -> Platform.runLater(() -> {
          selected.setStatus(newStatus);
          selected.setLockUntil(null);
          bidderTable.refresh();
          refreshLockButtonText(selected);
          applyFilter();
          updateCountLabel();
        }),
        error -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).showAndWait())
    );
  }

  private void applyFilter() {
    String kw = tfSearch.getText().trim().toLowerCase();
    String status = cbStatusFilter.getValue();

    filteredList.setPredicate(b -> {
      boolean matchKw = kw.isEmpty()
          || b.getUsername().toLowerCase().contains(kw)
          || b.getFullname().toLowerCase().contains(kw);
      boolean matchStatus = status == null || ALL_STATUS.equals(status)
          || b.getStatus().equalsIgnoreCase(status);
      return matchKw && matchStatus;
    });
    updateCountLabel();
  }

  private void refreshLockButtonText(Bidder bidder) {
    if (STATUS_LOCKED.equals(bidder.getStatus())) {
      btnToggleLock.setText("Mo khoa");
      btnToggleLock.setStyle(
          "-fx-background-color: #27AE60; -fx-text-fill: white;"
              + "-fx-font-size: 13px; -fx-font-weight: bold;"
              + "-fx-padding: 8 18 8 18; -fx-border-radius: 6px;"
              + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-border-width: 0;");
      return;
    }
    btnToggleLock.setText("Khoa tai khoan");
    btnToggleLock.setStyle(
        "-fx-background-color: #8B2C2C; -fx-text-fill: white;"
            + "-fx-font-size: 13px; -fx-font-weight: bold;"
            + "-fx-padding: 8 18 8 18; -fx-border-radius: 6px;"
            + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-border-width: 0;");
  }

  private void updateCountLabel() {
    int shown = filteredList.size();
    int total = masterList.size();
    lblBidderCount.setText(shown == total
        ? total + " nguoi dung"
        : shown + " / " + total + " nguoi dung");
  }

  private String formatLockUntil(Bidder bidder) {
    if (!STATUS_LOCKED.equals(bidder.getStatus())) {
      return "-";
    }
    String lockUntil = bidder.getLockUntil();
    if (lockUntil == null || lockUntil.isBlank()) {
      return "Vinh vien";
    }
    try {
      return LocalDateTime.parse(lockUntil).format(LOCK_TIME_FORMAT);
    } catch (Exception ignored) {
      return lockUntil;
    }
  }

  /**
   * Model dữ liệu cho từng dòng bảng.
   */
  public static class Bidder {
    private final SimpleStringProperty username;
    private final SimpleStringProperty fullname;
    private final SimpleStringProperty status;
    private final SimpleStringProperty lockUntil;

    public Bidder(String username, String fullname, String status, String lockUntil) {
      this.username = new SimpleStringProperty(username);
      this.fullname = new SimpleStringProperty(fullname);
      this.status = new SimpleStringProperty(status);
      this.lockUntil = new SimpleStringProperty(lockUntil);
    }

    public String getUsername() {
      return username.get();
    }

    public String getFullname() {
      return fullname.get();
    }

    public String getStatus() {
      return status.get();
    }

    public void setStatus(String value) {
      status.set(value);
    }

    public String getLockUntil() {
      return lockUntil.get();
    }

    public void setLockUntil(String value) {
      lockUntil.set(value);
    }

    public SimpleStringProperty usernameProperty() {
      return username;
    }

    public SimpleStringProperty fullnameProperty() {
      return fullname;
    }

    public SimpleStringProperty statusProperty() {
      return status;
    }

    public SimpleStringProperty lockUntilProperty() {
      return lockUntil;
    }
  }
}
