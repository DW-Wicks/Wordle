package com.dw.wordle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.List;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

public class Wordle extends JFrame
{
    private static final long serialVersionUID = 1L;
    public int nWords = 6;
    public int wordLen = 5;
    public LetterField[][] letters;
    public String[] anyPatternStr = {
        "",  ".", "..", "...", "....", ".....", "......"
    };
    public List<String> words, goodWords, tempWords;
    Container mainPane;
    JTable wordTable;

    enum State {GRAY, YELLOW, GREEN}

    public static void main(String[] args) {
        new Wordle();
    }
    
    Wordle() {
        super("Wordle Helper");
        letters = new LetterField[nWords][];
        for (int iWord=0; iWord<nWords; iWord++) {
            letters[iWord] = new LetterField[wordLen];
        }
        
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });

        mainPane = getContentPane();
        mainPane.setLayout(new BorderLayout());
        
        words = new LinkedList<String>();
        goodWords = new LinkedList<String>();
        tempWords = new LinkedList<String>();
        loadWords();
        for (String word : words) {
            goodWords.add(word);
        }
        buildMenu();
        buildBody();
        buildWordPane();
        //setSize(500,500);
        pack();
        setVisible(true);
    }
    
    protected void loadWords() {
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String wordPath = String.format("words/word%02d.txt", wordLen);
        System.out.println("Searching for wordPath: " + wordPath);
        
        try {
            URL url = classLoader.getResource(wordPath);
            System.out.println("Got uri = " + url.toURI());
            BufferedReader wordReader = new BufferedReader(
                    new FileReader(new File(url.toURI())));
            for (String line; (line=wordReader.readLine())!=null;) {
                words.add(line);
            }
            System.out.println(String.format("Read %d words",  words.size()));
        } catch(Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }
    
    protected void buildMenu() {
        JMenuBar menubar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        JMenuItem exit = new JMenuItem("Exit");
        exit.setMnemonic('x');
        file.add(exit);
        exit.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        });
        menubar.add(file);
        setJMenuBar(menubar);
    }
    
    protected void buildBody() {
        Box mainPanel = Box.createVerticalBox();
        
        mainPanel.add(Box.createVerticalStrut(20));
        for (int i=0; i<nWords; i++) {
            WordBox word= new WordBox(i, wordLen);
            mainPanel.add(word);
            mainPanel.add(Box.createVerticalStrut(10));
        }
        mainPanel.add(Box.createVerticalStrut(20));
        
        mainPane.add(mainPanel, BorderLayout.CENTER);
        
        Box buttonBox = new Box(BoxLayout.X_AXIS) {
            private static final long serialVersionUID = 1L;

            @Override
            public Insets getInsets()
            {
                return new Insets(20, 20, 20, 20);
            }
        };
        JButton findButton = new JButton("Find Words");
        findButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findWords();
            }
        });
        
        JButton clearButton = new JButton("     Clear     ");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               clearWords();
            }
        });
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(Box.createHorizontalStrut(20));
        buttonBox.add(findButton);
        buttonBox.add(Box.createHorizontalStrut(20));
        buttonBox.add(clearButton);
        buttonBox.add(Box.createHorizontalStrut(20));
        buttonBox.add(Box.createHorizontalGlue());
        mainPane.add(buttonBox, BorderLayout.SOUTH);
    }
    
    void buildWordPane() {
        int nRows = 20;
        int nCols = 5;
        String[]   tableHdrs = {"-----", "-----", "-----", "-----","-----"};
        String[][] tableWords = new String[nRows][];
        for (int iRow = 0; iRow<nRows; iRow++) {
            tableWords[iRow] = new String[nCols];
            for (int iCol=0; iCol<nCols; iCol++) {
                tableWords[iRow][iCol] = "";
            }
        }
        wordTable = new JTable(tableWords, tableHdrs );
        JScrollPane scrollPane = new JScrollPane(wordTable) {
            private static final long serialVersionUID = 1L;

            @Override
            public Insets getInsets()
            {
                return new Insets(20, 20, 20, 20);
            }
        };
        mainPane.add(scrollPane, BorderLayout.EAST);
    }
    
    void findWords() {
        System.out.println("Finding words...");
        filterGreen();
        filterYellow();
        filterGray();
        fillWordTable();
    }
    
    void fillWordTable() {
        int nRows = wordTable.getRowCount();
        int nCols = wordTable.getColumnCount();
        int nWords = goodWords.size();

        // Clear table
        for (int iRow=0; iRow<nRows; iRow++ ) {
            for (int iCol=0; iCol<nCols; iCol++) {
                wordTable.setValueAt("", iRow, iCol);
            }
        }
        
        if (nWords < nRows*nCols) {
            // Populate table
            for (int iWord=0; iWord<nWords; iWord++) {
                int iRow = iWord/nCols;
                int iCol = iWord%nCols;
                wordTable.setValueAt(goodWords.get(iWord), iRow, iCol);
            }
        }
        
    }
    
    void filterGreen() {
        
        System.out.println("filterGreen: Searching goodWords " + goodWords.size());
        tempWords.clear();
        for (String word : goodWords) {
            //System.out.println("filterGreen: Checking word: " + word);
            if (okGreen(word)) {
                //System.out.println("filterGreen: ok");
                tempWords.add(word);
            }
        }
        goodWords.clear();
        for (String word : tempWords) {
            goodWords.add(word);
        }
        System.out.println("filterGreen: goodWords = " + goodWords.size());
    }

    boolean okGreen(String word) {
        for (int iWord=0; iWord<nWords; iWord++) {
            for (int iChar=0; iChar<wordLen; iChar++) {
                if (letters[iWord][iChar].state == State.GREEN) {
                    char c1 = word.charAt(iChar);
                    char c2 = Character.toLowerCase(letters[iWord][iChar].getChar());
                    if (c1 != c2) return false;
                }
            }
        }
        return true;
    }

    void filterYellow() {
        System.out.println("filterGreen: Searching goodWords " + goodWords.size());
        tempWords.clear();
        for (String word : goodWords) {
            System.out.println("filterYellow: Checking word: " + word);
            if (okYellow(word)) {
                System.out.println("filterYellow: ok");
                tempWords.add(word);
            }
        }
        goodWords.clear();
        for (String word : tempWords) {
            goodWords.add(word);
        }
        System.out.println("filterYellow: goodWords = " + goodWords.size());        
    }

    boolean okYellow(String word) {
        for (int iWord=0; iWord<nWords; iWord++) {
            for (int iChar=0; iChar<wordLen; iChar++) {
                if (letters[iWord][iChar].state == State.YELLOW) {
                    boolean found=false;
                    char c2 = Character.toLowerCase(letters[iWord][iChar].getChar());
                    for (int ic=0; ic<wordLen; ic++) {
                        char c1 = word.charAt(ic);
                        if (c1 == c2) {
                            found=true;
                            break;  // Found yellow letter in word.  Keep looking for other yellow letters.
                        }
                    }
                    if (!found) return false; // Did not find yellow letter in word
                }
            }
        }
        return true;
    }

    void filterGray() {
        System.out.println("filterGray: Searching goodWords " + goodWords.size());
        tempWords.clear();
        for (String word : goodWords) {
            System.out.println("filterGray: Checking word: " + word);
            if (okGray(word)) {
                System.out.println("filterGray: ok");
                tempWords.add(word);
            }
        }
        goodWords.clear();
        for (String word : tempWords) {
            goodWords.add(word);
        }
        System.out.println("filterGray: goodWords = " + goodWords.size());        
    }
        
    boolean okGray(String word) {
        for (int iWord=0; iWord<nWords; iWord++) {
            for (int iChar=0; iChar<wordLen; iChar++) {
                if (letters[iWord][iChar].state == State.GRAY) {
                    boolean green=false;
                    boolean found=false;
                    char c2 = Character.toLowerCase(letters[iWord][iChar].getChar());
                    for (int ic=0; ic<wordLen; ic++) {
                        char c1 = word.charAt(ic);
                        if (c1 == c2) {
                            found = true;
                            if (letters[iWord][ic].state==State.GREEN) green=true;
                            if (letters[iWord][ic].state==State.YELLOW) green=true;
                            break;  // Found yellow letter in word.  Keep looking for other yellow letters.
                        }
                    }
                    if (found && !green) return false; // Reject if found unless a green was found.
                }
            }
        }
        return true;
    }

    
    void clearWords() {
        System.out.println("Clearing...");
        goodWords.clear();
        for (String word : words) {
            goodWords.add(word);
        }
        for (LetterField[] word : letters) {
            for (LetterField lf : word) {
                lf.setText("");
                lf.setBackground(Color.WHITE);
            }
        }
        fillWordTable();
        
    }
    
    class WordBox extends Box {
        private static final long serialVersionUID = 1L;
        int wordLen;
        
        public WordBox(int iWord, int len) {
            super(BoxLayout.X_AXIS);
            wordLen = len;
            add(Box.createHorizontalStrut(20));
            add(new JLabel(String.format("%d ===>", iWord+1)));
            add(Box.createHorizontalStrut(10));
            for (int iChar = 0; iChar<wordLen; iChar++) {
                letters[iWord][iChar] = new LetterField(" ");
                add(letters[iWord][iChar]);
                add(Box.createHorizontalStrut(10));
            }
            add(Box.createHorizontalStrut(20));
        }
    }
    
    class LetterField extends JTextField {
        private static final long serialVersionUID = 1L;
        public State state=State.GRAY;
        public int iWord;
        public char c;
        int iChar;

        public LetterField(String s) {
            super(s, 2);
            c = s.charAt(0);
            
            state=State.GRAY;
            setColor();
            Font defaultFont = getFont();
            this.setFont(new Font(defaultFont.getFontName(), Font.BOLD, defaultFont.getSize()+4));
            
            this.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent evt) {
                    // Replace existing text with new key.  Base class handles new.
                    evt.setKeyChar(Character.toUpperCase(evt.getKeyChar()));
                    setText("");
                }

                @Override
                public void keyPressed(KeyEvent e)
                {
                }

                @Override
                public void keyReleased(KeyEvent e)
                {
                }
            });
            
            this.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getButton() == MouseEvent.BUTTON3 ) {
                        
                        switch(state) {
                            case GRAY:
                                state=State.YELLOW; // Cycle to next
                                break;
                            case YELLOW:
                                state=State.GREEN; // Cycle to next
                                break;
                            case GREEN:
                                state=State.GRAY;  // Cycle to next
                                break;
                            default:
                                break;
                        }
                        setColor();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void mouseEntered(MouseEvent e)
                {
                    //((JTextField)this).mouseEntered(e);
                    
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    //((JTextField)this).mouseExit((Event)e, e.getX(), e.getY());
                    // TODO Auto-generated method stub
                    
                }
                
            });
            
        }
        
        char getChar() {
            String value = getText();
            return (value.length()>0) ? getText().charAt(0) : ' ';
        }

        void setColor() {
            switch(state) {
                case GRAY:
                    this.setBackground(Color.LIGHT_GRAY);
                    break;
                case GREEN:
                    this.setBackground(Color.GREEN);
                    break;
                case YELLOW:
                    this.setBackground(Color.YELLOW);
                    break;
                default:
                    break;
                
            }
        }
    }
    
}
