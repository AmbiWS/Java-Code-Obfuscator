import java.util.*;

class DataParser {

    Map<String, String> mainObfMap = new HashMap<>();
    HashSet<String> mainObfSet = new HashSet<>();

    synchronized void formObfuscationMap(String inputData) {

        // Удаление комментариев и константных строк для удобной и быстрой обфускации
        inputData = removeConstStrAndComments(inputData);

        // Разделение оставшейся информации на отдельные слова (классы, переменные, методы)
        String[] data = splitData(inputData);

        // Удаление ненужных слов и дубликатов
        ArrayList<String> dataList = correctAndParse(data);

        // Заполнение списка слов, которые нужно обфусцировать
        mainObfSet.addAll(dataList);

        // Заполнение 'map' структуры, вида "слово - шифр. значение"
        EternalObfuscator.obpIL32(mainObfSet, mainObfMap);
    }

    private synchronized ArrayList<String> correctAndParse(String[] wordsArray) {
        ArrayList<String> list = new ArrayList<>();

        for (String s : wordsArray) {

            /*
                    Если не пустая ячейка, не конструкция с указателем,
                    такого слова не существует в списке, корректное название,
                    отсутствует в черном списке пользователя
             */

            if (s.length() >= 1
                    && !s.contains(".")
                    && Collections.frequency(list, s) < 1
                    && s.matches("^[a-zA-Z_$][a-zA-Z_$0-9]*$")
                    && (Collections.frequency(Main.gui.blacklist, s) < 1))
                list.add(s);
        }

        return list;
    }

    private synchronized String removeConstStrAndComments(String inputData) {
        return codeMainParser(codeMainParser(inputData, ParserMode.removeComments), ParserMode.removeStaticStrings);
    }

    private synchronized String[] splitData(String inputData) {

        /*
                Отделение символов от слов, удаление символов,
                ключевых слов, некоторых классов и прочих конструкций
         */

        for (int i = 0; i < Java8ClassesOperatorsCharacters.java8ClassesOperatorsCharactersArray.length; i++) {
            if (i <= 25) // Удаление символов, даже в случае принадлежности к строке
                inputData = inputData.replaceAll(Java8ClassesOperatorsCharacters.java8ClassesOperatorsCharactersArray[i],
                        " ");
            else
                inputData = inputData.replaceAll("\\b" + Java8ClassesOperatorsCharacters.java8ClassesOperatorsCharactersArray[i]
                        + "\\b", " ");
        }

        return inputData.split(" ");
    }

    enum ParserState {
        outsideComment, insideLineComment, insideblockComment, insideString, tryCatchFinallyStruct, systemLogs, tryKeyword
    }

    enum ParserMode {
        removeComments, removeExceptionsStruct, removeStaticStrings, removeConsoleLogs, saveCodeIdx, findStaticStrings, saveCodeIdxWithStr
    }

