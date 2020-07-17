import java.io.*;
import java.util.*;

public class IniHandler {
    protected IniHandler(String fileName, boolean isCaseSensitive)
    {
        this.mIniFileName = fileName;
        this.mSections = new HashMap<String, IniSection>();
        this.mSectionOrderList = new LinkedList<String>();
        this.mIsCaseSensitive = isCaseSensitive;
    }

    public static IniHandler loadIniFile(String fileName, boolean isCaseSensitive, String charSet)
    {
        IniHandler iniHandlerObj = sIniObjMap.get(fileName);
        if ( iniHandlerObj != null )
            return iniHandlerObj;

        iniHandlerObj = new IniHandler(fileName, isCaseSensitive);
        iniHandlerObj = loadIniHandler( fileName, charSet, iniHandlerObj );
        return iniHandlerObj;
    }

    public static IniHandler loadIniFile(String fileName)
    {
        return IniHandler.loadIniFile(fileName, true, "UTF-8");
    }

    public static void unCacheIniFile( String filePath )
    {
        if ( sIniObjMap.containsKey( filePath ))
            sIniObjMap.remove( filePath );
    }

    public LinkedList<String> getPrivateProfileSectionNames()
    {
        return mSectionOrderList;
    }

    public HashMap<String, KeyValuePair> getPrivateProfileSection(String sectionName)
    {
        HashMap<String, KeyValuePair> keyValueMap = null;
        if ( hasSection(sectionName) )
        {
            IniSection iniSection = getSection(sectionName);
            keyValueMap = iniSection.getKeyValueMap();
        }
        return keyValueMap;
    }

    public TreeSet<String> getAllValues(String sectionName)
    {
        TreeSet<String> value= null;
        if ( hasSection(sectionName) )
        {
            IniSection iniSection = getSection(sectionName);
            value = iniSection.getAllValues();
        }
        return value;
    }

    public LinkedList<Object> getAllLinesOfSection(String sectionName)
    {
        LinkedList<Object> value= null;
        if ( hasSection(sectionName) )
        {
            IniSection iniSection = getSection(sectionName);
            value = iniSection.getIniSectionLines();
        }
        return value;
    }

    public LinkedList<String> getPrivateProfileString(String sectionName, String keyName)
    {
        LinkedList<String> keyValue = null;
        if ( sectionName == null )
        {
            return getPrivateProfileSectionNames();
        }
        if ( hasSection(sectionName) )
        {
            IniSection iniSection = getSection(sectionName);
            if ( keyName == null )
            {
                return getAllKeyNames(sectionName);
            }

            if ( iniSection.hasKey(keyName) )
            {
                keyValue = new LinkedList<String>();
                keyValue.add(iniSection.getKeyValue(keyName));
            }
        }
        return keyValue;
    }

    public int getPrivateProfileInt(String sectionName, String keyName)
    {
        int keyValue = -1;
        if ( hasSection(sectionName) )
        {
            IniSection iniSection = getSection(sectionName);
            if ( iniSection.hasKey(keyName) )
            {
                String keyValueStr = iniSection.getKeyValue(keyName);
                keyValue = Integer.valueOf(keyValueStr);
            }
        }
        return keyValue;
    }

