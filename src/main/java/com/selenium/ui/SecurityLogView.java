package com.selenium.ui;

import com.selenium.Main;
import com.selenium.dao.SecurityLogDAO;
import com.selenium.model.SecurityLog;
import com.selenium.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class SecurityLogView {

    public void show(Stage stage, Runnable onBack) {

        User admin = Main.getCurrentUser();
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            new Alert(Alert.AlertType.ERROR, "Admin only.").showAndWait();
            onBack.run();
            return;
        }

        Label title = new Label("🛡 Security Logs Dashboard");
        title.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: 900;
                -fx-text-fill: #EAF2FF;
        """);

        Label msg = new Label();
        msg.setStyle("-fx-text-fill: rgba(230,240,255,0.8); -fx-font-weight: 700;");



        ComboBox<String> levelBox = new ComboBox<>();
        levelBox.getItems().addAll("", "INFO", "WARN", "ERROR");
        levelBox.setValue("");

        TextField actionField = new TextField();
        actionField.setPromptText("Action (e.g. LOGIN_FAIL)");

        TextField emailField = new TextField();
        emailField.setPromptText("Email contains...");

        TextField limitField = new TextField("200");

        Button latestBtn = new Button("Latest");
        Button searchBtn = new Button("Search");
        Button backBtn = new Button("Back");

        styleButton(latestBtn);
        styleButton(searchBtn);
        styleButton(backBtn);

        HBox filters = new HBox(12,
                new Label("Level:"), levelBox,
                new Label("Action:"), actionField,
                new Label("Email:"), emailField,
                new Label("Limit:"), limitField,
                latestBtn, searchBtn, backBtn
        );
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(12));
        filters.setStyle("""
                -fx-background-color: rgba(15,25,50,0.6);
                -fx-background-radius: 14;
                -fx-border-radius: 14;
                -fx-border-color: rgba(120,190,255,0.25);
        """);



        TableView<SecurityLog> table = new TableView<>();
        table.setStyle("""
                -fx-background-color: rgba(15,25,50,0.85);
                -fx-control-inner-background: rgba(15,25,50,0.85);
                -fx-table-cell-border-color: rgba(120,190,255,0.15);
        """);

        TableColumn<SecurityLog, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("logId"));
        colId.setPrefWidth(70);

        TableColumn<SecurityLog, java.time.LocalDateTime> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colTime.setPrefWidth(170);

        TableColumn<SecurityLog, String> colLevel = new TableColumn<>("Level");
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        colLevel.setPrefWidth(90);

        colLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String level, boolean empty) {
                super.updateItem(level, empty);
                if (empty || level == null) {
                    setText(null);
                    return;
                }
                setText(level.toUpperCase());
                setStyle(levelStyle(level));
            }
        });

        TableColumn<SecurityLog, String> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAction.setPrefWidth(140);

        TableColumn<SecurityLog, Long> colUser = new TableColumn<>("User ID");
        colUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUser.setPrefWidth(90);

        TableColumn<SecurityLog, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(180);

        TableColumn<SecurityLog, String> colDetails = new TableColumn<>("Details");
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colDetails.setPrefWidth(500);

        table.getColumns().addAll(
                colId, colTime, colLevel, colAction,
                colUser, colEmail, colDetails
        );

        SecurityLogDAO dao = new SecurityLogDAO();

        Runnable loadLatest = () -> {
            try {
                int limit = parseLimit(limitField.getText());
                List<SecurityLog> rows = dao.getLatest(limit);
                table.setItems(FXCollections.observableArrayList(rows));
                msg.setText("Loaded latest " + rows.size() + " logs.");
            } catch (Exception ex) {
                msg.setText("Error: " + ex.getMessage());
            }
        };

        latestBtn.setOnAction(e -> loadLatest.run());

        searchBtn.setOnAction(e -> {
            try {
                int limit = parseLimit(limitField.getText());
                String lvl = levelBox.getValue();
                String act = actionField.getText();
                String em = emailField.getText();

                List<SecurityLog> rows = dao.search(lvl, act, em, limit);
                table.setItems(FXCollections.observableArrayList(rows));
                msg.setText("Found " + rows.size() + " logs.");
            } catch (Exception ex) {
                msg.setText("Error: " + ex.getMessage());
            }
        });

        backBtn.setOnAction(e -> onBack.run());

        loadLatest.run();

        VBox root = new VBox(18, title, filters, table, msg);
        root.setPadding(new Insets(20));
        root.setStyle("""
            -fx-background-color:
                linear-gradient(to bottom right, #050b18, #081a33, #02050d);
        """);

        VBox.setVgrow(table, Priority.ALWAYS);

        Scene scene = new Scene(root, 1300, 720);
        Main.setupStage(stage, scene, "Admin - Security Logs");
    }

    private void styleButton(Button btn) {
        btn.setStyle("""
                -fx-background-radius: 12;
                -fx-border-radius: 12;
                -fx-padding: 8 16;
                -fx-background-color: linear-gradient(to bottom, rgba(70,120,255,0.42), rgba(70,120,255,0.18));
                -fx-border-color: rgba(120,170,255,0.55);
                -fx-text-fill: #eef5ff;
                -fx-font-weight: 800;
        """);
    }

    private String levelStyle(String s) {
        return switch (s.toUpperCase()) {
            case "ERROR" -> "-fx-text-fill:#FF6B6B; -fx-font-weight:900;";
            case "WARN" -> "-fx-text-fill:#FFC86B; -fx-font-weight:900;";
            case "INFO" -> "-fx-text-fill:#7CFFB2; -fx-font-weight:900;";
            default -> "-fx-text-fill:#EAF2FF;";
        };
    }

    private int parseLimit(String s) {
        try {
            int x = Integer.parseInt(s.trim());
            if (x < 1) return 200;
            if (x > 2000) return 2000;
            return x;
        } catch (Exception e) {
            return 200;
        }
    }
}