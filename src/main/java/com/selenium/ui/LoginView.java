package com.selenium.ui;

import com.selenium.Main;
import com.selenium.dao.SecurityLogDAO;
import com.selenium.dao.UserDAO;
import com.selenium.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class LoginView {


    private static final int FAIL_THRESHOLD = 3;
    private static final int FAIL_WINDOW_MINUTES = 60;

    public void show(Stage stage, java.util.function.Consumer<User> onLoginSuccess) {

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full name (for register)");

        Label msg = new Label();

        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Register");

        Label title = new Label("🎟 Event Booking System");
        title.setStyle("""
                    -fx-font-size: 28px;
                    -fx-font-weight: 800;
                    -fx-text-fill: #EAF2FF;
                """);

        HBox btnRow = new HBox(15, loginBtn, registerBtn);
        btnRow.setAlignment(Pos.CENTER);

        VBox form = new VBox(15,
                title,
                emailField,
                passwordField,
                new Separator(),
                fullNameField,
                btnRow,
                msg
        );

        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(36));
        form.setMaxWidth(480);

        form.setStyle("""
                    -fx-background-color: rgba(15, 25, 50, 0.72);
                    -fx-background-radius: 20;
                    -fx-border-radius: 20;
                    -fx-border-color: rgba(120,190,255,0.45);
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 26, 0.30, 0, 8);
                """);

        StackPane root = new StackPane();
        root.setAlignment(Pos.CENTER);

        Region bg = new Region();
        bg.setStyle("""
                    -fx-background-color:
                        radial-gradient(radius 120%, rgba(90,160,255,0.22), transparent),
                        linear-gradient(to bottom right, #050b18, #081a33, #02050d);
                """);

        Region ticketWatermark = createTicketWatermark();

        root.getChildren().addAll(bg, ticketWatermark, form);

        bg.prefWidthProperty().bind(root.widthProperty());
        bg.prefHeightProperty().bind(root.heightProperty());

        ticketWatermark.prefWidthProperty().bind(root.widthProperty());
        ticketWatermark.prefHeightProperty().bind(root.heightProperty());

        UserDAO userDao = new UserDAO();
        SecurityLogDAO logDao = new SecurityLogDAO();

        loginBtn.setOnAction(e -> {
            String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
            String pass = passwordField.getText() == null ? "" : passwordField.getText();

            try {
                if (email.isBlank() || pass.isBlank()) {
                    msg.setText("Fill email + password.");
                    return;
                }

                Optional<User> opt = userDao.findByEmail(email);
                if (opt.isEmpty()) {
                    logDao.log("WARN", "LOGIN_FAIL", null, email, "User not found");
                    msg.setText("User not found.");
                    return;
                }

                User u = opt.get();

                if (!BCrypt.checkpw(pass, u.getPasswordHash())) {

                    logDao.log("WARN", "LOGIN_FAIL", u.getUserId(), email, "Wrong password");
                    msg.setText("Wrong password.");
                    return;
                }


                logDao.log("INFO", "LOGIN_OK", u.getUserId(), email, "Login successful");


                int recentFails = logDao.countByEmailAndAction(email, "LOGIN_FAIL", FAIL_WINDOW_MINUTES);
                boolean blocked = recentFails >= FAIL_THRESHOLD;

                Main.setSecurityBlocked(blocked);

                if (blocked) {
                    logDao.log("WARN", "SECURITY_BLOCK", u.getUserId(), email,
                            "Blocked bookings/payments due to repeated login failures (fails=" + recentFails + ", window=" + FAIL_WINDOW_MINUTES + "m)");
                    msg.setText("✅ Login successful (Restricted: bookings/payments blocked).");
                } else {
                    msg.setText("✅ Login successful!");
                }

                onLoginSuccess.accept(u);

            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error: " + ex.getMessage());
            }
        });

        registerBtn.setOnAction(e -> {
            String fullName = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
            String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
            String pass = passwordField.getText() == null ? "" : passwordField.getText();

            try {
                if (fullName.isBlank()) {
                    msg.setText("Full name required.");
                    return;
                }
                if (email.isBlank() || pass.isBlank()) {
                    msg.setText("Fill all fields.");
                    return;
                }
                if (userDao.findByEmail(email).isPresent()) {
                    msg.setText("Email already exists.");
                    return;
                }

                String hash = BCrypt.hashpw(pass, BCrypt.gensalt(12));
                User u = userDao.register(fullName, email, hash, "CUSTOMER");


                logDao.log("INFO", "REGISTER", u.getUserId(), email, "User registered");


                Main.setSecurityBlocked(false);

                msg.setText("✅ Registered! Logging in...");
                onLoginSuccess.accept(u);

            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error: " + ex.getMessage());
            }
        });

        Scene scene = new Scene(root, 1100, 760);
        Main.applyAppCss(scene);

        stage.setTitle("Login - Ticket Booking System");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }


    private Region createTicketWatermark() {
        Pane layer = new Pane();
        layer.setPickOnBounds(false);

        double W = 900;
        double H = 300;

        Rectangle body = new Rectangle(W, H);
        body.setArcWidth(40);
        body.setArcHeight(40);

        Circle leftNotch = new Circle(0, H / 2.0, 38);
        Circle rightNotch = new Circle(W, H / 2.0, 38);

        Shape ticket = Shape.subtract(body, leftNotch);
        ticket = Shape.subtract(ticket, rightNotch);

        ticket.setFill(new javafx.scene.paint.LinearGradient(
                0, 0, 1, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.00, Color.rgb(190, 230, 255, 0.08)),
                new javafx.scene.paint.Stop(0.45, Color.rgb(120, 190, 255, 0.05)),
                new javafx.scene.paint.Stop(1.00, Color.rgb(40, 90, 160, 0.07))
        ));

        ticket.setStroke(Color.rgb(170, 225, 255, 0.14));
        ticket.setStrokeWidth(2);

        Rectangle inner = new Rectangle(28, 24, W - 56, H - 48);
        inner.setArcWidth(26);
        inner.setArcHeight(26);
        inner.setFill(Color.rgb(255, 255, 255, 0.04));
        inner.setStroke(Color.rgb(210, 245, 255, 0.08));
        inner.setStrokeWidth(1.4);

        Pane perforation = new Pane();
        perforation.setPickOnBounds(false);

        double perfX = W * 0.62;
        double perfTop = 45;
        double perfBottom = H - 45;

        for (double y = perfTop; y <= perfBottom; y += 14) {
            Circle dot = new Circle(perfX, y, 2.2);
            dot.setFill(Color.rgb(235, 250, 255, 0.10));
            perforation.getChildren().add(dot);
        }

        Rectangle barcodeBg = new Rectangle(perfX + 22, 70, W - (perfX + 22) - 60, 110);
        barcodeBg.setArcWidth(18);
        barcodeBg.setArcHeight(18);
        barcodeBg.setFill(Color.rgb(0, 0, 0, 0.08));
        barcodeBg.setStroke(Color.rgb(220, 245, 255, 0.08));
        barcodeBg.setStrokeWidth(1);

        Pane barcode = new Pane();
        barcode.setPickOnBounds(false);

        double bx = barcodeBg.getX() + 18;
        double by = barcodeBg.getY() + 18;
        double bw = barcodeBg.getWidth() - 36;
        double bh = barcodeBg.getHeight() - 36;

        double x = bx;
        int i = 0;
        while (x < bx + bw) {
            double barW = (i % 7 == 0) ? 3.2 : (i % 3 == 0 ? 2.2 : 1.6);
            Rectangle bar = new Rectangle(x, by, barW, bh);
            bar.setFill(Color.rgb(235, 250, 255, 0.10));
            barcode.getChildren().add(bar);
            x += barW + 2.4;
            i++;
        }

        Label eventLbl = new Label("EVENT");
        eventLbl.setStyle("""
            -fx-font-size: 12px;
            -fx-font-weight: 800;
            -fx-text-fill: rgba(234,242,255,0.35);
            -fx-letter-spacing: 2px;
        """);

        Label titleLbl = new Label("TICKET");
        titleLbl.setStyle("""
            -fx-font-size: 36px;
            -fx-font-weight: 900;
            -fx-text-fill: rgba(234,242,255,0.20);
            -fx-letter-spacing: 3px;
        """);

        Label admitLbl = new Label("ADMIT ONE");
        admitLbl.setStyle("""
            -fx-font-size: 13px;
            -fx-font-weight: 800;
            -fx-text-fill: rgba(234,242,255,0.26);
        """);

        Label seatLbl = new Label("SEAT  •  ROW  •  TIME");
        seatLbl.setStyle("""
            -fx-font-size: 12px;
            -fx-font-weight: 700;
            -fx-text-fill: rgba(234,242,255,0.20);
        """);

        VBox leftText = new VBox(8, eventLbl, titleLbl, admitLbl, seatLbl);
        leftText.setPickOnBounds(false);
        leftText.setAlignment(Pos.TOP_LEFT);
        leftText.setLayoutX(58);
        leftText.setLayoutY(68);

        Rectangle shine = new Rectangle(-120, 0, 200, H + 80);
        shine.setRotate(-20);
        shine.setFill(new javafx.scene.paint.LinearGradient(
                0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.0, Color.rgb(255, 255, 255, 0.00)),
                new javafx.scene.paint.Stop(0.5, Color.rgb(255, 255, 255, 0.05)),
                new javafx.scene.paint.Stop(1.0, Color.rgb(255, 255, 255, 0.00))
        ));
        shine.setLayoutX(W * 0.12);
        shine.setLayoutY(-30);

        DropShadow soft = new DropShadow();
        soft.setRadius(26);
        soft.setSpread(0.10);
        soft.setOffsetY(10);
        soft.setColor(Color.rgb(0, 0, 0, 0.35));
        ticket.setEffect(soft);

        layer.setEffect(new GaussianBlur(0.85));

        Pane ticketGroup = new Pane();
        ticketGroup.setPickOnBounds(false);
        ticketGroup.setManaged(false);

        ticketGroup.getChildren().addAll(ticket, inner, perforation, barcodeBg, barcode, shine, leftText);
        ticketGroup.setRotate(-8);

        layer.widthProperty().addListener((obs, o, n) -> {
            double w = n.doubleValue();
            double scale = clamp(w / 1500.0, 0.78, 1.22);
            ticketGroup.setScaleX(scale);
            ticketGroup.setScaleY(scale);
            centerTicket(layer, ticketGroup, W, H);
        });

        layer.heightProperty().addListener((obs, o, n) -> centerTicket(layer, ticketGroup, W, H));
        layer.getChildren().add(ticketGroup);

        return new Region() {{
            StackPane wrapper = new StackPane(layer);
            wrapper.setPickOnBounds(false);
            wrapper.prefWidthProperty().bind(prefWidthProperty());
            wrapper.prefHeightProperty().bind(prefHeightProperty());
            getChildren().add(wrapper);
        }};
    }

    private void centerTicket(Pane layer, Pane ticketGroup, double W, double H) {
        double lw = layer.getWidth();
        double lh = layer.getHeight();
        if (lw <= 0 || lh <= 0) return;

        double x = (lw - W) / 2.0;
        double y = (lh - H) / 2.0;

        ticketGroup.setLayoutX(x);
        ticketGroup.setLayoutY(y - 30);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}