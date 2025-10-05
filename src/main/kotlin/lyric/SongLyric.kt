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

/**
 * SongLyric: Class that hold an entire song lyric with its timestamp.<br></br>
 * this code copy from [LyricConverter](https://github.com/IntelleBitnify/LyricConverter)<br></br>
 *
 * @author  IntelleBitnify
 * @version 1.0 (10/6/2019)
 */
class SongLyric {
    /************************************************************
     * Get the song from this class
     *
     * @return  song (List of Lyric)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    /************************************************************
     * Sets the lyric based on the imported inLyric
     *
     * @param   inSong                     The entire song that contain all of the lyric
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    //Private Class Fields
    var song: MutableList<Lyric>

    //CONSTRUCTOR
    /************************************************************
     * Default Constructor:
     * Creates the object with default SongLyric state
     *
     * @return address of new Lyric object
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    constructor() {
        this.song = ArrayList()
    }

    /************************************************************
     * Alternate Constructor:
     * Creates the object if the imports are valid and FAILS otherwise
     *
     * @param   inSong                     The List of Lyric and timestamp for the entire song
     *
     * @return  address of new Lyric object
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    constructor(inSong: MutableList<Lyric>) {
        this.song = inSong
    }

    /************************************************************
     * Copy Constructor:
     * Creates an object with an identical object state as the import
     *
     * @param   inSongLyric                  The SongLyric object
     *
     * @return  address of new Lyric object
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    constructor(inSongLyric: SongLyric) {
        this.song = inSongLyric.song
    }

    //ACCESSOR

    /********************************************************************
     * Generates cloned object address of SongLyric
     *
     * @return  cloneSongLyric (SongLyric)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    fun clone(): SongLyric {
        //Define data structure
        val cloneSongLyric = SongLyric(this.song)

        return cloneSongLyric
    }

    /********************************************************************
     * Check the literal value equality of the import
     *
     * @param   inObject                Any object to compare with the SongLyric object
     *
     * @return  isEqual (boolean)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    override fun equals(inObject: Any?): Boolean {
        //Define data structure
        var i: Int
        var isEqual: Boolean
        val inSongLyric: SongLyric
        val inSong: List<Lyric>

        //Define default value
        isEqual = false
        var countEquals = 0
        if (inObject is SongLyric) {
            inSongLyric = inObject
            inSong = inSongLyric.song
            i = 0
            while ((inSong[i].equals(song[i]) == true)) {
                countEquals++ //Keep track of the equals
                i++
            }
            if (countEquals == inSong.size) //If all is equals
            {
                isEqual = true
            }
        }
        return isEqual
    }

    /********************************************************************
     * Generates a String representation of this class state information
     *
     * @return  songLyricString (String)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    override fun toString(): String {
        //Define default value
        var songLyricString = ""
        //Define data structure
        var i = 0
        while (i < song.size) {
            songLyricString += song[i].toString() + "\n"
            i++
        }

        return songLyricString
    }

    //MUTATORS

    /********************************************************************
     * Add one line of lyric to the song
     *
     * @param   inLyric                    One line of lyric
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    fun addLyric(inLyric: Lyric) {
        song.add(inLyric)
    }

    /********************************************************************
     * Change the speed of song lyric
     *
     * @param   inLyric                    One line of lyric
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    fun changeSpeed(inPlaybackSpeed: Double) {
        //Change the timestamp to mach the speed
        //Define data structure
        var i = 0
        while (i < song.size) {
            song[i].timestamp = (song[i].timestamp / inPlaybackSpeed)
            i++
        }
    }
}