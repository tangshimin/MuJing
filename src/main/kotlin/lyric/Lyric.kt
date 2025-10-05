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
 * Lyric: Contain timestamp and lyric text.<br></br>
 * this code copy from [LyricConverter](https://github.com/IntelleBitnify/LyricConverter)<br></br>
 * @author  IntelleBitnify
 * @version 1.0 (10/6/2019)
 */
class Lyric {
    /************************************************************
     * Get the timestamp from this class
     *
     * @return  timestamp (real)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    /************************************************************
     * Sets the timestamp based on the imported inTimestamp
     *
     * @param   inTimestamp                 The type of fuel used by the engine ("BATTERY" or "DIESEL" or "BIO")
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    //Class Fields
    @JvmField
    var timestamp: Double
    /************************************************************
     * Get the lyric from this class
     *
     * @return  lyric (String)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    /************************************************************
     * Sets the lyric based on the imported inLyric
     *
     * @param   inLyric                     The lyric in this particular line
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    var lyric: String

    //CONSTRUCTORS
    /************************************************************
     * Default Constructor:
     * Creates the object with default Lyric state
     *
     * @return address of new Lyric object
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    constructor() {
        this.timestamp = 0.0
        this.lyric = "<no data>"
    }

    /************************************************************
     * Alternate Constructor:
     * Creates the object if the imports are valid and FAILS otherwise
     *
     * @param   inTimestamp                 The timestamp of a lyric in a song (in seconds)
     * @param   inLyric                     The Lyric of the song in that timestamp
     *
     * @return  address of new Lyric object
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    constructor(inTimestamp: Double, inLyric: String) {
        this.timestamp = inTimestamp
        this.lyric = inLyric
    }

    /************************************************************
     * Copy Constructor:
     * Creates an object with an identical object state as the import
     *
     * @param   inLyric                     The Lyric object that contain information about the lyric and timestamp
     *
     * @return  address of new Lyric object
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    constructor(inLyric: Lyric) {
        this.timestamp = inLyric.timestamp
        this.lyric = inLyric.lyric
    }

    //ACCESSOR

    /********************************************************************
     * Generates cloned object address of Lyric
     *
     * @return  cloneLyric (Lyric)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
     fun clone(): Lyric {
        //Define data structure
        val cloneLyric = Lyric(this.timestamp, this.lyric)

        return cloneLyric
    }

    /********************************************************************
     * Check the literal value equality of the import
     *
     * @param   inObject                Any object to compare with the Lyric object
     *
     * @return  isEqual (boolean)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    override fun equals(inObject: Any?): Boolean {
        //Define data structure
        var isEqual: Boolean
        val inLyric: Lyric

        //Define default value
        isEqual = false

        if (inObject is Lyric) {
            inLyric = inObject
            if (this.timestamp == inLyric.timestamp) //Check equality of timestamp
            {
                if (this.lyric == inLyric.lyric) //Check equality of lyric
                {
                    isEqual = true
                }
            }
        }
        return isEqual
    }

    /********************************************************************
     * Generates a String representation of this class state information
     *
     * @return  lyricString (String)
     *
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     */
    override fun toString(): String {
        //Generate a string representation about what this class contain
        //Define data structure
        val lyricString = String.format("%.2f", this.timestamp) + " " + this.lyric

        return lyricString
    }

    //MUTATORS
}