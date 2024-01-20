package net.mine_diver.magnummerge.test;

public class OriginalCode {
    public static void main(String[] args) {
        System.out.println("Hello, world!");

        int a = 0;
        int b = 1;
        for (int i = 0; i < 10; i++) {
            int c = a + b;
            a = b;
            b = c;
            System.out.println(c);
        }
    }
}
