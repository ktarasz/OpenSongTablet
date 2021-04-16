package com.garethevans.church.opensongtablet.export;

// This class prepares all of the different export formats for the app and returns them

import android.content.Context;
import android.net.Uri;

import com.garethevans.church.opensongtablet.interfaces.MainActivityInterface;
import com.garethevans.church.opensongtablet.songprocessing.Song;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class PrepareFormats {

    private Song thisSongSQL;

    // TODO IV Request to export Variations properly as it isn't in SQL database

    // When a user asks for a song to be exported, it is first copied to the Exports folder in the folder_filename format
    public ArrayList<Uri> makeSongExportCopies(Context c, MainActivityInterface mainActivityInterface, String folder, String filename,
                                               boolean desktop, boolean ost, boolean txt, boolean chopro, boolean onsong) {
        ArrayList<Uri> uris = new ArrayList<>();
        thisSongSQL = mainActivityInterface.getSQLiteHelper().getSpecificSong(c,mainActivityInterface,folder,filename);

        String newFilename = folder.replace("/","_");
        if (!newFilename.endsWith("_")) {
            newFilename = newFilename + "_";
        }
        newFilename = newFilename + filename;

        if (desktop) {
            uris.add(doMakeCopy(c, mainActivityInterface, folder, filename, newFilename));
        }
        if (ost) {
            uris.add(doMakeCopy(c,mainActivityInterface,folder,filename,newFilename+".ost"));
        }
        if (txt && thisSongSQL!=null) {
            String text = getSongAsText(thisSongSQL);
            if (mainActivityInterface.getStorageAccess().doStringWriteToFile(c,mainActivityInterface.getPreferences(),"Export","",newFilename+".txt",text)) {
                uris.add(mainActivityInterface.getStorageAccess().getUriForItem(c,mainActivityInterface.getPreferences(),"Export","",newFilename+".txt"));
            }
        }
        if (chopro && thisSongSQL!=null) {
            String text = getSongAsChoPro(c,mainActivityInterface,thisSongSQL);
            if (mainActivityInterface.getStorageAccess().doStringWriteToFile(c,mainActivityInterface.getPreferences(),"Export","",newFilename+".chopro",text)) {
                uris.add(mainActivityInterface.getStorageAccess().getUriForItem(c,mainActivityInterface.getPreferences(),"Export","",newFilename+".chopro"));
            }
        }
        if (onsong && thisSongSQL!=null) {
            String text = getSongAsOnSong(c,mainActivityInterface,thisSongSQL);
            if (mainActivityInterface.getStorageAccess().doStringWriteToFile(c,mainActivityInterface.getPreferences(),"Export","",newFilename+".onsong",text)) {
                uris.add(mainActivityInterface.getStorageAccess().getUriForItem(c,mainActivityInterface.getPreferences(),"Export","",newFilename+".onsong"));
            }
        }

        return uris;
    }
    private Uri doMakeCopy(Context c, MainActivityInterface mainActivityInterface, String currentFolder, String currentFilename, String newFilename) {
        Uri targetFile = mainActivityInterface.getStorageAccess().getUriForItem(c,mainActivityInterface.getPreferences(),"Songs",currentFolder,currentFilename);
        Uri destinationFile = mainActivityInterface.getStorageAccess().getUriForItem(c,mainActivityInterface.getPreferences(),"Export","",newFilename);
        mainActivityInterface.getStorageAccess().lollipopCreateFileForOutputStream(c,mainActivityInterface.getPreferences(),destinationFile,null,"Export","",newFilename);
        InputStream inputStream = mainActivityInterface.getStorageAccess().getInputStream(c,targetFile);
        OutputStream outputStream = mainActivityInterface.getStorageAccess().getOutputStream(c,destinationFile);
        if (mainActivityInterface.getStorageAccess().copyFile(inputStream,outputStream)) {
            return destinationFile;
        } else {
            return null;
        }
    }

    private String getSongAsText(Song thisSong) {
        StringBuilder s = new StringBuilder();

        s.append(replaceNulls("",thisSong.getTitle(),"\n"));
        s.append(replaceNulls("",thisSong.getAuthor(),"\n"));
        s.append(replaceNulls("Key: ",thisSong.getKey(),"\n"));
        s.append(replaceNulls("Tempo: ",thisSong.getMetronomebpm(),"\n"));
        s.append(replaceNulls("Time signature: ",thisSong.getTimesig(),"\n"));
        s.append(replaceNulls("Copyright: ",thisSong.getCopyright(),"\n"));
        s.append(replaceNulls("CCLI: ",thisSong.getCcli(),"\n"));

        s.append("\n\n");

        s.append(replaceNulls("\n",thisSong.getLyrics(),""));

        String string = s.toString();
        string = string.replace("\n\n\n", "\n\n");

        // IV - remove empty comments
        string = string.replaceAll("\\Q{c:}\\E\n", "");

        return string;
    }
    private String getSongAsChoPro(Context c, MainActivityInterface mainActivityInterface, Song thisSong) {
        // This converts an OpenSong file into a ChordPro file
        StringBuilder s = new StringBuilder("{new_song}\n");
        s.append(replaceNulls("{title:",thisSong.getTitle(),"}\n"));
        s.append(replaceNulls("{artist:",thisSong.getAuthor(),"}\n"));
        s.append(replaceNulls("{key:",thisSong.getKey(),"}\n"));
        s.append(replaceNulls("{tempo:",thisSong.getMetronomebpm(),"}\n"));
        s.append(replaceNulls("{time:",thisSong.getTimesig(),"}\n"));
        s.append(replaceNulls("{copyright:",thisSong.getCopyright(),"}\n"));
        s.append(replaceNulls("{ccli:",thisSong.getCcli(),"}\n"));

        s.append("\n\n");

        s.append(mainActivityInterface.getConvertChoPro().fromOpenSongToChordPro(c, mainActivityInterface, replaceNulls("\n",thisSong.getLyrics(),"")));
        
        String string = s.toString();
        string = string.replace("\n\n\n", "\n\n");

        // IV - remove empty comments
        string = string.replaceAll("\\Q{c:}\\E\n", "");

        return string;
    }
    private String getSongAsOnSong(Context c, MainActivityInterface mainActivityInterface, Song thisSong) {
        // This converts an OpenSong file into a OnSong file
        StringBuilder s = new StringBuilder();
        s.append(replaceNulls("",thisSong.getTitle(),"\n"));
        s.append(replaceNulls("",thisSong.getAuthor(),"\n"));
        s.append(replaceNulls("Key: ",thisSong.getKey(),"\n"));
        s.append(replaceNulls("Tempo: ",thisSong.getMetronomebpm(),"\n"));
        s.append(replaceNulls("Time signature: ",thisSong.getTimesig(),"\n"));
        s.append(replaceNulls("Copyright: ",thisSong.getCopyright(),"\n"));
        s.append(replaceNulls("CCLI: ",thisSong.getCcli(),"\n"));

        s.append("\n\n");

        s.append(mainActivityInterface.getConvertChoPro().fromOpenSongToChordPro(c, mainActivityInterface, replaceNulls("\n",thisSong.getLyrics(),"")));

        String string = s.toString();
        string = string.replace("\n\n\n", "\n\n");

        // IV - remove empty comments
        string = string.replaceAll("\\Q{c:}\\E\n", "");

        return string;
    }
    private String replaceNulls(String start, String end, String s) {
        if (s==null) {
            return "";
        } else {
            return start+s+end;
        }
    }

}