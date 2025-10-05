/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 *
 * This file contains code based on LyricConverter (https://github.com/IntelleBitnify/LyricConverter)
 * Original work Copyright (c) 2019 IntelleBitnify
 * Original work licensed under MIT License
 *
 * The original MIT License text:
 *
 * MIT License
 *
 * Copyright (c) 2019 IntelleBitnify
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lyric

import java.io.*

/**
 * FileManager: All method that deal with the file<br></br>
 * this code copy from [LyricConverter](https://github.com/IntelleBitnify/LyricConverter)
 * The following changes were made to the code.<br></br>
 * 1. removed all println<br></br>
 * 2. modified the readLRC() parameter to change the file name to an absolute path<br></br>
 * 3. modified processLRC(), added exception catch to Double.parseDouble()<br></br>
 * 4. modified the parameter of writeSRT() to change the file name to an absolute path<br></br>
 * Modified time: 2022/3/18
 *
 * @author IntelleBitnify
 * @version 1.0 (10/6/2019)
 */
object FileManager {
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
     */
    fun readLRC(inSong: SongLyric, inputPath: String) {
        //Define data structure
        var line: String?
        //Define default value
        line = null

        BufferedReader(FileReader(inputPath)).use{ bufRdr ->
            line = bufRdr.readLine()
            while (line != null) {
                processLRC(inSong, line!!)
                line = bufRdr.readLine()
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
     */
    fun processLRC(inSongLyric: SongLyric, inLine: String) {
        //Define data structure
        var inTimestamp: Double

        //Initialize object
        val lrc = Lyric()

        //LRC data: [minutes:seconds.miliseconds]lyric

        //Split data into time [hours:minutes.seconds] and lyric
        val subLyric = inLine.split("]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        //Process time data into real number in memory
        subLyric[0] = subLyric[0].replace("[", "")
        subLyric[0] = subLyric[0].replace(":", "")

        //Set Processed to each line lyric object
        try {
            //Step 1: Save the timestamp in seconds from [minutesseconds.miliseconds] the seconds used to be 100 for 1 minutes instead of 60
            inTimestamp = subLyric[0].toDouble() //Convert to real number from string
            inTimestamp = (((inTimestamp / 100).toInt()) * 60) + (inTimestamp % 100)
            //Step 2: Set the timestamp
            lrc.timestamp = inTimestamp

            //Step 3: Set the lyric
            if (subLyric.size == 1) //If the lyric is blank
            {
                lrc.lyric = "" //Set a blank lyric
            } else  //If the lyric contain a lyric
            {
                lrc.lyric = subLyric[1] //Set the lyric
            }

            //Add each line lyric to the song lyric
            inSongLyric.addLyric(lrc)
        } catch (formatException: NumberFormatException) {
            formatException.printStackTrace()
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
     */
    fun writeSRT(inSongLyric: SongLyric, outPath: String?) {
        //Define data structure
        var i: Int
        var to: Int
        var from: Int
        var from_hours: Int
        var from_minutes: Int
        var from_seconds: Int
        var from_miliseconds: Int
        var to_hours: Int
        var to_minutes: Int
        var to_seconds: Int
        var to_miliseconds: Int
        var fileWrtr: FileWriter?
        val pw: PrintWriter

        //Define default value
        fileWrtr = null
        val inSong = inSongLyric.song
        try  //Mandatory IO try catch
        {
            fileWrtr = FileWriter(outPath)
            pw = PrintWriter(fileWrtr)

            //SRT data:
            //index
            //time_from --> time_to
            //lyric
            //
            i = 0
            while (i < inSong.size) {
                //Define index value for timestamp
                to = i + 1
                from = i

                //Format the timestamp

                //Step 1: Time from
                from_hours = (inSong[from].timestamp / 3600).toInt()
                from_minutes = (inSong[from].timestamp / 60).toInt() // 181/60 = 3
                from_seconds = ((inSong[from].timestamp) - (from_minutes * 60)).toInt() //181-(3*60) = 1
                from_miliseconds = ((inSong[from].timestamp * 1000) % 1000).toInt() //.000

                //Step 2: Time to
                if (i == inSong.size - 1) //End of lyric
                {
                    to_hours = (inSong[from].timestamp / 3600).toInt()
                    to_minutes = (inSong[from].timestamp / 60).toInt()
                    to_seconds = ((inSong[from].timestamp) - (from_minutes * 60) + 5).toInt()
                    to_miliseconds = ((inSong[from].timestamp * 1000) % 1000).toInt()
                } else {
                    to_hours = (inSong[to].timestamp / 3600).toInt()
                    to_minutes = (inSong[to].timestamp / 60).toInt()
                    to_seconds = ((inSong[to].timestamp) - (to_minutes * 60)).toInt()
                    to_miliseconds = ((inSong[to].timestamp * 1000) % 1000).toInt()
                }

                //Begin to write the file based on srt format
                pw.println(i + 1) // Index in SRT file
                pw.print(String.format("%02d", from_hours))
                pw.print(":")
                pw.print(String.format("%02d", from_minutes))
                pw.print(":")
                pw.print(String.format("%02d", from_seconds))
                pw.print(",")
                pw.print(String.format("%03d", from_miliseconds))
                pw.print(" --> ")
                pw.print(String.format("%02d", to_hours))
                pw.print(":")
                pw.print(String.format("%02d", to_minutes))
                pw.print(":")
                pw.print(String.format("%02d", to_seconds))
                pw.print(",")
                pw.println(String.format("%03d", to_miliseconds))
                pw.println(inSong[from].lyric)
                pw.println()
                i++
            }
            fileWrtr.close()
        } catch (a: IOException) {
            try {
                if (fileWrtr != null) {
                    fileWrtr.close()
                }
            } catch (b: IOException) {
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
     */
    fun writeLRC(inSongLyric: SongLyric, outPath: String?) {
        //Define data structure
        var i: Int
        var minutes: Int
        var seconds: Int
        var miliseconds: Int
        var fileWrtr: FileWriter?
        val pw: PrintWriter


        //Define default value
        fileWrtr = null
        val inSong = inSongLyric.song
        try  //Mandatory IO try catch
        {
            fileWrtr = FileWriter(outPath)
            pw = PrintWriter(fileWrtr)

            //LRC data: [minutes:seconds.miliseconds]lyric
            i = 0
            while (i < inSong.size) {
                //Format the timestamp
                minutes = (inSong[i].timestamp / 60).toInt()
                seconds = ((inSong[i].timestamp) - (minutes * 60)).toInt()
                miliseconds = ((inSong[i].timestamp * 100) % 100).toInt()

                //Begin to write the file based on srt format
                pw.print(
                    "[" + String.format("%02d", minutes) + ":" + String.format(
                        "%02d",
                        seconds
                    ) + "." + String.format("%02d", miliseconds) + "]"
                )
                pw.println(inSong[i].lyric)
                i++
            }
            fileWrtr.close()
        } catch (a: IOException) {
            try {
                if (fileWrtr != null) {
                    fileWrtr.close()
                }
            } catch (b: IOException) {
            }
        }
    }
}