    void codeMainParser(String sourceCode, ParserMode mode, ArrayList<String> list) {

        /*
                Это перегруженный метод, подробное описание алгоритма его работы описано во втором методе
         */

        list.clear();
        String ls = System.lineSeparator();
        if (ls.length() == 2)
            ls = ls.substring(1, 2);

        ParserState state = ParserState.outsideComment;
        Scanner s = new Scanner(sourceCode);
        s.useDelimiter("");
        String previousStr = "";
        int index = 0;

        while (s.hasNext()) {

            String c = s.next();
            index++;

            switch (state) {

                case outsideComment:
                    if (c.equals("/") && s.hasNext()) {

                        String c2 = s.next();
                        index++;
                        switch (c2) {
                            case "/":
                                state = ParserState.insideLineComment;
                                if (list.size() > 0)
                                    if (mode == ParserMode.saveCodeIdx
                                            || mode == ParserMode.saveCodeIdxWithStr)
                                        list.add(String.valueOf(index - 2));
                                break;

                            case "*":
                                state = ParserState.insideblockComment;
                                if (list.size() > 0)
                                    if (mode == ParserMode.saveCodeIdx
                                            || mode == ParserMode.saveCodeIdxWithStr)
                                        list.add(String.valueOf(index - 2));
                                break;

                            default:
                                break;
                        }

                    } else {
                        if (c.equals("\"")) {
                            state = ParserState.insideString;
                            if (mode == ParserMode.saveCodeIdx)
                                list.add(String.valueOf(index - 1));
                        } else if (list.size() < 1) {
                            state = ParserState.outsideComment;
                            if (mode == ParserMode.saveCodeIdx
                                    || mode == ParserMode.saveCodeIdxWithStr)
                                list.add(String.valueOf(index - 1));
                        }
                    }
                    break;


                case insideString:

                    int counter = 0;
                    boolean isBreak = false;

                    if (c.equals("\\")) {

                        if (mode == ParserMode.findStaticStrings)
                            previousStr += c;

                        while (c.equals("\\")) {
                            counter++;
                            c = s.next();
                            index++;

                            switch (c) {
                                case "\"":
                                    if (counter % 2 == 0) {
                                        state = ParserState.outsideComment;
                                        if (mode == ParserMode.saveCodeIdx)
                                            list.add(String.valueOf(index));
                                        if (mode == ParserMode.findStaticStrings) {
                                            list.add(previousStr);
                                            previousStr = "";
                                        }

                                        isBreak = true;
                                    } else {
                                        if (mode == ParserMode.findStaticStrings)
                                            previousStr += c;
                                        isBreak = true;
                                    }
                                    break;

                                case "\\":
                                    if (mode == ParserMode.findStaticStrings)
                                        previousStr += c;
                                    continue;

                                default:
                                    if (mode == ParserMode.findStaticStrings)
                                        previousStr += c;
                                    isBreak = true;
                                    break;
                            }
                        }
                    } else {
                        if (mode == ParserMode.findStaticStrings)
                            previousStr += c;
                    }

                    if (isBreak) break;

                    if (c.equals("\"")) {
                        state = ParserState.outsideComment;
                        if (mode == ParserMode.saveCodeIdx)
                            list.add(String.valueOf(index));

                        if (mode == ParserMode.findStaticStrings) {
                            list.add(previousStr.substring(0, previousStr.length() - 1));
                            previousStr = "";
                        }
                    }
                    break;

                case insideLineComment:

                    if (c.equals(ls)) {
                        state = ParserState.outsideComment;
                        if (mode == ParserMode.saveCodeIdx
                                || mode == ParserMode.saveCodeIdxWithStr)
                            list.add(String.valueOf(index));
                    }
                    break;

                case insideblockComment:
                    if (c.equals("*") && s.hasNext()) {

                        String c2 = s.next();
                        index++;

                        if (c2.equals("/")) {
                            state = ParserState.outsideComment;
                            if (mode == ParserMode.saveCodeIdx
                                    || mode == ParserMode.saveCodeIdxWithStr)
                                list.add(String.valueOf(index));
                            break;
                        } else break;
                    }
                    break;
            }
        }

        if (list.size() == 0
                && mode == ParserMode.saveCodeIdxWithStr)
            list.add(String.valueOf(0));

        if (mode == ParserMode.saveCodeIdx
                || mode == ParserMode.saveCodeIdxWithStr) {
            list.add(String.valueOf(sourceCode.length()));
        }
    }

    private ParserState state, previousState, prevprevState;
    private StringBuilder result;
    private String currentChar;
    private ParserMode pmode;

