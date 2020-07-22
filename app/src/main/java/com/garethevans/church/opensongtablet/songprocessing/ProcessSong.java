package com.garethevans.church.opensongtablet.songprocessing;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.garethevans.church.opensongtablet.R;
import com.garethevans.church.opensongtablet.performance.PerformanceFragment;
import com.garethevans.church.opensongtablet.preferences.Preferences;
import com.garethevans.church.opensongtablet.preferences.StaticVariables;

import java.util.ArrayList;
import java.util.Map;


public class ProcessSong {

    // These is used when loading and converting songs (ChordPro, badly formatted XML, etc).
    public String parseHTML(String s) {
        if (s == null) {
            return "";
        }
        s = s.replace("&amp;apos;", "'");
        s = s.replace("&amp;quote;", "\"");
        s = s.replace("&amp;quot;", "\"");
        s = s.replace("&amp;lt;", "<");
        s = s.replace("&amp;gt;", ">");
        s = s.replace("&amp;", "&");
        s = s.replace("&lt;", "<");
        s = s.replace("&gt;", ">");
        s = s.replace("&apos;", "'");
        s = s.replace("&quote;", "\"");
        s = s.replace("&quot;", "\"");
        return s;
    }

    public String parseToHTMLEntities(String s) {
        if (s == null) {
            s = "";
        }
        // Make sure all ss are unencoded to start with
        // Now HTML encode everything that needs encoded
        // Protected are < > &
        // Change < to __lt;  We'll later replace the __ with &.  Do this to deal with &amp; separately
        s = s.replace("<", "__lt;");
        s = s.replace("&lt;", "__lt;");

        // Change > to __gt;  We'll later replace the __ with &.  Do this to deal with &amp; separately
        s = s.replace(">", "__gt;");
        s = s.replace("&gt;", "__gt;");

        // Change &apos; to ' as they don't need encoding in this format - also makes it compatible with desktop
        s = s.replace("&apos;", "'");
        //s = s.replace("\'", "'");

        // Change " to __quot;  We'll later replace the __ with &.  Do this to deal with &amp; separately
        s = s.replace("\"", "__quot;");
        s = s.replace("&quot;", "__quot;");

        // Now deal with the remaining ampersands
        s = s.replace("&amp;", "&");  // Reset any that already encoded - all need encoded now
        s = s.replace("&&", "&");     // Just in case we have wrongly encoded old ones e.g. &amp;&quot;
        s = s.replace("&", "&amp;");  // Reencode all remaining ampersands

        // Now replace the other protected encoded entities back with their leading ampersands
        s = s.replace("__lt;", "&lt;");
        s = s.replace("__gt;", "&gt;");
        s = s.replace("__quot;", "&quot;");

        return s;
    }

    public String parseFromHTMLEntities(String val) {
        //Fix broken stuff
        if (val == null) {
            val = "";
        }
        val = val.replace("&amp;apos;", "'");
        val = val.replace("&amp;quote;", "\"");
        val = val.replace("&amp;quot;", "\"");
        val = val.replace("&amp;lt;", "<");
        val = val.replace("&amp;gt;", ">");
        val = val.replace("&amp;", "&");
        val = val.replace("&lt;", "<");
        val = val.replace("&gt;", ">");
        val = val.replace("&apos;", "'");
        val = val.replace("&quote;", "\"");
        val = val.replace("&quot;", "\"");
        return val;
    }

    public String fixStartOfLines(String lyrics) {
        StringBuilder fixedlyrics = new StringBuilder();
        String[] lines = lyrics.split("\n");

        for (String line : lines) {
            if (!line.startsWith("[") && !line.startsWith(";") && !line.startsWith(".") && !line.startsWith(" ") &&
                    !line.startsWith("1") && !line.startsWith("2") && !line.startsWith("3") && !line.startsWith("4") &&
                    !line.startsWith("5") && !line.startsWith("6") && !line.startsWith("7") && !line.startsWith("8") &&
                    !line.startsWith("9") && !line.startsWith("-")) {
                line = " " + line;
            } else if (line.matches("^[0-9].*$") && line.length() > 1 && !line.substring(1, 2).equals(".")) {
                // Multiline verse
                line = line.substring(0, 1) + ". " + line.substring(1);
            }
            fixedlyrics.append(line).append("\n");
        }
        return fixedlyrics.toString();
    }

    String fixLineBreaksAndSlashes(String s) {
        s = s.replace("\r\n", "\n");
        s = s.replace("\r", "\n");
        s = s.replace("\n\n\n", "\n\n");
        s = s.replace("&quot;", "\"");
        s = s.replace("\\'", "'");
        s = s.replace("&quot;", "\"");
        s = s.replace("<", "(");
        s = s.replace(">", ")");
        s = s.replace("&#39;", "'");
        s = s.replace("\t", "    ");
        s = s.replace("\\'", "'");

        return s;
    }

    String determineLineTypes(String string, Context c) {
        String type;
        if (string.indexOf(".") == 0) {
            type = "chord";
        } else if (string.indexOf(";__" + c.getResources().getString(R.string.edit_song_capo)) == 0) {
            type = "capoinfo";
        } else if (string.indexOf(";__") == 0) {
            type = "extra";
            //} else if (string.startsWith(";"+c.getString(R.string.music_score))) {
            //    type = "abcnotation";
        } else if (string.startsWith(";") && string.length() > 4 && (string.indexOf("|") == 2 || string.indexOf("|") == 3)) {
            // Used to do this by identifying type of string start or drum start
            // Now just look for ;*| or ;**| where * is anything such as ;e | or ;BD|
            type = "tab";
        } else if (string.startsWith(";") && string.contains("1") && string.contains("+") && string.contains("2")) {
            // Drum tab count line
            type = "tab";
        } else if (string.startsWith(";")) {
            type = "comment";
        } else if (string.startsWith("[")) {
            type = "heading";
        } else {
            type = "lyric";
        }
        return type;
    }

    String howToProcessLines(int linenum, int totallines, String thislinetype, String nextlinetype, String previouslinetype) {
        String what;
        // If this is a chord line followed by a lyric line.
        if (linenum < totallines - 1 && thislinetype.equals("chord") &&
                (nextlinetype.equals("lyric") || nextlinetype.equals("comment"))) {
            what = "chord_then_lyric";
        } else if (thislinetype.equals("chord") && (nextlinetype.equals("") || nextlinetype.equals("chord"))) {
            what = "chord_only";
        } else if (thislinetype.equals("lyric") && !previouslinetype.equals("chord")) {
            what = "lyric_no_chord";
        } else if (thislinetype.equals("comment") && !previouslinetype.equals("chord")) {
            what = "comment_no_chord";
        } else if (thislinetype.equals("capoinfo")) {
            what = "capo_info";
        } else if (thislinetype.equals("extra")) {
            what = "extra_info";
        } else if (thislinetype.equals("tab")) {
            what = "guitar_tab";
        } else if (thislinetype.equals("heading")) {
            what = "heading";
            //} else if (thislinetype.equals("abcnotation")) {
            //    what = "abc_notation";
        } else {
            what = "null"; // Probably a lyric line with a chord above it - already dealt with
        }
        return what;
    }

    String fixLineLength(String string, int newlength) {
        int extraspacesrequired = newlength - string.length();
        StringBuilder stringBuilder = new StringBuilder(string);
        for (int x = 0; x < extraspacesrequired; x++) {
            stringBuilder.append(" ");
        }
        string = stringBuilder.toString();
        return string;
    }