    public int writePrivateProfileSection(String sectionName, HashMap<String, String> keyValuePair)
    {
        //Adds the section if it does not exist and returns true otherwise returns false.
        if ( addNewSection( sectionName ))
        {
            //add the new key value pairs.
            Set<String> keyNamesSet = keyValuePair.keySet();
            for ( String aKeyNamesSet : keyNamesSet )
            {
                String keyValue = keyValuePair.get(aKeyNamesSet);
                setKeyValue(sectionName, aKeyNamesSet, keyValue);
            }
        }
        else
        {
            //if the section exists, delete the existing keys and values of the section.
            LinkedList<String> oldKeyList = getAllKeyNames(sectionName);
            for ( String anOldKeyList : oldKeyList )
            {
                removeKey(sectionName, anOldKeyList);
            }
            //add the new key value pairs.
            boolean emptyLineAtEnd = false;
            IniSection iniSection = getSection(sectionName);
            if ( iniSection.getIniSectionLines().contains("") )
            {
                iniSection.getIniSectionLines().remove("");
                emptyLineAtEnd = true;
            }
            Set<String> keyNamesSet = keyValuePair.keySet();
            for (String aKeyNamesSet : keyNamesSet)
            {
                String keyValue = keyValuePair.get(aKeyNamesSet);
                setKeyValue(sectionName, aKeyNamesSet, keyValue);
            }
            if ( emptyLineAtEnd )
            {
                iniSection.getIniSectionLines().add("");
            }
        }
        try
        {
            //saving back into the ini file
            saveIniFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return FAILURE;
        }
        return SUCCESS;
    }

    public int writePrivateProfileString(String sectionName, String keyName, String keyValue)
    {
        addNewSection(sectionName);
        if ( keyName == null )
        {
            //removing the section from the ini file as the key name is null
            removeSection(sectionName);
            try
            {
                // saving back into the ini file
                saveIniFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return FAILURE;
            }
            return FAILURE;
        }

        if ( keyValue == null )
        {
            //removing the key from the section name as the key value is null
            removeKey(sectionName, keyName);
            try
            {
                // saving back into the ini file
                saveIniFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return FAILURE;
            }
            return FAILURE;
        }
        setKeyValue(sectionName, keyName, keyValue);
        try
        {
            // saving back into the ini file
            saveIniFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return FAILURE;
        }
        return SUCCESS;
    }

    // ---------------------------------------------------------------------------------------------------- //

    protected static IniHandler loadIniHandler( String fileName, String charSet, IniHandler iniHandlerObj )
    {
        try
        {
            File iniFile = new File(fileName);
            InputStream inputStream = new FileInputStream(iniFile);

            // Check for byte-order marks.
            final byte[] bom_utf16be = { (byte) 0xFE, (byte) 0xFF };
            final byte[] bom_utf16le = { (byte) 0xFF, (byte) 0xFE };
            final byte[] bom_utf8    = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

            // Check for UTF-16 byte-order marks.
            boolean reopen = true;
            byte[] bom = new byte[2];
            int size = inputStream.read( bom );
            if ( size == 2 )
            {
                if ( Arrays.equals( bom, bom_utf16be ) )
                {
                    // this is UTF-16BE with a byte-order mark
                    charSet = "UTF-16BE";
                    reopen = false;
                }
                else if ( Arrays.equals( bom, bom_utf16le ) )
                {
                    // this is UTF-16LE with a byte-order mark
                    charSet = "UTF-16LE";
                    reopen = false;
                }
                else
                {
                    // Check for a UTF-8 byte-order mark.
                    bom = new byte[]{bom[0], bom[1], (byte) 0};
                    int n = inputStream.read();
                    if ( n != -1 )
                    {
                        bom[2] = (byte) n;
                        if ( Arrays.equals( bom, bom_utf8 ) )
                        {
                            // This is a UTF-8 file
                            charSet = "UTF-8";
                            reopen = false;
                        }
                    }
                }
            }

            if ( reopen )
            {
                // Reopen and use the given Charset.
                inputStream.close();
                inputStream = new FileInputStream( iniFile );
            }

            InputStreamReader streamReader = new InputStreamReader(inputStream, charSet);
            BufferedReader buffReader = new BufferedReader(streamReader);
            String curSection = null;
            String curLine;

            while (buffReader.ready())
            {
                curLine = buffReader.readLine().trim();
                if ( curLine.length() > 0 && curLine.charAt(0) == IniSection.HEADER_START )
                {
                    int endIndex = curLine.indexOf( IniSection.HEADER_END);
                    if ( endIndex >= 0 )
                    {
                        curSection = curLine.substring(1, endIndex);
                        iniHandlerObj.addNewSection(curSection);
                    }
                }
                if ( curSection != null )
                {
                    IniSection iniSectionObj = iniHandlerObj.getSection(curSection);
                    iniSectionObj.loadSectionKeys(buffReader);
                }
            }
            buffReader.close();
            sIniObjMap.put(fileName, iniHandlerObj);
        }
        catch (FileNotFoundException e)
        {
            iniHandlerObj = null;
            e.printStackTrace();
        }
        catch (IOException e)
        {
            iniHandlerObj = null;
            e.printStackTrace();
        }
        return iniHandlerObj;
    }

