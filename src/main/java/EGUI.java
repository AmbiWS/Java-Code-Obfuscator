import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

public class EGUI {
    private JPanel rootPanel;
    private JTextField tfObfuscFile;
    private JButton obfuscButton;
    private JButton exitButton;
    private JButton chooseFileButton;
    private JCheckBox cmtsDeleteCB;
    private JComboBox stringCipherCBox;
    private JTextField blacklistTF;
    private JTable finfoTable;
    private JButton removeFileButton;
    private JCheckBox consoleLogDelCB;
    private JCheckBox exceptionsDelCB;
    private JProgressBar progressBar1;
    private JLabel statusLabel;
    private JTextField blacklistStrTF;
    static int pbCurrentVal;
    static float dividerLength;
    private ObfuscationWorker obfuscationWorker = new ObfuscationWorker();

    private boolean isStrChangerCBChanged = false;
    private boolean isVarChangerCBChanged = false;

    private JFrame jf;

    private DefaultTableModel tableModel;
    private static int counter = 0;

    private static ArrayList<String> fNames = new ArrayList<>();
    private static ArrayList<String> forms = new ArrayList<>();
    DataParser dp = new DataParser();

    HashSet<String> blacklist = new HashSet<>();
    HashSet<String> blacklistStr = new HashSet<>();

    JLabel getStatusLabel() {
        return statusLabel;
    }

    JProgressBar getProgressBar1() {
        return progressBar1;
    }

    JComboBox getStringCipherCBox() {
        return stringCipherCBox;
    }

    JCheckBox getCmtsDeleteCB() {
        return cmtsDeleteCB;
    }

    JCheckBox getConsoleLogDelCB() {
        return consoleLogDelCB;
    }

    JCheckBox getExceptionsDelCB() {
        return exceptionsDelCB;
    }

    EGUI() {

        jf = new JFrame();
        jf.getContentPane().add(rootPanel);
        jf.setTitle("Java Eternal Obfuscator <JEO>");
        jf.setSize(585, 500);
        jf.setLocationRelativeTo(null);
        jf.setResizable(false);
        jf.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        setupButtons();
        componentsInit();

        blacklistStrTF.setForeground(Color.GRAY);
        blacklistTF.setForeground(Color.GRAY);

        blacklistStrTF.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                trigger();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                trigger();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                trigger();
            }