    String[] getChordPositions(String string) {
        // Given a chord line, get the character positions that each chord starts at
        // Go through the line character by character
        // If the character isn't a " " and the character before is " " or "|" it's a new chord
        // Add the positions to an array
        ArrayList<String> chordpositions = new ArrayList<>();

        // Set the start of the line as the first bit
        chordpositions.add("0");

        // In order to identify chords at the end of the line
        // (My method looks for a following space)
        // Add a space to the search string.
        string += " ";

        for (int x = 1; x < string.length(); x++) {

            String thischar = "";
            boolean thischarempty = false;
            if (x < string.length() - 1) {
                thischar = string.substring(x, x + 1);
            }
            if (thischar.equals(" ") || thischar.equals("|")) {
                thischarempty = true;
            }

            String prevchar;
            boolean prevcharempty = false;
            prevchar = string.substring(x - 1, x);
            if (prevchar.equals(" ") || prevchar.equals("|")) {
                prevcharempty = true;
            }

            if (!thischarempty && prevcharempty) {
                // This is a chord position
                chordpositions.add(x + "");
            }
        }

        String[] chordpos = new String[chordpositions.size()];
        chordpos = chordpositions.toArray(chordpos);
        return chordpos;
    }

    String[] getChordSections(String string, String[] pos_string) {
        // Go through the chord positions and extract the substrings
        ArrayList<String> chordsections = new ArrayList<>();
        int startpos = 0;
        int endpos = -1;

        if (string == null) {
            string = "";
        }
        if (pos_string == null) {
            pos_string = new String[0];
        }

        for (int x = 0; x < pos_string.length; x++) {
            if (pos_string[x].equals("0")) {
                // First chord is at the start of the line
                startpos = 0;
            } else if (x == pos_string.length - 1) {
                // Last chord, so end position is end of the line
                // First get the second last section
                endpos = Integer.parseInt(pos_string[x]);
                if (startpos < endpos) {
                    chordsections.add(string.substring(startpos, endpos));
                }

                // Now get the last one
                startpos = Integer.parseInt(pos_string[x]);
                endpos = string.length();
                if (startpos < endpos) {
                    chordsections.add(string.substring(startpos, endpos));
                }

            } else {
                // We are at the start of a chord somewhere other than the start or end
                // Get the bit of text in the previous section;
                endpos = Integer.parseInt(pos_string[x]);
                if (startpos < endpos) {
                    chordsections.add(string.substring(startpos, endpos));
                }
                startpos = endpos;
            }
        }
        if (startpos == 0 && endpos == -1) {
            // This is just a chord line, so add the whole line
            chordsections.add(string);
        }
        String[] sections = new String[chordsections.size()];
        sections = chordsections.toArray(sections);

        return sections;
    }

    String[] getLyricSections(String string, String[] pos_string) {
        // Go through the chord positions and extract the substrings
        ArrayList<String> lyricsections = new ArrayList<>();
        int startpos = 0;
        int endpos = -1;

        if (string == null) {
            string = "";
        }
        if (pos_string == null) {
            pos_string = new String[0];
        }

        for (int x = 0; x < pos_string.length; x++) {
            if (pos_string[x].equals("0")) {
                // First chord is at the start of the line
                startpos = 0;
            } else if (x == pos_string.length - 1) {
                // Last chord, so end position is end of the line
                // First get the second last section
                endpos = Integer.parseInt(pos_string[x]);
                if (startpos < endpos) {
                    lyricsections.add(string.substring(startpos, endpos));
                }

                // Now get the last one
                startpos = Integer.parseInt(pos_string[x]);
                endpos = string.length();
                if (startpos < endpos) {
                    lyricsections.add(string.substring(startpos, endpos));
                }

            } else {
                // We are at the start of a chord somewhere other than the start or end
                // Get the bit of text in the previous section;
                endpos = Integer.parseInt(pos_string[x]);
                if (startpos < endpos) {
                    lyricsections.add(string.substring(startpos, endpos));
                }
                startpos = endpos;
            }
        }

        if (startpos == 0 && endpos < 0) {
            // Just add the line
            lyricsections.add(string);
        }

        String[] sections = new String[lyricsections.size()];
        sections = lyricsections.toArray(sections);

        return sections;
    }

    public String parseLyrics(String myLyrics, Context c) {
        myLyrics = myLyrics.replace("]\n\n", "]\n");
        myLyrics = myLyrics.replaceAll("\r\n", "\n");
        myLyrics = myLyrics.replaceAll("\r", "\n");
        myLyrics = myLyrics.replaceAll("\\t", "    ");
        myLyrics = myLyrics.replaceAll("\f", "    ");
        myLyrics = myLyrics.replace("\r", "");
        myLyrics = myLyrics.replace("\t", "    ");
        myLyrics = myLyrics.replace("\b", "    ");
        myLyrics = myLyrics.replace("\f", "    ");
        myLyrics = myLyrics.replace("&#27;", "'");
        myLyrics = myLyrics.replace("&#027;", "'");
        myLyrics = myLyrics.replace("&#39;", "'");
        myLyrics = myLyrics.replace("&#34;", "'");
        myLyrics = myLyrics.replace("&#039;", "'");
        myLyrics = myLyrics.replace("&ndash;", "-");
        myLyrics = myLyrics.replace("&mdash;", "-");
        myLyrics = myLyrics.replace("&apos;", "'");
        myLyrics = myLyrics.replace("&lt;", "<");
        myLyrics = myLyrics.replace("&gt;", ">");
        myLyrics = myLyrics.replace("&quot;", "\"");
        myLyrics = myLyrics.replace("&rdquo;", "'");
        myLyrics = myLyrics.replace("&rdquor;", "'");
        myLyrics = myLyrics.replace("&rsquo;", "'");
        myLyrics = myLyrics.replace("&rdquor;", "'");
        myLyrics = myLyrics.replaceAll("\u0092", "'");
        myLyrics = myLyrics.replaceAll("\u0093", "'");
        myLyrics = myLyrics.replaceAll("\u2018", "'");
        myLyrics = myLyrics.replaceAll("\u2019", "'");

        // If UG has been bad, replace these bits:
        myLyrics = myLyrics.replace("pre class=\"\"", "");

        if (!StaticVariables.whichSongFolder.contains(c.getResources().getString(R.string.slide)) &&
                !StaticVariables.whichSongFolder.contains(c.getResources().getString(R.string.image)) &&
                !StaticVariables.whichSongFolder.contains(c.getResources().getString(R.string.note)) &&
                !StaticVariables.whichSongFolder.contains(c.getResources().getString(R.string.scripture))) {
            myLyrics = myLyrics.replace("Slide 1", "[V1]");
            myLyrics = myLyrics.replace("Slide 2", "[V2]");
            myLyrics = myLyrics.replace("Slide 3", "[V3]");
            myLyrics = myLyrics.replace("Slide 4", "[V4]");
            myLyrics = myLyrics.replace("Slide 5", "[V5]");
        }

        // Make double tags into single ones
        myLyrics = myLyrics.replace("[[", "[");
        myLyrics = myLyrics.replace("]]", "]");

        // Make lowercase start tags into caps
        myLyrics = myLyrics.replace("[v", "[V");
        myLyrics = myLyrics.replace("[b", "[B");
        myLyrics = myLyrics.replace("[c", "[C");
        myLyrics = myLyrics.replace("[t", "[T");
        myLyrics = myLyrics.replace("[p", "[P");

        // Replace [Verse] with [V] and [Verse 1] with [V1]
        String languageverse = c.getResources().getString(R.string.tag_verse);
        String languageverse_lowercase = languageverse.toLowerCase(StaticVariables.locale);
        String languageverse_uppercase = languageverse.toUpperCase(StaticVariables.locale);
        myLyrics = myLyrics.replace("[" + languageverse_lowercase, "[" + languageverse);
        myLyrics = myLyrics.replace("[" + languageverse_uppercase, "[" + languageverse);
        myLyrics = myLyrics.replace("[" + languageverse + "]", "[V]");
        myLyrics = myLyrics.replace("[" + languageverse + " 1]", "[V1]");
        myLyrics = myLyrics.replace("[" + languageverse + " 2]", "[V2]");
        myLyrics = myLyrics.replace("[" + languageverse + " 3]", "[V3]");
        myLyrics = myLyrics.replace("[" + languageverse + " 4]", "[V4]");
        myLyrics = myLyrics.replace("[" + languageverse + " 5]", "[V5]");
        myLyrics = myLyrics.replace("[" + languageverse + " 6]", "[V6]");
        myLyrics = myLyrics.replace("[" + languageverse + " 7]", "[V7]");
        myLyrics = myLyrics.replace("[" + languageverse + " 8]", "[V8]");
        myLyrics = myLyrics.replace("[" + languageverse + " 9]", "[V9]");

        // Replace [Chorus] with [C] and [Chorus 1] with [C1]
        String languagechorus = c.getResources().getString(R.string.tag_chorus);
        String languagechorus_lowercase = languagechorus.toLowerCase(StaticVariables.locale);
        String languagechorus_uppercase = languagechorus.toUpperCase(StaticVariables.locale);
        myLyrics = myLyrics.replace("[" + languagechorus_lowercase, "[" + languagechorus);
        myLyrics = myLyrics.replace("[" + languagechorus_uppercase, "[" + languagechorus);
        myLyrics = myLyrics.replace("[" + languagechorus + "]", "[C]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 1]", "[C1]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 2]", "[C2]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 3]", "[C3]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 4]", "[C4]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 5]", "[C5]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 6]", "[C6]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 7]", "[C7]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 8]", "[C8]");
        myLyrics = myLyrics.replace("[" + languagechorus + " 9]", "[C9]");

        // Try to convert ISO / Windows
        myLyrics = myLyrics.replace("\0x91", "'");

        // Get rid of BOMs and stuff
        myLyrics = myLyrics.replace("\uFEFF", "");
        myLyrics = myLyrics.replace("\uFEFF", "");
        myLyrics = myLyrics.replace("[&#x27;]", "");
        myLyrics = myLyrics.replace("[\\xEF]", "");
        myLyrics = myLyrics.replace("[\\xBB]", "");
        myLyrics = myLyrics.replace("[\\xFF]", "");
        myLyrics = myLyrics.replace("\\xEF", "");
        myLyrics = myLyrics.replace("\\xBB", "");
        myLyrics = myLyrics.replace("\\xFF", "");

        return myLyrics;
    }

