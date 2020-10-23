import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

class EternalObfuscator {

    static int undefFilesCounter = 0;

    synchronized static void obpIL32(HashSet<String> mainObfSet, Map<String, String> mainObfMap) {
        Random r;
        HashSet<String> encryptedWordsForMap = new HashSet<>();

        while (encryptedWordsForMap.size() != mainObfSet.size()) {
            String temp = "";

            for (int i = 0; i < 32; i++) {
                // Рандомизация числа от 0 до 1
                r = new Random();
                int v = r.nextInt(2);
                temp += v == 0 ? "I" : "l";
            }

            encryptedWordsForMap.add(temp);
        }

        Iterator iteratorKeys = mainObfSet.iterator(),
                iteratorValues = encryptedWordsForMap.iterator();

        while (iteratorKeys.hasNext() && iteratorValues.hasNext()) {
            mainObfMap.put(iteratorKeys.next().toString(), iteratorValues.next().toString());
        }
    }

    synchronized static void changeFormBindings(ArrayList<String> filesToObfuscate, Map<String, String> mainObfMap) {

        try {

            for (String s : filesToObfuscate) {

                String fName = s.substring(s.lastIndexOf("\\") + 1);
                String nameOnly = fName.substring(0, fName.lastIndexOf("."));
                Main.gui.getStatusLabel().setText("Текущий файл: " + fName);

                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(s), "UTF-8"));
                StringBuilder builder = new StringBuilder();
                String newLine;

                while ((newLine = br.readLine()) != null)
                    builder.append(newLine).append(System.lineSeparator());

                String code = builder.toString();
                br.close();

                for (Map.Entry<String, String> map : mainObfMap.entrySet()) {

                    if (code.contains("binding=\"" + map.getKey() + "\"")) {
                        code = code.replaceAll("binding=\"" + map.getKey() + "\"", "binding=\"" + map.getValue() + "\"");
                    }
                }

                if (code.contains("bind-to-class=\"" + nameOnly + "\"")) {
                    code = code.replaceAll("bind-to-class=\"" + nameOnly + "\"", "bind-to-class=\"" + mainObfMap.get(nameOnly) + "\"");
                }

                String fileDir = s.substring(0, s.lastIndexOf("\\") + 1);
                String fileName = s.substring(s.lastIndexOf("\\") + 1, s.lastIndexOf("."));
                String fileFormat = s.substring(s.lastIndexOf("."));
                Files.createDirectories(Paths.get(fileDir + "Eternal_Obfuscator\\"));

                String toFName;
                if (mainObfMap.get(fileName) != null)
                    toFName = mainObfMap.get(fileName);
                else toFName = "Undef_Class_Name_" + (++undefFilesCounter);

                BufferedWriter bw = Files.newBufferedWriter(Paths.get(fileDir + "Eternal_Obfuscator\\" + toFName + fileFormat));
                bw.write(code); // Завершение процесса обфускации и сохранение в новый файл
                bw.flush();
                bw.close();

                EGUI.pbCurrentVal += EGUI.dividerLength;
                Main.gui.getProgressBar1().setValue(EGUI.pbCurrentVal);
            }

