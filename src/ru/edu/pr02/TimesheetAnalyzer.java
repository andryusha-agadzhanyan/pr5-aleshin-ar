package ru.edu.pr02;

public class TimesheetAnalyzer {
    public static int[] parseHours(String line) {
        if (line == null || line.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ошибка: структура -- строка отсутствует или пуста");
        }

        String[] parts = line.split(";", -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                    "Ошибка: структура -- ожидалось 5 значений, получено "
                            + parts.length);
        }

        int[] hours = new int[5];
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("^(0|[1-9][0-9]*)$")) {
                throw new IllegalArgumentException(
                        "Ошибка: позиция " + (i + 1)
                                + " -- не целое число без знака и ведущих нулей");
            }

            int value;
            try {
                value = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                // Формат уже проверен: слишком длинное число вне диапазона.
                throw new IllegalArgumentException(
                        "Ошибка: позиция " + (i + 1)
                                + " -- значение вне диапазона [0, 16]");
            }

            if (value < 0 || value > 16) {
                throw new IllegalArgumentException(
                        "Ошибка: позиция " + (i + 1)
                                + " -- значение вне диапазона [0, 16]");
            }
            hours[i] = value;
        }
        return hours;
    }

    public static int total(int[] hours) {
        checkArray(hours);
        int sum = 0;
        for (int i = 0; i < hours.length; i++) {
            sum += hours[i];
        }
        return sum;
    }

    public static double average(int[] hours) {
        double mean = (double) total(hours) / hours.length;
        return Math.round(mean * 100) / 100.0;
    }

    public static int overtimeDays(int[] hours) {
        checkArray(hours);
        int count = 0;
        for (int i = 0; i < hours.length; i++) {
            if (hours[i] > 8) {
                count++;
            }
        }
        return count;
    }

    public static int weeklyOvertime(int[] hours) {
        // Вариант 3, часть А: недоработка до нормы 40 часов.
        return Math.max(0, 40 - total(hours));
    }

    public static int addRestHours(int[] hours) {
        // Вариант 3, часть Б: формула совпадает с частью А.
        return weeklyOvertime(hours);
    }

    private static void checkArray(int[] hours) {
        if (hours == null || hours.length != 5) {
            throw new IllegalArgumentException(
                    "Ошибка: структура -- нужен массив из 5 значений");
        }
    }
}