    String codeMainParser(String sourceCode, ParserMode mode) {

        /*
                Удаление комментариев, в зависимости от текущего положения парсера в исходном коде
         */

        String ls = System.lineSeparator();
        if (ls.length() == 2)
            ls = ls.substring(1, 2);

        pmode = mode;
        state = ParserState.outsideComment;
        previousState = null;
        prevprevState = null;
        result = new StringBuilder();
        Scanner s = new Scanner(sourceCode);
        s.useDelimiter("");
        currentChar = "";
        boolean isOnTryCatchFinally = false;
        boolean isOnTryKeyword = false;
        int tryCatchFinallyCodeLevel = 0;
        int tryKeywordCodeLevel = 0;
        String previousChar;

        /*
                Пока парсер не дойдет до конца
         */
        while (s.hasNext()) {

            previousChar = currentChar;
            currentChar = s.next();

            switch (state) {

                /*
                        Вне структур с комметариями и статическими строками
                 */
                case outsideComment:
                    if (currentChar.equals("/") && s.hasNext()) {

                        String c2 = s.next();

                        /*
                                Поиск комментариев
                         */
                        switch (c2) {
                            case "/":
                                state = ParserState.insideLineComment;
                                previousState = ParserState.outsideComment;

                                if (mode != ParserMode.removeComments)
                                    result.append(currentChar).append(c2);

                                break;

                            case "*":
                                state = ParserState.insideblockComment;
                                previousState = ParserState.outsideComment;

                                if (mode != ParserMode.removeComments)
                                    result.append(currentChar).append(c2);

                                break;

                            default:

                                result.append(currentChar).append(c2);

                                break;
                        }

                    } else {

                        /*
                                Поиск статических строк
                         */
                        if (currentChar.equals("\"")) { // Парсер вошел в строку
                            state = ParserState.insideString;
                            previousState = ParserState.outsideComment;

                            if (mode != ParserMode.removeStaticStrings)
                                result.append(currentChar);

                            /*
                                    Проверка на 'try-catch-finally' и System.out.print...
                             */
                        } else if (!previousChar.matches("[a-zA-Z0-9_$]*")
                                && currentChar.equals("S")) {

                            checkOnSout(s, ParserState.outsideComment);

                        } else if (currentChar.equals("c") || currentChar.equals("f")
                                || currentChar.equals("t")) {

                            if (!previousChar.matches("[a-zA-Z0-9_$]*")) {

                                switch (currentChar) {

                                    case "t":

                                        currentChar = s.next();

                                        if (currentChar.equals("r")) {

                                            currentChar = s.next();

                                            if (currentChar.equals("y")) {

                                                currentChar = s.next();

                                                    /*
                                                            Является ли символ после try продолжением переменной
                                                    */
                                                if (!currentChar.matches("[a-zA-Z0-9_$]*")) {

                                                    state = ParserState.tryKeyword;

                                                    if (mode != ParserMode.removeExceptionsStruct)
                                                        result.append("try").append(currentChar);

                                                    break;

                                                } else {

                                                    result.append("try").append(currentChar);

                                                }

                                            } else {

                                                result.append("tr").append(currentChar);

                                            }

                                        } else {

                                            result.append("t").append(currentChar);

                                        }

                                        break;

                                    case "c":

                                        currentChar = s.next();

                                        if (currentChar.equals("a")) {

                                            currentChar = s.next();

                                            if (currentChar.equals("t")) {

                                                currentChar = s.next();

                                                if (currentChar.equals("c")) {

                                                    currentChar = s.next();

                                                    if (currentChar.equals("h")) {

                                                        currentChar = s.next();

                                                            /*
                                                                    Является ли символ после catch продолжением переменной
                                                             */
                                                        if (!currentChar.matches("[a-zA-Z0-9_$]*")) {

                                                            state = ParserState.tryCatchFinallyStruct;

                                                            if (mode != ParserMode.removeExceptionsStruct)
                                                                result.append("catch").append(currentChar);

                                                            break;

                                                        } else {

                                                            result.append("catch").append(currentChar);

                                                        }

                                                    } else {

                                                        result.append("catc").append(currentChar);

                                                    }

                                                } else {

                                                    result.append("cat").append(currentChar);

                                                }

                                            } else {

                                                result.append("ca").append(currentChar);

                                            }

                                        } else {

                                            result.append("c").append(currentChar);

                                        }

                                        break;

                                    case "f":

                                        currentChar = s.next();

                                        if (currentChar.equals("i")) {

                                            currentChar = s.next();

                                            if (currentChar.equals("n")) {

                                                currentChar = s.next();

                                                if (currentChar.equals("a")) {

                                                    currentChar = s.next();

                                                    if (currentChar.equals("l")) {

                                                        currentChar = s.next();

                                                        if (currentChar.equals("l")) {

                                                            currentChar = s.next();

                                                            if (currentChar.equals("y")) {

                                                                currentChar = s.next();

                                                                /*
                                                                        Является ли символ после finally продолжением переменной
                                                                */
                                                                if (!currentChar.matches("[a-zA-Z0-9_$]*")) {

                                                                    state = ParserState.tryCatchFinallyStruct;

                                                                    if (mode != ParserMode.removeExceptionsStruct)
                                                                        result.append("finally").append(currentChar);

                                                                    break;

                                                                } else {

                                                                    result.append("finally").append(currentChar);

                                                                }

                                                            } else {

                                                                result.append("finall").append(currentChar);

                                                            }

                                                        } else {

                                                            result.append("final").append(currentChar);

                                                        }

                                                    } else {

                                                        result.append("fina").append(currentChar);

                                                    }

                                                } else {

                                                    result.append("fin").append(currentChar);

                                                }

                                            } else {

                                                result.append("fi").append(currentChar);

                                            }

                                        } else {

                                            result.append("f").append(currentChar);

                                        }

                                        break;

                                    default:
                                        break;

                                }

                                /*
                                        Если перед ключевым словом или вызовом 'System' класса был найден символ от переменной
                                 */

                            } else {

                                result.append(currentChar);

                            }

                            /*
                                    Код
                             */

                        } else {

                            result.append(currentChar);

                        }
                    }

                    break;

                /*
                        Строка
                */
                case insideString:

                    if (mode != ParserMode.removeStaticStrings
                            && (previousState != ParserState.systemLogs || mode != ParserMode.removeConsoleLogs)
                            && (previousState != ParserState.tryCatchFinallyStruct || mode != ParserMode.removeExceptionsStruct))
                        result.append(currentChar);

                    int counter = 0;
                    boolean isBreak = false;

                    if (currentChar.equals("\\")) {

                        while (currentChar.equals("\\")) { // Избегание возможной ошибки на выход из строки

                            counter++;
                            currentChar = s.next();

                            boolean isAllowToAppend = mode != ParserMode.removeStaticStrings
                                    && (previousState != ParserState.systemLogs || mode != ParserMode.removeConsoleLogs)
                                    && (previousState != ParserState.tryCatchFinallyStruct || mode != ParserMode.removeExceptionsStruct);

                            if (currentChar.matches("[rnt]")
                                    && !isAllowToAppend) {

                                result = new StringBuilder().append(result.substring(0, result.length() - 1));

                            } else if (isAllowToAppend) {

                                result.append(currentChar);
                            }

                            switch (currentChar) {
                                case "\"":
                                    if (counter % 2 == 0) {
                                        state = previousState; // Прошлое местоположение, перед входом в строку
                                        isBreak = true;

                                    } else {

                                        isBreak = true;
                                    }
                                    break;

                                case "\\":

                                    continue;

                                default:

                                    isBreak = true;
                                    break;
                            }

                            if (isBreak) break;
                        }
                    }

                    if (isBreak) break;

                    if (currentChar.equals("\"")) {
                        state = previousState; // Прошлое местоположение, перед входом в строку
                    }

                    break;

                /*
                        Однострочный комментарий
                */
                case insideLineComment:

                    if (currentChar.equals(ls)) { // Парсер достиг конца строки

                        state = previousState;
                        result.append(ls);

                    } else {

                        if (mode != ParserMode.removeComments
                                && (previousState != ParserState.tryCatchFinallyStruct || mode != ParserMode.removeExceptionsStruct))
                            result.append(currentChar);

                    }
                    break;

                /*
                        Многострочный комментарий
                */
                case insideblockComment:

                    if (currentChar.equals("*") && s.hasNext()) {

                            String c2 = s.next();

                            if (mode != ParserMode.removeComments
                                    && (previousState != ParserState.systemLogs || mode != ParserMode.removeConsoleLogs)
                                    && (previousState != ParserState.tryCatchFinallyStruct || mode != ParserMode.removeExceptionsStruct))
                                result.append(currentChar).append(c2);

                            if (c2.equals("/")) {

                                state = previousState;
                                break;

                            } else {

                                break;
                            }
                    } else {

                        if (mode != ParserMode.removeComments
                                && (previousState != ParserState.systemLogs || mode != ParserMode.removeConsoleLogs)
                                && (previousState != ParserState.tryCatchFinallyStruct || mode != ParserMode.removeExceptionsStruct))
                            result.append(currentChar);
                    }

                    break;

                case tryKeyword:

                    /*
                            Проверяем, нет ли комментариев, после ключевого слова 'try'
                     */

                    if (currentChar.equals("/") && s.hasNext()) {

                        String c2 = s.next();

                        switch (c2) {
                            case "/":
                                state = ParserState.insideLineComment;
                                previousState = ParserState.tryKeyword;

                                if (mode != ParserMode.removeComments)
                                    result.append(currentChar).append(c2);

                                break;

                            case "*":
                                state = ParserState.insideblockComment;
                                previousState = ParserState.tryKeyword;

                                if (mode != ParserMode.removeComments)
                                    result.append(currentChar).append(c2);

                                break;

                            default:

                                result.append(currentChar).append(c2);

                                break;
                        }

                    } else if (currentChar.equals("\"")) {

                        state = ParserState.insideString;
                        previousState = ParserState.tryKeyword;

                        if (mode != ParserMode.removeStaticStrings)
                            result.append(currentChar);

                    } else if (currentChar.equals("{") || isOnTryKeyword) {

                        isOnTryKeyword = true;
                        boolean firstEnter = true;

                        while (tryKeywordCodeLevel > 0 || isOnTryKeyword) {

                            if (!firstEnter)
                                currentChar = s.next();

                            firstEnter = false;

                            if (currentChar.equals("/") && s.hasNext()) {

                                String c2 = s.next();

                                switch (c2) {
                                    case "/":
                                        state = ParserState.insideLineComment;
                                        previousState = ParserState.tryKeyword;

                                        if (mode != ParserMode.removeComments)
                                            result.append(currentChar).append(c2);

                                        break;

                                    case "*":
                                        state = ParserState.insideblockComment;
                                        previousState = ParserState.tryKeyword;

                                        if (mode != ParserMode.removeComments)
                                            result.append(currentChar).append(c2);

                                        break;

                                    default:
                                        result.append(currentChar).append(c2);

                                        break;
                                }

                            } else if (currentChar.equals("\"")) {

                                state = ParserState.insideString;
                                previousState = ParserState.tryKeyword;

                                if (mode != ParserMode.removeStaticStrings)
                                    result.append(currentChar);

                            } else if (currentChar.matches("[^a-zA-Z0-9_$]")) {

                                if (currentChar.equals("{")) {

                                    tryKeywordCodeLevel++;

                                    if (tryKeywordCodeLevel == 1
                                            && mode == ParserMode.removeExceptionsStruct)
                                        continue;

                                    result.append(currentChar);
                                } else if (currentChar.equals("}")) {
                                    tryKeywordCodeLevel--;

                                    if (tryKeywordCodeLevel < 1) {
                                        isOnTryKeyword = false;
                                        tryKeywordCodeLevel = 0;
                                        state = ParserState.outsideComment;

                                        if (mode != ParserMode.removeExceptionsStruct)
                                            result.append(currentChar);
                                    } else result.append(currentChar);

                                } else {
                                    result.append(currentChar);

                                    if (s.hasNext())
                                        currentChar = s.next();
                                    else break;

                                    if (currentChar.equals("S")) {
                                        checkOnSout(s, ParserState.tryKeyword);
                                    } else {
                                        firstEnter = true;
                                        continue;
                                    }
                                }

                            } else {

                                result.append(currentChar);

                            }

                            if (state != ParserState.tryKeyword)
                                break;

                        }

                    } else {

                        /*
                                Возможна функция сохранения кода, в скобках после try '()'.
                                Необходима доработка условий в данном случае.
                         */

                        if (mode != ParserMode.removeExceptionsStruct)
                            result.append(currentChar);

                    }

                    break;

                case tryCatchFinallyStruct:

                    if (currentChar.equals("/") && s.hasNext()) {

                        String c2 = s.next();

                        switch (c2) {
                            case "/":
                                state = ParserState.insideLineComment;
                                previousState = ParserState.tryCatchFinallyStruct;

                                if (mode != ParserMode.removeComments
                                        && mode != ParserMode.removeExceptionsStruct)
                                    result.append(currentChar).append(c2);

                                break;

                            case "*":
                                state = ParserState.insideblockComment;
                                previousState = ParserState.tryCatchFinallyStruct;

                                if (mode != ParserMode.removeComments
                                        && mode != ParserMode.removeExceptionsStruct)
                                    result.append(currentChar).append(c2);

                                break;

                            default:

                                if (mode != ParserMode.removeExceptionsStruct)
                                    result.append(currentChar).append(c2);

                                break;
                        }

                    } else if (currentChar.equals("\"")) {

                        state = ParserState.insideString;
                        previousState = ParserState.tryCatchFinallyStruct;

                        if (mode != ParserMode.removeStaticStrings
                                && mode != ParserMode.removeExceptionsStruct)
                            result.append(currentChar);

                    } else if (currentChar.equals("{") || isOnTryCatchFinally) {

                        isOnTryCatchFinally = true;
                        boolean firstEnter = true;

                        while (tryCatchFinallyCodeLevel > 0 || isOnTryCatchFinally) {

                            if (!firstEnter)
                                currentChar = s.next();

                            firstEnter = false;

                            if (currentChar.equals("/") && s.hasNext()) {

                                String c2 = s.next();

                                switch (c2) {
                                    case "/":
                                        state = ParserState.insideLineComment;
                                        previousState = ParserState.tryCatchFinallyStruct;

                                        if (mode != ParserMode.removeComments
                                                && mode != ParserMode.removeExceptionsStruct)
                                            result.append(currentChar).append(c2);

                                        break;

                                    case "*":
                                        state = ParserState.insideblockComment;
                                        previousState = ParserState.tryCatchFinallyStruct;

                                        if (mode != ParserMode.removeComments
                                                && mode != ParserMode.removeExceptionsStruct)
                                            result.append(currentChar).append(c2);

                                        break;

                                    default:

                                        if (mode != ParserMode.removeExceptionsStruct)
                                            result.append(currentChar).append(c2);

                                        break;
                                }

                            } else if (currentChar.equals("\"")) {

                                state = ParserState.insideString;
                                previousState = ParserState.tryCatchFinallyStruct;

                                if (mode != ParserMode.removeStaticStrings
                                        && mode != ParserMode.removeExceptionsStruct)
                                    result.append(currentChar);

                            } else if (currentChar.matches("[^a-zA-Z0-9_$]")) {

                                if (currentChar.equals("{")) {

                                    tryCatchFinallyCodeLevel++;

                                    if (mode != ParserMode.removeExceptionsStruct)
                                        result.append(currentChar);

                                } else if (currentChar.equals("}")) {

                                    tryCatchFinallyCodeLevel--;

                                    if (mode != ParserMode.removeExceptionsStruct)
                                        result.append(currentChar);

                                    if (tryCatchFinallyCodeLevel < 1) {
                                        isOnTryCatchFinally = false;
                                        tryCatchFinallyCodeLevel = 0;
                                        state = ParserState.outsideComment;
                                    }

                                } else {

                                    if (mode != ParserMode.removeExceptionsStruct)
                                        result.append(currentChar);

                                    if (s.hasNext())
                                        currentChar = s.next();
                                    else break;

                                    if (currentChar.equals("S")) {
                                        checkOnSout(s, ParserState.tryCatchFinallyStruct);
                                    } else {
                                        firstEnter = true;
                                        continue;
                                    }
                                }

                            } else {

                                if (mode != ParserMode.removeExceptionsStruct)
                                    result.append(currentChar);

                            }

                            if (state != ParserState.tryCatchFinallyStruct)
                                break;

                        }

                    } else {

                        if (mode != ParserMode.removeExceptionsStruct)
                            result.append(currentChar);

                    }

                    break;

                case systemLogs:

                    /*
                            Проверка, нет ли комментариев, после вызова метода print и его производных
                     */

                    if (currentChar.equals("/") && s.hasNext()) {

                        String c2 = s.next();

                        switch (c2) {
                            case "/":

                                state = ParserState.insideLineComment;
                                previousState = ParserState.systemLogs;

                                if (mode != ParserMode.removeComments
                                        && mode != ParserMode.removeConsoleLogs)
                                    result.append(currentChar).append(c2);

                                break;

                            case "*":

                                state = ParserState.insideblockComment;
                                previousState = ParserState.systemLogs;

                                if (mode != ParserMode.removeComments
                                        && mode != ParserMode.removeConsoleLogs)
                                    result.append(currentChar).append(c2);

                                break;

                            default:

                                if (mode != ParserMode.removeConsoleLogs)
                                    result.append(currentChar).append(c2);

                                break;
                        }

                    } else if (currentChar.equals("\"")) {

                        state = ParserState.insideString;
                        previousState = ParserState.systemLogs;

                        if (mode != ParserMode.removeStaticStrings
                                && mode != ParserMode.removeConsoleLogs)
                            result.append(currentChar);

                    } else if (currentChar.equals(";")) {

                        if (prevprevState != null) {
                            previousState = ParserState.outsideComment;
                            state = prevprevState;
                            prevprevState = null;
                        } else {
                            state = previousState;
                        }

                        if (mode != ParserMode.removeConsoleLogs)
                            result.append(currentChar);

                    } else {

                        if (mode != ParserMode.removeConsoleLogs)
                            result.append(currentChar);

                    }

                    break;
            }
        }

        s.close();
        return result.toString();
    }

