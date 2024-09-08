package com.dw.wordle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import javax.swing.border.Border;

import org.ini4j.Ini;

public class Wordle extends JFrame
{
    // Class variables
    private static final long serialVersionUID = 1L;
    final int       _nWords  = 6;
    int             _wordLen = 5;
    LetterField[][] _letters;
    JComboBox<String> _lengthSelector;
    List<String>    _words5, _words6, _words, _goodWords, _tempWords;
    Container       _mainPane;
    Box             _mainPanel;
    Box             _buttonBox;
    JTable          _wordTable;
    JLabel          _wordCountLabel;
    Ini             _ini;
    int             _x, _y;
    
    // Class enums
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
        
        int getValue() {
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
        new Wordle().run();
    }
    
    Wordle() {
        super("Wordle Helper");
        this.loadIni();
        this.setIconImage(getIcon());

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                saveIni();
                System.exit(0);
            }
        });

        _mainPane = getContentPane();
        _mainPane.setLayout(new BorderLayout());
        
        _words5 = loadWords(5);
        _words6 = loadWords(6);
        if (_wordLen == 5) _words = _words5;
        else _words=_words6;
        _goodWords = new LinkedList<String>();
        _tempWords = new LinkedList<String>();
        buildMenu();
        buildHeader();
        buildWordPane();
        wordSizeInit();
        this.setLocation(_x, _y);
    }
    
    protected void run() {
        setVisible(true);
        saveIni();
    }
    
    protected void loadIni() {
        try
        {
            String userHome = System.getProperty("user.home");
            System.out.println("DEBUG: Got user home = " + userHome);
            File file = new File(userHome, "Wordle.ini");
            // Check if the file exists, if not, create it
            if (!file.exists()) {
                file.createNewFile();
            }
            _ini = new Ini(file);
            String xStr = _ini.get("Window", "x");
            String yStr = _ini.get("Window", "y");
            _x = (xStr!=null) ? Integer.valueOf(xStr) : 0;
            _y = (xStr!=null) ? Integer.valueOf(yStr) : 0;
            System.out.println("DEBUG: Got x=" + _x +", y=" + _y);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    protected void saveIni() {
        Point p  = this.getLocationOnScreen();
        if (p.x != _x || p.y != _y) {
            _ini.put("Window", "x", p.x);
            _ini.put("Window", "y", p.y);
            try
            {
                _ini.store();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    protected void wordSizeInit() {
        if (_wordLen == 5) {
            System.out.println("Switching to _words5");
            _words = _words5;
        } else {
            System.out.println("Switching to _words6");
            _words = _words6;
        }
        for (String word : _words) {
            _goodWords.add(word);
        }
        
        _letters = new LetterField[_nWords][];
        for (int iWord=0; iWord<_nWords; iWord++) {
            _letters[iWord] = new LetterField[_wordLen];
        }

        buildBody();
        pack();
        findWords();
        fillWordTable();

        // Set focus to first letter.  Disable focus traversal on last letter.
        _letters[0][0].grabFocus();
    }
    
    protected Image getIcon() {
        URL iconURL = getClass().getResource("/WordleIcon.png");
        Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
        return icon;
    }
    
    protected List<String> loadWords(int wordLen) {
        // This version of loadWords finds the word file based on CLASSPATH.
        // If a jar file is used, it will read the word file from that jar file.

        // Sometimes when opening the word file we get an IOException saying
        // "Device not ready.".  This can happen if a USB drive takes too
        // long to wake up.  This loop waits a little bit and retries maxTries
        // times before finally giving up.
        System.out.println("Reading " + wordLen + " letter words");
        List<String> words = new LinkedList<String>();
        int iTry = 0;
        int maxTries = 30;
        while (true) {
            String wordPath = String.format("/WORDS/WORD%02d.TXT", wordLen);
            InputStream input = getClass().getResourceAsStream(wordPath);
            BufferedReader wordReader = new BufferedReader(new InputStreamReader(input));

            try
            {
                iTry++;
                for (String line; (line=wordReader.readLine())!=null;) {
                    words.add(line);
                }
                //System.out.println(String.format("Read %d words",  words.size()));
                return words;
            }
            catch (Exception e) {
                if (iTry>=maxTries) {
                    // Exceeded retry loop limit.  Abort.
                    e.printStackTrace();
                    System.exit(1);
                } else {
                    // Sleep 1000 milliseconds before continuing retry loop
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e1)
                    {
                    }
                }
            }
            // Continue retry loop.
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
                saveIni();
                System.exit(0);
            }
        });
        menubar.add(file);
        setJMenuBar(menubar);
    }
    
    protected void buildHeader() {
        _lengthSelector = new JComboBox<String> (
                new String[] {
                        WordLength.LETTERS5.getString(),
                        WordLength.LETTERS6.getString()
                });
        _lengthSelector.setBorder(BorderFactory.createTitledBorder("Select word length"));
        _lengthSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String s = _lengthSelector.getSelectedItem().toString();
                _wordLen = WordLength.fromString(s).getValue();
                wordSizeInit();
            }
        });
        Box headerBox = Box.createHorizontalBox();
        headerBox.add(Box.createHorizontalGlue());
        headerBox.add(_lengthSelector);
        headerBox.add(Box.createHorizontalGlue());
        _mainPane.add(headerBox, BorderLayout.NORTH);
    }
    
    protected void buildBody() {
        if (_mainPanel != null) {
            _mainPane.remove(_mainPanel);
            _mainPane.remove(_buttonBox);
        }
        _mainPanel = Box.createVerticalBox();
        
        _mainPanel.add(Box.createVerticalStrut(20));
        for (int i=0; i<_nWords; i++) {
            WordBox word= new WordBox(i, _wordLen);
            _mainPanel.add(word);
            _mainPanel.add(Box.createVerticalStrut(10));
        }
        _mainPanel.add(Box.createVerticalStrut(20));
        
        _mainPane.add(_mainPanel, BorderLayout.CENTER);
        
        _buttonBox = new Box(BoxLayout.X_AXIS) {
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
        _buttonBox.add(Box.createHorizontalGlue());
        _buttonBox.add(Box.createHorizontalStrut(20));
        _buttonBox.add(findButton);
        _buttonBox.add(Box.createHorizontalStrut(40));
        _buttonBox.add(clearButton);
        _buttonBox.add(Box.createHorizontalStrut(20));
        _buttonBox.add(Box.createHorizontalGlue());
        _mainPane.add(_buttonBox, BorderLayout.SOUTH);
    }
    
    void buildWordPane() {
        JPanel wordPane = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Insets getInsets()
            {
                return new Insets(3, 3, 3, 20);
            }
        };
            
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
        _wordTable = new JTable(tableWords, tableHdrs ) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JScrollPane scrollPane = new JScrollPane(_wordTable) {
            private static final long serialVersionUID = 1L;

            @Override
            public Insets getInsets()
            {
                return new Insets(6, 6, 6, 6);
            }
        };
        Border raisedbevel = BorderFactory.createRaisedBevelBorder();
        Border loweredbevel = BorderFactory.createLoweredBevelBorder();
        scrollPane.setBorder(BorderFactory.createCompoundBorder(raisedbevel, loweredbevel));
        _wordTable.getTableHeader().setUI(null);
        _wordCountLabel = new JLabel("Number of words");
        setWordCount();
        wordPane.add(_wordCountLabel, BorderLayout.NORTH);
        wordPane.add(scrollPane, BorderLayout.CENTER);
        _mainPane.add(wordPane, BorderLayout.EAST);
    }
    
    void setWordCount() {
        _wordCountLabel.setText("Number of words: " + this._goodWords.size());
    }
    
    void findWords() {
        System.out.println("Finding words...");

        // Reset goodWords
        _goodWords.clear();
        for (String word : _words) {
            _goodWords.add(word);
        }

        filterGreen();
        filterYellow();
        filterGray();
        fillWordTable();
    }
    
    void fillWordTable() {
        int nRows = _wordTable.getRowCount();
        int nCols = _wordTable.getColumnCount();        
        int nWords = _goodWords.size();

        // Clear table
        for (int iRow=0; iRow<nRows; iRow++ ) {
            for (int iCol=0; iCol<nCols; iCol++) {
                _wordTable.setValueAt("", iRow, iCol);
            }
        }
        
        if (nWords < nRows*nCols) {
            // Populate table
            for (int iWord=0; iWord<nWords; iWord++) {
                int iRow = iWord/nCols;
                int iCol = iWord%nCols;
                _wordTable.setValueAt(_goodWords.get(iWord), iRow, iCol);
            }
        }
        setWordCount();
    }
    
    void filterGreen() {
        // Eliminates words from goodWords that are missing green letters.
                
        // Check words in goodWords.
        // Save words that pass the green test to tempWords. 
        _tempWords.clear();
        for (String word : _goodWords) {
            if (okGreen(word)) {
                _tempWords.add(word);
            }
        }

        // Update goodWords with values from tempWords.
        _goodWords.clear();
        for (String word : _tempWords) {
            _goodWords.add(word);
        }
        System.out.println("filterGreen: goodWords = " + _goodWords.size());
    }

    boolean okGreen(String word) {
        // Tests a word from the goodWords list.
        // Returns:
        //    true:   if the word contains all the necessary green letters.
        //    false:  if the word is missing any green letters.
        
        // Loop over user input words
        for (int iWord=0; iWord<_nWords; iWord++) {
            // Loop over characters in this user input word
            for (int iChar=0; iChar<_wordLen; iChar++) {
                char c1 = _letters[iWord][iChar].getChar();
                if (c1 == ' ') return true; // Got into dead space of user input
                if (_letters[iWord][iChar].state == State.GREEN) {
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
        _tempWords.clear();
        for (String word : _goodWords) {
            if (okYellow(word)) {
                _tempWords.add(word);
            }
        }

        // Update goodWords with values from tempWords.
        _goodWords.clear();
        for (String word : _tempWords) {
            _goodWords.add(word);
        }
        System.out.println("filterYellow: goodWords = " + _goodWords.size());        
    }

    boolean okYellow(String word) {
        // Tests a word from the goodWords list.
        // Returns:
        //    true:  if the word contains a yellow letter anywhere in the word
        //           except for the character at the yellow position itself.
        //    false: if the word is missing any yellow letters or only contains
        //           the character at the yellow position.

        // Loop over user input words
        char c1, c11, c2, c22;
        for (int iWord=0; iWord<_nWords; iWord++) {
            // Loop over characters in this user input word
            for (int iChar=0; iChar<_wordLen; iChar++) {
                c1 = _letters[iWord][iChar].getChar();
                c2 = word.charAt(iChar);
                if (c1 == ' ') return true; // Got into dead space of user input

                if (_letters[iWord][iChar].state != State.YELLOW) continue;
                // This is a yellow letter.
                // The input word must contain this letter somewhere else

                for (int iw=0; iw<_nWords; iw++) {
                    c11 = _letters[iw][iChar].getChar();
                    if (c11 == ' ') break;
                    if (c11 == c2 && _letters[iw][iChar].state == State.YELLOW) {
                        // This letter at this position is yellow in one of the words.  Reject.
                        //System.out.println(String.format(
                        //        "OKYellow:RejectYellowA: word=%s, iWord=%d, iChar=%d, iw=%d, c1=%c, c11=%c",
                        //        word, iWord, iChar, iw, c1, c11));
                        return false;
                    }
                    if (_letters[iw][iChar].state == State.GREEN && c11 != c2) {
                        // Another letter is known to be at this position.  Reject.
                        if (word.equals("lunch")) {
                            System.out.println(String.format(
                                    "OKYellow:RejectGreenA: word=%s, iWord=%d, iChar=%d, iw=%d, c1=%c, c11=%c",
                                    word, iWord, iChar, iw, c1, c11));
                            
                        }
                        return false;
                    }

                }
                
                // Make sure that this yellow letter is somewhere else in the word
                boolean found = false;
                for (int ic=0; ic<_wordLen; ic++) {
                    if (ic == iChar) continue;  // Don't count character being examined
                    c22 = word.charAt(ic);
                    if (c1 == c22) found=true;
                }
                if (!found) return false;
            }
        }
        return true; // Everything passed
    }

    void filterGray() {
        // Eliminates words from goodWords that contain gray letters
        // unless the letter is also present as green or yellow 

        // Check words in goodWords.
        // Save words that pass the gray test to tempWords. 
        _tempWords.clear();
        for (String word : _goodWords) {
            if (okGray(word)) {
                _tempWords.add(word);
            }
        }

        // Update goodWords with values from tempWords.
        _goodWords.clear();
        for (String word : _tempWords) {
            _goodWords.add(word);
        }

        System.out.println("filterGray: goodWords = " + _goodWords.size());        
    }
        
    boolean okGray(String word) {
        // Tests a word from the goodWords list.
        // Returns:
        //    true:  if the word contains no lone gray letters
        //        or if the word contains a gray letter but also has a matching green or yellow letter   
        //    false: if the word contains any lone gray letters
        
        // Loop over user input words
        for (int iWord=0; iWord<_nWords; iWord++) {
            boolean greenYellow;
            char c1, c11, c2;

            // Loop over characters in this user input word
            for (int iChar=0; iChar<_wordLen; iChar++) {
                greenYellow = false;
                c1 = _letters[iWord][iChar].getChar();
                if (c1 == ' ') return true; // Got into dead space of user input
                if (_letters[iWord][iChar].state != State.GRAY) continue;
                // This is a gray letter.
                // The input word must not contain this letter unless
                // the letter is also present as a green or yellow letter.
                    
                // Check for this letter also there as green or yellow
                for (int ic=0; ic<_wordLen; ic++) {
                    c11 = _letters[iWord][ic].getChar();
                    if ((c11 == c1)
                            && (
                                    _letters[iWord][ic].state==State.GREEN ||
                                    _letters[iWord][ic].state==State.YELLOW
                                )
                             ) {
                        greenYellow = true;
                        break;
                    }
                }
                    
                if (greenYellow) {
                    // Loop over letters in test word
                    for (int ic=0; ic<_wordLen; ic++) {
                        c2 = word.charAt(ic);
                        if ((c1 == c2) && (ic == iChar)) {
                            // We already know this letter is gray
                            //System.out.println(String.format(
                            //        "OKGray:RejectA: word=%s, iWord=%d, iChar=%d, ic=%d, c1=%c, c2=%c",
                            //        word, iWord, iChar, ic, c1, c2));
                            return false;
                        }
                    }
                        
                } else {
                    // Loop over letters in test word
                    for (int ic=0; ic<_wordLen; ic++) {
                        c2 = word.charAt(ic);
                        if (c1 == c2) {
                            // This letter should not be in the word
                            //System.out.println(String.format(
                            //        "OKGray:RejectB: word=%s, iWord=%d, iChar=%d, c1=%c, c2=%c",
                            //        word, iWord, iChar, c1, c2));
                            return false;
                        }
                    }
                }
            } // Loop over letters in user input word
        } // Loop over user nput words
        return true;  // Everything passed
    }

    
    void clearWords() {
        System.out.println("Clearing...");
        _goodWords.clear();
        for (String word : _words) {
            _goodWords.add(word);
        }
        for (LetterField[] word : _letters) {
            for (LetterField lf : word) {
                lf.setText(" ");
                lf.setBackground(Color.LIGHT_GRAY);
                lf.state = State.GRAY;
            }
        }
        fillWordTable();
        _letters[0][0].grabFocus();
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
                _letters[iWord][iChar] = new LetterField(iWord, iChar, " ");
                add(_letters[iWord][iChar]);
                add(Box.createHorizontalStrut(10));
            }
            add(Box.createHorizontalStrut(20));
            this.setFocusTraversalPolicyProvider(true);
            this.setFocusTraversalPolicy(new FocusTraversalPolicy() {

                @Override
                public Component getComponentAfter(Container aContainer,
                        Component aComponent)
                {
                    LetterField lf = (LetterField)aComponent;
                    int iw = lf.wordIndex;
                    int ic = lf.charIndex;
                    int nextic = ic+1;
                    int nextiw = iw;
                    if (nextic == wordLen) {nextic=0; nextiw=iw+1;}
                    if (nextiw == _nWords) {nextic=ic; nextiw=iw;} 
                    return _letters[nextiw][nextic];
                }

                @Override
                public Component getComponentBefore(Container aContainer,
                        Component aComponent)
                {
                    LetterField lf = (LetterField)aComponent;
                    int iw = lf.wordIndex;
                    int ic = lf.charIndex;
                    int nextic = ic-1;
                    int nextiw = iw;
                    if (nextic<0) {nextic = wordLen-1; nextiw=iw-1;}
                    if (nextiw<0) {nextic = 0; nextiw=0;}
                    return _letters[nextiw][nextic];
                }

                @Override
                public Component getFirstComponent(Container aContainer)
                {
                    return _letters[0][0];
                }

                @Override
                public Component getLastComponent(Container aContainer)
                {
                    return _letters[_nWords-1][wordLen-1];
                }

                @Override
                public Component getDefaultComponent(Container aContainer)
                {
                    return _letters[0][0];
                }
            
            });
        }
    }
    
    class LetterField extends JTextField {
        private static final long serialVersionUID = 1L;
        public State state=State.GRAY;
        public char c;
        public int wordIndex;
        public int charIndex;

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
                    
                    if (keyVal == '\n') {
                        // Enter key pressed.  Update matching words.
                        findWords();
                        if (iw < _nWords-1 && ic== _wordLen-1) {
                            // Move to start of next word if at end of word
                            _letters[iw+1][0].requestFocus();
                        }
                        return;
                    }
                    
                    if (keyVal == KeyEvent.VK_ESCAPE) {
                        // Escape key pressed.  Clear words and reset focus.
                        clearWords();
                        return;
                    }
                    
                    if (keyVal == KeyEvent.VK_5) {
                        // 5 key pressed.  Load 5 letter words.
                        _lengthSelector.setSelectedIndex(WordLength.LETTERS5.ordinal());
                        _wordLen = 5;
                        wordSizeInit();
                        return;
                    }
                                        
                    if (keyVal == KeyEvent.VK_6) {
                        // 6 key pressed.  Load 6 letter words.
                        _lengthSelector.setSelectedIndex(WordLength.LETTERS6.ordinal());
                        _wordLen = 6;
                        wordSizeInit();
                        return;
                    }
                                        
                    // Ignore anything non-alphabetic except space or slash.
                    if (!"abcdefghigjklmnopqrstuvwxyz /".contains(String.valueOf(keyVal))) {
                        keyVal = curVal;
                        evt.setKeyChar(keyVal);  // Ignore key
                        //tabToNext = false;
                    }
                    
                    // Space key.  Reset state to gray.
                    if (keyVal == ' ') state = State.GRAY;
                    
                    // Slash key or repeated key: cycle through states
                    if (keyVal == '/' || keyVal==curVal) {
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

                }

                @Override
                public void keyPressed(KeyEvent evt)
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