    private String getLineType(String string) {
        if (string.startsWith(".")) {
            return "chord";
        } else if (string.startsWith(";") && string.contains("|")) {
            return "tab";
        } else if (string.startsWith(";")) {
            return "comment";
        } else if (string.startsWith("[")) {
            return "heading";
        } else {
            return "lyrics";
        }
    }

    private String trimOutLineIdentifiers(Context c, String linetype, String string) {
        switch (linetype) {
            case "heading":
                string = fixHeading(c, string);
                break;
            case "chord":
            case "comment":
            case "tab":
                if (string.length() > 0) {
                    string = string.substring(1);
                }
                break;
            case "lyric":
            default:
                if (string.startsWith(" ")) {
                    string = string.replaceFirst(" ", "");
                }
                break;
        }
        return string;
    }

    private String fixHeading(Context c, String line) {
        line = line.replace("[", "");
        line = line.replace("]", "");

        switch (line) {
            case "V-":
            case "V - ":
            case "V":
            case "V1":
            case "V2":
            case "V3":
            case "V4":
            case "V5":
            case "V6":
            case "V7":
            case "V8":
            case "V9":
            case "V10":
            case "V1-":
            case "V2-":
            case "V3-":
            case "V4-":
            case "V5-":
            case "V6-":
            case "V7-":
            case "V8-":
            case "V9-":
            case "V10-":
            case "V1 -":
            case "V2 -":
            case "V3 -":
            case "V4 -":
            case "V5 -":
            case "V6 -":
            case "V7 -":
            case "V8 -":
            case "V9 -":
            case "V - 10":
                line = line.replace("V", c.getResources().getString(R.string.tag_verse) + " ");
                line = line.replace("-", "");
                break;

            case "T-":
            case "T -":
            case "T":
            case "T1":
            case "T2":
            case "T3":
            case "T4":
            case "T5":
            case "T6":
            case "T7":
            case "T8":
            case "T9":
            case "T10":
                line = line.replace("T", c.getResources().getString(R.string.tag_tag) + " ");
                break;

            case "C-":
            case "C -":
            case "C":
            case "C1":
            case "C2":
            case "C3":
            case "C4":
            case "C5":
            case "C6":
            case "C7":
            case "C8":
            case "C9":
            case "C10":
                line = line.replace("C", c.getResources().getString(R.string.tag_chorus) + " ");
                break;

            case "B-":
            case "B -":
            case "B":
            case "B1":
            case "B2":
            case "B3":
            case "B4":
            case "B5":
            case "B6":
            case "B7":
            case "B8":
            case "B9":
            case "B10":
                line = line.replace("B", c.getResources().getString(R.string.tag_bridge) + " ");
                break;

            case "P-":
            case "P -":
            case "P":
            case "P1":
            case "P2":
            case "P3":
            case "P4":
            case "P5":
            case "P6":
            case "P7":
            case "P8":
            case "P9":
            case "P10":
                line = line.replace("P", c.getResources().getString(R.string.tag_prechorus) + " ");
                break;
        }
        return line;
    }

    // This is used for preparing the lyrics as views
    // When processing the lyrics, chords+lyrics or chords+comments or multiple chords+chords are processed
    // as groups of lines and returned as a TableLayout containing two or more rows to allow alignment
    @SuppressWarnings("ConstantConditions")


    // Splitting the song up in to manageable chunks
    private String makeGroups(String string) {
        String[] lines = string.split("\n");
        StringBuilder sb = new StringBuilder();

        // Go through each line and add bits together as groups ($_groupline_$ between bits, \n for new group)
        int i = 0;
        while (i < lines.length) {
            if (lines[i].startsWith(".")) {
                // This is a chord line = this needs to be part of a group
                sb.append("\n").append(lines[i]);
                // If the next line is a lyric or comment add this to the group and stop there
                int nl = i + 1;
                boolean stillworking = true;
                if (shouldNextLineBeAdded(nl, lines, true)) {
                    sb.append("____groupline_____").append(lines[nl]);
                    while (stillworking) {
                        // Keep going for multiple lines to be added
                        if (shouldNextLineBeAdded(nl + 1, lines, false)) {
                            i = nl;
                            nl++;
                            sb.append("____groupline_____").append(lines[nl]);
                        } else {
                            i++;
                            stillworking = false;
                        }
                    }
                } else if (nl < lines.length && lines[nl].startsWith(".")) {
                    // While the next line is still a chordline add this line
                    while (nl < lines.length && lines[nl].startsWith(".")) {
                        sb.append("____groupline_____").append(lines[nl]);
                        i = nl;
                        nl++;
                    }
                }
            } else {
                sb.append("\n").append(lines[i]);
            }
            i++;
        }
        return sb.toString();
    }

    private boolean shouldNextLineBeAdded(int nl, String[] lines, boolean incnormallyricline) {
        if (incnormallyricline) {
            return (nl < lines.length && (lines[nl].startsWith(" ") || lines[nl].startsWith(";") ||
                    lines[nl].matches("^[0-9].*$")));
        } else {
            return (nl < lines.length && (lines[nl].matches("^[0-9].*$")));
        }
    }

    private String makeSections(String string) {
        string = string.replace("\n\n\n", "\n \n____SPLIT____");
        string = string.replace("\n \n \n", "\n \n____SPLIT____");
        string = string.replace("\n\n", "\n \n____SPLIT____");
        string = string.replace("\n \n", "\n \n____SPLIT____");
        string = string.replace("\n[", "\n____SPLIT____[");
        string = string.replace("\n [", "\n____SPLIT____[");
        string = string.replace("\n[", "\n____SPLIT____[");
        string = string.replace("____SPLIT________SPLIT____", "____SPLIT____");
        if (string.trim().startsWith("____SPLIT____")) {
            string = string.replaceFirst(" ____SPLIT____", "");
            while (string.startsWith("\n") || string.startsWith(" ")) {
                if (string.startsWith(" ")) {
                    string = string.replaceFirst(" ", "");
                } else {
                    string = string.replaceFirst("\n", "");
                }
            }
        }
        if (string.startsWith("____SPLIT____")) {
            string = string.replaceFirst("____SPLIT____", "");
        }
        return string;

    }