    private String checkOnSout(Scanner s, ParserState ps) {

        currentChar = s.next();

        if (currentChar.equals("y")) {

            currentChar = s.next();

            if (currentChar.equals("s")) {

                currentChar = s.next();

                if (currentChar.equals("t")) {

                    currentChar = s.next();

                    if (currentChar.equals("e")) {

                        currentChar = s.next();

                        if (currentChar.equals("m")) {

                            currentChar = s.next();

                            if (currentChar.equals(".")) {

                                currentChar = s.next();

                                if (currentChar.equals("o")) {

                                    currentChar = s.next();

                                    if (currentChar.equals("u")) {

                                        currentChar = s.next();

                                        if (currentChar.equals("t")) {

                                            currentChar = s.next();

                                            if (currentChar.equals(".")) {

                                                    /*
                                                            Проверка на переменную не произовдиться т.к. всё понятно по точке
                                                    */

                                                if (pmode != ParserMode.removeConsoleLogs) {
                                                    if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct) {
                                                        result.append("System.out.");
                                                    }
                                                }

                                                state = ParserState.systemLogs;
                                                previousState = ps;

                                                if (ps == ParserState.tryCatchFinallyStruct)
                                                    prevprevState = ParserState.tryCatchFinallyStruct;
                                                else if (ps == ParserState.tryKeyword)
                                                    prevprevState = ParserState.tryKeyword;
                                                else if (ps == ParserState.outsideComment)
                                                    prevprevState = ParserState.outsideComment;

                                            } else {

                                                if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                                                    result.append("System.out").append(currentChar);

                                            }

                                        } else {

                                            if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                                                result.append("System.ou").append(currentChar);

                                        }

                                    } else {

                                        if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                                            result.append("System.o").append(currentChar);

                                    }

                                } else {

                                    if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                                        result.append("System.").append(currentChar);

                                }

                            } else {

                                if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                                    result.append("System").append(currentChar);

                            }

                        } else {

                            if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                                result.append("Syste").append(currentChar);

                        }

                    } else {

                        if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                            result.append("Syst").append(currentChar);

                    }

                } else {

                    if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                        result.append("Sys").append(currentChar);

                }

            } else {

                if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                    result.append("Sy").append(currentChar);

            }

        } else {

            if (state != ParserState.tryCatchFinallyStruct || pmode != ParserMode.removeExceptionsStruct)
                result.append("S").append(currentChar);

        }