    protected void saveIniFile() throws IOException
    {
        File iniFile = new File(this.mIniFileName);
        OutputStream outStream = new FileOutputStream(iniFile);
        OutputStreamWriter streamWriter = new OutputStreamWriter(outStream);
        PrintWriter writer = new PrintWriter(streamWriter, true);
        for (String aMSectionOrderList : mSectionOrderList)
        {
            IniSection iniSection = getSection( aMSectionOrderList );
            writer.println(iniSection.getBracketedHeader());
            iniSection.save(writer);
        }
        outStream.close();
    }

    protected void setKeyValue(String section, String keyName, String value)
    {
        if ( hasSection(section) )
        {
            getSection(section).setValueOfKey(keyName, value);
        }
        else
        {
            throw new NoSuchSectionException(section);
        }
    }

    protected boolean removeSection(String sectionName)
    {
        String normSecName = normalizeSectionName(sectionName);
        if ( hasSection(normSecName) )
        {
            this.mSections.remove(normSecName);
            this.mSectionOrderList.remove(normSecName);
            return true;
        }
        return false;
    }

    protected boolean removeKey(String section, String keyName)
    {
        if ( hasSection(section) )
        {
            return getSection(section).removeKeyFromSection(keyName);
        }
        throw new NoSuchSectionException(section);
    }

    protected boolean hasSection(String sectionName)
    {
        return this.mSections.containsKey(normalizeSectionName(sectionName));
    }

    protected boolean addNewSection(String sectionName)
    {
        String normSecName = normalizeSectionName(sectionName);
        if ( !hasSection(normSecName) )
        {
            // Section constructor might throw IllegalArgumentException
            IniSection section = new IniSection(normSecName, this.mIsCaseSensitive);
            this.mSections.put(normSecName, section);
            this.mSectionOrderList.add(normSecName);
            return true;
        }
        return false;
    }

    protected LinkedList<String> getAllKeyNames(String section)
    {
        if ( hasSection(section) )
        {
            return getSection(section).getAllKeyNames();
        }
        throw new NoSuchSectionException(section);
    }

    protected String normalizeSectionName(String sectionName)
    {
        if ( !this.mIsCaseSensitive )
        {
            sectionName = sectionName.toLowerCase();
        }
        return sectionName.trim();
    }

    protected IniSection getSection(String name)
    {
        return mSections.get(normalizeSectionName(name));
    }

    protected static int SUCCESS = 1;

    protected static int FAILURE = 0;

    protected static HashMap<String, IniHandler> sIniObjMap = new HashMap<String, IniHandler>();

    protected Map<String, IniSection> mSections;

    protected LinkedList<String> mSectionOrderList;

    protected boolean mIsCaseSensitive;

    protected String mIniFileName;

    //////////////////////////Inner Classes //////////////////////////
    /**
     * This class loads, edits and saves a section of an INI configuration file.
     */
    private static class IniSection
    {
        // constant to store the default key value pair format
        public static final String DEFAULT_KEY_VALUE_FORMAT = "%s%s%s";

        // constant for the start of the section header
        public static final char HEADER_START = '[';

        // constant for the end of the section header
        public static final char HEADER_END = ']';

