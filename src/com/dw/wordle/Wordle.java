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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
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
    Box mainPanel;
    Box buttonBox;
    JTable wordTable;
    JLabel wordCountLabel;

    enum State {GRAY, YELLOW, GREEN}
    enum WordLength {
        LETTERS5, LETTERS6;
        
        String getString() {
            switch(this) {
                case LETTERS5:
                    return "5 letters";
                case LETTERS6:
                    return "6 letters";
                default:
                    return "";
            }                
        }
        
        static WordLength fromString(String s) {
            for (WordLength wl : WordLength.values()) {
                if (wl.getString().equals(s)) return wl;
            }
            return WordLength.LETTERS5;
        }
        
        int getInt() {
            switch(this) {
                case LETTERS5:
                    return 5;
                case LETTERS6:
                    return 6;
                default:
                    return 0;
            }                
        }
    }

    public static void main(String[] args) {
        new Wordle();
    }
    
    Wordle() {
        super("Wordle Helper");
        
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
        buildMenu();
        buildHeader();
        buildWordPane();
        wordSizeInit();
        //setSize(500,500);
        setVisible(true);
    }
    
    protected void wordSizeInit() {
        loadWords();
        letters = new LetterField[nWords][];
        for (int iWord=0; iWord<nWords; iWord++) {
            letters[iWord] = new LetterField[wordLen];
        }
        buildBody();
        pack();
        findWords();
        fillWordTable();
    }
    
    protected void loadWords() {
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String wordPath = String.format("words/word%02d.txt", wordLen);
        //System.out.println("Searching for wordPath: " + wordPath);
        words.clear();
        
        try {
            URL url = classLoader.getResource(wordPath);
            System.out.println("Reading words from: " + url.toURI());
            BufferedReader wordReader = new BufferedReader(
                    new FileReader(new File(url.toURI())));
            for (String line; (line=wordReader.readLine())!=null;) {
                words.add(line);
            }
            for (String word : words) {
                goodWords.add(word);
            }

            //System.out.println(String.format("Read %d words",  words.size()));
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
    
    protected void buildHeader() {
        JComboBox<String> lengthSelector = new JComboBox<String> (
                new String[] {
                        WordLength.LETTERS5.getString(),
                        WordLength.LETTERS6.getString()
                });
        lengthSelector.setBorder(BorderFactory.createTitledBorder("Select word length"));
        lengthSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String s = lengthSelector.getSelectedItem().toString();
                wordLen = WordLength.fromString(s).getInt();
                wordSizeInit();
            }
        });
        Box headerBox = Box.createHorizontalBox();
        headerBox.add(Box.createHorizontalGlue());
        headerBox.add(lengthSelector);
        headerBox.add(Box.createHorizontalGlue());
        mainPane.add(headerBox, BorderLayout.NORTH);
    }
    
    protected void buildBody() {
        if (mainPanel != null) {
            mainPane.remove(mainPanel);
            mainPane.remove(buttonBox);
        }
        mainPanel = Box.createVerticalBox();
        
        mainPanel.add(Box.createVerticalStrut(20));
        for (int i=0; i<nWords; i++) {
            WordBox word= new WordBox(i, wordLen);
            mainPanel.add(word);
            mainPanel.add(Box.createVerticalStrut(10));
        }
        mainPanel.add(Box.createVerticalStrut(20));
        
        mainPane.add(mainPanel, BorderLayout.CENTER);
        
        buttonBox = new Box(BoxLayout.X_AXIS) {
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
        JPanel wordPane = new JPanel(new BorderLayout());
        int nRows = 100;
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
        wordTable.getTableHeader().setUI(null);
        wordCountLabel = new JLabel("Number of words");
        setWordCount();
        wordPane.add(wordCountLabel, BorderLayout.NORTH);
        wordPane.add(scrollPane, BorderLayout.CENTER);
        mainPane.add(wordPane, BorderLayout.EAST);
    }
    
    void setWordCount() {
        wordCountLabel.setText("Number of words: " + this.goodWords.size());
    }
    
    void findWords() {
        System.out.println("Finding words...");

        // Reset goodWords
        goodWords.clear();
        for (String word : words) {
            goodWords.add(word);
        }

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
        setWordCount();
    }
    
    void filterGreen() {
        // Eliminates words from goodWords that are missing green letters.
                
        // Check words in goodWords.
        // Save words that pass the green test to tempWords. 
        tempWords.clear();
        for (String word : goodWords) {
            if (okGreen(word)) {
                tempWords.add(word);
            }
        }

        // Update goodWords with values from tempWords.
        goodWords.clear();
        for (String word : tempWords) {
            goodWords.add(word);
        }
        System.out.println("filterGreen: goodWords = " + goodWords.size());
    }

    boolean okGreen(String word) {
        // Tests a word from the goodWords list.
        // Returns:
        //    true:   if the word contains all the necessary green letters.
        //    false:  if the word is missing any green letters.
        
        // Loop over user input words
        for (int iWord=0; iWord<nWords; iWord++) {
            // Loop over characters in this user input word
            for (int iChar=0; iChar<wordLen; iChar++) {
                char c1 = letters[iWord][iChar].getChar();
                if (c1 == ' ') return true; // Got into dead space of user input
                if (letters[iWord][iChar].state == State.GREEN) {
                    // This is a green letter.
                    // The input word must contain this letter in this position.
                    char c2 = word.charAt(iChar);
                    if (c1 != c2) return false;
                }
            }
        }
        return true;  // Everything passed.
    }

    void filterYellow() {
        // Eliminates words from goodWords that are missing yellow letters.

        // Check words in goodWords.
        // Save words that pass the yellow test to tempWords. 
        tempWords.clear();
        for (String word : goodWords) {
            if (okYellow(word)) {
                tempWords.add(word);
            }
        }

        // Update goodWords with values from tempWords.
        goodWords.clear();
        for (String word : tempWords) {
            goodWords.add(word);
        }
        System.out.println("filterYellow: goodWords = " + goodWords.size());        
    }

    boolean okYellow(String word) {
        // Tests a word from the goodWords list.
        // Returns:
        //    true:  if the word contains a yellow letter anywhere in the word
        //           except for the character at the yellow position itself.
        //    false: if the word is missing any yellow letters or only contains
        //           the character at the yellow position.

        // Loop over user input words
        for (int iWord=0; iWord<nWords; iWord++) {
            // Loop over characters in this user input word
            for (int iChar=0; iChar<wordLen; iChar++) {
                char c1 = letters[iWord][iChar].getChar();
                if (c1 == ' ') return true; // Got into dead space of user input
                if (letters[iWord][iChar].state == State.YELLOW) {
                    // This is a yellow letter.
                    // The input word must contain this letter somewhere other
                    // than at the current position.
                    boolean found=false;
                    
                    // Loop over letters in test word
                    for (int ic=0; ic<wordLen; ic++) {
                        char c2 = word.charAt(ic);
                        if (c1 == c2) {
                            if (ic != iChar &&
                                letters[iWord][ic].state != State.GREEN) {
                                // The character we are looking for can't
                                // be in the current position or be green.
                                found=true;
                                break;
                            }
                        }
                    }
                    if (!found) return false; // Did not find yellow letter in word
                }
            }
        }
        return true; // Everything passed
    }

    void filterGray() {
        // Eliminates words from goodWords that contain gray letters
        // unless the letter is also present as green or yellow 

        // Check words in goodWords.
        // Save words that pass the yellow test to tempWords. 
        tempWords.clear();
        for (String word : goodWords) {
            if (okGray(word)) {
                tempWords.add(word);
            }
        }

        // Update goodWords with values from tempWords.
        goodWords.clear();
        for (String word : tempWords) {
            goodWords.add(word);
        }

        System.out.println("filterGray: goodWords = " + goodWords.size());        
    }
        
    boolean okGray(String word) {
        // Tests a word from the goodWords list.
        // Returns:
        //    true:  if the word contains no lone gray letters
        //        or if the word contains a gray letter but also has a matching green or yellow letter   
        //    false: if the word contains any lone gray letters
        
        // Loop over user input words
        for (int iWord=0; iWord<nWords; iWord++) {
            boolean greenYellow=false;
            char c1, c2;

            // Loop over characters in this user input word
            for (int iChar=0; iChar<wordLen; iChar++) {
                c1 = letters[iWord][iChar].getChar();
                if (c1 == ' ') return true; // Got into dead space of user input
                if (letters[iWord][iChar].state == State.GRAY) {
                    // This is a gray letter.
                    // The input word must not contain this letter unless
                    // the letter is also present as a green or yellow letter.
                    
                    // Check for this letter also there as green or yellow
                    for (int ic=0; ic<wordLen; ic++) {
                        c2 = letters[iWord][ic].getChar();
                        if ((c2 == c1)
                                && (
                                        letters[iWord][ic].state==State.GREEN ||
                                        letters[iWord][ic].state==State.YELLOW
                                    )
                                 ) {
                            greenYellow = true;
                            break;
                        }
                    }
                    
                    if (greenYellow) {
                        // Loop over letters in test word
                        for (int ic=0; ic<wordLen; ic++) {
                            c2 = word.charAt(ic);
                            if ((c1 == c2) && (ic == iChar)) {
                                // We already know this letter is gray
                                return false;
                            }
                        }
                        
                    } else {
                        // Loop over letters in test word
                        for (int ic=0; ic<wordLen; ic++) {
                            c2 = word.charAt(ic);
                            if (c1 == c2) {
                                // This letter should not be in the word
                                return false;
                            }
                        }
                    }
                } // State=GRAY
            } // Loop over letters in user input word
        } // Loop over user nput words
        return true;  // Everything passed
    }

    
    void clearWords() {
        System.out.println("Clearing...");
        goodWords.clear();
        for (String word : words) {
            goodWords.add(word);
        }
        for (LetterField[] word : letters) {
            for (LetterField lf : word) {
                lf.setText(" ");
                lf.setBackground(Color.LIGHT_GRAY);
                lf.state = State.GRAY;
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
                letters[iWord][iChar] = new LetterField(iWord, iChar, " ");
                add(letters[iWord][iChar]);
                add(Box.createHorizontalStrut(10));
            }
            add(Box.createHorizontalStrut(20));
        }
    }
    
    class LetterField extends JTextField {
        private static final long serialVersionUID = 1L;
        public State state=State.GRAY;
        int iWord;
        int iChar;
        char c;
        int wordIndex;
        int charIndex;

        public LetterField(int iw, int ic, String s) {
            super(s, 2);
            wordIndex = iw;
            charIndex = ic;
            c = s.charAt(0);
            
            state=State.GRAY;
            setColor();
            Font defaultFont = getFont();
            this.setFont(new Font(defaultFont.getFontName(), Font.BOLD, defaultFont.getSize()+4));
            
            this.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent evt) {
                    // Replace existing text with new key.  Base class handles new.
                    //boolean tabToNext = true;
                    char curVal = getChar();
                    char keyVal = evt.getKeyChar();
                    
                    // Ignore anything non-alphabetic except space or slash.
                    if (!"abcdefghigjklmnopqrstuvwxyz /".contains(String.valueOf(keyVal))) {
                        keyVal = curVal;
                        evt.setKeyChar(keyVal);  // Ignore key
                        //tabToNext = false;
                    }
                    
                    // Slash key: cycle through states
                    if (keyVal == '/') {
                        if (curVal != ' ') {
                            switch(state) {
                                case GRAY:
                                    state=State.YELLOW;
                                    break;
                                case YELLOW:
                                    state=State.GREEN;
                                    break;
                                case GREEN:
                                    state=State.GRAY;
                                    break;
                            }
                        }
                        keyVal = curVal;
                        evt.setKeyChar(keyVal);  // Replace / with curVal
                        //tabToNext = false;
                    }

                    if (keyVal != curVal){
                        // New letter.  Reset state to GRAY.
                        state=State.GRAY;
                    }
                    setColor();

                    evt.setKeyChar(Character.toUpperCase(evt.getKeyChar()));
                    setText("");

//                    Commented out because auto-advance interferes with "/" state entry
//                    System.out.println("tabToNext = " + tabToNext);
//                    if (tabToNext) {
//                        int nextChar = (charIndex + 1) % wordLen;
//                        int nextWord = wordIndex;
//                        if (nextChar == 0) {
//                            nextWord = (nextWord + 1) % nWords;
//                        }
//                        System.out.println("nextWord="+nextWord+", nextChar="+nextChar);
//                        letters[nextWord][nextChar].grabFocus();
//                    }
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
            return (value.length()>0) 
                ? Character.toLowerCase(value.charAt(0))
                : ' ';
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