    private TableLayout groupTable(Context c, String string, float headingScale, float commentScale,
                                   float chordScale, int lyricColor, int chordColor,
                                   boolean trimLines, float lineSpacing) {
        TableLayout tableLayout = newTableLayout(c);
        // Split the group into lines
        String[] lines = string.split("____groupline_____");

        // Line 0 is the chord line.  All other lines need to be at least this size
        // Make it 1 char bigger to identify the end of it
        lines[0] += " ";
        if (lineIsChordForMultiline(lines)) {
            lines[0] = ". " + lines[0].substring(1);
        }

        int minlength = lines[0].length();
        for (int i = 0; i < lines.length; i++) {
            int length = lines[i].length();
            if (length < minlength) {
                for (int z = 0; z < (minlength - length); z++) {
                    lines[i] += " ";
                }
            }
        }

        // Get the positions of the chords.  Each will be the start of a new section
        // Start at the beginning
        ArrayList<Integer> pos = new ArrayList<>();
        pos.add(0);
        boolean lookforstart = false;

        for (int s = 1; s < (lines[0].length() - 1); s++) {
            String mychar = lines[0].substring(s, s + 1);
            if (!mychar.equals(" ") && lookforstart) {
                // This is the start of a new chord!
                pos.add(s);
                lookforstart = false;
            } else if (mychar.equals(" ") && !lookforstart) {
                // We've finished the chord.  Look for new one
                lookforstart = true;
            }
        }

        // Now we have the sizes, split into individual TextViews inside a TableRow for each line
        for (int t = 0; t < lines.length; t++) {
            TableRow tableRow = newTableRow(c);
            String linetype = getLineType(lines[t]);
            Typeface typeface = getTypeface(linetype);
            float size = getFontSize(linetype, headingScale, commentScale, chordScale);
            int color = getFontColor(linetype, lyricColor, chordColor);
            int startpos = 0;
            for (int endpos : pos) {
                if (endpos != 0) {
                    TextView textView = newTextView(c, linetype, typeface, size, color, trimLines, lineSpacing);

                    String str = lines[t].substring(startpos, endpos);
                    if (startpos == 0) {
                        str = trimOutLineIdentifiers(c, linetype, str);
                    }
                    if (t == 0) {
                        str = str.trim() + " "; // Chords lines are the splitters, only have one blank space after the chord
                    }
                    textView.setText(str);
                    tableRow.addView(textView);
                    startpos = endpos;
                }
            }
            // Add the final position
            TextView textView = newTextView(c, linetype, typeface, size, color, trimLines, lineSpacing);
            String str = lines[t].substring(startpos);
            if (str.startsWith(".")) {
                str = str.replaceFirst(".", "");
            }
            if (t == 0) {
                str = str.trim() + " ";
            }
            textView.setText(str);
            tableRow.addView(textView);

            tableLayout.addView(tableRow);
        }
        return tableLayout;
    }