        public IniSection(String sectionName, boolean isCaseSensitive)
        {
            if ( !isValidName(sectionName) )
            {
                throw new IllegalArgumentException("Illegal section name:" + sectionName);
            }
            this.mSectioName = sectionName;
            this.mIsCaseSensitive = isCaseSensitive;
            this.mKeyValueMap = new HashMap<String, KeyValuePair>();
            this.mIniSectionLines = new LinkedList<Object>();
            this.mKeyDelimitors = DEFAULT_KEY_VALUE_DELIMS;
            this.mCommentDelims = DEFAULT_COMMENT_DELIMS;
            this.mKeyFormat = new KeyFormat(DEFAULT_KEY_VALUE_FORMAT);
        }

        public void addComment(String comment, char delim)
        {
            StringTokenizer st = new StringTokenizer(comment.trim(), NEWLINE_CHARS);
            while (st.hasMoreTokens())
            {
                this.mIniSectionLines.add(new Comment(st.nextToken(), delim));
            }
        }

        public void addBlankLine()
        {
            this.mIniSectionLines.add("");
        }

        public LinkedList<String> getAllKeyNames()
        {
            LinkedList<String> keyNames = new LinkedList<String>();
            for (Object line : mIniSectionLines)
            {
                if (line instanceof KeyValuePair)
                {
                    keyNames.add(((KeyValuePair) line).getKeyName());
                }
            }
            return keyNames;
        }

        public TreeSet<String> getAllValues()
        {
            TreeSet<String> values = new TreeSet<String>();
            for (Object line : this.mIniSectionLines)
            {
                if (line instanceof KeyValuePair)
                {
                    values.add(((KeyValuePair) line).getKeyValue());
                }
            }
            return values;
        }

        public boolean hasKey(String name)
        {
            return this.mKeyValueMap.containsKey(normalizeKeyName(name));
        }

        public String getKeyValue(String keyName)
        {
            String normKey = normalizeKeyName(keyName);
            String keyValue = null;
            if ( hasKey(normKey) )
            {
                keyValue = getKey(normKey).getKeyValue();
            }
            return keyValue;
        }

        public void setValueOfKey(String key, String value)
        {
            String normKeyName = normalizeKeyName(key);
            if ( hasKey(normKeyName) )
            {
                getKey(normKeyName).set(value);
            }
            else
            {
                // key constructor might throw IllegalArgumentException
                KeyValuePair keyValuePairObj = new KeyValuePair(normKeyName, value, this.mKeyDelimitors[0], this.mKeyFormat);
                this.mKeyValueMap.put(normKeyName, keyValuePairObj);
                this.mIniSectionLines.add(keyValuePairObj);
            }
        }

        public boolean removeKeyFromSection(String keyName)
        {
            String normKeyName = normalizeKeyName(keyName);
            if ( hasKey(normKeyName) )
            {
                this.mIniSectionLines.remove(getKey(normKeyName));
                this.mKeyValueMap.remove(normKeyName);
                return true;
            }
            return false;
        }