        return result.toString();
    }

    String removeCommentsV1(String code) {

        /*
                Дання функция не тестировалась должным образом, и служит как один из вариантов для удаления комментариев
         */

        /*
                Перед использованием данной функции, необходимо временно удалить все константные строки
         */

        /*
                Временное удаление константных строк, для удобства работы
         */
        String tempCode = codeMainParser(code, ParserMode.removeStaticStrings);

        /*
                Удаление многострочных комментариев, перед однострочными
         */
        ArrayList<String> commentsMap = new ArrayList<>();
        int idx = -2;
        while (tempCode.indexOf("/*", idx + 2) != -1 && idx + 2 < tempCode.length()) { // Пока код содержит '/*', и алгоритм не дошел до конца

            /*
                    Заполнение списка строк, для вычитания их из оригинального кода
             */
            idx = tempCode.indexOf("/*", idx + 2);
            boolean isFakeMulticomment = false;

            /*
                    Алгоритм для исключения возможной ошибки в многострочных комментариях
            */
            int tempIdx = idx - 2;
            while (tempIdx > 0) {

                if (!tempCode.substring(tempIdx, tempIdx + 2).contains(System.lineSeparator())) tempIdx--;
                else {
                    if (tempCode.substring(tempIdx, idx).contains("//")) {
                        isFakeMulticomment = true;
                        break;
                    } else break;
                }

                if (tempIdx == 0)
                    if (tempCode.substring(0, idx).contains("//")) {
                        isFakeMulticomment = true;
                        break;
                    }
            }

            if (isFakeMulticomment) continue;

            String element = tempCode.substring(idx, tempCode.indexOf("*/", idx) + 2);
            commentsMap.add(element);
        }

        /*
                Удаление однострочных комментариев
         */
        idx = -2;
        while (tempCode.indexOf("//", idx + 2) != -1 && idx + 2 < tempCode.length()) { // Пока код содержит '//', и алгоритм не дошел до конца

            /*
                    Заполнение списка строк, для вычитания их из оригинального кода
             */
            idx = tempCode.indexOf("//", idx + 2);
            String element;

            if (tempCode.indexOf(System.lineSeparator(), idx) != -1)
                element = tempCode.substring(idx, tempCode.indexOf(System.lineSeparator(), idx));
            else element = tempCode.substring(idx, tempCode.length());

            commentsMap.add(element);
        }

        if (commentsMap.size() > 0)
            for (String s : commentsMap)
                if (code.contains(s))
                    code = code.replace(s, " ");

        return code;
    }
}