            private void trigger() {
                isStrChangerCBChanged = true;
            }
        });

        blacklistStrTF.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                blacklistStrTF.setForeground(Color.BLACK);
            }

            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if (!isStrChangerCBChanged)
                    blacklistStrTF.setForeground(Color.GRAY);
            }
        });

        blacklistTF.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                trigger();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                trigger();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                trigger();
            }

            private void trigger() {
                isVarChangerCBChanged = true;
            }
        });

        blacklistTF.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                blacklistTF.setForeground(Color.BLACK);
            }

            @Override
            public void focusLost(FocusEvent e) {
                super.focusLost(e);
                if (!isVarChangerCBChanged)
                    blacklistTF.setForeground(Color.GRAY);
            }
        });

        jf.setVisible(true);
    }

    private void addTableItem(String path) {
        Object[] myRow = {++counter, path};
        tableModel.addRow(myRow);
    }

    private void componentsInit() {
        cmtsDeleteCB.setSelected(true);
        tableModel.setRowCount(0);
        removeFileButton.setMargin(new java.awt.Insets(1, 2, 1, 2));
    }

    private void fillBlacklist() {
        blacklist.clear();
        if (!isVarChangerCBChanged) return;

        String[] blWords;

        if (blacklistTF.getText().contains(",") && blacklistTF.getText().length() > 1)
            blWords = blacklistTF.getText().replaceAll(" ", "").split(",");
        else if (!blacklistTF.getText().contains(",") && blacklistTF.getText().length() > 0)
            blWords = new String[]{blacklistTF.getText().replaceAll(" ", "")};
        else blWords = null;

        if (blWords != null)
            for (String s : blWords)
                if (s.length() > 0)
                    blacklist.add(s);
    }

    private void fillBlacklistStr() {
        blacklistStr.clear();
        blacklistStr.add("\" \"");
        if (!isStrChangerCBChanged) return;

        String[] blWords;
        String tmp = blacklistStrTF.getText().replaceAll("\"\\s*,\\s*\"", "\"^\"\"\"\"\"^\"");

        if (tmp.contains("^\"\"\"\"\"^") && tmp.length() > 7)
            blWords = tmp.split("\\^\"\"\"\"\"\\^");
        else if (!tmp.contains("^\"\"\"\"\"^") && tmp.length() > 2)
            blWords = new String[] { tmp.trim() };
        else blWords = null;

        if (blWords != null)
            for (String s : blWords)
                if (s.length() > 0)
                    blacklistStr.add(s);
    }

    private void setupButtons() {
        removeFileButton.addActionListener((ActionEvent e) -> {
            int[] selectedRows = finfoTable.getSelectedRows();

            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(jf, "Оберіть один або декілька файлів, щоб видалити їх з таблиці.");
                return;
            }

            for (int i : selectedRows) {
                String s = tableModel.getValueAt(i, 1).toString();

                if (fNames.contains(s))
                    fNames.remove(s);

                if (forms.contains(s))
                    forms.remove(s);
            }

            tableModel.setRowCount(0);
            counter = 0;

            for (String s : fNames) {
                addTableItem(s);
            }
        });

        exitButton.addActionListener((ActionEvent e) -> System.exit(0));

        chooseFileButton.addActionListener((ActionEvent e) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setDialogTitle("Оберіть файли");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Текстовые файлы с кодом и GUI Form (*.txt, *.doc, *.docx, *.rtf, *.java, *.form)", "txt", "doc", "docx", "rtf", "java", "form"));

            if (fileChooser.showOpenDialog(jf) ==
                    JFileChooser.APPROVE_OPTION) {

                File[] filenames = fileChooser.getSelectedFiles();

                for (File f : filenames) {
                    if (fNames.contains(f.getAbsolutePath())) continue;
                    if (f.getAbsolutePath().lastIndexOf((".form")) == f.getAbsolutePath().length() - 5) {
                        forms.add(f.getAbsolutePath());
                        addTableItem(f.getAbsolutePath());
                        continue;
                    }

                    fNames.add(f.getAbsolutePath());
                    addTableItem(f.getAbsolutePath());
                    tfObfuscFile.setText(f.getAbsolutePath());
                }

            } else tfObfuscFile.setText("");
        });

        obfuscButton.addActionListener((ActionEvent e) -> {
            progressBar1.setValue(progressBar1.getMinimum());
            obfuscationWorker.execute();
        });
    }

    private void createUIComponents() {

        /*
                JTable custom create
         */

        String[] col = {"# ", "Файл"};
        tableModel = new DefaultTableModel(col, 0) {

            /*
                    Ячейки в таблице всегда неизменны
             */
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        finfoTable = new JTable(tableModel);
        finfoTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        finfoTable.getTableHeader().setResizingAllowed(false);
        finfoTable.getTableHeader().setReorderingAllowed(false);

        finfoTable.getColumnModel().getColumn(0).setMinWidth(31);
        finfoTable.getColumnModel().getColumn(0).setMaxWidth(31);
        finfoTable.getColumnModel().getColumn(1).setMinWidth(473);
        finfoTable.getColumnModel().getColumn(1).setMaxWidth(523);
    }

    class ObfuscationWorker extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {

            try {
                dp.mainObfSet.clear();
                dp.mainObfMap.clear();
                fillBlacklist();
                fillBlacklistStr();

                int rowsCt = finfoTable.getRowCount();
                if (rowsCt == 0)
                    JOptionPane.showMessageDialog(jf, "Оберіть один або декілька файлів, щоб провести обфускацію.");

                /*
                        Добавить остальные методы шифрования для исходного кода
                 */
                if (stringCipherCBox.getSelectedIndex() != 0) {
                    String firstFileDir = tableModel.getValueAt(0, 1).toString();
                    firstFileDir = firstFileDir.substring(0, firstFileDir.lastIndexOf("\\") + 1);
                    String fileWithReader = firstFileDir + "EternalObfuscatorStringsReader.java";

                    if (!fNames.contains(fileWithReader))
                        fNames.add(fileWithReader);
                    addTableItem(fileWithReader);

                    BufferedWriter bw = new BufferedWriter(new FileWriter(fileWithReader));

                    if (stringCipherCBox.getSelectedIndex() == 1) {
                        bw.write(EOStringsReaderSource.forAsciiToStr);
                        bw.close();
                        dp.formObfuscationMap(EOStringsReaderSource.forAsciiToStr);
                    }
                    else {
                        bw.write(EOStringsReaderSource.forBase64ToStr);
                        bw.close();
                        dp.formObfuscationMap(EOStringsReaderSource.forBase64ToStr);

                    }
                }

                int overallFiles = fNames.size() + forms.size();
                dividerLength = 100.0f / overallFiles;
                pbCurrentVal = 0;

                for (int i = 0; i < rowsCt; i++) {
                    String s = tableModel.getValueAt(i, 1).toString();
                    String fileName = s.substring(s.lastIndexOf("\\") + 1);
                    statusLabel.setText("Поточний файл: " + fileName);

                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(s), "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    String newLine;

                    while ((newLine = br.readLine()) != null)
                        builder.append(newLine).append(System.lineSeparator());

                    br.close();
                    dp.formObfuscationMap(builder.toString()); // Вызов метода сбора названий
                    pbCurrentVal += dividerLength;
                    progressBar1.setValue(pbCurrentVal);

                    if (forms.size() < 1
                            && i + 1 >= rowsCt) {
                        progressBar1.setValue(progressBar1.getMaximum());
                        statusLabel.setText("Обфускация успешно завершена");
                    }
                }

                EternalObfuscator.undefFilesCounter = 0;
                EternalObfuscator.obfuscate(fNames, dp.mainObfMap); // Вызов главного метода по обфускации
                if (forms.size() > 0)
                    EternalObfuscator.changeFormBindings(forms, dp.mainObfMap); // Изменение названий в form файлах

            } catch (FileNotFoundException exc) {
                JOptionPane.showMessageDialog(jf, "Обраний файл не був знайдений!");
                exc.printStackTrace();
            } catch (IOException exc) {
                JOptionPane.showMessageDialog(jf, "Помилка IOException!");
                exc.printStackTrace();
            }

            return null;
        }

        @Override
        protected void done() {
            super.done();
            obfuscationWorker.cancel(true);
            obfuscationWorker = new ObfuscationWorker();
        }
    }
}
