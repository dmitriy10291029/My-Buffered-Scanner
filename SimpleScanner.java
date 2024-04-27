import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.IllegalStateException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;

public class SimpleScanner implements AutoCloseable {
    private Reader reader;
    private boolean readerClosed = true;
    private boolean scannerClosed = false;
    
    private static final int BUFFER_SIZE = 1024;
    private char[] buffer;
    private int bufferPos = 0;
    private int limit = 0;

    private Exception lastException = null;

    enum Block {WORD, LINE, WHITESPACE};

    public SimpleScanner(Reader reader) {
        this.reader = reader;
        readerClosed = false;
        buffer = new char[BUFFER_SIZE];
        updateBuffer();
    }

    public SimpleScanner(File source, String charsetName) 
        throws FileNotFoundException, UnsupportedEncodingException {
        this(new InputStreamReader(new FileInputStream(source), charsetName));
    }

    public SimpleScanner(InputStream inputStream) {
        this(new InputStreamReader(inputStream));
    }

    public SimpleScanner(String inputString) {
        this(new StringReader(inputString));
    }

    public String next() 
        throws IllegalStateException, NoSuchElementException {
        String word;
        do {
            word = next(Block.WORD);
        } while (word.isEmpty());
        return word;
    }

    public int nextInt()
        throws IllegalStateException, NoSuchElementException, InputMismatchException {
        try {
            return Integer.parseInt(next());
        } catch (NumberFormatException nfe) {
            throw new InputMismatchException("Next has another type.");
        }
    }

    public String nextLine()
        throws IllegalStateException, NoSuchElementException {
        return next(Block.LINE);
    }

    public String next(Block block) 
        throws IllegalStateException, NoSuchElementException {
        if (!hasNextLine()) {
            throw new NoSuchElementException("Input has been reached.");
        }
        boolean separatorFound = false;
        StringBuilder blockBuilder = new StringBuilder();
        int end = bufferPos, start = bufferPos;
        do {
            for (end = start; end < limit; end++) {
                if (isSeparator(buffer[end], block)) {
                    separatorFound = true;
                    break;
                }
            }
            if (separatorFound) {
                blockBuilder.append(buffer, start, end - start);
                break;
            } else {
                blockBuilder.append(buffer, start, limit - start);
                start = 0;
            }
        } while (updateBuffer());
        bufferPos = end;
        if (block != Block.WHITESPACE) {
            bufferPos++;
        }

        return blockBuilder.toString();
    }

    private boolean isSeparator(char ch, Block block) {
        if (block == Block.LINE) {
            return ch == '\n' || ch == '\r';

        } else if (block == Block.WORD) {
            return (!Character.isLetter(ch) && ch != '\'' &&
                Character.getType(ch) != Character.DASH_PUNCTUATION);

        } else if (block == Block.WHITESPACE) {
            return (Character.isLetter(ch) || ch == '\'' ||
                Character.getType(ch) == Character.DASH_PUNCTUATION);
        }
        return true;
    }

    private boolean updateBuffer() {
        if (readerClosed) {
            return false;
        }
        int readResult = 0;
        try {
            readResult = reader.read(buffer);
        } catch (IOException ioe) {
            lastException = ioe;
            readResult = -1;
        }
        if (readResult == -1) {
            closeReader();
            return false;
        } else {
            if (readResult != BUFFER_SIZE) {
                closeReader();
            }
            limit = readResult;
            return true;
        }
    }

    public boolean hasNextLine() throws IllegalStateException {
        if (scannerClosed) {
            throw new IllegalStateException("Scanner closed.");
        }
        return !readerClosed || bufferPos < limit;
    }

    public boolean hasNext() throws IllegalStateException {
        if (!hasNextLine()) {
            return false;
        }
        next(Block.WHITESPACE);
        return !readerClosed || bufferPos < limit;
    }

    public void close() {
        if (!scannerClosed) {
            closeReader();
            scannerClosed = true;
        }
    }

    private void closeReader() {
        try {
            readerClosed = true;
            reader.close();
        } catch (IOException e) {
            lastException = e;
        }
    }

    public Exception getLastException() {
        return lastException;
    }
}