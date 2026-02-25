package com.selenium.security;

import com.selenium.dao.SecurityLogDAO;
import com.selenium.model.User;

import java.util.ArrayList;
import java.util.List;

public class FraudScoreService {

    public record FraudResult(int score, boolean blocked, String reason) {}

    private final SecurityLogDAO logDao = new SecurityLogDAO();

    private static final int BLOCK_THRESHOLD = 3;

    private static final int WIN_LOGIN_FAIL = 10;
    private static final int WIN_BOOK_FAIL  = 10;
    private static final int WIN_PAY_FAIL   = 10;
    private static final int WIN_BOOK_OK_FAST = 2;

    public FraudResult evaluateBooking(User user, long eventId, long seatId) {

        if (user == null) {
            return new FraudResult(3, true, "BLOCKED | No user context");
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();
        Long userId = user.getUserId();

        int loginFails = logDao.countByUserAndAction(userId, "LOGIN_FAIL", WIN_LOGIN_FAIL);
        if (loginFails >= 3) {
            score += 3;
            reasons.add("LOGIN_FAIL=" + loginFails + " in " + WIN_LOGIN_FAIL + "m");
        }

        int bookFails = logDao.countByUserAndAction(userId, "BOOK_FAIL", WIN_BOOK_FAIL);
        if (bookFails >= 2) {
            score += 1;
            reasons.add("BOOK_FAIL=" + bookFails + " in " + WIN_BOOK_FAIL + "m");
        }

        int bookOkFast = logDao.countByUserAndAction(userId, "BOOK_OK", WIN_BOOK_OK_FAST);
        if (bookOkFast >= 3) {
            score += 2;
            reasons.add("BOOK_OK fast=" + bookOkFast + " in " + WIN_BOOK_OK_FAST + "m");
        }

        boolean blocked = score >= BLOCK_THRESHOLD;

        String reasonBase = reasons.isEmpty() ? "OK" : String.join(" | ", reasons);
        String reason = blocked ? ("BLOCKED | " + reasonBase) : reasonBase;

        return new FraudResult(score, blocked, reason);
    }

    public FraudResult evaluatePayment(User user, long bookingId) {

        if (user == null) {
            return new FraudResult(3, true, "BLOCKED | No user context");
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();
        Long userId = user.getUserId();

        int loginFails = logDao.countByUserAndAction(userId, "LOGIN_FAIL", WIN_LOGIN_FAIL);
        if (loginFails >= 3) {
            score += 3;
            reasons.add("LOGIN_FAIL=" + loginFails + " in " + WIN_LOGIN_FAIL + "m");
        }

        int payFails = logDao.countByUserAndAction(userId, "PAY_FAIL", WIN_PAY_FAIL);
        if (payFails >= 2) {
            score += 1;
            reasons.add("PAY_FAIL=" + payFails + " in " + WIN_PAY_FAIL + "m");
        }

        boolean blocked = score >= BLOCK_THRESHOLD;

        String reasonBase = reasons.isEmpty() ? "OK" : String.join(" | ", reasons);
        String reason = blocked ? ("BLOCKED | " + reasonBase) : reasonBase;

        return new FraudResult(score, blocked, reason);
    }
}