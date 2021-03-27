package com.garethevans.church.opensongtablet.sqlite;

// This file holds the column and table names
// It also has the build SQL query for the table.

// This class is used to build the song database and then query it for searches
// If the database is good to go (after full index) we get songs from here = quicker
// If the database isn't fully indexed, we load the song from the file
// If we create, update, delete songs we do that to both the SQL database and the file
// Non OpenSong songs (PDF, Images) have their details stored in a persistent database
// This is stored in the Settings folder to allow syncing between devices
// After indexing songs, PDFs and images will just have the songid, folder and filename to begin with
// We then update their records in the SQL database using the persistent NonSQLDatabase entries
// Updates to PDFs and images therefore require edits in the SQL database and the NonSQLDatabase
// We only need to touch the PDF/image if we delete it


public class SQLite {

    // The initialisers
    public SQLite() {}

/*
    The values of the database and Song object should be in alphabetical order...
    (Values in brackets are the xml file fields if they are different)

    abc (abcnotation)
    aka
    alttheme
    author
    autoscrolldelay
    autoscrolllength
    capo
    capoprint
    ccli
    copyright
    customchords
    filename
    filetype
    folder
    hymnnum
    id
    key
    linkaudio
    linkother
    linkweb
    linkyoutube
    lyrics
    metronomebpm
    midi
    midiindex
    notes
    padfile
    padloop
    presentationorder (presentation)
    songid
    theme
    timesig
    title
    user1
    user2
    user3

*/


    // The table columns
    public static final String DATABASE_NAME = "Songs.db";
    public static final String NON_OS_DATABASE_NAME = "NonOpenSongSongs.db";
    public static final String TABLE_NAME = "songs";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SONGID = "songid";
    public static final String COLUMN_FILENAME = "filename";
    public static final String COLUMN_FOLDER = "folder";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_COPYRIGHT = "copyright";
    public static final String COLUMN_LYRICS = "lyrics";
    public static final String COLUMN_HYMNNUM = "hymn_num";
    public static final String COLUMN_CCLI = "ccli";
    public static final String COLUMN_THEME = "theme";
    public static final String COLUMN_ALTTHEME = "alttheme";
    public static final String COLUMN_USER1 = "user1";
    public static final String COLUMN_USER2 = "user2";
    public static final String COLUMN_USER3 = "user3";
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_TIMESIG = "timesig";
    public static final String COLUMN_AKA = "aka";
    public static final String COLUMN_AUTOSCROLL_DELAY = "autoscrolldelay";
    public static final String COLUMN_AUTOSCROLL_LENGTH = "autoscrolllength";
    public static final String COLUMN_METRONOME_BPM = "metronomebpm";
    public static final String COLUMN_PAD_FILE = "padfile";
    public static final String COLUMN_PAD_LOOP = "padloop";
    public static final String COLUMN_MIDI = "midi";
    public static final String COLUMN_MIDI_INDEX = "midiindex";
    public static final String COLUMN_CAPO = "capo";
    public static final String COLUMN_CAPO_PRINT = "capoprint";
    public static final String COLUMN_CUSTOM_CHORDS = "customchords";
    public static final String COLUMN_NOTES = "notes";
    public static final String COLUMN_ABC = "abc";
    public static final String COLUMN_LINK_YOUTUBE = "linkyoutube";
    public static final String COLUMN_LINK_WEB = "linkweb";
    public static final String COLUMN_LINK_AUDIO = "linkaudio";
    public static final String COLUMN_LINK_OTHER = "linkother";
    public static final String COLUMN_PRESENTATIONORDER = "presentationorder";
    public static final String COLUMN_FILETYPE = "filetype";

    // Create table SQL query.  Because this will have non OpenSong stuff too, include all useable fields
    static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_SONGID + " TEXT UNIQUE,"
                    + COLUMN_FILENAME + " TEXT,"
                    + COLUMN_FOLDER + " TEXT,"
                    + COLUMN_TITLE + " TEXT,"
                    + COLUMN_AUTHOR + " TEXT,"
                    + COLUMN_COPYRIGHT + " TEXT,"
                    + COLUMN_LYRICS + " TEXT,"
                    + COLUMN_HYMNNUM + " TEXT,"
                    + COLUMN_CCLI + " TEXT,"
                    + COLUMN_THEME + " TEXT,"
                    + COLUMN_ALTTHEME + " TEXT,"
                    + COLUMN_USER1 + " TEXT,"
                    + COLUMN_USER2 + " TEXT,"
                    + COLUMN_USER3 + " TEXT,"
                    + COLUMN_KEY + " TEXT,"
                    + COLUMN_TIMESIG + " TEXT,"
                    + COLUMN_AKA + " TEXT,"
                    + COLUMN_AUTOSCROLL_DELAY + " TEXT,"
                    + COLUMN_AUTOSCROLL_LENGTH + " TEXT,"
                    + COLUMN_METRONOME_BPM + " TEXT,"
                    + COLUMN_PAD_FILE + " TEXT,"
                    + COLUMN_PAD_LOOP + " TEXT,"
                    + COLUMN_MIDI + " TEXT,"
                    + COLUMN_MIDI_INDEX + " TEXT,"
                    + COLUMN_CAPO + " TEXT,"
                    + COLUMN_CAPO_PRINT + " TEXT,"
                    + COLUMN_CUSTOM_CHORDS + " TEXT,"
                    + COLUMN_NOTES + " TEXT,"
                    + COLUMN_ABC + " TEXT,"
                    + COLUMN_LINK_YOUTUBE + " TEXT,"
                    + COLUMN_LINK_WEB + " TEXT,"
                    + COLUMN_LINK_AUDIO + " TEXT,"
                    + COLUMN_LINK_OTHER + " TEXT,"
                    + COLUMN_PRESENTATIONORDER + " TEXT,"
                    + COLUMN_FILETYPE + " TEXT"
                    + ");";
}

