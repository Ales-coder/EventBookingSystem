package com.selenium.ui;

import com.selenium.Main;
import com.selenium.dao.EventDAO;
import com.selenium.dao.SeatDAO;
import com.selenium.dao.SecurityLogDAO;
import com.selenium.model.Event;
import com.selenium.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminEventView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String ROOT_BG =
            "-fx-background-color: linear-gradient(to bottom right, #0a1224, #0b1b2f 55%, #061019);"
                    + "-fx-font-family: 'Segoe UI','Inter','Arial';"
                    + "-fx-font-size: 13px;"
                    + "-fx-text-fill: #e8f0ff;";

    private static final String LABEL_TXT = "-fx-text-fill: #e8f0ff;";

    private static final String INPUT =
            "-fx-background-radius: 10;"
                    + "-fx-border-radius: 10;"
                    + "-fx-padding: 8 10;"
                    + "-fx-control-inner-background: rgba(10, 18, 36, 0.90);"
                    + "-fx-background-color: rgba(10, 18, 36, 0.90);"
                    + "-fx-border-color: rgba(140, 190, 255, 0.30);"
                    + "-fx-text-fill: #eaf2ff;"
                    + "-fx-prompt-text-fill: rgba(210, 230, 255, 0.45);"
                    + "-fx-highlight-fill: rgba(90, 160, 255, 0.40);"
                    + "-fx-highlight-text-fill: #ffffff;";

    private static final String TEXTAREA_CONTENT =
            "-fx-control-inner-background: rgba(10, 18, 36, 0.90);"
                    + "-fx-background-color: rgba(10, 18, 36, 0.90);"
                    + "-fx-text-fill: #eaf2ff;";

    private static final String BTN =
            "-fx-background-radius: 10;"
                    + "-fx-border-radius: 10;"
                    + "-fx-padding: 8 14;"
                    + "-fx-background-color: linear-gradient(to bottom, rgba(70,120,255,0.40), rgba(70,120,255,0.18));"
                    + "-fx-border-color: rgba(120,170,255,0.55);"
                    + "-fx-text-fill: #eef5ff;"
                    + "-fx-font-weight: 700;";

    private static final String LIST =
            "-fx-background-color: rgba(10, 18, 36, 0.60);"
                    + "-fx-border-color: rgba(140, 190, 255, 0.22);"
                    + "-fx-border-radius: 12;"
                    + "-fx-background-radius: 12;";

    private static final String SEPARATOR = "-fx-border-color: rgba(130, 170, 255, 0.25);";

    private static void styleLabel(Label l) { l.setStyle(LABEL_TXT); }

    private static void styleInput(TextField tf) {
        tf.setStyle(INPUT);
        tf.setMaxWidth(Double.MAX_VALUE);
    }

    private static void styleTextArea(TextArea ta) {
        ta.setStyle(INPUT);
        ta.setWrapText(true);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.lookupAll(".content").forEach(n -> n.setStyle(TEXTAREA_CONTENT));
        ta.skinProperty().addListener((obs, oldV, newV) -> {
            ta.lookupAll(".content").forEach(n -> n.setStyle(TEXTAREA_CONTENT));
        });
    }

    private static void styleButton(Button b) { b.setStyle(BTN); }
    private static void styleSeparator(Separator s) { s.setStyle(SEPARATOR); }

    public void show(Stage stage, Runnable onDone) {

        User admin = Main.getCurrentUser();
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            new Alert(Alert.AlertType.ERROR, "Admin only.").showAndWait();
            onDone.run();
            return;
        }

        SecurityLogDAO logDao = new SecurityLogDAO();
        EventDAO eventDAO = new EventDAO();
        SeatDAO seatDAO = new SeatDAO();

        Label titleLbl = new Label("Admin Panel (Create / Delete events)");
        titleLbl.setStyle(LABEL_TXT + "-fx-font-size: 16px; -fx-font-weight: 800;");

        Label msg = new Label();
        styleLabel(msg);

        TextField venueIdField = new TextField("1");
        TextField titleField = new TextField("Cyber Night Tirana");
        TextField categoryField = new TextField("SECURITY");
        TextArea descArea = new TextArea("Talks + live demo: brute-force detection & suspicious bookings.");
        descArea.setPrefRowCount(3);

        TextField startField = new TextField("2026-03-15 20:30");
        TextField priceField = new TextField("25.00");

        TextField sectionField = new TextField("A");
        TextField rowsField = new TextField("A,B,C,D");
        TextField seatsPerRowField = new TextField("10");

        styleInput(venueIdField);
        styleInput(titleField);
        styleInput(categoryField);
        styleTextArea(descArea);
        styleInput(startField);
        styleInput(priceField);
        styleInput(sectionField);
        styleInput(rowsField);
        styleInput(seatsPerRowField);

        Button createBtn = new Button("Create Event + Attach Seats");
        Button reloadBtn = new Button("Reload");
        Button deleteBtn = new Button("Delete Selected Event");
        Button backBtn = new Button("Back");

        styleButton(createBtn);
        styleButton(reloadBtn);
        styleButton(deleteBtn);
        styleButton(backBtn);

        createBtn.setMaxWidth(Double.MAX_VALUE);


        ListView<Event> eventsList = new ListView<>();
        eventsList.setStyle(LIST);

        eventsList.setFocusTraversable(true);
        eventsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        eventsList.setCellFactory(lv -> new ListCell<>() {

            private void applyStyle(Event item, boolean selected) {
                if (item == null) return;

                boolean deleted = "DELETED".equalsIgnoreCase(item.getStatus());


                String textFill = deleted
                        ? "rgba(232,240,255,0.55)"
                        : "#eaf2ff";


                String bg = selected
                        ? "rgba(90,160,255,0.28)"
                        : "transparent";


                String border = selected
                        ? "rgba(160,210,255,0.55)"
                        : "transparent";

                setStyle(
                        "-fx-background-color: " + bg + ";" +
                                "-fx-text-fill: " + textFill + ";" +
                                "-fx-padding: 10 10;" +
                                "-fx-border-color: " + border + ";" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-radius: 10;"
                );
            }

            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                boolean deleted = "DELETED".equalsIgnoreCase(item.getStatus());
                setText(deleted ? (item.toString() + "  (DELETED)") : item.toString());

                applyStyle(item, isSelected());
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                applyStyle(getItem(), selected);
            }
        });


        Runnable reload = () -> {
            try {
                List<Event> events = eventDAO.getAllEventsAdmin();
                eventsList.setItems(FXCollections.observableArrayList(events));
                msg.setText("Loaded " + events.size() + " event(s).");


                eventsList.getSelectionModel().clearSelection();
            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error loading events: " + ex.getMessage());
            }
        };

        reload.run();


        createBtn.setOnAction(e -> {
            try {
                long venueId = Long.parseLong(venueIdField.getText().trim());
                String t = titleField.getText();
                String cat = categoryField.getText();
                String desc = descArea.getText();

                if (t == null || t.isBlank()) { msg.setText("Title is required."); return; }
                if (cat == null || cat.isBlank()) { msg.setText("Category is required."); return; }

                LocalDateTime start = LocalDateTime.parse(startField.getText().trim(), FMT);
                BigDecimal price = new BigDecimal(priceField.getText().trim());

                long eventId = eventDAO.createEvent(
                        venueId,
                        t.trim(),
                        (desc == null ? "" : desc.trim()),
                        cat.trim(),
                        Timestamp.valueOf(start)
                );

                logDao.log("INFO", "ADMIN_CREATE_EVENT", admin.getUserId(), admin.getEmail(),
                        "eventId=" + eventId + " venueId=" + venueId + " title=" + t.trim());

                int seatCount = seatDAO.countSeatsInVenue(venueId);
                if (seatCount == 0) {
                    String section = sectionField.getText().trim();
                    List<String> rows = Arrays.stream(rowsField.getText().split(","))
                            .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
                    int seatsPerRow = Integer.parseInt(seatsPerRowField.getText().trim());

                    int inserted = seatDAO.generateSeatsForVenue(venueId, section, rows, seatsPerRow);
                    msg.setText("Venue had 0 seats -> generated seats: " + inserted);
                }

                int attached = seatDAO.attachVenueSeatsToEvent(eventId, venueId, price);
                msg.setText("✅ Event created (ID=" + eventId + "). Attached seats: " + attached);
                reload.run();

            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error: " + ex.getMessage());
                logDao.log("ERROR", "ADMIN_CREATE_EVENT_ERROR", admin.getUserId(), admin.getEmail(),
                        "msg=" + ex.getMessage());
            }
        });


        deleteBtn.setOnAction(e -> {

            eventsList.requestFocus();

            Event selected = eventsList.getSelectionModel().getSelectedItem();
            if (selected == null) { msg.setText("Select an event first."); return; }
            if ("DELETED".equalsIgnoreCase(selected.getStatus())) { msg.setText("This event is already DELETED."); return; }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete event ID " + selected.getEventId() + "?");
            confirm.setContentText("Soft delete: event -> DELETED, seats -> BLOCKED.");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

            try {
                eventDAO.softDeleteEvent(admin.getUserId(), selected.getEventId());
                msg.setText("✅ Event deleted (soft).");
                reload.run();
            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error: " + ex.getMessage());
            }
        });

        reloadBtn.setOnAction(e -> reload.run());
        backBtn.setOnAction(e -> onDone.run());


        GridPane form = new GridPane();
        form.setPadding(new Insets(14));
        form.setHgap(12);
        form.setVgap(12);
        form.setStyle("-fx-background-color: transparent;");

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(260);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c0, c1);

        int r = 0;

        Label lVenue = new Label("Venue ID:"); styleLabel(lVenue);
        Label lTitle = new Label("Title:"); styleLabel(lTitle);
        Label lCat = new Label("Category:"); styleLabel(lCat);
        Label lDesc = new Label("Description:"); styleLabel(lDesc);
        Label lStart = new Label("Start time (yyyy-MM-dd HH:mm):"); styleLabel(lStart);

        Label lPrice = new Label("Seat price:"); styleLabel(lPrice);
        Label lSec = new Label("Seat section (if venue has 0 seats):"); styleLabel(lSec);
        Label lRows = new Label("Rows (comma separated):"); styleLabel(lRows);
        Label lSpr = new Label("Seats per row:"); styleLabel(lSpr);

        form.add(lVenue, 0, r); form.add(venueIdField, 1, r++);
        form.add(lTitle, 0, r); form.add(titleField, 1, r++);
        form.add(lCat, 0, r); form.add(categoryField, 1, r++);
        form.add(lDesc, 0, r); form.add(descArea, 1, r++);
        form.add(lStart, 0, r); form.add(startField, 1, r++);

        Separator sep1 = new Separator(); styleSeparator(sep1);
        form.add(sep1, 0, r++, 2, 1);

        form.add(lPrice, 0, r); form.add(priceField, 1, r++);
        form.add(lSec, 0, r); form.add(sectionField, 1, r++);
        form.add(lRows, 0, r); form.add(rowsField, 1, r++);
        form.add(lSpr, 0, r); form.add(seatsPerRowField, 1, r++);

        GridPane.setHgrow(createBtn, Priority.ALWAYS);
        form.add(createBtn, 0, r, 2, 1);

        HBox adminActions = new HBox(10, reloadBtn, deleteBtn, backBtn);
        adminActions.setPadding(new Insets(6, 0, 0, 0));
        adminActions.setStyle("-fx-background-color: transparent;");

        Label listTitle = new Label("Events (Admin view):");
        listTitle.setStyle(LABEL_TXT + "-fx-font-weight: 800;");

        VBox content = new VBox(12,
                titleLbl,
                form,
                new Separator(),
                listTitle,
                eventsList,
                adminActions,
                msg
        );
        content.setPadding(new Insets(16));
        content.setStyle(ROOT_BG);

        VBox.setVgrow(eventsList, Priority.ALWAYS);
        eventsList.setMinHeight(320);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPannable(true);
        sp.setStyle("-fx-background-color: transparent;");

        sp.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
            if (sp.lookup(".viewport") != null) {
                sp.lookup(".viewport").setStyle("-fx-background-color: transparent;");
            }
        });

        Scene scene = new Scene(sp, 1200, 720);

        stage.setTitle("Admin Panel - Events");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setMinWidth(1100);
        stage.setMinHeight(650);
        stage.show();
    }
}