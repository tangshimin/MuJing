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