        public void loadSectionKeys(BufferedReader reader) throws IOException
        {
            while (reader.ready())
            {
                reader.mark(NAME_MAXLENGTH);
                String line = reader.readLine().trim();

                // Check for section header
                if ( line.length() > 0 && line.charAt(0) == HEADER_START )
                {
                    reader.reset();
                    return;
                }

                int delimIndex;
                // blank line
                if ( line.equals("") )
                {
                    addBlankLine();
                }
                // comment line
                else if ( (delimIndex = Arrays.binarySearch(this.mCommentDelims, line.charAt(0))) >= 0 )
                {
                    addComment(line.substring(1), this.mCommentDelims[delimIndex]);
                }
                // key line
                else
                {
                    delimIndex = -1;
                    int delimNum;
                    int lastSpaceIndex = -1;
                    for (int i = 0, l = line.length(); i < l && delimIndex < 0; i++)
                    {
                        delimNum = Arrays.binarySearch(this.mKeyDelimitors, line.charAt(i));
                        if ( delimNum >= 0 )
                        {
                            delimIndex = i;
                        }
                        else
                        {
                            if(IniSection.KEY_DELIMS_WHITESPACE==line.charAt(i))
                            {
                                lastSpaceIndex=i;
                            }
                            else if(lastSpaceIndex>=0)
                            {
                                break;
                            }
                        }
                    }
                    // delimiter at start of line
                    if ( delimIndex < 0 )
                    {
                        if ( lastSpaceIndex < 0 )
                        {
                            this.setValueOfKey(line, "");
                        }
                        else
                        {
                            this.setValueOfKey(line.substring(0, lastSpaceIndex), line.substring(lastSpaceIndex + 1));
                        }
                    }
                    // delimiter found
                    else if ( delimIndex > 0 )
                    {
                        this.setValueOfKey(line.substring(0, delimIndex), line.substring(delimIndex + 1));
                    }
                }
            }
        }

        /**
         * Prints this section to a print writer.
         *
         * @param writer where to write
         * @throws IOException at an I/O problem
         */
        public void save(PrintWriter writer) throws IOException
        {
            for (Object sectionName : this.mIniSectionLines)
            {
                if (!sectionName.toString().equals(""))
                {
                    writer.println(sectionName.toString());
                }
                else
                {
                    writer.println("");
                }
            }
            if ( writer.checkError() )
            {
                throw new IOException();
            }
        }

        public HashMap<String, KeyValuePair> getKeyValueMap()
        {
            return mKeyValueMap;
        }

        public LinkedList<Object> getIniSectionLines()
        {
            return mIniSectionLines;
        }

        /**
         * Returns the bracketed header of this section as appearing in an
         * actual INI file.
         *
         * @return the section's name in brackets
         */
        private String getBracketedHeader()
        {
            return HEADER_START + this.mSectioName + HEADER_END;
        }

        /**
         * Checks a string for validity as a section name. It can't contain the
         * characters '[' and ']'. An empty string or one consisting only of
         * white space isn't allowed either.
         *
         * @param sectionName
         *            the name to validate
         * @return true if the name validates as a section name
         */
        private static boolean isValidName(String sectionName)
        {
            if ( sectionName.trim().equals("") )
            {
                return false;
            }
            for (char aChar : INVALID_NAME_CHARS)
            {
                if (sectionName.indexOf(aChar) >= 0)
                {
                    return false;
                }
            }
            return true;
        }

        /**
         * Normalizes an arbitrary string for use as an key name, ie makes it
         * lower-case (provided this section isn't case-sensitive) and trims
         * leading and trailing white space.
         *
         * @param name
         *            the string to be used as key name
         * @return a normalized key name
         */
        private String normalizeKeyName(String name)
        {
            if ( !this.mIsCaseSensitive )
            {
                name = name.toLowerCase();
            }
            return name.trim();
        }

        /**
         * Returns an actual KeyValuePair instance.
         *
         * @param keyName
         *            the name of the key, assumed to be normed already (!)
         * @return the requested KeyValuePair instance
         * @throws NullPointerException
         *             if no option with the specified name exists
         */
        private KeyValuePair getKey(String keyName)
        {
            return this.mKeyValueMap.get(keyName);
        }

        /**
         * character array of default key value delimiters.
         */
        private static final char[] DEFAULT_KEY_VALUE_DELIMS = new char[]
                {
                        '=', ':'
                };

        /**
         * character array of default comment delimiters.
         */
        private static final char[] DEFAULT_COMMENT_DELIMS = new char[]
                {
                        '#', ';'
                };

        /**
         * array of default charater that could be present between key, delimiters and value.
         */
        private static final char KEY_DELIMS_WHITESPACE = '\t';


