package com.selenium.ui;

import com.selenium.Main;
import com.selenium.dao.SecurityLogDAO;
import com.selenium.dao.SecurityLogDAO.EmailCount;
import com.selenium.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class SecurityDashboardView {

    private Timeline alertBlink;

    public void show(Stage stage, Runnable onBack) {

        User admin = Main.getCurrentUser();
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            new Alert(Alert.AlertType.ERROR, "Admin only.").showAndWait();
            onBack.run();
            return;
        }

        Label title = new Label("🛡 Security Dashboard");
        title.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: 900;
                -fx-text-fill: #EAF2FF;
        """);

        Label alertIndicator = new Label();
        alertIndicator.setStyle("-fx-font-size:16px; -fx-font-weight:900;");

        Label msg = new Label();
        msg.setStyle("-fx-text-fill:#EAF2FF; -fx-font-weight:700;");

        SecurityLogDAO dao = new SecurityLogDAO();

        TextField minutesField = new TextField("60");
        minutesField.setPrefWidth(70);
        minutesField.setStyle(inputStyle());

        Button refreshBtn = styledButton("Refresh");
        Button openLogsBtn = styledButton("Open Logs");
        Button backBtn = styledButton("Back");

        Label kpiLoginFail = kpiLabel();
        Label kpiBookFail = kpiLabel();
        Label kpiBookBlocked = kpiLabel();
        Label kpiPayFail = kpiLabel();

        HBox kpiRow = new HBox(
                15,
                createKpiCard("LOGIN_FAIL", kpiLoginFail),
                createKpiCard("BOOK_FAIL", kpiBookFail),
                createKpiCard("BOOK_BLOCKED", kpiBookBlocked),
                createKpiCard("PAY_FAIL", kpiPayFail)
        );
        kpiRow.setAlignment(Pos.CENTER_LEFT);


        TableView<EmailCount> topEmailsTable = new TableView<>();
        VBox.setVgrow(topEmailsTable, Priority.ALWAYS);

        TableColumn<EmailCount, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().email()));
        colEmail.setPrefWidth(320);

        TableColumn<EmailCount, Number> colCount = new TableColumn<>("Count");
        colCount.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(c.getValue().count()));
        colCount.setPrefWidth(100);

        TableColumn<EmailCount, String> colLast = new TableColumn<>("Last Seen");
        colLast.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().lastAt() == null ? "" : c.getValue().lastAt().toString()));
        colLast.setPrefWidth(250);

        topEmailsTable.getColumns().addAll(colEmail, colCount, colLast);


        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Time");
        yAxis.setLabel("Events");

        LineChart<String, Number> trendChart =
                new LineChart<>(xAxis, yAxis);

        trendChart.setTitle("Security Events Trend");
        trendChart.setPrefHeight(240);
        trendChart.setCreateSymbols(false);
        trendChart.setAnimated(false);


        Runnable reload = () -> {
            try {
                int minutes = parseInt(minutesField.getText(), 60);

                int loginFail = countAction("LOGIN_FAIL", minutes);
                int bookFail = countAction("BOOK_FAIL", minutes);
                int bookBlocked = countAction("BOOK_BLOCKED", minutes);
                int payFail = countAction("PAY_FAIL", minutes);

                kpiLoginFail.setText(String.valueOf(loginFail));
                kpiBookFail.setText(String.valueOf(bookFail));
                kpiBookBlocked.setText(String.valueOf(bookBlocked));
                kpiPayFail.setText(String.valueOf(payFail));


                if (bookBlocked > 0) {
                    alertIndicator.setText("🚨 ACTIVE SECURITY ALERTS");
                    startBlink(alertIndicator);
                } else {
                    alertIndicator.setText("");
                    stopBlink();
                }


                List<EmailCount> loginRows =
                        dao.topEmailsByAction("LOGIN_FAIL", minutes, 20);
                List<EmailCount> blockedRows =
                        dao.topEmailsByAction("BOOK_BLOCKED", minutes, 20);

                Map<String, EmailCount> merged = new HashMap<>();

                for (EmailCount e : loginRows)
                    merged.put(e.email(), e);

                for (EmailCount e : blockedRows) {
                    if (merged.containsKey(e.email())) {
                        merged.compute(e.email(),
                                (k, existing) -> new EmailCount(
                                        e.email(),
                                        existing.count() + e.count(),
                                        e.lastAt().isAfter(existing.lastAt())
                                                ? e.lastAt()
                                                : existing.lastAt()
                                ));
                    } else {
                        merged.put(e.email(), e);
                    }
                }

                List<EmailCount> finalList = new ArrayList<>(merged.values());
                finalList.sort((a, b) ->
                        Integer.compare(b.count(), a.count()));

                if (finalList.size() > 10)
                    finalList = finalList.subList(0, 10);

                topEmailsTable.setItems(
                        FXCollections.observableArrayList(finalList)
                );


                XYChart.Series<String, Number> series =
                        new XYChart.Series<>();

                String sqlTrend = """
                        SELECT to_char(date_trunc('minute', created_at), 'HH24:MI') AS minute,
                               COUNT(*) AS cnt
                        FROM security_logs
                        WHERE created_at >= now() - (? * INTERVAL '1 minute')
                        GROUP BY date_trunc('minute', created_at)
                        ORDER BY date_trunc('minute', created_at)
                        """;

                try (var con = com.selenium.db.DB.getConnection();
                     var ps = con.prepareStatement(sqlTrend)) {

                    ps.setInt(1, minutes);

                    try (var rs = ps.executeQuery()) {
                        while (rs.next()) {
                            series.getData().add(
                                    new XYChart.Data<>(
                                            rs.getString("minute"),
                                            rs.getInt("cnt")
                                    )
                            );
                        }
                    }
                }

                trendChart.getData().clear();
                trendChart.getData().add(series);

                msg.setText("Updated.");

            } catch (Exception ex) {
                msg.setText("Error: " + ex.getMessage());
            }
        };

        refreshBtn.setOnAction(e -> reload.run());
        openLogsBtn.setOnAction(e ->
                new SecurityLogView().show(stage, () -> show(stage, onBack)));
        backBtn.setOnAction(e -> onBack.run());

        reload.run();

        VBox root = new VBox(
                15,
                title,
                alertIndicator,
                new HBox(10,
                        new Label("Window (minutes):"),
                        minutesField,
                        refreshBtn,
                        openLogsBtn,
                        backBtn),
                kpiRow,
                new Label("⚠ Top suspicious emails"),
                topEmailsTable,
                trendChart,
                msg
        );

        root.setPadding(new Insets(20));
        root.setStyle("""
                -fx-background-color:
                linear-gradient(to bottom right, #050b18, #081a33, #02050d);
        """);

        Scene scene = new Scene(root, 1200, 750);
        Main.setupStage(stage, scene, "Admin - Security Dashboard");
    }

    private void startBlink(Label label) {
        if (alertBlink != null) return;

        alertBlink = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e ->
                        label.setStyle("-fx-text-fill:#ff4d4d; -fx-font-weight:900;")),
                new KeyFrame(Duration.seconds(1), e ->
                        label.setStyle("-fx-text-fill:#EAF2FF; -fx-font-weight:900;"))
        );
        alertBlink.setCycleCount(Timeline.INDEFINITE);
        alertBlink.play();
    }

    private void stopBlink() {
        if (alertBlink != null) {
            alertBlink.stop();
            alertBlink = null;
        }
    }

    private VBox createKpiCard(String label, Label value) {
        Label title = new Label(label);
        title.setStyle("-fx-text-fill:#9EC9FF; -fx-font-weight:700;");
        value.setStyle("-fx-text-fill:#EAF2FF; -fx-font-size:18px; -fx-font-weight:900;");
        VBox box = new VBox(5, title, value);
        box.setPadding(new Insets(12));
        box.setPrefWidth(220);
        box.setStyle("""
                -fx-background-color: rgba(15,25,50,0.75);
                -fx-background-radius: 15;
                -fx-border-radius: 15;
                -fx-border-color: rgba(120,190,255,0.35);
        """);
        box.setEffect(new DropShadow(15, Color.rgb(90,160,255,0.25)));
        return box;
    }

    private Label kpiLabel() { return new Label(); }

    private Button styledButton(String text) {
        Button b = new Button(text);
        b.setStyle("""
                -fx-background-radius: 12;
                -fx-padding: 6 16;
                -fx-background-color: linear-gradient(to bottom, rgba(70,120,255,0.42), rgba(70,120,255,0.18));
                -fx-text-fill: #eef5ff;
                -fx-font-weight: 800;
        """);
        return b;
    }

    private String inputStyle() {
        return """
                -fx-background-radius: 10;
                -fx-padding: 6 10;
                -fx-background-color: rgba(10, 18, 36, 0.9);
                -fx-text-fill: #eaf2ff;
                -fx-border-color: rgba(140, 190, 255, 0.30);
        """;
    }

    private int parseInt(String s, int def) {
        try {
            int x = Integer.parseInt(s.trim());
            if (x < 1) return def;
            if (x > 24 * 60) return 24 * 60;
            return x;
        } catch (Exception e) {
            return def;
        }
    }

    private int countAction(String action, int minutesBack) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM security_logs
                WHERE action = ?
                  AND created_at >= now() - (? * INTERVAL '1 minute')
                """;

        try (var con = com.selenium.db.DB.getConnection();
             var ps = con.prepareStatement(sql)) {
            ps.setString(1, action.trim().toUpperCase());
            ps.setInt(2, minutesBack);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}