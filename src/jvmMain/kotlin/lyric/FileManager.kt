package lyric;

import java.io.*;
import java.util.List;
/**
 * FileManager: All method that deal with the file<br>
 * this code copy from <a href="https://github.com/IntelleBitnify/LyricConverter">LyricConverter</a>
 * The following changes were made to the code.<br>
 * 1. removed all println<br>
 * 2. modified the readLRC() parameter to change the file name to an absolute path<br>
 * 3. modified processLRC(), added exception catch to Double.parseDouble()<br>
 * 4. modified the parameter of writeSRT() to change the file name to an absolute path<br>
 * Modified time: 2022/3/18
 * 
 * @author IntelleBitnify
 * @version 1.0 (10/6/2019)
 */
public class FileManager
{
    /********************************************************************
     * Read the .lrc file
     * 
     * @param   inSong                   The object of SongLyric that will hold entire lyric from the file
     * @param   inputPath               The path that it reads from
     * 
     * @return  Modify the state of the SongLyric object
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     *********************************************************************/
    public static void readLRC(SongLyric inSong, String inputPath)
    {
        //Define data structure
        int i;
        String line;
        BufferedReader bufRdr;
        FileReader fileRdr;

        //Define default value
        line = null;
        fileRdr = null;

        try //Mandatory IO try catch
        {
            fileRdr = new FileReader(inputPath);
            bufRdr = new BufferedReader(fileRdr);

            //Read the first line
            line = bufRdr.readLine();
            while (line != null) //Proceed to read till the last line if there is a first line
            {
                processLRC(inSong, line);   
                line = bufRdr.readLine();       
            }
            fileRdr.close();
        }
        catch (IOException e)
        {
            try
            {
                if (line != null)
                {
                    fileRdr.close();
                }
            }
            catch (IOException b)
            {
            }
        }
    }

    /************************************************************
     * Process to read the .lrc file and save it to the {(seconds), (lyric)} format
     * 
     * @return Modify the state of SongLyric object
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     ************************************************************/
    public static void processLRC(SongLyric inSongLyric, String inLine)
    {
        //Define data structure
        double inTimestamp;
        String[] subLyric;
        Lyric lrc;

        //Initialize object
        lrc = new Lyric();

        //LRC data: [minutes:seconds.miliseconds]lyric

        //Split data into time [hours:minutes.seconds] and lyric
        subLyric = inLine.split("]");

        //Process time data into real number in memory
        subLyric[0] = subLyric[0].replace("[","");
        subLyric[0] = subLyric[0].replace(":","");

        //Set Processed to each line lyric object

        try{
            //Step 1: Save the timestamp in seconds from [minutesseconds.miliseconds] the seconds used to be 100 for 1 minutes instead of 60
            inTimestamp = Double.parseDouble(subLyric[0]); //Convert to real number from string
            inTimestamp = (((int) (inTimestamp / 100)) * 60) + (inTimestamp % 100);
            //Step 2: Set the timestamp
            lrc.setTimestamp(inTimestamp);

            //Step 3: Set the lyric
            if (subLyric.length == 1) //If the lyric is blank
            {
                lrc.setLyric(""); //Set a blank lyric
            }
            else //If the lyric contain a lyric
            {
                lrc.setLyric(subLyric[1]); //Set the lyric
            }

            //Add each line lyric to the song lyric
            inSongLyric.addLyric(lrc);
        }catch (NumberFormatException formatException ){
            formatException.printStackTrace();
        }
    }