            Main.gui.getProgressBar1().setValue(Main.gui.getProgressBar1().getMaximum());
            Main.gui.getStatusLabel().setText("Обфускация успешно завершена");

        } catch (FileNotFoundException exc) {
            JOptionPane.showMessageDialog(null, "Во время обфускации (form bindings) изначальный файл не был найден!");
            exc.printStackTrace();
        } catch (IOException exc) {
            JOptionPane.showMessageDialog(null, "Возникла ошибка IOException во время обфускации (form bindings)!");
            exc.printStackTrace();
        }

    }

    synchronized static void obfuscate(ArrayList<String> filesToObfuscate, Map<String, String> mainObfMap) {

        try {
            for (String s : filesToObfuscate) {

                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(s), "UTF-8"));
                StringBuilder builder = new StringBuilder();
                String newLine;


                while ((newLine = br.readLine()) != null) {
                    builder.append(newLine).append(System.lineSeparator());
                }

                br.close();
                String tempCode = builder.toString();

                /*
                        Шифрование константных строк (обязетельно выполнять перед обфускацией переменных)
                 */

                ArrayList<String> staticStrList = new ArrayList<>();
                if (Main.gui.getStringCipherCBox().getSelectedIndex() != 0) {
                    staticStrList = new ArrayList<>();
                    Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.findStaticStrings, staticStrList);
                }

                switch (Main.gui.getStringCipherCBox().getSelectedIndex()) {
                    case 0:
                        /*
                                Без шифрования
                         */
                        break;

                    case 1: // ASCII

                        ArrayList<String> c_code = new ArrayList<>();
                        Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.saveCodeIdxWithStr, c_code);

                        if (staticStrList.size() > 0) {

                            int index = 0;
                            int offset = 0;
                            for (String str : staticStrList) {
                                if (Collections.frequency(Main.gui.blacklistStr, "\"" + str + "\"") == 0) { // Проверка на BL
                                    if (str.length() > 0) {

                                        boolean isInCCodeRange;
                                        isInCCodeRange = false;
                                        index = tempCode.indexOf("\"" + str + "\"", index);

                                        for (int i = 0; i < c_code.size() - 1; i += 2) {
                                            boolean c1 = index >= (Integer.parseInt(c_code.get(i)) + offset);
                                            boolean c2 = index <= (Integer.parseInt(c_code.get(i + 1)) + offset);
                                            if (c1 && c2) {
                                                isInCCodeRange = true;
                                                break;
                                            }
                                        }

                                        if (isInCCodeRange) {
                                            int tempCodeLt = tempCode.length();

                                            String def_str = str;
                                            str = staticStrToDefaultView(str);
                                            char[] strArray = str.toCharArray();
                                            String result = "";
                                            for (char c : strArray)
                                                result += (int) c + " ";
                                            result = result.substring(0, result.length() - 1);

                                                /*
                                                        Можно изменить статичное назвение EternalObfuscatorStringsReader на динамичное,
                                                        для удобства пользователей
                                                */

                                            tempCode = tempCode.substring(0, index)
                                                    + "EternalObfuscatorStringsReader.asciiToStr(\""
                                                    + result
                                                    + "\")"
                                                    + tempCode.substring((index + def_str.length() + 2), tempCode.length());

                                            offset += tempCode.length() - tempCodeLt;
                                        }

                                        index++;
                                    }
                                }
                            }
                        }

                        break;

                    case 2: // Base64

                        ArrayList<String> c_code_b64 = new ArrayList<>();
                        Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.saveCodeIdxWithStr, c_code_b64);

                        if (staticStrList.size() > 0) {

                            int index = 0;
                            int offset = 0;
                            for (String str : staticStrList) {
                                if (Collections.frequency(Main.gui.blacklistStr, "\"" + str + "\"") == 0) { // Проверка на BL
                                    if (str.length() > 0) {

                                        boolean isInCCodeRange;
                                        isInCCodeRange = false;
                                        index = tempCode.indexOf("\"" + str + "\"", index);

                                        for (int i = 0; i < c_code_b64.size() - 1; i += 2) {
                                            boolean c1 = index >= (Integer.parseInt(c_code_b64.get(i)) + offset);
                                            boolean c2 = index <= (Integer.parseInt(c_code_b64.get(i + 1)) + offset);
                                            if (c1 && c2) {
                                                isInCCodeRange = true;
                                                break;
                                            }
                                        }

                                        if (isInCCodeRange) {
                                            int tempCodeLt = tempCode.length();

                                            String result;
                                            String def_str = str;
                                            str = staticStrToDefaultView(str);
                                            try {
                                                result = staticStrToBase64(str);
                                            } catch (UnsupportedEncodingException exc) {
                                                exc.printStackTrace();
                                                result = str;
                                            }

                                                /*
                                                        Можно изменить статическое назвение EternalObfuscatorStringsReader на динамичнеское,
                                                        для удобства пользователей
                                                */

                                            tempCode = tempCode.substring(0, index)
                                                    + "EternalObfuscatorStringsReader.base64ToStr(\""
                                                    + result
                                                    + "\")"
                                                    + tempCode.substring((index + def_str.length() + 2), tempCode.length());

                                            offset += tempCode.length() - tempCodeLt;
                                        }

                                        index++;
                                    }
                                }
                            }
                        }

                        break;

                    default:
                        break;
                }

                /*
                        Список для исключения возможной ошибки в обфускации переменных
                        Обфускация переменных
                */

                ArrayList<String> codeRange = new ArrayList<>();
                Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.saveCodeIdx, codeRange);

                for (Map.Entry<String, String> map : mainObfMap.entrySet()) {
                    int kwIdx = 0;

                    while (kwIdx <= tempCode.length()) {

                        if (tempCode.contains(map.getKey()) && kwIdx + 1 < tempCode.lastIndexOf(map.getKey())) {

                            kwIdx = tempCode.indexOf(map.getKey(), kwIdx + 1);
                            String substrToCheck = tempCode.substring(kwIdx - 1, kwIdx + map.getKey().length() + 1);

                            if (!substrToCheck.substring(0, 1).matches("[a-zA-Z0-9_$]")
                                    && !substrToCheck.substring(substrToCheck.length() - 1, substrToCheck.length()).matches("[a-zA-Z0-9_$]")) {

                                boolean isAllowedScope = false;
                                for (int i = 0; i < codeRange.size() - 1; i += 2) {
                                    if (kwIdx >= Integer.parseInt(codeRange.get(i))
                                            && kwIdx <= Integer.parseInt(codeRange.get(i + 1))) {
                                        isAllowedScope = true;
                                        break;
                                    }
                                }
                                if (!isAllowedScope) continue;
                                if (tempCode.substring(kwIdx - 1, kwIdx).equals("'") && map.getKey().length() == 1) continue; // Если char-const

                                /*
                                        Изменение индексов, соответственно к коду
                                 */

                                int tmpLength = tempCode.length();
                                tempCode = tempCode.substring(0, kwIdx)
                                        + substrToCheck.substring(1, substrToCheck.length() - 1).replace(map.getKey(), map.getValue())
                                        + tempCode.substring(kwIdx + map.getKey().length());
                                int change = tempCode.length() - tmpLength;

                                for (int i = 0; i < codeRange.size(); i++) {
                                    if (Integer.parseInt(codeRange.get(i)) > kwIdx)
                                        codeRange.set(i, String.valueOf(Integer.parseInt(codeRange.get(i)) + change));
                                }

                            }
                        } else break;
                    }
                }

                /*
                        Удаление строк консольного вывода
                 */
                if (Main.gui.getConsoleLogDelCB().isSelected()) {
                    tempCode = Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.removeConsoleLogs);
                }

                /*
                        Удаление комментариев с исходного кода
                 */
                if (Main.gui.getCmtsDeleteCB().isSelected()) {
                    tempCode = Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.removeComments);
                }

                /*
                        Удаление структур с исключениями
                 */
                if (Main.gui.getExceptionsDelCB().isSelected()) {
                    int c_len = 1;
                    int c_after_len = 0;

                    /*
                            Удаление всех уровней try
                     */

                    while (c_len != c_after_len) {
                        c_len = tempCode.length();
                        tempCode = Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.removeExceptionsStruct);
                        c_after_len = tempCode.length();
                    }
                }

                /*
                       Повторное удаление комментариев с исходного кода
                 */

                if (Main.gui.getCmtsDeleteCB().isSelected()) {
                    tempCode = Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.removeComments);
                }

                /*
                        Сохранение обфусцированного кода в файл
                 */

                String fileDir = s.substring(0, s.lastIndexOf("\\") + 1);
                String fileName = s.substring(s.lastIndexOf("\\") + 1, s.lastIndexOf("."));
                String fileFormat = s.substring(s.lastIndexOf("."));
                Files.createDirectories(Paths.get(fileDir + "Eternal_Obfuscator\\"));

                String toFName;
                if (mainObfMap.get(fileName) != null) {
                    toFName = mainObfMap.get(fileName);
                }
                else if (tempCode.contains("public static void main")) {
                    ArrayList<String> c_code_new = new ArrayList<>();
                    Main.gui.dp.codeMainParser(tempCode, DataParser.ParserMode.saveCodeIdx, c_code_new);

                    boolean isOnClearRegion = false;
                    for (int i = 0; i < c_code_new.size() - 1; i+=2) {
                        if (tempCode.indexOf("public static void main") >= Integer.parseInt(c_code_new.get(i))
                                && tempCode.indexOf("public static void main") <= Integer.parseInt(c_code_new.get(i + 1)))
                            isOnClearRegion = true;
                    }

                    if (isOnClearRegion) toFName = "Main";
                    else toFName = "Undef_Class_Name_" + (++undefFilesCounter);
                }
                else {
                    toFName = "Undef_Class_Name_" + (++undefFilesCounter);
                }

                BufferedWriter bw = Files.newBufferedWriter(Paths.get(fileDir + "Eternal_Obfuscator\\" + toFName + fileFormat));
                bw.write(tempCode); // Завершение процесса обфускации и сохранение в новый файл
                bw.flush();
                bw.close();
            }
        } catch (FileNotFoundException exc) {
            JOptionPane.showMessageDialog(null, "Під час обфускації обраний файл не був знайдений!");
            exc.printStackTrace();
        } catch (IOException exc) {
            JOptionPane.showMessageDialog(null, "Помилка IOException під час обфускації!");
            exc.printStackTrace();
        }

    }

    private static String staticStrToDefaultView(String str) {

        while (str.contains("\\" + "r" + "\\" + "n"))
            str = str.replace("\\" + "r" + "\\" + "n", System.lineSeparator());

        while (str.contains("\\" + "n"))
            str = str.replace("\\" + "n", System.lineSeparator());

        while (str.contains("\\" + "r"))
            str = str.replace("\\" + "r", System.lineSeparator());

        while (str.contains("\\" + "t"))
            str = str.replace("\\" + "t", "\t");

        Scanner s = new Scanner(str);
        s.useDelimiter("");
        String currentChar;
        StringBuilder result = new StringBuilder();

        while (s.hasNext()) {

            currentChar = s.next();

            if (currentChar.equals("\\")) {

                String nextChar = s.next();
                result.append(nextChar);

            } else {

                result.append(currentChar);
            }

        }

        return result.toString();
    }

    private static String staticStrToBase64(String str) throws UnsupportedEncodingException {
        return Base64.getEncoder().encodeToString(str.getBytes("UTF-8"));
    }

    void checkOnStaticStringV1(String tempCode, int kwIdx) {

        /*
                Данный метод не проверялся и был написан как один из вариантов для проверки на нахождение названия в строке,
                алгоритм в таком виде должен находится в главной функции по обфускации
         */

        while (kwIdx <= tempCode.length()) {
            int leftBorder, rightBorder;
            if (tempCode.indexOf(System.lineSeparator(), kwIdx) != -1)
                rightBorder = tempCode.indexOf(System.lineSeparator(), kwIdx);
            else rightBorder = tempCode.length();

            StringBuilder stringBuilder = new StringBuilder().append(tempCode).reverse();
            String tempCodeReversed = stringBuilder.toString();
            int kwIdxReversed = tempCode.length() - kwIdx;
            if (tempCodeReversed.indexOf(System.lineSeparator(), kwIdxReversed) != -1)
                leftBorder = tempCode.length() - tempCodeReversed.indexOf(System.lineSeparator(), kwIdxReversed);
            else leftBorder = 0;

            int i;
            ArrayList<String> constStrPos = new ArrayList<>();
            boolean flip_flop = false;
            String str = "";
            while (leftBorder < rightBorder) {
                if ((i = tempCode.indexOf("\"", leftBorder)) != -1) {
                    if (!tempCode.substring(i - 1, i).contains("\\")) {
                        leftBorder = i + 1;

                        if (!flip_flop) {
                            str = i + " ";
                            flip_flop = !flip_flop;
                        } else {
                            constStrPos.add(str + (i + 1));
                            flip_flop = !flip_flop;
                        }
                    }
                } else break;
            }

            boolean isInConstStr = false;
            for (String string : constStrPos) {
                if (kwIdx >= Integer.parseInt(string.split(" ")[0])
                        && kwIdx <= Integer.parseInt(string.split(" ")[1]))
                    isInConstStr = true;
            }

            if (isInConstStr) {
                continue;
            }
            /*
                    Место для обфускации через replace
             */
        }
    }
}