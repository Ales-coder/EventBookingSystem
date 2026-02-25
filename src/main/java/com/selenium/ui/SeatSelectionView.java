package com.selenium.ui;

import com.selenium.Main;
import com.selenium.dao.BookingDAO;
import com.selenium.dao.SeatDAO;
import com.selenium.model.Event;
import com.selenium.model.SeatInfo;
import com.selenium.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class SeatSelectionView {

    private static String euro(BigDecimal price) {
        if (price == null) return "€0.00";
        return "€" + price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public void show(Stage stage, User user, Event event, Runnable onBack) {

        Label title = new Label("🎟 Seats for: " + event.getTitle());
        title.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: 900;
            -fx-text-fill: #EAF2FF;
        """);

        Label msg = new Label();
        msg.setStyle("-fx-text-fill: rgba(234,242,255,0.80); -fx-font-weight: 700;");

        ListView<SeatInfo> seatList = new ListView<>();
        seatList.setStyle("-fx-background-color: transparent;");

        SeatDAO dao = new SeatDAO();

        Runnable reload = () -> {
            try {
                List<SeatInfo> seats = dao.getSeatsForEvent(event.getEventId());
                seatList.getItems().setAll(seats);
            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error: " + ex.getMessage());
            }
        };

        seatList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SeatInfo item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                String state = item.getState().toUpperCase();

                Label seatLbl = new Label("💺 " + item.getLabel());
                seatLbl.setStyle("-fx-text-fill:#EAF2FF; -fx-font-weight:900;");

                Label priceLbl = new Label(euro(item.getPrice()));
                priceLbl.setStyle("""
                    -fx-text-fill:#EAF2FF;
                    -fx-font-weight:900;
                    -fx-padding:6 12;
                    -fx-background-radius:999;
                    -fx-background-color:rgba(120,190,255,0.18);
                    -fx-border-color:rgba(120,190,255,0.4);
                    -fx-border-radius:999;
                """);

                Label stateLbl = new Label(state);
                stateLbl.setStyle(statusStyle(state));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(10, seatLbl, stateLbl, spacer, priceLbl);
                row.setAlignment(Pos.CENTER_LEFT);

                StackPane card = new StackPane(row);
                card.setPadding(new Insets(12));
                card.setStyle("""
                    -fx-background-color: rgba(15,25,50,0.75);
                    -fx-background-radius: 16;
                    -fx-border-radius: 16;
                    -fx-border-color: rgba(120,190,255,0.35);
                """);

                card.setEffect(new DropShadow(18, Color.rgb(90,160,255,0.18)));

                setGraphic(card);
            }
        });

        reload.run();

        Button bookBtn = new Button("Book Selected Seat");
        Button backBtn = new Button("Back");

        bookBtn.setDisable(true);

        seatList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) {
                bookBtn.setDisable(true);
                return;
            }
            bookBtn.setDisable(!"AVAILABLE".equalsIgnoreCase(n.getState()));
        });

        bookBtn.setOnAction(e -> {

            if (Main.isActionsBlocked()) {
                showSecurityBlockedPopup(stage);
                msg.setText("🚫 Bookings are blocked due to security.");
                return;
            }

            SeatInfo selected = seatList.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            try {
                BookingDAO bookingDAO = new BookingDAO();

                long bookingId = bookingDAO.bookSingleSeat(
                        user,
                        event.getEventId(),
                        selected.getSeatId(),
                        selected.getPrice()
                );

                msg.setText("✅ Seat booked. ID: " + bookingId);
                reload.run();

            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error: " + ex.getMessage());
            }
        });

        backBtn.setOnAction(e -> onBack.run());

        HBox actions = new HBox(12, bookBtn, backBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(12, title, seatList, actions, msg);
        content.setPadding(new Insets(16));

        VBox.setVgrow(seatList, Priority.ALWAYS);

        StackPane watermarkLayer = new StackPane();
        watermarkLayer.setMouseTransparent(true);

        Label watermark = new Label("🎟");
        watermark.setStyle("""
                -fx-font-size: 700px;
                -fx-opacity: 0.025;
        """);

        watermarkLayer.getChildren().add(watermark);
        watermarkLayer.setAlignment(Pos.CENTER);

        StackPane mainLayout = new StackPane(watermarkLayer, content);

        mainLayout.setStyle("""
            -fx-background-color:
                radial-gradient(radius 120%, rgba(90,160,255,0.18), transparent),
                linear-gradient(to bottom right, #050b18, #081a33, #02050d);
        """);

        Scene scene = new Scene(mainLayout, 1200, 720);
        Main.setupStage(stage, scene, "Ticket Booking System - Seats");
    }

    private void showSecurityBlockedPopup(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(owner);
        alert.setTitle("Security");
        alert.setHeaderText("🚫 Action blocked");
        alert.setContentText("Bookings are blocked due to suspicious activity.\nPlease try again later.");

        DialogPane dp = alert.getDialogPane();
        dp.setStyle("""
            -fx-background-color:
                radial-gradient(radius 120%, rgba(90,160,255,0.14), transparent),
                linear-gradient(to bottom right, #050b18, #081a33, #02050d);
            -fx-border-color: rgba(120,190,255,0.35);
            -fx-border-width: 1.2;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
        """);

        dp.lookup(".header-panel").setStyle("""
            -fx-background-color: transparent;
        """);

        Label header = (Label) dp.lookup(".header-panel .label");
        if (header != null) {
            header.setStyle("""
                -fx-text-fill: #EAF2FF;
                -fx-font-weight: 900;
                -fx-font-size: 16px;
            """);
        }

        Label content = (Label) dp.lookup(".content.label");
        if (content != null) {
            content.setStyle("""
                -fx-text-fill: rgba(234,242,255,0.85);
                -fx-font-weight: 700;
                -fx-font-size: 13px;
            """);
        }

        Button ok = (Button) dp.lookupButton(ButtonType.OK);
        if (ok != null) {
            ok.setStyle("""
                -fx-background-radius: 10;
                -fx-padding: 10 22;
                -fx-background-color:
                    linear-gradient(to bottom, rgba(70,120,255,0.35), rgba(50,85,200,0.18));
                -fx-text-fill: #eef5ff;
                -fx-font-weight: 900;
                -fx-border-radius: 10;
                -fx-border-color: rgba(255,255,255,0.10);
                -fx-border-width: 1;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 16, 0.25, 0, 6);
            """);
        }

        alert.showAndWait();
    }

    private String statusStyle(String state) {

        return switch (state) {
            case "AVAILABLE" -> """
                -fx-text-fill: #b9ffce;
                -fx-background-color: rgba(60,200,120,0.18);
                -fx-border-color: rgba(60,200,120,0.4);
                -fx-padding:5 10;
                -fx-background-radius:999;
                -fx-border-radius:999;
                -fx-font-weight:900;
            """;
            case "BOOKED" -> """
                -fx-text-fill: #ffbcbc;
                -fx-background-color: rgba(255,80,80,0.18);
                -fx-border-color: rgba(255,80,80,0.4);
                -fx-padding:5 10;
                -fx-background-radius:999;
                -fx-border-radius:999;
                -fx-font-weight:900;
            """;
            default -> """
                -fx-text-fill: #eaf2ff;
                -fx-padding:5 10;
                -fx-font-weight:900;
            """;
        };
    }
}