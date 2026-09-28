package com.example;

public final class DamageHealthEstimatorTestTodoAi {
    public static void main(String[] args) {
        check(7, DamageHealthEstimatorTodoAi.hitsToDefeat(3.0F), "three damage per hit");
        check(1, DamageHealthEstimatorTodoAi.hitsToDefeat(20.0F), "lethal hit");
        check(0, DamageHealthEstimatorTodoAi.hitsToDefeat(0.0F), "no damage");
        check(0, DamageHealthEstimatorTodoAi.hitsToDefeat(-1.0F), "negative damage");
        System.out.println("PASS: damage health estimator hit calculations");
    }

    private static void check(int expected, int actual, String scenario) {
        if (expected != actual) {
            throw new AssertionError(scenario + ": expected " + expected + ", got " + actual);
        }
    }
}