        /**
         * constant containing the number of characters that may be read while still preserving the mark.
         */
        private static final int NAME_MAXLENGTH = 1024;

        /**
         * character array containing the invalid characters for a section name.
         */
        private static final char[] INVALID_NAME_CHARS =
                {
                        HEADER_START, HEADER_END
                };

        /**
         * constant for new line character.
         */
        private static final String NEWLINE_CHARS = "\n\r";

        /**
         * string to store this section's name.
         */
        private String mSectioName;

        /**
         * Map to store the normalized key name and the corresponding KeyValuePair instance.
         */
        private HashMap<String, KeyValuePair> mKeyValueMap;

        /**
         * List storing the comments, blank lines, key value pairs of this section.
         */
        private LinkedList<Object> mIniSectionLines;

        /**
         * the key value delimiters.
         */
        private char[] mKeyDelimitors;

        /**
         * the comment delimiters.
         */
        private char[] mCommentDelims;

        /**
         * boolean variable which is true if the ini file is case sensitive or false if it is case-insensitive.
         */
        private boolean mIsCaseSensitive;

        /**
         * the key value format instance.
         */
        private KeyFormat mKeyFormat;
    }

    /**
     * Interface to be implemented by other private classes which has only one method toString().
     *
     */
    private interface ILine
    {
        /**
         * This method is to be implemented which returns the string represtation of the class.
         * @return The value as a String
         */
        public String toString();
    }

    /**
     * Private class to format the comments in the ini file.
     */
    private static class Comment implements ILine
    {
        /** Constructor
         *
         * @param comment the comment in the ini file
         */
        public Comment(String comment)
        {
            this(comment, DEFAULT_DELIMITER);
        }

        /** Constructor
         *
         * @param comment the comment in the ini file
         * @param delimiter the comments delimiter character.
         */
        public Comment(String comment, char delimiter)
        {
            this.mComment = comment.trim();
            this.mDelimiterChar = delimiter;
        }

        /**
         * Implementation of the ILine.toString()
         * @return string representation of this instance of comment.
         */
        public String toString()
        {
            return this.mDelimiterChar + " " + this.mComment;
        }

        /**
         * the default comment delimiter.
         */
        private static final char DEFAULT_DELIMITER = '#';

        /**
         * string storing this comment.
         */
        private String mComment;

        /**
         * the delimiter character.
         */
        private char mDelimiterChar;
    }

    /**
     * This class handles the key value pairs for a section
     *
     */
    public static class KeyValuePair implements ILine
    {
        /**
         * Constructor
         *
         * @param name the key name
         * @param value the key value
         * @param separator the key value separator
         * @param format the format of the key.
         */
        public KeyValuePair(String name, String value, char separator, KeyFormat format)
        {
            if ( !isValidName(name, separator) )
            {
                throw new IllegalArgumentException("Illegal key name:" + name);
            }
            this.mKeyName = name;
            this.mKeyValueSeparator = separator;
            this.mKeyValueFormat = format;
            set(value);
        }

        /**
         * Implementation of the ILine.toString()
         * @return string representation of this instance of key value pair.
         */
        public String toString()
        {
            return this.mKeyValueFormat.format(this.mKeyName, this.mKeyValue, this.mKeyValueSeparator);
        }

        /**
         * Sets the value of the key for this KeyValuePair.
         * @param value the key value.
         */
        public void set(String value)
        {
            if ( value == null )
            {
                this.mKeyValue = value;
            }
            else
            {
                StringTokenizer st = new StringTokenizer(value.trim(), ILLEGAL_VALUE_CHARS);
                StringBuffer sb = new StringBuffer();
                while (st.hasMoreTokens())
                {
                    sb.append(st.nextToken());
                }
                this.mKeyValue = sb.toString();
            }
        }

        public String getKeyName()
        {
            return this.mKeyName;
        }

        public String getKeyValue()
        {
            return this.mKeyValue;
        }

