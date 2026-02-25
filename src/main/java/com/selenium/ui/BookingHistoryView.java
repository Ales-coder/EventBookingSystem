package com.selenium.ui;

import com.selenium.Main;
import com.selenium.dao.BookingDAO;
import com.selenium.model.BookingHistoryItem;
import com.selenium.model.User;
import com.selenium.payment.PaymentService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BookingHistoryView {

    private static final DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");

    private static String euro(BigDecimal price) {
        if (price == null) return "€0.00";
        return "€" + price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public void show(Stage stage, User user, Runnable onBack) {

        Label title = new Label("🎟 My Bookings - " + user.getFullName());
        title.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 26));
        title.setTextFill(Color.web("#EAF2FF"));

        Label msg = new Label();
        msg.setStyle("-fx-text-fill:#EAF2FF; -fx-font-weight:700;");

        ListView<BookingHistoryItem> list = new ListView<>();
        list.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0 0 10 0;
        """);

        BookingDAO dao = new BookingDAO();

        Runnable reload = () -> {
            try {
                List<BookingHistoryItem> rows = dao.getBookingHistory(user.getUserId());
                list.setItems(FXCollections.observableArrayList(rows));
                msg.setText(rows.isEmpty() ? "No bookings yet." : "");
            } catch (Exception ex) {
                msg.setText("Error: " + ex.getMessage());
            }
        };

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BookingHistoryItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Label event = new Label(item.getEventTitle());
                event.setFont(Font.font("System", FontWeight.BOLD, 19));
                event.setTextFill(Color.WHITE);

                Label time = new Label(
                        item.getEventStartTime() != null ? item.getEventStartTime().format(dtf) : ""
                );
                time.setTextFill(Color.web("#9EC9FF"));
                time.setStyle("-fx-font-weight:700;");

                Label seat = new Label("💺 " + item.getSeatLabel());
                seat.setTextFill(Color.web("#CFE3FF"));
                seat.setStyle("-fx-font-weight:700;");

                Label price = new Label(euro(item.getPrice()));
                price.setTextFill(Color.WHITE);
                price.setStyle("""
                        -fx-font-weight:900;
                        -fx-padding:9 16;
                        -fx-background-radius:8;
                        -fx-background-color:
                            linear-gradient(to bottom, rgba(80,140,220,0.95), rgba(40,95,170,0.90));
                        -fx-border-color: rgba(255,255,255,0.08);
                        -fx-border-radius:8;
                """);

                Label status = new Label(item.getBookingStatus());
                status.setStyle(statusStyle(item.getBookingStatus()));

                Label eventStatus = new Label(item.getEventStatus());
                eventStatus.setStyle(eventStatusStyle(item.getEventStatus()));

                boolean isPending = "PENDING".equalsIgnoreCase(item.getBookingStatus());
                boolean isPaid = "PAID".equalsIgnoreCase(item.getBookingStatus());

                Button pay = new Button("Pay");
                Button cancel = new Button("Cancel");

                pay.setStyle(payButtonStyle());
                cancel.setStyle(cancelButtonStyle());

                pay.setDisable(!(isPending && item.isEventAvailable()));
                cancel.setDisable(!isPending);

                pay.setOnAction(e -> {
                    try {
                        boolean ok = dao.canStartPayment(user.getUserId(), item.getBookingId());
                        if (!ok) {
                            msg.setText("⏳ Hold expired.");
                            reload.run();
                            return;
                        }

                        PaymentService.PaymentResult pr = PaymentDialog.show("MockPay", item.getPrice());
                        if (!pr.approved()) {
                            msg.setText("Payment cancelled.");
                            return;
                        }

                        dao.payBooking(user.getUserId(), item.getBookingId());
                        msg.setText("✅ Paid. Tx: " + pr.transactionId());
                        reload.run();
                    } catch (Exception ex) {
                        msg.setText("Error: " + ex.getMessage());
                        reload.run();
                    }
                });

                cancel.setOnAction(e -> {
                    try {
                        dao.cancelBooking(user.getUserId(), item.getBookingId());
                        msg.setText("Booking cancelled.");
                        reload.run();
                    } catch (Exception ex) {
                        msg.setText("Cancel failed: " + ex.getMessage());
                    }
                });

                HBox top = new HBox(18, seat, price);
                top.setAlignment(Pos.CENTER_LEFT);

                HBox statuses = new HBox(14, status, eventStatus);
                statuses.setAlignment(Pos.CENTER_LEFT);

                HBox actions = new HBox(12);
                actions.setAlignment(Pos.CENTER);

                if (!isPaid) {
                    actions.getChildren().addAll(pay, cancel);
                }

                VBox leftInfo = new VBox(8, event, time, top, statuses);
                leftInfo.setAlignment(Pos.CENTER_LEFT);

                VBox content = new VBox(14, leftInfo, actions);
                content.setPadding(new Insets(22));

                Label cardWatermark = new Label("🎟");
                cardWatermark.setStyle("""
                        -fx-font-size: 220px;
                        -fx-opacity: 0.05;
                """);
                StackPane.setAlignment(cardWatermark, Pos.CENTER_RIGHT);
                StackPane.setMargin(cardWatermark, new Insets(0, 25, 0, 0));
                cardWatermark.setMouseTransparent(true);

                StackPane card = new StackPane(cardWatermark, content);
                card.setPrefWidth(940);
                card.setStyle("""
                        -fx-background-color:
                            linear-gradient(to bottom right,
                                rgba(28,40,74,0.88),
                                rgba(14,22,48,0.92),
                                rgba(30,52,90,0.70)
                            );
                        -fx-background-radius:10;
                        -fx-border-radius:10;
                        -fx-border-width: 1.5;
                        -fx-border-color: rgba(120,190,255,0.45);
                """);

                DropShadow glow = new DropShadow();
                glow.setRadius(30);
                glow.setSpread(0.08);
                glow.setColor(Color.rgb(120,190,255,0.25));
                card.setEffect(glow);

                setGraphic(card);
                setStyle("-fx-background-color: transparent; -fx-padding: 16 14;");
            }
        });

        reload.run();

        Button backBtn = new Button("Back");
        backBtn.setStyle(backButtonStyle());
        backBtn.setOnAction(e -> onBack.run());

        VBox root = new VBox(26, title, list, backBtn, msg);
        root.setPadding(new Insets(38));
        root.setMaxWidth(1100);

        VBox.setVgrow(list, Priority.ALWAYS);

        StackPane watermarkLayer = new StackPane();
        watermarkLayer.setMouseTransparent(true);

        Label watermark = new Label("🎟");
        watermark.setStyle("""
        -fx-font-size: 700px;
        -fx-opacity: 0.025;
""");

        watermarkLayer.getChildren().add(watermark);
        watermarkLayer.setAlignment(Pos.CENTER);

        StackPane mainLayout = new StackPane(watermarkLayer, root);

        mainLayout.setStyle("""
                -fx-background-color:
                    linear-gradient(to bottom right,#050b18,#081a33,#02050d);
        """);

        Scene scene = new Scene(mainLayout, 1200, 760);
        Main.setupStage(stage, scene, "Ticket Booking System - My Bookings");
    }

    private String payButtonStyle() {
        return """
                -fx-background-radius:10;
                -fx-padding:10 26;
                -fx-background-color:
                    linear-gradient(to bottom, rgba(34,120,90,0.95), rgba(12,55,40,0.98));
                -fx-text-fill: white;
                -fx-font-weight: 900;
        """;
    }

    private String cancelButtonStyle() {
        return """
                -fx-background-radius:10;
                -fx-padding:10 26;
                -fx-background-color:
                    linear-gradient(to bottom, rgba(150,35,55,0.98), rgba(70,10,20,0.98));
                -fx-text-fill: white;
                -fx-font-weight: 900;
        """;
    }

    private String backButtonStyle() {
        return """
                -fx-background-radius:10;
                -fx-padding:10 26;
                -fx-background-color:
                    linear-gradient(to bottom, rgba(70,120,255,0.35), rgba(50,85,200,0.18));
                -fx-text-fill: #eef5ff;
                -fx-font-weight: 900;
        """;
    }

    private String statusStyle(String s) {
        if (s == null) return "-fx-text-fill:#EAF2FF;";
        return switch (s.toUpperCase()) {
            case "PAID" -> "-fx-text-fill:#7CFFB2; -fx-font-weight:900;";
            case "PENDING" -> "-fx-text-fill:#FFC86B; -fx-font-weight:900;";
            case "CANCELLED" -> "-fx-text-fill:#FF8A8A; -fx-font-weight:900;";
            case "EXPIRED" -> "-fx-text-fill:#BBBBBB; -fx-font-weight:900;";
            default -> "-fx-text-fill:#EAF2FF;";
        };
    }

    private String eventStatusStyle(String s) {
        if (s == null) return "-fx-text-fill:#EAF2FF;";
        return switch (s.toUpperCase()) {
            case "ACTIVE" -> "-fx-text-fill:#7CFFB2; -fx-font-weight:900;";
            case "DELETED" -> "-fx-text-fill:#FF8A8A; -fx-font-weight:900;";
            default -> "-fx-text-fill:#EAF2FF;";
        };
    }
}