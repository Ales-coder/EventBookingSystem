package com.selenium.Hash;

import org.mindrot.jbcrypt.BCrypt;

public class HashTest {
    public static void main(String[] args) {
        String hash = BCrypt.hashpw("amore7", BCrypt.gensalt(12));
        System.out.println(hash);
    }
}
