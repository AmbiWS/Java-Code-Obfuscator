
class EOStringsReaderSource {
    static String forAsciiToStr =
            "public class EternalObfuscatorStringsReader {" + System.lineSeparator() +
                    "\tstatic String asciiToStr(String ascii) {" + System.lineSeparator() +
                    "\t\tString result = \"\";" + System.lineSeparator() +
                    "\t\tString[] characters = ascii.split(\" \");" + System.lineSeparator() +
                    "\t\tfor (String s: characters)" + System.lineSeparator() +
                    "\t\t\tresult += (char)Integer.parseInt(s);" + System.lineSeparator() +
                    "\t\treturn result;" + System.lineSeparator() +
                    "\t}" + System.lineSeparator() +
                    "}";

    static String forBase64ToStr =
            "import java.nio.charset.StandardCharsets;" + System.lineSeparator() +
                    "import java.util.Base64;" + System.lineSeparator() +
                    "public class EternalObfuscatorStringsReader {" + System.lineSeparator() +
                    "\tstatic String base64ToStr(String base64) {" + System.lineSeparator() +
                    "\t\treturn new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);"  + System.lineSeparator() +
                    "\t}" + System.lineSeparator() +
                    "}";
}