/*

package com.garethevans.church.opensongtablet;

// This class is used to build the song database and then query it for searches
class SQLite {

    static final String DATABASE_NAME = "Songs.db";
    static final String TABLE_NAME = "songs";
    static final String COLUMN_ID = "id";
    static final String COLUMN_SONGID = "songid";
    static final String COLUMN_FILENAME = "filename";
    static final String COLUMN_FOLDER = "folder";
    static final String COLUMN_TITLE = "title";
    static final String COLUMN_AUTHOR = "author";
    static final String COLUMN_COPYRIGHT = "copyright";
    static final String COLUMN_LYRICS = "lyrics";
    static final String COLUMN_HYMNNUM = "hymn_num";
    static final String COLUMN_CCLI = "ccli";
    static final String COLUMN_THEME = "theme";
    static final String COLUMN_ALTTHEME = "alttheme";
    static final String COLUMN_USER1 = "user1";
    static final String COLUMN_USER2 = "user2";
    static final String COLUMN_USER3 = "user3";
    static final String COLUMN_KEY = "key";
    static final String COLUMN_TIMESIG = "timesig";
    static final String COLUMN_TEMPO = "tempo";
    static final String COLUMN_AKA = "aka";

    private int id;
    private String songid;
    private String filename;
    private String folder;
    private String title;
    private String author;
    private String copyright;
    private String lyrics;
    private String hymn_num;
    private String ccli;
    private String theme;
    private String alttheme;
    private String user1;
    private String user2;
    private String user3;
    private String key;
    private String timesig;
    private String tempo;
    private String aka;

    SQLite() {

    }

    SQLite(int id, String songid, String filename, String folder, String title, String author,
           String copyright, String lyrics, String hymn_num, String ccli, String theme,
           String alttheme, String user1, String user2, String user3, String key, String timesig, String tempo, String aka) {
        this.id = id;
        this.songid = songid;
        this.filename = filename;
        this.folder = folder;
        this.title = title;
        this.author = author;
        this.copyright = copyright;
        this.lyrics = lyrics;
        this.hymn_num = hymn_num;
        this.ccli = ccli;
        this.theme = theme;
        this.alttheme = alttheme;
        this.user1 = user1;
        this.user2 = user2;
        this.user3 = user3;
        this.key = key;
        this.aka = aka;
        this.timesig = timesig;
        this.tempo = tempo;
    }

    int getId() {
        return id;
    }
    String getSongid() {
        return songid;
    }
    String getFilename() {
        return filename;
    }
    String getFolder() {
        return folder;
    }
    String getTitle() {
        return title;
    }
    String getAuthor() {
        return author;
    }
    String getCopyright() {
        return copyright;
    }
    String getLyrics() {
        return lyrics;
    }
    String getHymn_num() {
        return hymn_num;
    }
    String getCcli() {
        return ccli;
    }
    String getTheme() {
        return theme;
    }
    String getAlttheme() {
        return alttheme;
    }
    String getUser1() {
        return user1;
    }
    String getUser2() {
        return user2;
    }
    String getUser3() {
        return user3;
    }
    String getKey() {
        return key;
    }
    String getTimesig() {return timesig;}
    String getTempo() {return tempo;}
    String getAka() {
        return aka;
    }

    void setId(int id) {
        this.id = id;
    }
    void setSongid(String songid) {
        this.songid = songid;
    }
    void setFilename(String filename) {
        this.filename = filename;
    }
    void setFolder(String folder) {
        this.folder = folder;
    }
    void setTitle(String title) {
        this.title = title;
    }
    void setAuthor(String author) {
        this.author = author;
    }
    void setCopyright(String copyright) {
        this.copyright = copyright;
    }
    void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }
    void setHymn_num(String hymn_num) {
        this.hymn_num = hymn_num;
    }
    void setCcli(String ccli) {
        this.ccli = ccli;
    }
    void setTheme(String theme) {
        this.theme = theme;
    }
    void setAlttheme(String alttheme) {
        this.alttheme = alttheme;
    }
    void setUser1(String user1) {
        this.user1 = user1;
    }
    void setUser2(String user2) {
        this.user2 = user2;
    }
    void setUser3(String user3) {
        this.user3 = user3;
    }
    void setKey(String key) {
        this.key = key;
    }
    void setTimesig(String timesig) {this.timesig = timesig;}
    void setTempo(String tempo) {this.tempo = tempo;}
    void setAka(String aka) {
        this.aka = aka;
    }

    // Create table SQL query - only including fields which are searchable or used in the song index
    static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_SONGID + " TEXT UNIQUE,"
                    + COLUMN_FILENAME + " TEXT,"
                    + COLUMN_FOLDER + " TEXT,"
                    + COLUMN_TITLE + " TEXT,"
                    + COLUMN_AUTHOR + " TEXT,"
                    + COLUMN_COPYRIGHT + " TEXT,"
                    + COLUMN_LYRICS + " TEXT,"
                    + COLUMN_HYMNNUM + " TEXT,"
                    + COLUMN_CCLI + " TEXT,"
                    + COLUMN_THEME + " TEXT,"
                    + COLUMN_ALTTHEME + " TEXT,"
                    + COLUMN_USER1 + " TEXT,"
                    + COLUMN_USER2 + " TEXT,"
                    + COLUMN_USER3 + " TEXT,"
                    + COLUMN_KEY + " TEXT,"
                    + COLUMN_TIMESIG + " TEXT,"
                    + COLUMN_TEMPO + " TEXT,"
                    + COLUMN_AKA + " TEXT"
                    + ");";

}
*/