    /********************************************************************
     * Write the file to the format of .srt file
     * 
     * @param   inSongLyric              The object of SongLyric that hold entire lyric
     * @param   outPath               The path that it will write to
     * 
     * @return  Output to the .srt file
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     *********************************************************************/
    public static void writeSRT(SongLyric inSongLyric, String outPath)
    {
        //Define data structure
        int i, to, from, from_hours, from_minutes, from_seconds, from_miliseconds, to_hours, to_minutes, to_seconds, to_miliseconds;
        List<Lyric> inSong;
        FileWriter fileWrtr;
        PrintWriter pw;

        //Define default value
        fileWrtr = null;
        inSong = inSongLyric.getSong();
        try //Mandatory IO try catch
        {
            fileWrtr = new FileWriter(outPath);
            pw = new PrintWriter(fileWrtr);

            //SRT data:
            //index
            //time_from --> time_to
            //lyric
            //

            for (i=0; i<inSong.size(); i++)
            {
                //Define index value for timestamp
                to = i+1;
                from = i;

                //Format the timestamp

                //Step 1: Time from
                from_hours = (int) ((inSong.get(from)).getTimestamp() / 3600);
                from_minutes = (int) ((inSong.get(from)).getTimestamp() / 60); // 181/60 = 3
                from_seconds = (int) (((inSong.get(from)).getTimestamp()) - (from_minutes * 60)); //181-(3*60) = 1
                from_miliseconds = (int) (((inSong.get(from)).getTimestamp() * 1000) % 1000); //.000

                //Step 2: Time to
                if (i == inSong.size() -1) //End of lyric
                {
                    to_hours = (int) ((inSong.get(from)).getTimestamp() / 3600);
                    to_minutes = (int) ((inSong.get(from)).getTimestamp() / 60);
                    to_seconds = (int) (((inSong.get(from)).getTimestamp()) - (from_minutes * 60) + 5);
                    to_miliseconds = (int) (((inSong.get(from)).getTimestamp() * 1000) % 1000);
                }
                else
                {
                    to_hours = (int) ((inSong.get(to)).getTimestamp() / 3600);
                    to_minutes = (int) ((inSong.get(to)).getTimestamp() / 60);
                    to_seconds = (int) (((inSong.get(to)).getTimestamp()) - (to_minutes * 60));
                    to_miliseconds = (int) (((inSong.get(to)).getTimestamp() * 1000) % 1000);
                }

                //Begin to write the file based on srt format
                pw.println(i+1); // Index in SRT file
                pw.print(String.format("%02d", from_hours));
                pw.print(":");
                pw.print(String.format("%02d", from_minutes));
                pw.print(":");
                pw.print(String.format("%02d", from_seconds));
                pw.print(",");
                pw.print(String.format("%03d", from_miliseconds));
                pw.print(" --> ");
                pw.print(String.format("%02d", to_hours));
                pw.print(":");
                pw.print(String.format("%02d", to_minutes));
                pw.print(":");
                pw.print(String.format("%02d", to_seconds));
                pw.print(",");
                pw.println(String.format("%03d", to_miliseconds));
                pw.println((inSong.get(from)).getLyric());
                pw.println();
            }
            fileWrtr.close();
        }

        catch (IOException a)
        {
            try
            {
                if (fileWrtr != null)
                {
                    fileWrtr.close();
                }
            }
            catch (IOException b)
            {
            }
        }
    }

    /********************************************************************
     * Write the file to the format of .lrc file
     * 
     * @param   inSongLyric              The object of SongLyric that hold entire lyric
     * @param   outPath               The path that it will write to
     * 
     * @return  Output to the .srt file
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     *********************************************************************/
    public static void writeLRC(SongLyric inSongLyric, String outPath)
    {
        //Define data structure
        int i, minutes, seconds, miliseconds;
        List<Lyric> inSong;
        FileWriter fileWrtr;
        PrintWriter pw;


        //Define default value
        fileWrtr = null;
        inSong = inSongLyric.getSong();
        try //Mandatory IO try catch
        {
            fileWrtr = new FileWriter(outPath);
            pw = new PrintWriter(fileWrtr);

            //LRC data: [minutes:seconds.miliseconds]lyric

            for (i=0; i<inSong.size(); i++)
            {
                //Format the timestamp
                minutes = (int) ((inSong.get(i)).getTimestamp() / 60);
                seconds = (int) (((inSong.get(i)).getTimestamp()) - (minutes * 60));
                miliseconds = (int) (((inSong.get(i)).getTimestamp() * 100) % 100);

                //Begin to write the file based on srt format
                pw.print("[" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds) + "." + String.format("%02d", miliseconds) + "]");
                pw.println((inSong.get(i)).getLyric());
            }
            fileWrtr.close();
        }

        catch (IOException a)
        {
            try
            {
                if (fileWrtr != null)
                {
                    fileWrtr.close();
                }
            }
            catch (IOException b)
            {
            }
        }
    }
}