        /**
         * Checks a string for validity as a key name. It can't contain key value separator character.
         *
         * @param name
         *            the name to validate
         * @param separator the key value separator character
         * @return true if the name validates as a section name
         */
        private static boolean isValidName(String name, char separator)
        {
            return ! name.trim().equals( "" ) && name.indexOf( separator ) < 0;
        }

        /**
         * the illegal character to be present in the key value.
         */
        private static final String ILLEGAL_VALUE_CHARS = "\n\r";

        /**
         * this key name string represntation.
         */
        private String mKeyName;

        /**
         * this key value string represntation.
         */
        private String mKeyValue;

        /**
         * the key value separator.
         */
        private char mKeyValueSeparator;

        /**
         * key value pair format example %s %s %s.
         */
        private KeyFormat mKeyValueFormat;
    }

    /**
     * This class takes care of the key value pair format. The default is "%s %s %s" with "=" as the default delimiter
     *
     */
    private static class KeyFormat
    {
        /**
         * Constructor for specifying the format of key value. The format specified is varified before being set.
         * @param formatString the key format
         */
        public KeyFormat(String formatString)
        {
            this.mFormatTokens = this.compileFormat(formatString);
        }

        /**
         * Returns formatted string representation of the key name, value and separator.
         * @param name the key name
         * @param value the key value
         * @param separator the key value separator
         * @return formatted string representation of the key.
         */
        public String format(String name, String value, char separator)
        {
            StringBuffer formattedString = new StringBuffer();
            formattedString.append(mFormatTokens[0]);
            formattedString.append(name);
            formattedString.append(mFormatTokens[1]);
            formattedString.append(separator);
            formattedString.append(mFormatTokens[2]);
            formattedString.append(value);
            formattedString.append(mFormatTokens[3]);
            return formattedString.toString();
        }

        /**
         * This method checks the key format supplied. Throws IllegalArgumentException in
         * case of key format not being proper.
         * @param formatString the key format
         * @return the format token array.
         */
        private String[] compileFormat(String formatString)
        {
            String[] tokens =
                    {
                            "", "", "", ""
                    };
            int tokenCount = 0;
            boolean seenPercent = false;
            StringBuffer token = new StringBuffer();
            for (int i = 0; i < formatString.length(); i++)
            {
                switch (formatString.charAt(i))
                {
                    case '%':
                        if ( seenPercent )
                        {
                            token.append("%");
                            seenPercent = false;
                        }
                        else
                        {
                            seenPercent = true;
                        }
                        break;
                    case 's':
                        if ( seenPercent )
                        {
                            if ( tokenCount >= EXPECTED_TOKENS )
                            {
                                throw new IllegalArgumentException("Illegal key format. Too many %s placeholders.");
                            }
                            tokens[tokenCount] = token.toString();
                            tokenCount++;
                            token = new StringBuffer();
                            seenPercent = false;
                        }
                        else
                        {
                            token.append("s");
                        }
                        break;
                    default:
                        if ( seenPercent )
                        {
                            throw new IllegalArgumentException("Illegal key format. Unknown format specifier.");
                        }
                        token.append(formatString.charAt(i));
                        break;
                }
            }
            if ( tokenCount != EXPECTED_TOKENS - 1 )
            {
                throw new IllegalArgumentException("Illegal key format. Not enough %s placeholders.");
            }
            tokens[tokenCount] = token.toString();
            return tokens;
        }

        /**
         * maximum expected number of tokens.
         */
        private static final int EXPECTED_TOKENS = 4;

        /**
         * key value pair format tokens which will be used to parse the ini file.
         */
        private String[] mFormatTokens;
    }

    /**
     * Thrown when an inexistent section is addressed.
     */
    public static class NoSuchSectionException extends RuntimeException
    {
        public NoSuchSectionException()
        {
            super();
        }

        public NoSuchSectionException(String msg)
        {
            super(msg);
        }
    }
}
