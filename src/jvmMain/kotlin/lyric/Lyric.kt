package lyric;

/**
 * Lyric: Contain timestamp and lyric text.<br>
 * this code copy from <a href="https://github.com/IntelleBitnify/LyricConverter">LyricConverter</a><br>
 * @author  IntelleBitnify
 * @version 1.0 (10/6/2019)
 */
public class Lyric
{
    //Class Fields
    private double timestamp;
    private String lyric;

    //CONSTRUCTORS

    /************************************************************
     * Default Constructor: 
     * Creates the object with default Lyric state
     * 
     * @return address of new Lyric object
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     ************************************************************/
    public Lyric ()
    {
        this.timestamp = 0;
        this.lyric = "<no data>";
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
     ************************************************************/
    public Lyric (double inTimestamp, String inLyric)
    {
        this.timestamp = inTimestamp;
        this.lyric = inLyric;
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
     *************************************************************/
    public Lyric (Lyric inLyric)
    {
        this.timestamp = inLyric.getTimestamp();
        this.lyric = inLyric.getLyric();
    }

    //ACCESSOR

    /************************************************************
     * Get the lyric from this class
     * 
     * @return  lyric (String)
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     ************************************************************/
    public String getLyric()
    {
        return this.lyric;
    }

    /************************************************************
     * Get the timestamp from this class
     * 
     * @return  timestamp (real)
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     ************************************************************/
    public double getTimestamp()
    {
        return this.timestamp;
    }

    /********************************************************************
     * Generates cloned object address of Lyric
     * 
     * @return  cloneLyric (Lyric)
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     *********************************************************************/
    public Lyric clone()
    {
        //Define data structure
        Lyric cloneLyric;

        cloneLyric = new Lyric(this.timestamp, this.lyric);

        return cloneLyric;
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
     *********************************************************************/
    public boolean equals(Object inObject) 
    { 
        //Define data structure
        boolean isEqual; 
        Lyric inLyric;

        //Define default value
        isEqual = false;

        if(inObject instanceof Lyric)
        {
            inLyric = (Lyric) inObject;
            if (this.timestamp == inLyric.getTimestamp()) //Check equality of timestamp
            {
                if (this.lyric.equals(inLyric.getLyric())) //Check equality of lyric
                {
                    isEqual = true;
                }
            }
        }
        return isEqual;
    }

    /********************************************************************
     * Generates a String representation of this class state information
     * 
     * @return  lyricString (String)
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     *********************************************************************/
    public String toString()
    {
        //Define data structure
        String lyricString;

        //Generate a string representation about what this class contain
        lyricString = String.format("%.2f", this.timestamp) + " " + this.lyric;

        return lyricString;
    }

    //MUTATORS

    /************************************************************
     * Sets the lyric based on the imported inLyric
     * 
     * @param   inLyric                     The lyric in this particular line
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     ************************************************************/
    public void setLyric(String inLyric)
    {
        this.lyric = inLyric;
    }

    /************************************************************
     * Sets the timestamp based on the imported inTimestamp
     * 
     * @param   inTimestamp                 The type of fuel used by the engine ("BATTERY" or "DIESEL" or "BIO")
     * 
     * @author  IntelleBitnify
     * @version 1.0 (10/6/2019)
     ************************************************************/
    public void setTimestamp(double inTimestamp)
    {
        this.timestamp = inTimestamp;
    }
}