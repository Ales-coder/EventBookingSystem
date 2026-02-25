package com.selenium;

import com.selenium.model.User;
import com.selenium.payment.MockPaymentService;
import com.selenium.payment.PaymentService;
import com.selenium.ui.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {



    private static User currentUser;
    private static Stage primaryStage;


    private static volatile boolean securityBlocked = false;


    private static PaymentService paymentService = new MockPaymentService();

    private static final String APP_CSS = "/styles/app.css";



    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }



    public static void setSecurityBlocked(boolean blocked) {
        securityBlocked = blocked;
    }

    public static boolean isActionsBlocked() {
        return securityBlocked;
    }



    public static PaymentService getPaymentService() {
        return paymentService;
    }

    public static void setPaymentService(PaymentService ps) {
        if (ps != null) paymentService = ps;
    }



    public static void applyAppCss(Scene scene) {
        if (scene == null) return;

        try {
            URL url = Main.class.getResource(APP_CSS);
            if (url == null) {
                System.out.println("⚠ CSS not found at " + APP_CSS);
                return;
            }

            String css = url.toExternalForm();

            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }

        } catch (Exception ex) {
            System.out.println("⚠ CSS load failed: " + ex.getMessage());
        }
    }



    private static void forceFullScreenFit(Stage stage) {
        if (stage == null) return;

        Rectangle2D vb = Screen.getPrimary().getVisualBounds();

        stage.setX(vb.getMinX());
        stage.setY(vb.getMinY());
        stage.setWidth(vb.getWidth());
        stage.setHeight(vb.getHeight());
        stage.setResizable(true);

        Platform.runLater(() -> {
            stage.setMaximized(false);
            stage.setMaximized(true);
        });
    }

    public static void setupStage(Stage stage, Scene scene, String title) {
        applyAppCss(scene);

        stage.setTitle(title);
        stage.setScene(scene);

        if (!stage.isShowing()) {
            stage.show();
        } else {
            stage.requestFocus();
        }

        forceFullScreenFit(stage);
    }



    public static void showEventsView() {
        new EventsView().show(primaryStage);
    }

    public static void showBookingHistoryView() {

        if (currentUser == null) {
            new LoginView().show(primaryStage, user -> {
                setCurrentUser(user);
                showEventsView();
            });
            return;
        }

        new BookingHistoryView().show(primaryStage, currentUser, Main::showEventsView);
    }

    public static void showAdminEventView() {

        if (currentUser == null ||
                !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {

            new Alert(Alert.AlertType.ERROR, "Admin only.")
                    .showAndWait();

            showEventsView();
            return;
        }

        new AdminEventView().show(primaryStage, Main::showEventsView);
    }

    public static void showSecurityLogView() {

        if (currentUser == null ||
                !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {

            new Alert(Alert.AlertType.ERROR, "Admin only.")
                    .showAndWait();

            showEventsView();
            return;
        }

        new SecurityLogView().show(primaryStage, Main::showEventsView);
    }

    public static void showSecurityDashboardView() {

        if (currentUser == null ||
                !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {

            new Alert(Alert.AlertType.ERROR, "Admin only.")
                    .showAndWait();

            showEventsView();
            return;
        }

        new SecurityDashboardView().show(primaryStage, Main::showEventsView);
    }

    public static void logout() {

        currentUser = null;
        securityBlocked = false;

        new LoginView().show(primaryStage, user -> {
            setCurrentUser(user);
            showEventsView();
        });
    }



    @Override
    public void start(Stage stage) {

        primaryStage = stage;

        primaryStage.sceneProperty().addListener((obs, oldS, newS) -> {
            applyAppCss(newS);
            forceFullScreenFit(primaryStage);
        });

        new LoginView().show(primaryStage, user -> {
            setCurrentUser(user);
            showEventsView();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}