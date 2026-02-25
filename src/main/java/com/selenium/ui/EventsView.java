package com.selenium.ui;

import com.selenium.Main;
import com.selenium.dao.EventDAO;
import com.selenium.model.Event;
import com.selenium.model.User;
import com.selenium.search.LuceneEventIndexer;
import com.selenium.search.LuceneSearchService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EventsView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void show(Stage stage) {

        User currentUser = Main.getCurrentUser();
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());

        EventDAO dao = new EventDAO();


        TextField searchField = new TextField();
        searchField.setPromptText("Search events (Lucene)...");

        Button searchBtn = new Button("Search");
        Button refreshBtn = new Button("Refresh");


        Button myBookingsBtn = new Button("My Bookings");
        Button adminBtn = new Button("Admin Panel");
        Button securityBtn = new Button("Security Dashboard");
        Button logoutBtn = new Button("Logout");

        adminBtn.setVisible(isAdmin);
        adminBtn.setManaged(isAdmin);

        securityBtn.setVisible(isAdmin);
        securityBtn.setManaged(isAdmin);

        Label msg = new Label();


        final List<Event>[] allEventsRef = new List[]{List.of()};
        final List<Event>[] recEventsRef = new List[]{List.of()};
        final LuceneSearchService[] searchServiceRef = new LuceneSearchService[1];


        VBox content = new VBox(18);
        content.setPadding(new Insets(14));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);


        Runnable buildIndex = () -> {
            try {
                LuceneEventIndexer indexer = new LuceneEventIndexer();
                indexer.indexEvents(allEventsRef[0]);
                searchServiceRef[0] = new LuceneSearchService(indexer.getDirectory(), indexer.getAnalyzer());
            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Lucene error: " + ex.getMessage());
            }
        };


        Runnable loadAllEvents = () -> {
            try {
                List<Event> allEvents = dao.getActiveEvents();
                allEventsRef[0] = allEvents;
                buildIndex.run();
                msg.setText("Loaded " + allEvents.size() + " active event(s).");
            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error loading events: " + ex.getMessage());
            }
        };


        Runnable loadRecommendations = () -> {
            try {
                User u = Main.getCurrentUser();
                if (u == null) {
                    recEventsRef[0] = List.of();
                    return;
                }
                recEventsRef[0] = dao.getRecommendedEventsForUser(u.getUserId(), 10);
            } catch (Exception ex) {
                ex.printStackTrace();
                recEventsRef[0] = List.of();
            }
        };


        Runnable renderHome = () -> {
            content.getChildren().clear();


            if (Main.getCurrentUser() != null) {
                List<Event> rec = recEventsRef[0] == null ? List.of() : recEventsRef[0];
                content.getChildren().add(buildRecommendationsSection("⭐ Recommended for you", rec, stage));
            } else {
                Label l = new Label("Login to see recommendations.");
                l.setStyle("-fx-opacity: 0.85;");
                content.getChildren().add(l);
            }


            List<Event> events = allEventsRef[0] == null ? List.of() : allEventsRef[0];

            Map<String, List<Event>> byCategory = events.stream()
                    .collect(Collectors.groupingBy(
                            e -> safeString(e, "getCategory", "category", "UNKNOWN"),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<String> preferredOrder = List.of("SPORT", "THEATRE", "CINEMA", "MUSIC", "BOOK", "WORKSHOP", "CONFERENCE", "FOOD", "SECURITY");
            List<String> keys = new ArrayList<>(byCategory.keySet());
            keys.sort(Comparator.comparingInt(k -> {
                int idx = preferredOrder.indexOf(k.toUpperCase());
                return idx == -1 ? 999 : idx;
            }));

            for (String cat : keys) {
                List<Event> list = byCategory.getOrDefault(cat, List.of());
                content.getChildren().add(buildSection("🎫 " + prettyCat(cat), list, stage));
            }

            scroll.setVvalue(0);
        };


        java.util.function.BiConsumer<String, List<Event>> renderSearchOnly = (query, results) -> {
            content.getChildren().clear();

            Label title = new Label("🔎 Search results for: " + query);
            title.setStyle("""
                -fx-font-size: 20px;
                -fx-font-weight: 900;
                -fx-padding: 6 0 0 0;
            """);

            if (results == null || results.isEmpty()) {
                Label empty = new Label("No events matched your search.");
                empty.setStyle("-fx-opacity: 0.80;");
                content.getChildren().addAll(title, empty);
            } else {
                Map<String, List<Event>> byCategory = results.stream()
                        .collect(Collectors.groupingBy(
                                e -> safeString(e, "getCategory", "category", "UNKNOWN"),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

                VBox box = new VBox(12);
                for (var entry : byCategory.entrySet()) {
                    box.getChildren().add(buildSection("🎫 " + prettyCat(entry.getKey()), entry.getValue(), stage));
                }
                content.getChildren().addAll(title, box);
            }

            scroll.setVvalue(0);
        };


        Runnable doSearch = () -> {
            try {
                String q = searchField.getText();

                if (q == null || q.isBlank()) {
                    renderHome.run();
                    msg.setText("Showing all active events.");
                    return;
                }

                if (searchServiceRef[0] == null) {
                    renderHome.run();
                    msg.setText("Search not ready, showing all active events.");
                    return;
                }

                List<Long> matchedIds = searchServiceRef[0].searchEventIds(q);

                List<Event> filtered = allEventsRef[0].stream()
                        .filter(ev -> matchedIds.contains(ev.getEventId()))
                        .collect(Collectors.toList());

                renderSearchOnly.accept(q.trim(), filtered);
                msg.setText("Found " + filtered.size() + " event(s).");

            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Search error: " + ex.getMessage());
            }
        };


        Runnable doRefresh = () -> {
            searchField.clear();
            loadAllEvents.run();
            loadRecommendations.run();
            renderHome.run();
            msg.setText("Refreshed events & recommendations.");
        };


        loadAllEvents.run();
        loadRecommendations.run();
        renderHome.run();

        myBookingsBtn.setOnAction(e -> Main.showBookingHistoryView());
        logoutBtn.setOnAction(e -> Main.logout());

        adminBtn.setOnAction(e -> Main.showAdminEventView());
        securityBtn.setOnAction(e -> Main.showSecurityDashboardView());


        searchBtn.setOnAction(e -> doSearch.run());
        searchField.setOnAction(e -> doSearch.run());


        refreshBtn.setOnAction(e -> doRefresh.run());


        HBox topBar = new HBox(10,
                searchField, searchBtn, refreshBtn,
                myBookingsBtn, adminBtn, securityBtn, logoutBtn
        );
        topBar.setPadding(new Insets(10));

        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setMaxWidth(Double.MAX_VALUE);

        VBox root = new VBox(10, topBar, scroll, msg);
        root.setPadding(new Insets(10));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Scene scene = new Scene(root, 1200, 720);
        Main.setupStage(stage, scene, "Ticket Booking System - Events");
    }

    private VBox buildRecommendationsSection(String titleText, List<Event> events, Stage stage) {
        Label title = new Label(titleText);
        title.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: 900;
            -fx-padding: 6 0 0 0;
        """);

        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(14));
        wrapper.setStyle("""
            -fx-background-color: rgba(90,160,255,0.10);
            -fx-background-radius: 18;
            -fx-border-radius: 18;
            -fx-border-color: rgba(160,210,255,0.20);
        """);

        FlowPane grid = new FlowPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPrefWrapLength(1100);

        if (events == null || events.isEmpty()) {
            Label empty = new Label("No recommendations yet (book some events first).");
            empty.setStyle("-fx-opacity: 0.75;");
            wrapper.getChildren().addAll(title, empty);
            return wrapper;
        }

        List<Event> top = events.size() > 6 ? events.subList(0, 6) : events;
        for (Event e : top) {
            grid.getChildren().add(buildRecommendationCard(e, stage));
        }

        Label hint = new Label("Tip: these are personalized based on your booking history.");
        hint.setStyle("-fx-opacity: 0.75; -fx-font-size: 12px;");

        wrapper.getChildren().addAll(title, grid, hint);
        return wrapper;
    }

    private Region buildRecommendationCard(Event event, Stage stage) {

        StackPane card = new StackPane();
        card.setPrefWidth(360);
        card.setMinWidth(320);
        card.setMaxWidth(420);
        card.setPadding(new Insets(16));

        card.setStyle("""
            -fx-background-color:
                linear-gradient(to bottom right, rgba(90,160,255,0.22), rgba(15,25,50,0.65));
            -fx-background-radius: 18;
            -fx-border-radius: 18;
            -fx-border-color: rgba(200,230,255,0.40);
        """);

        String cat = safeString(event, "getCategory", "category", "UNKNOWN");

        Label watermark = new Label("⭐");
        watermark.setStyle("""
            -fx-font-size: 72px;
            -fx-opacity: 0.10;
        """);
        StackPane.setAlignment(watermark, Pos.CENTER_RIGHT);
        StackPane.setMargin(watermark, new Insets(0, 10, 0, 0));

        Label watermark2 = new Label(iconForCategory(cat));
        watermark2.setStyle("""
            -fx-font-size: 56px;
            -fx-opacity: 0.08;
        """);
        StackPane.setAlignment(watermark2, Pos.CENTER_LEFT);
        StackPane.setMargin(watermark2, new Insets(0, 0, 0, 10));

        Label chip = new Label("RECOMMENDED");
        chip.setStyle("""
            -fx-font-size: 11px;
            -fx-font-weight: 900;
            -fx-text-fill: rgba(255,255,255,0.95);
            -fx-background-color: rgba(120,190,255,0.22);
            -fx-background-radius: 999;
            -fx-border-radius: 999;
            -fx-border-color: rgba(200,230,255,0.30);
            -fx-padding: 4 10;
        """);

        String title = safeString(event, "getTitle", "title", "Untitled");
        String desc = safeString(event, "getDescription", "getDesc", "getDetails", "");
        String time = safeStartTime(event);

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("""
            -fx-font-size: 16px;
            -fx-font-weight: 900;
        """);

        Label sub = new Label(prettyCat(cat) + "   •   🕒 " + time);
        sub.setStyle("-fx-opacity: 0.85;");

        Label descLabel = new Label(shorten(desc, 90));
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-opacity: 0.85;");

        Label hint = new Label("Double-click to choose seats →");
        hint.setStyle("-fx-opacity: 0.75; -fx-font-size: 12px;");

        VBox body = new VBox(8, chip, titleLabel, sub, descLabel, hint);

        BorderPane bp = new BorderPane();
        bp.setCenter(body);

        DropShadow normal = new DropShadow(26, Color.rgb(120, 210, 255, 0.28));
        normal.setOffsetY(8);

        DropShadow hover = new DropShadow(34, Color.rgb(180, 240, 255, 0.35));
        hover.setOffsetY(12);

        card.setEffect(normal);

        card.setOnMouseEntered(e -> {
            card.setTranslateY(-5);
            card.setEffect(hover);
            card.setStyle("""
                -fx-background-color:
                    linear-gradient(to bottom right, rgba(120,190,255,0.28), rgba(18,32,70,0.72));
                -fx-background-radius: 18;
                -fx-border-radius: 18;
                -fx-border-color: rgba(220,245,255,0.55);
            """);
        });

        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setEffect(normal);
            card.setStyle("""
                -fx-background-color:
                    linear-gradient(to bottom right, rgba(90,160,255,0.22), rgba(15,25,50,0.65));
                -fx-background-radius: 18;
                -fx-border-radius: 18;
                -fx-border-color: rgba(200,230,255,0.40);
            """);
        });

        card.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                User u = Main.getCurrentUser();
                if (u == null) {
                    new Alert(Alert.AlertType.ERROR, "Please login first.").showAndWait();
                    return;
                }
                new SeatSelectionView().show(stage, u, event, () -> show(stage));
            }
        });

        card.getChildren().addAll(bp, watermark, watermark2);
        return card;
    }


    private VBox buildSection(String titleText, List<Event> events, Stage stage) {
        Label title = new Label(titleText);
        title.setStyle("""
            -fx-font-size: 18px;
            -fx-font-weight: 800;
            -fx-padding: 6 0 0 0;
        """);

        FlowPane grid = new FlowPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(6, 0, 0, 0));
        grid.setPrefWrapLength(1100);

        if (events == null || events.isEmpty()) {
            Label empty = new Label("No events here yet.");
            empty.setStyle("-fx-opacity: 0.75;");
            return new VBox(6, title, empty);
        }

        for (Event e : events) {
            grid.getChildren().add(buildTicketCard(e, stage));
        }

        return new VBox(8, title, grid);
    }

    private Region buildTicketCard(Event event, Stage stage) {

        StackPane card = new StackPane();
        card.setPrefWidth(360);
        card.setMinWidth(320);
        card.setMaxWidth(420);

        card.setPadding(new Insets(16));
        card.setStyle("""
            -fx-background-color: rgba(15, 25, 50, 0.72);
            -fx-background-radius: 18;
            -fx-border-radius: 18;
            -fx-border-color: rgba(120,190,255,0.40);
        """);

        Label watermark = new Label(iconForCategory(safeString(event, "getCategory", "category", "")));
        watermark.setStyle("""
            -fx-font-size: 64px;
            -fx-opacity: 0.10;
        """);
        StackPane.setAlignment(watermark, Pos.CENTER_RIGHT);
        StackPane.setMargin(watermark, new Insets(0, 10, 0, 0));

        String title = safeString(event, "getTitle", "title", "Untitled");
        String cat = safeString(event, "getCategory", "category", "UNKNOWN");
        String desc = safeString(event, "getDescription", "getDesc", "getDetails", "");
        String time = safeStartTime(event);

        Label catLabel = new Label(prettyCat(cat));
        catLabel.setStyle("""
            -fx-font-size: 12px;
            -fx-font-weight: 800;
            -fx-text-fill: rgba(210,230,255,0.85);
            -fx-background-color: rgba(90,160,255,0.15);
            -fx-background-radius: 10;
            -fx-padding: 4 10;
            -fx-border-color: rgba(120,190,255,0.20);
            -fx-border-radius: 10;
        """);

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("""
            -fx-font-size: 16px;
            -fx-font-weight: 900;
        """);

        Label timeLabel = new Label("🕒 " + time);
        timeLabel.setStyle("-fx-opacity: 0.85;");

        Label descLabel = new Label(shorten(desc, 90));
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-opacity: 0.85;");

        VBox text = new VBox(8, catLabel, titleLabel, timeLabel, descLabel);

        Label hint = new Label("Double-click to choose seats →");
        hint.setStyle("-fx-opacity: 0.75; -fx-font-size: 12px;");

        VBox body = new VBox(10, text, hint);

        BorderPane bp = new BorderPane();
        bp.setCenter(body);

        DropShadow normal = new DropShadow(18, Color.rgb(90, 160, 255, 0.18));
        normal.setOffsetY(6);

        DropShadow hover = new DropShadow(26, Color.rgb(120, 210, 255, 0.28));
        hover.setOffsetY(10);

        card.setEffect(normal);

        card.setOnMouseEntered(e -> {
            card.setTranslateY(-4);
            card.setEffect(hover);
            card.setStyle("""
                -fx-background-color: rgba(18, 32, 70, 0.78);
                -fx-background-radius: 18;
                -fx-border-radius: 18;
                -fx-border-color: rgba(160,210,255,0.55);
            """);
        });

        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setEffect(normal);
            card.setStyle("""
                -fx-background-color: rgba(15, 25, 50, 0.72);
                -fx-background-radius: 18;
                -fx-border-radius: 18;
                -fx-border-color: rgba(120,190,255,0.40);
            """);
        });

        card.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                User u = Main.getCurrentUser();
                if (u == null) {
                    new Alert(Alert.AlertType.ERROR, "Please login first.").showAndWait();
                    return;
                }
                new SeatSelectionView().show(stage, u, event, () -> show(stage));
            }
        });

        card.getChildren().addAll(bp, watermark);
        return card;
    }


    private String safeStartTime(Event event) {
        try {
            Object v = tryInvoke(event, "getStartTime");
            if (v == null) v = tryInvoke(event, "getStart");
            if (v == null) v = tryInvoke(event, "getStart_time");

            if (v == null) return "TBA";

            if (v instanceof Timestamp ts) {
                LocalDateTime ldt = ts.toLocalDateTime();
                return FMT.format(ldt);
            }
            if (v instanceof LocalDateTime ldt) {
                return FMT.format(ldt);
            }
            return String.valueOf(v);

        } catch (Exception e) {
            return "TBA";
        }
    }

    private String safeString(Object obj, String method1, String method2, String fallback) {
        return safeString(obj, method1, method2, null, fallback);
    }

    private String safeString(Object obj, String method1, String method2, String method3, String fallback) {
        try {
            Object v = tryInvoke(obj, method1);
            if (v == null && method2 != null) v = tryInvoke(obj, method2);
            if (v == null && method3 != null) v = tryInvoke(obj, method3);
            if (v == null) return fallback;
            String s = String.valueOf(v);
            return s == null ? fallback : s;
        } catch (Exception e) {
            return fallback;
        }
    }

    private Object tryInvoke(Object obj, String methodName) {
        if (obj == null || methodName == null) return null;
        try {
            Method m = obj.getClass().getMethod(methodName);
            return m.invoke(obj);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        String x = s.trim();
        if (x.length() <= max) return x;
        return x.substring(0, max).trim() + "…";
    }

    private String prettyCat(String cat) {
        if (cat == null) return "Unknown";
        String c = cat.trim().toUpperCase();
        return switch (c) {
            case "SPORT" -> "Sport";
            case "THEATRE" -> "Theatre";
            case "CINEMA" -> "Cinema";
            case "MUSIC" -> "Music";
            case "BOOK" -> "Book / Literature";
            case "WORKSHOP" -> "Workshop";
            case "CONFERENCE" -> "Conference";
            case "FOOD" -> "Food";
            default -> c.charAt(0) + c.substring(1).toLowerCase();
        };
    }

    private String iconForCategory(String cat) {
        if (cat == null) return "🎟";
        String c = cat.trim().toUpperCase();
        return switch (c) {
            case "CINEMA" -> "🎥";
            case "THEATRE" -> "🎭";
            case "MUSIC" -> "🎶";
            case "SPORT" -> "🏟";
            case "CONFERENCE" -> "🎤";
            case "WORKSHOP" -> "🛠";
            case "BOOK" -> "📚";
            case "FOOD" -> "🍔";
            default -> "🎟";
        };
    }
}