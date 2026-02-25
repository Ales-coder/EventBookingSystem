package com.selenium.ui;

import com.selenium.Main;
import com.selenium.payment.PaymentService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

public class PaymentDialog {

    public static PaymentService.PaymentResult show(String provider, BigDecimal amount) {

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Secure Checkout");

        Label title = new Label("💳 Secure Checkout");
        title.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: 900;
                -fx-text-fill: #EAF2FF;
        """);

        Label sub = new Label("Provider: " + provider + "   •   Amount: €" + amount);
        sub.setStyle("-fx-text-fill: rgba(234,242,255,0.75); -fx-font-weight: 700;");

        TextField nameField = new TextField();
        nameField.setPromptText("Cardholder name (e.g., Alesia Gjeta)");

        TextField cardField = new TextField();
        cardField.setPromptText("Card number (16 digits) e.g., 4242 4242 4242 4242");

        TextField expiryField = new TextField();
        expiryField.setPromptText("Expiry (MM/YY) e.g., 08/27");

        PasswordField cvvField = new PasswordField();
        cvvField.setPromptText("CVV (3-4 digits) e.g., 123");


        cardField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            String digits = newV.replaceAll("\\D", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) sb.append(' ');
                sb.append(digits.charAt(i));
            }
            String formatted = sb.toString();
            if (!formatted.equals(newV)) cardField.setText(formatted);
        });


        expiryField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            String digits = newV.replaceAll("\\D", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);

            String formatted = (digits.length() <= 2) ? digits : digits.substring(0, 2) + "/" + digits.substring(2);
            if (!formatted.equals(newV)) expiryField.setText(formatted);
        });


        cvvField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            String digits = newV.replaceAll("\\D", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            if (!digits.equals(newV)) cvvField.setText(digits);
        });

        Label msg = new Label();
        msg.setStyle("-fx-text-fill: rgba(255,190,190,0.95); -fx-font-weight: 700;");

        ProgressIndicator loader = new ProgressIndicator();
        loader.setVisible(false);
        loader.setMaxSize(42, 42);

        Label dots = new Label();
        dots.setVisible(false);
        dots.setStyle("-fx-text-fill: rgba(234,242,255,0.75); -fx-font-weight: 700;");

        Button payBtn = new Button("Pay Now");
        Button cancelBtn = new Button("Cancel");

        String INPUT = """
                -fx-background-radius: 12;
                -fx-border-radius: 12;
                -fx-padding: 10 12;
                -fx-control-inner-background: rgba(10, 18, 36, 0.90);
                -fx-background-color: rgba(10, 18, 36, 0.90);
                -fx-border-color: rgba(140, 190, 255, 0.30);
                -fx-text-fill: #eaf2ff;
                -fx-prompt-text-fill: rgba(210, 230, 255, 0.45);
        """;

        String BTN = """
                -fx-background-radius: 12;
                -fx-border-radius: 12;
                -fx-padding: 10 18;
                -fx-background-color: linear-gradient(to bottom, rgba(70,120,255,0.42), rgba(70,120,255,0.18));
                -fx-border-color: rgba(120,170,255,0.55);
                -fx-text-fill: #eef5ff;
                -fx-font-weight: 800;
        """;

        nameField.setStyle(INPUT);
        cardField.setStyle(INPUT);
        expiryField.setStyle(INPUT);
        cvvField.setStyle(INPUT);

        payBtn.setStyle(BTN);
        cancelBtn.setStyle(BTN + "-fx-opacity: 0.92;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(140);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        int r = 0;
        grid.add(styledKeyLabel("Name:"), 0, r);   grid.add(nameField, 1, r++);
        grid.add(styledKeyLabel("Card #:"), 0, r); grid.add(cardField, 1, r++);
        grid.add(styledKeyLabel("Expiry:"), 0, r); grid.add(expiryField, 1, r++);
        grid.add(styledKeyLabel("CVV:"), 0, r);    grid.add(cvvField, 1, r++);

        HBox buttons = new HBox(12, payBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(14, title, sub, new Separator(), grid, msg, new HBox(10, loader, dots), buttons);
        card.setPadding(new Insets(20));
        card.setMaxWidth(560);

        card.setStyle("""
                -fx-background-color: rgba(15, 25, 50, 0.85);
                -fx-background-radius: 18;
                -fx-border-radius: 18;
                -fx-border-color: rgba(120,190,255,0.45);
        """);

        DropShadow ds = new DropShadow(28, Color.rgb(0, 0, 0, 0.55));
        ds.setOffsetY(10);
        card.setEffect(ds);


        card.setOnMouseEntered(e -> {
            card.setTranslateY(-2);
            card.setRotate(-0.6);
        });
        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setRotate(0);
        });

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(18));
        root.setStyle("""
                -fx-background-color:
                    radial-gradient(radius 120%, rgba(90,160,255,0.18), transparent),
                    linear-gradient(to bottom right, #050b18, #081a33, #02050d);
        """);

        Scene scene = new Scene(root, 720, 490);
        Main.applyAppCss(scene);

        final PaymentService.PaymentResult[] result =
                { new PaymentService.PaymentResult(false, "Cancelled", null) };


        Timeline dotsAnim = new Timeline(
                new KeyFrame(Duration.ZERO, ev -> dots.setText("Processing")),
                new KeyFrame(Duration.millis(350), ev -> dots.setText("Processing.")),
                new KeyFrame(Duration.millis(700), ev -> dots.setText("Processing..")),
                new KeyFrame(Duration.millis(1050), ev -> dots.setText("Processing..."))
        );
        dotsAnim.setCycleCount(Animation.INDEFINITE);

        payBtn.setOnAction(e -> {

            String name = safe(nameField.getText());
            String cardNo = safe(cardField.getText()).replaceAll("\\s+", "");
            String exp = safe(expiryField.getText());
            String cvv = safe(cvvField.getText());

            String err = validate(name, cardNo, exp, cvv);
            if (err != null) {
                msg.setStyle("-fx-text-fill: rgba(255,190,190,0.95); -fx-font-weight: 700;");
                msg.setText("❌ " + err);
                return;
            }

            setBusy(true, payBtn, cancelBtn, loader, dots, dotsAnim);
            msg.setStyle("-fx-text-fill: rgba(234,242,255,0.80); -fx-font-weight: 700;");
            msg.setText("🔒 Securing payment...");


            Task<PaymentService.PaymentResult> task = new Task<>() {
                @Override
                protected PaymentService.PaymentResult call() {

                    sleepSilently(1200 + ThreadLocalRandom.current().nextInt(900));

                    if (ThreadLocalRandom.current().nextInt(100) < 3) {
                        return new PaymentService.PaymentResult(false, "Bank declined (simulated 3%). Try again.", "DECLINED");
                    }

                    PaymentService.PaymentRequest req = new PaymentService.PaymentRequest(
                            provider, amount, name, cardNo, exp, cvv
                    );

                    return Main.getPaymentService().pay(req);
                }
            };

            task.setOnSucceeded(ok -> {
                PaymentService.PaymentResult pr = task.getValue();
                result[0] = pr;

                if (pr != null && pr.approved()) {
                    successAnim(card, msg, loader, dots, dotsAnim);
                    PauseTransition closeSoon = new PauseTransition(Duration.millis(650));
                    closeSoon.setOnFinished(x -> dialog.close());
                    closeSoon.play();
                } else {
                    setBusy(false, payBtn, cancelBtn, loader, dots, dotsAnim);
                    msg.setStyle("-fx-text-fill: rgba(255,190,190,0.95); -fx-font-weight: 700;");
                    msg.setText("❌ " + (pr == null ? "Payment failed." : pr.message()));
                    shake(card);
                }
            });

            task.setOnFailed(fail -> {
                setBusy(false, payBtn, cancelBtn, loader, dots, dotsAnim);
                msg.setStyle("-fx-text-fill: rgba(255,190,190,0.95); -fx-font-weight: 700;");
                msg.setText("❌ Payment error: " + (task.getException() == null ? "Unknown" : task.getException().getMessage()));
                shake(card);
            });

            Thread t = new Thread(task, "payment-task");
            t.setDaemon(true);
            t.start();
        });

        cancelBtn.setOnAction(e -> {
            result[0] = new PaymentService.PaymentResult(false, "Cancelled", null);
            dialog.close();
        });

        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }

    private static Label styledKeyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#eaf2ff; -fx-font-weight:800;");
        return l;
    }

    private static void setBusy(boolean busy, Button payBtn, Button cancelBtn,
                                ProgressIndicator loader, Label dots, Timeline dotsAnim) {
        payBtn.setDisable(busy);
        cancelBtn.setDisable(busy);
        loader.setVisible(busy);
        dots.setVisible(busy);
        if (busy) dotsAnim.play();
        else dotsAnim.stop();
    }

    private static void successAnim(VBox card, Label msg,
                                    ProgressIndicator loader, Label dots, Timeline dotsAnim) {

        dotsAnim.stop();
        loader.setVisible(false);
        dots.setVisible(false);

        msg.setStyle("-fx-text-fill: rgba(170,255,190,0.98); -fx-font-weight: 900;");
        msg.setText("✅ Payment approved!");

        DropShadow greenGlow = new DropShadow(34, Color.rgb(80, 255, 140, 0.35));
        greenGlow.setOffsetY(10);

        Timeline glow = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(card.effectProperty(), card.getEffect())),
                new KeyFrame(Duration.millis(180), new KeyValue(card.effectProperty(), greenGlow))
        );
        glow.setAutoReverse(true);
        glow.setCycleCount(2);
        glow.play();
    }

    private static void shake(Region node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    private static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String validate(String name, String cardNo, String exp, String cvv) {
        if (name.isBlank()) return "Cardholder name is required.";
        if (!name.matches("[A-Za-zÀ-ž\\s.'-]{2,60}")) return "Name format looks invalid.";

        if (!cardNo.matches("\\d{16}")) return "Card number must be 16 digits.";

        if (!luhn(cardNo)) return "Card number failed validation.";

        if (!exp.matches("\\d{2}/\\d{2}")) return "Expiry must be MM/YY.";
        int mm = Integer.parseInt(exp.substring(0, 2));
        int yy = Integer.parseInt(exp.substring(3, 5));
        if (mm < 1 || mm > 12) return "Expiry month must be 01-12.";

        int year = 2000 + yy;
        java.time.YearMonth now = java.time.YearMonth.now();
        java.time.YearMonth given = java.time.YearMonth.of(year, mm);
        if (given.isBefore(now)) return "Card is expired.";

        if (!cvv.matches("\\d{3,4}")) return "CVV must be 3 or 4 digits.";
        return null;
    }

    private static boolean luhn(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }
}