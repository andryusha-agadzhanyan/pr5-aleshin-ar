package ru.edu.pr02;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class SelfCheck {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--interactive")) {
            runInteractive();
            return;
        }

        Path file = Path.of("data", "sample.csv");
        if (args.length == 2 && args[0].equals("--data")) {
            file = Path.of(args[1]);
        } else if (args.length != 0) {
            System.err.println("Запуск: SelfCheck [--interactive | --data путь.csv]");
            System.exit(2);
        }

        System.out.println("PR5 / variant 3");
        System.out.println("weeklyOvertime = max(0, 40 - total)");
        runUnitChecks();
        int unitPassed = passed;
        int unitFailed = failed;
        System.out.printf("UNIT: %d PASS, %d FAIL%n", unitPassed, unitFailed);

        try {
            runDataChecks(file);
        } catch (IOException e) {
            check("data/file", false, e.getMessage());
        }

        System.out.printf("DATA: %d PASS, %d FAIL%n",
                passed - unitPassed, failed - unitFailed);
        System.out.printf("TOTAL: %d PASS, %d FAIL%n", passed, failed);
        if (failed != 0) {
            System.exit(1);
        }
    }

    private static void runUnitChecks() {
        // 19 обязательных проверок из методички.
        check("unit/01 parse valid", Arrays.equals(
                TimesheetAnalyzer.parseHours("8;9;10;8;8"),
                new int[]{8, 9, 10, 8, 8}), "массив часов");
        expectParseError("unit/02 empty", "", "структура");
        expectParseError("unit/03 value 17", "8;8;17;8;8",
                "позиция 3 -- значение вне диапазона");
        expectParseError("unit/04 text", "8;abc;8;8;8", "позиция 2 -- не целое");
        expectParseError("unit/05 four values", "8;8;8;8", "структура");
        expectParseError("unit/06 trailing separator", "8;8;8;8;8;", "структура");
        expectParseError("unit/07 leading zero", "08;8;8;8;8", "позиция 1 -- не целое");

        check("unit/08 total typical",
                TimesheetAnalyzer.total(new int[]{8, 9, 10, 8, 8}) == 43, "43");
        check("unit/09 total minimum",
                TimesheetAnalyzer.total(new int[]{0, 0, 0, 0, 0}) == 0, "0");
        check("unit/10 total maximum",
                TimesheetAnalyzer.total(new int[]{16, 16, 16, 16, 16}) == 80, "80");
        check("unit/11 average typical",
                TimesheetAnalyzer.average(new int[]{8, 9, 10, 8, 8}) == 8.6, "8.6");
        check("unit/12 average integer",
                TimesheetAnalyzer.average(new int[]{8, 8, 8, 8, 8}) == 8.0, "8.0");
        check("unit/13 average fraction",
                TimesheetAnalyzer.average(new int[]{7, 8, 9, 8, 7}) == 7.8, "7.8");
        check("unit/14 no overtime days",
                TimesheetAnalyzer.overtimeDays(new int[]{8, 8, 8, 8, 8}) == 0, "0");
        check("unit/15 two overtime days",
                TimesheetAnalyzer.overtimeDays(new int[]{9, 8, 7, 10, 8}) == 2, "2");
        check("unit/16 strict boundary 8",
                TimesheetAnalyzer.overtimeDays(new int[]{8, 9, 8, 8, 8}) == 1, "1");

        // Ожидания 17-19 изменены под вариант 3.
        check("unit/17 week total 40",
                TimesheetAnalyzer.weeklyOvertime(new int[]{8, 8, 8, 8, 8}) == 0, "0");
        check("unit/18 week total 50",
                TimesheetAnalyzer.weeklyOvertime(new int[]{10, 10, 10, 10, 10}) == 0, "0");
        check("unit/19 week total 30",
                TimesheetAnalyzer.weeklyOvertime(new int[]{6, 6, 6, 6, 6}) == 10, "10");

        // Часть А: две дополнительные проверки границы 40.
        check("unit/20 variant A total 39",
                TimesheetAnalyzer.weeklyOvertime(new int[]{7, 8, 8, 8, 8}) == 1, "1");
        check("unit/21 variant A total 41",
                TimesheetAnalyzer.weeklyOvertime(new int[]{9, 8, 8, 8, 8}) == 0, "0");
        // Часть Б: две дополнительные проверки нового метода.
        check("unit/22 variant B total 30",
                TimesheetAnalyzer.addRestHours(new int[]{6, 6, 6, 6, 6}) == 10, "10");
        check("unit/23 variant B total 50",
                TimesheetAnalyzer.addRestHours(new int[]{10, 10, 10, 10, 10}) == 0, "0");

        // Собственные проверки строгого формата и граничных случаев.
        check("extra/01 parse minimum", Arrays.equals(
                TimesheetAnalyzer.parseHours("0;0;0;0;0"),
                new int[]{0, 0, 0, 0, 0}), "нули допустимы");
        check("extra/02 parse maximum", Arrays.equals(
                TimesheetAnalyzer.parseHours("16;16;16;16;16"),
                new int[]{16, 16, 16, 16, 16}), "16 допустимо");
        expectParseError("extra/03 fraction", "8;8.5;8;8;8", "позиция 2 -- не целое");
        expectParseError("extra/04 negative", "8;-3;8;8;8", "позиция 2 -- не целое");
        expectParseError("extra/05 plus sign", "+8;8;8;8;8", "позиция 1 -- не целое");
        expectParseError("extra/06 spaces", "8; 9;10;8;8", "позиция 2 -- не целое");
        expectParseError("extra/07 empty field", "8;;9;10;8", "позиция 2 -- не целое");
        expectParseError("extra/08 fifth position", "8;8;8;8;abc", "позиция 5 -- не целое");
        expectParseError("extra/09 fail fast", "8;abc;17;8;8", "позиция 2 -- не целое");
        expectParseError("extra/10 integer overflow", "8;99999999999999999999;8;8;8",
                "позиция 2 -- значение вне диапазона");
        expectParseError("extra/11 six values", "8;8;8;8;8;8", "структура");
        expectParseError("extra/12 null line", null, "структура");
        check("extra/13 rest all zero",
                TimesheetAnalyzer.addRestHours(new int[]{0, 0, 0, 0, 0}) == 40, "40");
        check("extra/14 rest exact norm",
                TimesheetAnalyzer.addRestHours(new int[]{8, 8, 8, 8, 8}) == 0, "0");

        // Каждый вычислительный метод отвергает null и неверную длину.
        String[] methods = {"total", "average", "overtimeDays",
                "weeklyOvertime", "addRestHours"};
        for (int i = 0; i < methods.length; i++) {
            expectArrayError(methods[i], null);
            expectArrayError(methods[i], new int[]{8, 8, 8, 8});
        }
    }

    private static void check(String name, boolean ok, String details) {
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name + ": " + details);
        }
    }

    private static void expectParseError(String name, String input, String messagePart) {
        try {
            TimesheetAnalyzer.parseHours(input);
            check(name, false, "ожидалась IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            check(name, e.getClass() == IllegalArgumentException.class
                    && e.getMessage().contains(messagePart), e.toString());
        }
    }

    private static void expectArrayError(String method, int[] hours) {
        String name = "guard/" + method + (hours == null ? " null" : " length 4");
        try {
            switch (method) {
                case "total": TimesheetAnalyzer.total(hours); break;
                case "average": TimesheetAnalyzer.average(hours); break;
                case "overtimeDays": TimesheetAnalyzer.overtimeDays(hours); break;
                case "weeklyOvertime": TimesheetAnalyzer.weeklyOvertime(hours); break;
                case "addRestHours": TimesheetAnalyzer.addRestHours(hours); break;
                default: throw new IllegalStateException("Неизвестный метод проверки");
            }
            check(name, false, "ожидалась IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            check(name, e.getMessage().contains("структура"), e.toString());
        }
    }

    private static void runDataChecks(Path file) throws IOException {
        int number = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                number++;
                String name = "data/" + number;
                String[] parts = line.split("\\|", -1);
                if (parts.length != 3) {
                    check(name, false, "нужно 3 поля: вход|ожидание|комментарий");
                    continue;
                }

                String input = parts[0].equals("EMPTY") ? "" : parts[0];
                String expected = parts[1];
                if (!expected.startsWith("OK:") && !expected.startsWith("ERROR:")) {
                    check(name, false, "ожидание должно начинаться с OK: или ERROR:");
                    continue;
                }

                try {
                    int[] hours = TimesheetAnalyzer.parseHours(input);
                    String actual = resultLine(hours);
                    if (expected.contains(",rh=")) {
                        actual += ",rh=" + TimesheetAnalyzer.addRestHours(hours);
                    }
                    check(name, actual.equals(expected),
                            "ожидалось " + expected + ", получено " + actual);
                } catch (IllegalArgumentException e) {
                    boolean ok = expected.startsWith("ERROR:")
                            && e.getMessage().contains(expected.substring(6));
                    check(name, ok, "ожидалось " + expected + ", получено " + e.getMessage());
                }
            }
        }
        if (number == 0) {
            check("data/empty file", false, "в файле нет проверок");
        }
    }

    private static String resultLine(int[] hours) {
        return String.format(Locale.ROOT, "OK:sum=%d,avg=%.2f,od=%d,wo=%d",
                TimesheetAnalyzer.total(hours), TimesheetAnalyzer.average(hours),
                TimesheetAnalyzer.overtimeDays(hours), TimesheetAnalyzer.weeklyOvertime(hours));
    }

    private static void runInteractive() {
        System.out.println("Вариант 3: weeklyOvertime и restHours = недоработка до 40 часов.");
        System.out.println("Введите 5 чисел через ; без пробелов. Выход: exit или пустая строка.");
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.equals("exit") || line.isEmpty()) {
                    break;
                }
                try {
                    int[] hours = TimesheetAnalyzer.parseHours(line);
                    System.out.println(resultLine(hours)
                            + ",rh=" + TimesheetAnalyzer.addRestHours(hours));
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
