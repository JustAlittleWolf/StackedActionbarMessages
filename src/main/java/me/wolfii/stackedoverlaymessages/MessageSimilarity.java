package me.wolfii.stackedoverlaymessages;

public final class MessageSimilarity {
    private MessageSimilarity() {
    }

    public static boolean matches(String left, String right, int requiredPercent) {
        if (requiredPercent <= 0) {
            return true;
        }
        int required = Math.min(requiredPercent, 100);
        return similarity(left, right) * 100.0 >= required;
    }

    static double similarity(String left, String right) {
        if (left.equals(right)) {
            return 1.0;
        }
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 1.0;
        }
        return 1.0 - (double) levenshtein(left, right) / maxLength;
    }

    private static int levenshtein(String left, String right) {
        int leftLength = left.length();
        int rightLength = right.length();
        if (leftLength == 0) {
            return rightLength;
        }
        if (rightLength == 0) {
            return leftLength;
        }

        int[] previous = new int[rightLength + 1];
        int[] current = new int[rightLength + 1];
        for (int j = 0; j <= rightLength; j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= leftLength; i++) {
            current[0] = i;
            char leftChar = left.charAt(i - 1);
            for (int j = 1; j <= rightLength; j++) {
                int cost = leftChar == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[rightLength];
    }
}