    private boolean isMultiLineFormatSong(String string) {
        // Best way to determine if the song is in multiline format is
        // Look for [v] or [c] case insensitive
        // And it needs to be followed by a line starting with 1 and 2
        try {
            String[] sl = string.split("\n");
            boolean has_multiline_vtag = false;
            boolean has_multiline_ctag = false;
            boolean has_multiline_1tag = false;
            boolean has_multiline_2tag = false;

            for (String l : sl) {
                if (l.toLowerCase(StaticVariables.locale).startsWith("[v]")) {
                    has_multiline_vtag = true;
                } else if (l.toLowerCase(StaticVariables.locale).startsWith("[c]")) {
                    has_multiline_ctag = true;
                } else if (l.toLowerCase(StaticVariables.locale).startsWith("1") ||
                        l.toLowerCase(StaticVariables.locale).startsWith(" 1")) {
                    has_multiline_1tag = true;
                } else if (l.toLowerCase(StaticVariables.locale).startsWith("2") ||
                        l.toLowerCase(StaticVariables.locale).startsWith(" 2")) {
                    has_multiline_2tag = true;
                }
            }

            return (has_multiline_vtag || has_multiline_ctag) && has_multiline_1tag && has_multiline_2tag;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean lineIsChordForMultiline(String[] lines) {
        return (lines[0].length()>1 && lines.length>1 && lines[1].matches("^[0-9].*$"));
    }
    String fixMultiLineFormat(Context c, Preferences preferences, String string) {

        if (!preferences.getMyPreferenceBoolean(c,"multiLineVerseKeepCompact",false) && isMultiLineFormatSong(string)) {
            // Reset the available song sections
            // Ok the song is in the multiline format
            // [V]
            // .G     C
            // 1Verse 1
            // 2Verse 2

            // Create empty verse and chorus strings up to 9 verses/choruses
            String[] verse = {"", "", "", "", "", "", "", "", ""};
            String[] chorus = {"", "", "", "", "", "", "", "", ""};

            StringBuilder versechords = new StringBuilder();
            StringBuilder choruschords = new StringBuilder();

            // Split the string into separate lines
            String[] lines = string.split("\n");

            // Go through the lines and look for tags and line numbers
            boolean gettingverse = false;
            boolean gettingchorus = false;
            for (int z = 0; z < lines.length; z++) {
                String l = lines[z];
                String l_1 = "";
                String l_2 = "";

                if (lines.length > z + 1) {
                    l_1 = lines[z + 1];
                }
                if (lines.length > z + 2) {
                    l_2 = lines[z + 2];
                }

                boolean mlv = isMultiLine(l, l_1, l_2, "v");
                boolean mlc = isMultiLine(l, l_1, l_2, "c");

                if (mlv) {
                    lines[z] = "__VERSEMULTILINE__";
                    gettingverse = true;
                    gettingchorus = false;
                } else if (mlc) {
                    lines[z] = "__CHORUSMULTILINE__";
                    gettingverse = false;
                    gettingchorus = true;
                } else if (l.startsWith("[")) {
                    gettingverse = false;
                    gettingchorus = false;
                }

                if (gettingverse) {
                    if (lines[z].startsWith(".")) {
                        versechords.append(lines[z]).append("\n");
                        lines[z] = "__REMOVED__";
                    } else if (Character.isDigit((lines[z] + " ").charAt(0))) {
                        int vnum = Integer.parseInt((lines[z] + " ").substring(0, 1));
                        if (verse[vnum].equals("")) {
                            verse[vnum] = "[V" + vnum + "]\n";
                        }
                        verse[vnum] += lines[z].substring(2) + "\n";
                        lines[z] = "__REMOVED__";
                    }
                } else if (gettingchorus) {
                    if (lines[z].startsWith(".")) {
                        choruschords.append(lines[z]).append("\n");
                        lines[z] = "__REMOVED__";
                    } else if (Character.isDigit((lines[z] + " ").charAt(0))) {
                        int cnum = Integer.parseInt((lines[z] + " ").substring(0, 1));
                        if (chorus[cnum].equals("")) {
                            chorus[cnum] = "[C" + cnum + "]\n";
                        }
                        chorus[cnum] += lines[z].substring(2) + "\n";
                        lines[z] = "__REMOVED__";
                    }
                }
            }

            // Get the replacement text
            String versereplacement = addchordstomultiline(verse, versechords.toString());
            String chorusreplacement = addchordstomultiline(chorus, choruschords.toString());

            // Now go back through the lines and extract the new improved version
            StringBuilder improvedlyrics = new StringBuilder();
            for (String thisline : lines) {
                if (thisline.equals("__VERSEMULTILINE__")) {
                    thisline = versereplacement;
                } else if (thisline.equals("__CHORUSMULTILINE__")) {
                    thisline = chorusreplacement;
                }
                if (!thisline.equals("__REMOVED__")) {
                    improvedlyrics.append(thisline).append("\n");
                }
            }

            return improvedlyrics.toString();
        } else {
            // Not multiline format, or not wanting to expand it
            return string;
        }
    }
    private boolean isMultiLine(String l, String l_1, String l_2, String type) {
        boolean isit = false;
        l = l.toLowerCase(StaticVariables.locale);

        if (l.startsWith("["+type+"]") &&
                (l_1.startsWith("1") || l_1.startsWith(" 1") || l_2.startsWith("1") || l_2.startsWith(" 1"))) {
            isit = true;
        }
        return isit;
    }
    private String addchordstomultiline(String[] multiline, String chords) {
        String[] chordlines = chords.split("\n");
        StringBuilder replacementtext = new StringBuilder();

        // Go through each verse/chorus in turn
        for (String sections:multiline) {
            String[] section = sections.split("\n");

            if (section.length == chordlines.length+1) {
                replacementtext.append(section[0]).append("\n");
                // Only works if there are the same number of lyric lines as chords!
                for (int x=0; x<chordlines.length; x++) {
                    replacementtext.append(chordlines[x]).append("\n").append(section[x + 1]).append("\n");
                }
                replacementtext.append("\n");
            } else {
                replacementtext.append(sections).append("\n");
            }
        }
        return replacementtext.toString();
    }


    private TextView lineText(Context c, String linetype, String string, Typeface typeface, float size,
                              int color, boolean trimLines, float lineSpacing) {
        TextView textView = newTextView(c,linetype,typeface,size,color,trimLines,lineSpacing);
        string = trimOutLineIdentifiers(c,linetype,string);
        textView.setText(string);
        return textView;
    }


    // Prepare the views
    private void clearAndResetLinearLayout(LinearLayout linearLayout, boolean removeViews) {
        if (linearLayout!=null) {
            if (removeViews) {
                linearLayout.removeAllViews();
            }
            linearLayout.setScaleX(1.0f);
            linearLayout.setScaleY(1.0f);
        }
    }
    private void clearAndResetRelativeLayout(RelativeLayout relativeLayout,boolean removeViews) {
        if (relativeLayout!=null) {
            if (removeViews) {
                relativeLayout.removeAllViews();
            }
            relativeLayout.setScaleX(1.0f);
            relativeLayout.setScaleY(1.0f);
        }
    }
    private void columnVisibility(LinearLayout c1, LinearLayout c2, LinearLayout c3, boolean v1, boolean v2, boolean v3) {
        if (v1) {
            c1.setVisibility(View.VISIBLE);
        } else {
            c1.setVisibility(View.GONE);
        }
        if (v2) {
            c2.setVisibility(View.VISIBLE);
        } else {
            c2.setVisibility(View.GONE);
        }
        if (v3) {
            c3.setVisibility(View.VISIBLE);
        } else {
            c3.setVisibility(View.GONE);
        }
    }
    public ArrayList<View> setSongInLayout(Context c, Preferences preferences, boolean trimSections,
                                           boolean addSectionSpace, boolean trimLines, float lineSpacing,
                                           Map<String,Integer> colorMap, float headingScale,
                                           float chordScale, float commentScale, String string) {
        ArrayList<View> sectionViews = new ArrayList<>();

        // This goes through processing the song

        // First check for multiverse/multiline formatting
        string = fixMultiLineFormat(c,preferences,string);

        // First up we go through the lyrics and group lines that should be in a table for alignment purposes
        string = makeGroups(string);
        // Next we generate the split points for sections
        string = makeSections(string);

        // Split into sections an process each separately
        String[] sections = string.split("____SPLIT____");

        for (int sect = 0; sect<sections.length; sect++) {
            String section = sections[sect];
            if (trimSections) {
                section = section.trim();
            }
            if (addSectionSpace && sect!=(sections.length-1)) { // Don't do for last section
                section = section + "\n ";
            }
            LinearLayout linearLayout = newLinearLayout(c); // transparent color
            int backgroundColor = colorMap.get("lyricsVerse");
            // Now split by line
            String[] lines = section.split("\n");
            for (String line:lines) {
                // Get the text stylings
                String linetype = getLineType(line);
                if (linetype.equals("heading") || linetype.equals("comment") || linetype.equals("tab")) {
                    backgroundColor = getBGColor(c,colorMap,line);
                }
                Typeface typeface = getTypeface(linetype);
                float size = getFontSize(linetype,headingScale,commentScale,chordScale);
                int color = getFontColor(linetype,colorMap.get("lyricsText"),colorMap.get("lyricsChords"));
                if (line.contains("____groupline_____")) {
                    linearLayout.addView(groupTable(c,line,headingScale,commentScale,chordScale,
                            colorMap.get("lyricsText"),colorMap.get("lyricsChords"),trimLines,lineSpacing));
                } else {
                    linearLayout.addView(lineText(c,linetype,line,typeface,size,color,
                            trimLines,lineSpacing));
                }
            }
            linearLayout.setBackgroundColor(backgroundColor);
            sectionViews.add(linearLayout);
        }
        return sectionViews;
    }


    // Get properties for creating the views
    private Typeface getTypeface(String string) {
        if (string.equals("chord")) {
            return StaticVariables.typefaceChords;
        } else if (string.equals("tab")) {
            return StaticVariables.typefaceMono;
        } else {
            return StaticVariables.typefaceLyrics;
        }
    }
    private int getFontColor(String string, int lyricColor, int chordColor) {
        if (string.equals("chord")) {
            return chordColor;
        } else {
            return lyricColor;
        }
    }
    private float getFontSize(String string, float headingScale, float commentScale, float chordScale) {
        float f = defFontSize;
        switch (string) {
            case "chord":
                f = defFontSize * chordScale;
                break;
            case "comment":
                f = defFontSize * commentScale;
                break;
            case "heading":
                f = defFontSize * headingScale;
                break;
        }
        return f;
    }
    private int getBGColor(Context c, Map<String,Integer> colorMap,String line) {
        if (line.startsWith(";")) {
            return colorMap.get("lyricsComment");
        } else if (fixHeading(c,line).contains(c.getString(R.string.tag_verse))) {
            return colorMap.get("lyricsVerse");
        } else if (fixHeading(c,line).contains(c.getString(R.string.tag_prechorus))) {
            return colorMap.get("lyricsPreChorus");
        } else if (fixHeading(c,line).contains(c.getString(R.string.tag_chorus))) {
            return colorMap.get("lyricsChorus");
        } else if (fixHeading(c,line).contains(c.getString(R.string.tag_bridge))) {
            return colorMap.get("lyricsBridge");
        } else if (fixHeading(c,line).contains(c.getString(R.string.tag_tag))) {
            return colorMap.get("lyricsTag");
        } else if (fixHeading(c,line).contains(c.getString(R.string.custom))) {
            return colorMap.get("lyricsCustom");
        } else {
            return colorMap.get("lyricsVerse");
        }
    }



    // Creating new blank views
    private TableLayout newTableLayout(Context c) {
        TableLayout tableLayout = new TableLayout(c);
        tableLayout.setPadding(0,0,0,0);
        tableLayout.setClipChildren(false);
        tableLayout.setClipToPadding(false);
        tableLayout.setDividerPadding(0);
        return tableLayout;
    }
    private TableRow newTableRow(Context c) {
        TableRow tableRow = new TableRow(c);
        tableRow.setPadding(0,0,0,0);
        tableRow.setDividerPadding(0);
        return tableRow;
    }
    private LinearLayout newLinearLayout(Context c) {
        LinearLayout linearLayout = new LinearLayout(c);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0,0,0,0);
        linearLayout.setLayoutParams(llp);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(0,0,0,0);
        linearLayout.setClipChildren(false);
        linearLayout.setClipToPadding(false);
        return linearLayout;
    }
    private TextView newTextView(Context c, String linetype, Typeface typeface, float size, int color,
                                 boolean trimLines, float lineSpacing) {
        TextView textView = new TextView(c);
        if (trimLines && Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            int trimval = (int) (size * lineSpacing);
            textView.setPadding(0, -trimval, 0, -trimval);
        } else {
            textView.setPadding(0,0,0,0);
        }
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setTextSize(size);
        textView.setTypeface(typeface);
        textView.setTextColor(color);
        textView.setIncludeFontPadding(false);
        if (linetype.equals("heading")) {
            textView.setPaintFlags(textView.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
        }
        return textView;
    }
    private FrameLayout newFrameLayout(Context c, int color) {
        FrameLayout frameLayout = new FrameLayout(c);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0,0,0,0);
        frameLayout.setLayoutParams(llp);
        frameLayout.setPadding(0,0,0,0);
        frameLayout.setClipToPadding(false);
        frameLayout.setClipChildren(false);
        frameLayout.setBackgroundColor(color);
        return frameLayout;
    }


    // Stuff for resizing/scaling
    private int padding = 8;
    private float defFontSize = 8.0f;
    private String thisAutoScale;
    private int getMaxValue(ArrayList<Integer> values, int start, int end) {
        int maxValue = 0;
        if (start>values.size()) {
            start = values.size();
        }
        if (end>values.size()) {
            end = values.size();
        }
        for (int i=start; i<end; i++) {
            maxValue = Math.max(maxValue,values.get(i));
        }
        return maxValue;
    }
    private int getTotal(ArrayList<Integer> values, int start, int end) {
        int total = 0;
        if (start>values.size()) {
            start = values.size();
        }
        if (end>values.size()) {
            end = values.size();
        }
        for (int i=start; i<end; i++) {
            total += values.get(i);
        }
        return total;
    }
    private void setMargins(LinearLayout linearLayout,int leftMargin, int rightMargin) {
        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams)linearLayout.getLayoutParams();
        llp.setMargins(leftMargin,0,rightMargin,0);
        linearLayout.setClipToPadding(false);
        linearLayout.setClipChildren(false);
        linearLayout.setLayoutParams(llp);
        linearLayout.setPadding(0,0,0,0);
    }
    private int dp2px(Context c) {
        float scale = c.getResources().getDisplayMetrics().density;
        return (int) (8 * scale);
    }
    private void setScaledView(LinearLayout innerColumn, float scaleSize, float maxFontSize) {
        innerColumn.setPivotX(0);
        innerColumn.setPivotY(0);
        // Don't scale above the preferred maximum font size
        float maxScaleSize = maxFontSize / defFontSize;
        if (scaleSize>maxScaleSize) {
            scaleSize = maxScaleSize;
        }
        innerColumn.setScaleX(scaleSize);
        innerColumn.setScaleY(scaleSize);
    }
    private void resizeColumn(LinearLayout column, int startWidth, int startHeight, float scaleSize) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (startWidth * scaleSize),
                (int) (startHeight * scaleSize));
        column.setLayoutParams(lp);
    }
    private int howManyColumnsAreBest(float col1, float[] col2, float[] col3, String autoScale,
                                      float fontSizeMin, boolean songAutoScaleOverrideFull) {
        // There's a few things to consider here.  Firstly, if scaling is off, best is 1 column.
        // If we are overriding full scale to width only, or 1 col to off, best is 1 column.

        if (autoScale.equals("N")||autoScale.equals("W")) {
            return 1;
        }

        float col2best = Math.min(col2[0],col2[1]);
        float col3best = Math.min(col3[0],Math.min(col3[1],col3[2]));
        int best;
        if (col1>col2best) {
            best = 1;
            if (col3best>col1) {
                best = 3;
            }
        } else {
            best = 2;
            if (col3best>col2best) {
                best = 3;
            }
        }
        // Default font size is 14sp when drawing. If scaling takes this below the min font Size, override back to 1 column
        if (best==2) {
            if (col2[2]==0) {
                return 1;
            }
            float newFontSize2Col = defFontSize*col2best;
            if (songAutoScaleOverrideFull && newFontSize2Col<fontSizeMin) {
                thisAutoScale = "W";
                return 1;
            }
        }
        if (best==3) {
            if (col3[3]==0) {
                return 2;
            }
            float newFontSize3Col = defFontSize*col3best;
            if (songAutoScaleOverrideFull && newFontSize3Col<fontSizeMin) {
                thisAutoScale = "W";
                return 1;
            }
        }
        return best;
    }


    // These are called from the VTO listener - draw the stuff to the screen as 1,2 or 3 columns
    public void addViewsToScreen(Context c, RelativeLayout testPane, RelativeLayout pageHolder, LinearLayout songView,
                                 int screenWidth, int screenHeight, LinearLayout column1,
                                 LinearLayout column2, LinearLayout column3, String autoScale,
                                 boolean songAutoScaleOverrideFull, boolean songAutoScaleOverrideWidth,
                                 boolean songAutoScaleColumnMaximise, float fontSize,
                                 float fontSizeMin, float fontSizeMax, ArrayList<View> sectionViews,
                                 ArrayList<Integer> sectionWidths, ArrayList<Integer> sectionHeights) {
        // Now we have all the sizes in, determines the best was to show the song
        // This will be single, two or three columns.  The best one will be the one
        // which gives the best scale size

        // Clear and reset the views
        clearAndResetRelativeLayout(testPane,true);
        clearAndResetRelativeLayout(pageHolder,false);
        clearAndResetLinearLayout(songView,false);
        pageHolder.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.WRAP_CONTENT,ScrollView.LayoutParams.WRAP_CONTENT));
        songView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT));
        clearAndResetLinearLayout(column1,true);
        clearAndResetLinearLayout(column2,true);
        clearAndResetLinearLayout(column3,true);

        // Set the padding and boxpadding from dp to px
        padding = dp2px(c);

        int currentWidth = getMaxValue(sectionWidths,0, sectionWidths.size());
        int currentHeight = getTotal(sectionHeights,0, sectionHeights.size());

        thisAutoScale = autoScale;

        // All scaling types need to process the single column view, either to use it or compare to 2/3 columns
        float[] scaleSize_2cols = new float[3];
        float[] scaleSize_3cols = new float[4];
        if (autoScale.equals("Y")) {
            // Figure out two and three columns.  Only do this if we need to to save processing time.
            scaleSize_2cols = col2Scale(screenWidth,screenHeight,currentHeight,songAutoScaleColumnMaximise,sectionWidths,sectionHeights);
            scaleSize_3cols = col3Scale(screenWidth,screenHeight,currentHeight,songAutoScaleColumnMaximise,sectionWidths,sectionHeights);
        }

        float scaleSize_1col = col1Scale(screenWidth,screenHeight,currentWidth,currentHeight);

        // Now we've used the views in measure, we need to remove them from the test pane, so we can reallocate them
        testPane.removeAllViews();

        // Now decide if 1,2 or 3 columns is best
        int howmany = howManyColumnsAreBest(scaleSize_1col,scaleSize_2cols,scaleSize_3cols,autoScale,fontSizeMin,songAutoScaleOverrideFull);

        switch (howmany) {
            case 1:
                // If we are using one column and resizing to width only, change the scale size
                if (autoScale.equals("W") || thisAutoScale.equals("W")) {
                    scaleSize_1col = (float)screenWidth /(float)currentWidth;
                    if (defFontSize*scaleSize_1col<fontSizeMin && songAutoScaleOverrideWidth) {
                        thisAutoScale = "N";
                    }
                }
                // If autoscale is off, scale to the desired fontsize
                if (autoScale.equals("N") || thisAutoScale.equals("N")) {
                    scaleSize_1col = fontSize / defFontSize;
                }
                setOneColumn(c,sectionViews,column1,column2,column3,currentWidth,currentHeight,scaleSize_1col,fontSizeMax);
                break;

            case 2:
                setTwoColumns(c,sectionViews,column1,column2,column3,sectionHeights,scaleSize_2cols,fontSizeMax,(int)((float)screenWidth/2.0f-padding));
                break;

            case 3:
                setThreeColumns(c,sectionViews,column1,column2,column3,sectionWidths,sectionWidths,scaleSize_3cols,fontSizeMax);
                break;
        }
    }


    // 1 column stuff
    private float col1Scale(int screenWidth, int screenHeight, int viewWidth, int viewHeight) {
        float x_scale = screenWidth / (float) viewWidth;
        float y_scale = screenHeight / (float) viewHeight;
        return Math.min(x_scale, y_scale);
    }
    private void setOneColumn(Context c, ArrayList<View> sectionViews, LinearLayout column1, LinearLayout column2, LinearLayout column3,
                              int currentWidth, int currentHeight, float scaleSize, float maxFontSize) {
        columnVisibility(column1,column2,column3,false,false,false);
        LinearLayout innerCol1 = newLinearLayout(c);

        int color;
        // For each section, add it to a relayivelayout to deal with the background colour.
        for (View v:sectionViews) {
            color = Color.TRANSPARENT;
            Drawable background = v.getBackground();
            if (background instanceof ColorDrawable) {
                color = ((ColorDrawable) background).getColor();
            }
            FrameLayout frameLayout = newFrameLayout(c,color);
            frameLayout.addView(v);
            innerCol1.addView(frameLayout);
        }
        setScaledView(innerCol1, scaleSize, maxFontSize);
        resizeColumn(innerCol1, currentWidth, currentHeight, scaleSize);
        setMargins(column1,0,0);
        column1.setPadding(0,0,0,0);
        column1.setClipChildren(false);
        column1.setClipToPadding(false);
        column1.addView(innerCol1);
        columnVisibility(column1,column2,column3,true,false,false);
        PerformanceFragment.songViewWidth = (int) (currentWidth * scaleSize);
        PerformanceFragment.songViewHeight = (int) (currentHeight * scaleSize);
    }


    // 2 column stuff
    private float[] col2Scale(int screenWidth, int screenHeight, int totalViewHeight, boolean songAutoScaleColumnMaximise,
                              ArrayList<Integer> viewWidth, ArrayList<Integer> viewHeight) {
        float[] colscale = new float[3];

        // Now go through the views and decide on the number for the first column (the rest is the second column)
        int col1Height = 0;
        int col2Height = totalViewHeight;
        int preHalfWay = 0;
        int postHalfWay = 0;
        int i = 0;
        while (i<viewHeight.size() && col1Height<col2Height) {
            preHalfWay = i;
            postHalfWay = preHalfWay+1;
            col1Height += viewHeight.get(i);
            col2Height -= viewHeight.get(i);
            i++;
        }

        // Get the max width for pre halfway split column 1
        int maxWidth_preHalfWay1 = getMaxValue(viewWidth,0,preHalfWay);
        int maxWidth_preHalfWay2 = getMaxValue(viewWidth,preHalfWay,viewWidth.size());
        int totaHeight_preHalfWay1 = getTotal(viewHeight,0,preHalfWay);
        int totaHeight_preHalfWay2 = getTotal(viewHeight,preHalfWay,viewHeight.size());
        int maxWidth_postHalfWay1 = getMaxValue(viewWidth,0,postHalfWay);
        int maxWidth_postHalfWay2 = getMaxValue(viewWidth,postHalfWay,viewWidth.size());
        int totaHeight_postHalfWay1 = getTotal(viewHeight,0,postHalfWay);
        int totaHeight_postHalfWay2 = getTotal(viewHeight,postHalfWay,viewHeight.size());

        // Get pre and post halfway scales
        float halfWidth = ((float)screenWidth/2.0f)- padding - 0.5f;
        float preCol1scaleX = halfWidth / (float) maxWidth_preHalfWay1;
        float preCol1scaleY = (float) screenHeight / (float) totaHeight_preHalfWay1;
        float preCol1Scale = Math.min(preCol1scaleX,preCol1scaleY);
        float preCol2scaleX = halfWidth / (float) maxWidth_preHalfWay2;
        float preCol2scaleY = (float) screenHeight / (float) totaHeight_preHalfWay2;
        float preCol2Scale = Math.min(preCol2scaleX,preCol2scaleY);

        float postCol1scaleX = halfWidth / (float) maxWidth_postHalfWay1;
        float postCol1scaleY = (float) screenHeight/ (float) totaHeight_postHalfWay1;
        float postCol1Scale = Math.min(postCol1scaleX,postCol1scaleY);
        float postCol2scaleX = halfWidth / (float) maxWidth_postHalfWay2;
        float postCol2scaleY = (float) screenHeight / (float) totaHeight_postHalfWay2;
        float postCol2Scale = Math.min(postCol2scaleX,postCol2scaleY);

        // Prefer the method that gives the largest scaling of col1 + col2
        float preScaleTotal = preCol1Scale + preCol2Scale;
        float postScaleTotal = postCol1Scale + postCol2Scale;

        if (preScaleTotal>=postScaleTotal) {
            colscale[0] = preCol1Scale;
            colscale[1] = preCol2Scale;
            colscale[2] = preHalfWay;
        } else {
            colscale[0] = postCol1Scale;
            colscale[1] = postCol2Scale;
            colscale[2] = postHalfWay;
        }

        if (!songAutoScaleColumnMaximise) {
            // make 2 all the values of the smallest (but the same)
            float min = Math.min(colscale[0],colscale[1]);
            colscale[0] = min;
            colscale[1] = min;
        }

        return colscale;
    }
    private void setTwoColumns(Context c, ArrayList<View> sectionViews, LinearLayout column1,
                               LinearLayout column2, LinearLayout column3,
                               ArrayList<Integer> sectionHeights, float[] scaleSize,
                               float maxFontSize, int halfwidth) {
        // Use 2 column
        columnVisibility(column1,column2,column3,false,false,false);
        LinearLayout innerCol1 = newLinearLayout(c);
        LinearLayout innerCol2 = newLinearLayout(c);

        int col1Height = getTotal(sectionHeights,0,(int)scaleSize[2]);
        int col2Height = getTotal(sectionHeights,(int)scaleSize[2],sectionHeights.size());
        setScaledView(innerCol1,scaleSize[0],maxFontSize);
        setScaledView(innerCol2,scaleSize[1],maxFontSize);
        resizeColumn(innerCol1, halfwidth, col1Height, 1);
        resizeColumn(innerCol2, halfwidth, col2Height, 1);

        int color;
        for (int i=0; i<scaleSize[2]; i++) {
            color = Color.TRANSPARENT;
            Drawable background = sectionViews.get(i).getBackground();
            if (background instanceof ColorDrawable) {
                color = ((ColorDrawable) background).getColor();
            }
            FrameLayout frameLayout = newFrameLayout(c,color);
            frameLayout.addView(sectionViews.get(i));
            innerCol1.addView(frameLayout);
        }
        for (int i=(int)scaleSize[2]; i<sectionViews.size(); i++) {
            color = Color.TRANSPARENT;
            Drawable background = sectionViews.get(i).getBackground();
            if (background instanceof ColorDrawable) {
                color = ((ColorDrawable) background).getColor();
            }
            FrameLayout frameLayout = newFrameLayout(c,color);
            frameLayout.addView(sectionViews.get(i));
            innerCol2.addView(frameLayout);
        }
        columnVisibility(column1,column2,column3,true,true,false);
        column1.addView(innerCol1);
        column2.addView(innerCol2);
        setMargins(column1,0,padding);
        setMargins(column2,padding,0);
        PerformanceFragment.songViewWidth = PerformanceFragment.screenWidth;
        int col1h = (int) (col1Height*scaleSize[0]);
        int col2h = (int) (col2Height*scaleSize[1]);
        PerformanceFragment.songViewHeight = Math.max(col1h,col2h);
    }

    // 3 column stuff
    private float[] col3Scale(int screenWidth, int screenHeight, int totalViewHeight,
                              boolean songAutoScaleColumnMaximise, ArrayList<Integer> viewWidth,
                              ArrayList<Integer> viewHeight) {
        float[] colscale = new float[5];

        // Find the third height of all of the views together
        float thirdViewheight = (float)totalViewHeight / 3.0f;

        // Go through the three sections and try to get them similar
        int col1Height = 0;
        int preThirdWay = 0;
        int postThirdWay = 0;
        int i=0;
        while (i<viewHeight.size() && col1Height<thirdViewheight) {
            preThirdWay = i;
            postThirdWay = preThirdWay+1;
            col1Height += viewHeight.get(i);
            i++;
        }
        if (postThirdWay>viewHeight.size()) {
            postThirdWay = preThirdWay;
        }

        // Decide if we're closer underheight or overheight
        int col1Height_pre = getTotal(viewHeight,0,preThirdWay);
        int col1Height_post = getTotal(viewHeight,0,postThirdWay);
        int diff_pre = Math.abs((int)thirdViewheight - getTotal(viewHeight,0,preThirdWay));
        int diff_post = Math.abs((int)thirdViewheight - getTotal(viewHeight, 0,postThirdWay));

        int thirdWay;
        if (diff_pre<=diff_post) {
            thirdWay = preThirdWay;
            col1Height = col1Height_pre;
        } else {
            thirdWay = postThirdWay;
            col1Height = col1Height_post;
        }

        // Now we have the best first column, we compare column2 and column3 in ths same way as 2 columns
        int col2Height = 0;
        int col3Height = totalViewHeight-col1Height;
        int preTwoThirdWay = 0;
        int postTwoThirdWay = 0;
        i=thirdWay;
        while (i<viewHeight.size() && col2Height<col3Height) {
            preTwoThirdWay = i;
            postTwoThirdWay = preTwoThirdWay+1;
            col2Height += viewHeight.get(i);
            col3Height -= viewHeight.get(i);
        }
        if (postTwoThirdWay>viewHeight.size()) {
            postTwoThirdWay = preTwoThirdWay;
        }

        // Decide if we're closer underheight or overheight
        int col2Height_pre = getTotal(viewHeight,thirdWay,preTwoThirdWay);
        int col2Height_post = getTotal(viewHeight,thirdWay,postTwoThirdWay);
        int col3Height_pre = totalViewHeight-col2Height_pre;
        int col3Height_post = totalViewHeight-col2Height_post;
        diff_pre = Math.abs(col2Height_pre - col3Height_pre);
        diff_post = Math.abs(col2Height_post - col3Height_post);

        int twoThirdWay;
        if (diff_pre<=diff_post) {
            twoThirdWay = preTwoThirdWay;
            col2Height = col2Height_pre;
            col3Height = col3Height_pre;
        } else {
            twoThirdWay = postTwoThirdWay;
            col2Height = col2Height_post;
            col3Height = col3Height_post;
        }

        // Now decide on the x and y scaling available for each column
        int maxWidthCol1 = getMaxValue(viewWidth,0,thirdWay);
        int maxWidthCol2 = getMaxValue(viewWidth,thirdWay,twoThirdWay);
        int maxWidthCol3 = getMaxValue(viewWidth,twoThirdWay,viewWidth.size());

        float thirdWidth = ((float)screenWidth/3.0f)-padding;

        float col1Xscale = thirdWidth / (float)maxWidthCol1;
        float col1Yscale = (float)screenHeight / (float)col1Height;
        float col1Scale = Math.min(col1Xscale,col1Yscale);
        float col2Xscale = thirdWidth / (float)maxWidthCol2;
        float col2Yscale = (float)screenHeight / (float)col2Height;
        float col2Scale = Math.min(col2Xscale,col2Yscale);
        float col3Xscale = thirdWidth / (float)maxWidthCol3;
        float col3Yscale = (float)screenHeight / (float)col3Height;
        float col3Scale = Math.min(col3Xscale,col3Yscale);

        colscale[0] = col1Scale;
        colscale[1] = col2Scale;
        colscale[2] = col3Scale;
        colscale[3] = thirdWay;
        colscale[4] = twoThirdWay;

        if (!songAutoScaleColumnMaximise) {
            // make 2 all the values of the smallest (but the same)
            float min = Math.min(colscale[0],Math.min(colscale[1],colscale[2]));
            colscale[0] = min;
            colscale[1] = min;
            colscale[2] = min;
        }

        return colscale;
    }
    private void setThreeColumns(Context c, ArrayList<View> sectionViews, LinearLayout column1,
                                 LinearLayout column2, LinearLayout column3, ArrayList<Integer> sectionWidths,
                                 ArrayList<Integer> sectionHeights, float[] scaleSize,
                                 float maxFontSize) {
        // Use 2 column
        columnVisibility(column1,column2,column3,false,false,false);
        LinearLayout innerCol1 = newLinearLayout(c);
        LinearLayout innerCol2 = newLinearLayout(c);
        LinearLayout innerCol3 = newLinearLayout(c);
        int color;
        for (int i=0; i<scaleSize[3]; i++) {
            color = Color.TRANSPARENT;
            Drawable background = sectionViews.get(i).getBackground();
            if (background instanceof ColorDrawable) {
                color = ((ColorDrawable) background).getColor();
            }
            FrameLayout frameLayout = newFrameLayout(c,color);
            frameLayout.addView(sectionViews.get(i));
            innerCol1.addView(frameLayout);
        }
        for (int i=(int)scaleSize[3]; i<(int)scaleSize[4]; i++) {
            color = Color.TRANSPARENT;
            Drawable background = sectionViews.get(i).getBackground();
            if (background instanceof ColorDrawable) {
                color = ((ColorDrawable) background).getColor();
            }
            FrameLayout frameLayout = newFrameLayout(c,color);
            frameLayout.addView(sectionViews.get(i));
            innerCol2.addView(frameLayout);
        }
        for (int i=(int)scaleSize[4]; i<sectionViews.size(); i++) {
            color = Color.TRANSPARENT;
            Drawable background = sectionViews.get(i).getBackground();
            if (background instanceof ColorDrawable) {
                color = ((ColorDrawable) background).getColor();
            }
            FrameLayout frameLayout = newFrameLayout(c,color);
            frameLayout.addView(sectionViews.get(i));
            innerCol3.addView(frameLayout);
        }
        int col1Width = getMaxValue(sectionWidths,0,(int)scaleSize[3]);
        int col1Height = getTotal(sectionHeights,0,(int)scaleSize[3]);
        int col2Width = getMaxValue(sectionWidths,(int)scaleSize[3],(int)scaleSize[4]);
        int col2Height = getTotal(sectionHeights,(int)scaleSize[3],(int)scaleSize[4]);
        int col3Width = getMaxValue(sectionWidths,(int)scaleSize[4],sectionWidths.size());
        int col3Height = getTotal(sectionHeights,(int)scaleSize[4],sectionHeights.size());
        setScaledView(innerCol1,scaleSize[0],maxFontSize);
        setScaledView(innerCol2,scaleSize[1],maxFontSize);
        setScaledView(innerCol3,scaleSize[2],maxFontSize);
        resizeColumn(innerCol1, col1Width, col1Height, scaleSize[0]);
        resizeColumn(innerCol2, col2Width, col2Height, scaleSize[1]);
        resizeColumn(innerCol3, col3Width, col3Height, scaleSize[2]);
        columnVisibility(column1,column2,column3,true,true,true);
        column1.addView(innerCol1);
        column2.addView(innerCol2);
        column3.addView(innerCol3);
        PerformanceFragment.songViewWidth = PerformanceFragment.screenWidth;
        int col1h = (int) (col1Height*scaleSize[0]);
        int col2h = (int) (col2Height*scaleSize[1]);
        int col3h = (int) (col3Height*scaleSize[2]);
        PerformanceFragment.songViewHeight = Math.max(col1h,Math.max(col2h,col3h));
    }

}