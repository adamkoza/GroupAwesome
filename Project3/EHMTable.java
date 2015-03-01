/****************************************************************************************
 * @file  EHMTable.java
 *
 * @author   John Miller
 */

import java.io.*;
import java.util.*;
import java.util.function.*;

import static java.lang.System.out;

/****************************************************************************************
 * This class implements relational database tables (including attribute names, domains
 * and a list of tuples.  Five basic relational algebra operators are provided: project,
 * select, union, minus join.  The insert data manipulation operator is also provided.
 * Missing are update and delete data manipulation operators.
 */
public class EHMTable
       implements Serializable
{
    /** Relative path for storage directory
     */
    private static final String DIR = "store" + File.separator;

    /** Filename extension for database files
     */
    private static final String EXT = ".dbf";

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /** EHMTable name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  string types: Character, String
     */
    private final Class [] domain;

    /** Collection of tuples (data storage).
     */
    private final List <Comparable []> tuples;

    /** Primary key. 
     */
    private final String [] key;

    private int primaryKeyIdx = 0;

    /** Index into tuples (maps key to tuple number).
     */
    private final ExtHashMap<Comparable,Integer> index;

    //----------------------------------------------------------------------------------
    // Constructors
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     */  
    public EHMTable(String _name, String[] _attribute, Class[] _domain, String[] _key)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new ArrayList <> ();
        /*EXTHASHMAP SETUP*/
        int primaryKeyIdx = 0;
        for( int i=0;i<_attribute.length; i++){
            if(_attribute[i] == _key[0]){
                primaryKeyIdx = i;
            }
        }
        index     = new ExtHashMap(Comparable.class,Integer.class,4);        // also try BPTreeMap, LinHashMap or ExtHashMap
    } // constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in _tuples list.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     * @param _tuples      the list of tuples containing the data
     */  
    public EHMTable(String _name, String[] _attribute, Class[] _domain, String[] _key,
                    List<Comparable[]> _tuples)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = _tuples;
        /*EXTHASHMAP SETUP*/
        primaryKeyIdx = 0;
        for( int i=0;i<_attribute.length; i++){
            if(_attribute[i] == _key[0]){
                primaryKeyIdx = i;
            }
        }
        index     = new ExtHashMap(Comparable.class,Integer.class,4);       // also try BPTreeMap, LinHashMap or ExtHashMap
        /*EXTHASHMAP SETUP*/

    } // constructor

    /************************************************************************************
     * Construct an empty table from the raw string specifications.
     *
     * @param name        the name of the relation
     * @param attributes  the string containing attributes names
     * @param domains     the string containing attribute domains (data types)
     */
    public EHMTable(String name, String attributes, String domains, String _key)
    {
        this (name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "));

        out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // constructor

    //----------------------------------------------------------------------------------
    // Public Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     *
     * #usage movie.project ("title year studioNo")
     *
     * @param attributes  the attributes to project onto
     * @return  a table of projected tuples
     */
    public EHMTable project (String attributes)
    {
        out.println ("RA> " + name + ".project (" + attributes + ")");
        String [] attrs     = attributes.split (" ");
        Class []  colDomain = extractDom (match (attrs), domain);
        String [] newKey    = (Arrays.asList (attrs).containsAll (Arrays.asList (key))) ? key : attrs;

        List <Comparable []> rows = null;
        rows = new ArrayList<Comparable []>();
        int [] columns = match(attrs);

        Comparable [] t;
        for (int i = 0; i < tuples.size(); i++) {
            t = new Comparable[columns.length];
            for (int j = 0; j < columns.length; j++) {
                t[j] = this.tuples.get(i)[columns[j]];
            }
            rows.add(t);
        }

        return new EHMTable(name + count++, attrs, colDomain, newKey, rows);
    } // project

    /************************************************************************************
     * Select the tuples satisfying the given predicate (Boolean function).
     *
     * #usage movie.select (t -> t[movie.col("year")].equals (1977))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
    public EHMTable select (Predicate <Comparable []> predicate)
    {
        out.println ("\nRA> " + name + ".select (" + predicate.toString() + ")");

        List <Comparable []> rows = null;
        rows = new ArrayList<Comparable[]>();
        int rowLength = this.tuples.size();
        Comparable [] tuple;
        String key_s = this.key[0];


        for(int i = 0; i < rowLength; i++) {
            tuple = this.tuples.get(i);
            if(predicate.test(tuple) == true) {
                rows.add(tuple);
                out.println("found tuple at index: " + i);

            }
        }

        return new EHMTable(name + count++, attribute, domain, key, rows);
    } // select
    public EHMTable indexSelect (Comparable predicate)
    {
        out.println ("\nRA> " + name + ".select (" + predicate + ")");

        List <Comparable []> rows = null;
        rows = new ArrayList<Comparable[]>();
        int rowLength = this.tuples.size();
        Comparable [] tuple;

        Integer tuple_index = index.get(predicate);
        if(tuple_index != null) {
            tuple = tuples.get(tuple_index);
            rows.add(tuple);
            out.println("found tuple at index: " + tuple_index);

        }
        else
            out.println("Couldn't find tuple in index select.");
        return new EHMTable(name + count++, attribute, domain, key, rows);
    } // select


    /************************************************************************************
     * Union this table and EHMTable2.  Check that the two tables are compatible.
     *
     * #usage movie.union (show)
     *
     * @param EHMTable2  the rhs table in the union operation
     * @return  a table representing the union
     */
    public EHMTable union (EHMTable EHMTable2)
    {
        out.println ("RA> " + name + ".union (" + EHMTable2.name + ")");
        if (! compatible (EHMTable2)) return null;

        List <Comparable []> rows = null;
        rows = new ArrayList<Comparable[]>();
        int rowLength_t1 = this.tuples.size();
        int rowLength_t2 = EHMTable2.tuples.size();
        Comparable [] tuple;

        for(int i = 0; i < rowLength_t1; i++) {
            tuple = this.tuples.get(i);
            rows.add(tuple);
        }

        for(int i = 0; i < rowLength_t2; i++) {
            tuple = EHMTable2.tuples.get(i);
            if (!rows.contains(tuple))
                rows.add(tuple);
        }

        return new EHMTable(name + count++, attribute, domain, key, rows);
    } // union

    /************************************************************************************
     * Take the difference of this table and EHMTable2.  Check that the two tables are
     * compatible.
     *
     * #usage movie.minus (show)
     *
     * @param EHMTable2  The rhs table in the minus operation
     * @return  a table representing the difference
     */
     public EHMTable minus (EHMTable EHMTable2)
     {
        out.println ("RA> " + name + ".minus (" + EHMTable2.name + ")");

        if (! compatible (EHMTable2)) return null;

        List<Comparable[]> rows = new ArrayList<Comparable[]>();
        int rowLength_t1 = this.tuples.size();
        Comparable [] tuple;

        for (int i = 0; i < rowLength_t1; i++) {
            tuple = this.tuples.get(i);
            if (!EHMTable2.tuples.contains(tuple))
                rows.add(tuple);
        }

        return new EHMTable(name + count++, attribute, domain, key, rows);
     } // minus

    /************************************************************************************
     * Join this table and EHMTable2 by performing an equijoin.  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by append "2" to the end of any duplicate attribute name.
     *
     * #usage movie.join ("studioNo", "name", studio)
     * #usage movieStar.join ("name == s.name", starsIn)
     *
     * @param attributes1  the attributes of this table to be compared (Foreign Key)
     * @param attributes2  the attributes of EHMTable2 to be compared (Primary Key)
     * @param EHMTable2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public EHMTable join (String attributes1, String attributes2, EHMTable EHMTable2)
    {
        out.println ("\nRA> " + name + ".indexJoin (" + attributes1 + ", " + attributes2 + ", "
                                               + EHMTable2.name + ")");

        String [] t_attrs = attributes1.split (" ");
        String [] u_attrs = attributes2.split (" ");

        //lists of attributes' column indexes for each table
        int [] t_attrs_index = new int[t_attrs.length];
        int [] u_attrs_index = new int[u_attrs.length];

        boolean type_check = true;

        //checks for uneven attribute numbers to compare
        if(t_attrs.length != u_attrs.length){
            return null;
        }

        //generate list of column indexes from attribute parameters to compare for table1
        for(int i = 0; i < this.attribute.length; i++){
            for(int j = 0; j < t_attrs.length; j++){
                if(this.attribute[i].equals(t_attrs[j])){
                    t_attrs_index[j] = i;
                    break;
                }
            }
        }
        //generate list of column indexes from attribute parameters to compare for EHMTable2
        for(int i = 0; i < EHMTable2.attribute.length; i++){
            for(int j = 0; j < u_attrs.length; j++){
                if(EHMTable2.attribute[i].equals(u_attrs[j])){
                    u_attrs_index[j] = i;
                    break;
                }
            }
        }
        //ensures column domains for the keys to compare match eachother
        for(int i=0; i < t_attrs_index.length; i++){
            if(this.domain[i] != EHMTable2.domain[i]) {
                type_check = false;
                break;
            }
        }

        if(!type_check){
            return null;
        }

        List <Comparable []> rows = null;
        rows = new ArrayList<Comparable[]>();
        Comparable [] tuple;

        //loop through both sets of rows for tables 
        for(int i = 0; i < this.tuples.size(); i++){
            for(int j = 0; j < EHMTable2.tuples.size(); j++){

                boolean attrs_equal = false;
                //loop through all relevant attribute fields using the generated index list for each table and compare their values 
                for (int k = 0; k < t_attrs_index.length; k++){

                    if(this.tuples.get(i)[t_attrs_index[k]].equals(EHMTable2.tuples.get(j)[u_attrs_index[k]])){
                        
                        attrs_equal = true;
                    }
                    else{
                        attrs_equal = false;
                        break;
                    }
                }
                //concatenates and adds the tuples to our new row list if their key values are equal
                if(attrs_equal){
                    tuple = ArrayUtil.concat(this.tuples.get(i), EHMTable2.tuples.get(j));
                    rows.add(tuple);
                }
            }
        }

        //create new key from two existing keys
        String [] key = ArrayUtil.concat(this.key, EHMTable2.key);

        return new EHMTable(name + count++, ArrayUtil.concat (attribute, EHMTable2.attribute),
                                          ArrayUtil.concat (domain, EHMTable2.domain), key, rows);
    } // join
    /************************************************************************************
     * Join this table and EHMTable2 by performing an equijoin.  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by append "2" to the end of any duplicate attribute name.
     *
     * #usage movie.join ("studioNo", "name", studio)
     * #usage movieStar.join ("name == s.name", starsIn)
     *
     *
     * @param EHMTable2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public EHMTable indexJoin (EHMTable EHMTable2)
    {
        out.println ("\nRA> " + name + ".join (" + EHMTable2.name + ")");
        List <Comparable []> rows = new ArrayList<Comparable[]>();
        Comparable[] tuple;
        for(int i = 0; i < this.tuples.size();i++){

            Integer tuple_index = EHMTable2.index.get(this.tuples.get(i)[primaryKeyIdx]);
            if(tuple_index != null) {
                tuple = ArrayUtil.concat(this.tuples.get(i), EHMTable2.tuples.get(tuple_index));
                rows.add(tuple);
            }
        }
        //create new key from two existing keys
        String [] key = ArrayUtil.concat(this.key, EHMTable2.key);
        return new EHMTable(name + count++, ArrayUtil.concat (attribute, EHMTable2.attribute),
                ArrayUtil.concat (domain, EHMTable2.domain), key, rows);
    } // join
    /************************************************************************************
     * Return the column position for the given attribute name.
     *
     * @param attr  the given attribute name
     * @return  a column position
     */
    public int col (String attr)
    {
        for (int i = 0; i < attribute.length; i++) {
           if (attr.equals (attribute [i])) return i;
        } // for

        return -1;  // not found
    } // col

    /************************************************************************************
     * Insert a tuple to the table.
     *
     * #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
     *
     * @param tup  the array of attribute values forming the tuple
     * @return  whether insertion was successful
     */
    public boolean insert (Comparable [] tup)
    {
        out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            int []        cols   = match (key);
            for (int j = 0; j < keyVal.length; j++) keyVal [j] = tup [cols [j]];
            Integer inttoadd = tuples.size() -1;
            index.put (tup[primaryKeyIdx],inttoadd);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /************************************************************************************
     * Get the name of the table.
     *
     * @return  the table's name
     */
    public String getName ()
    {
        return name;
    } // getName

    /************************************************************************************
     * Print this table.
     */
    public void print ()
    {
        out.println ("\n EHMTable " + name);
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        out.print ("| ");
        for (String a : attribute) out.printf ("%15s", a);
        out.println (" |");
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        for (Comparable [] tup : tuples) {
            out.print ("| ");
            for (Comparable attr : tup) out.printf ("%15s", attr);
            out.println (" |");
        } // for
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex ()
    {
        /*
        out.println ("\n Index for " + name);
        out.println ("-------------------");
        for (Map.Entry <KeyType, Integer> e : index.entrySet ()) {
            out.println (e.getKey () + " -> " + e.toString());
        } // for
        out.println ("-------------------");
        */
        index.print();
    } // printIndex

    /************************************************************************************
     * Load the table with the given name into memory. 
     *
     * @param name  the name of the table to load
     */
    public static EHMTable load (String name)
    {
        EHMTable tab = null;
        try {
            ObjectInputStream ois = new ObjectInputStream (new FileInputStream (DIR + name + EXT));
            tab = (EHMTable) ois.readObject ();
            ois.close ();
        } catch (IOException ex) {
            out.println ("load: IO Exception");
            ex.printStackTrace ();
        } catch (ClassNotFoundException ex) {
            out.println ("load: Class Not Found Exception");
            ex.printStackTrace ();
        } // try
        return tab;
    } // load

    /************************************************************************************
     * Save this table in a file.
     */
    public void save ()
    {
        try {
            ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (DIR + name + EXT));
            oos.writeObject (this);
            oos.close ();
        } catch (IOException ex) {
            out.println ("save: IO Exception");
            ex.printStackTrace ();
        } // try
    } // save

    public int numTuples() {
        return this.tuples.size();
    }

    //----------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Determine whether the two tables (this and EHMTable2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param EHMTable2  the rhs table
     * @return  whether the two tables are compatible
     */
    private boolean compatible (EHMTable EHMTable2)
    {
        if (domain.length != EHMTable2.domain.length) {
            out.println ("compatible ERROR: table have different arity");
            return false;
        } // if
        for (int j = 0; j < domain.length; j++) {
            if (domain [j] != EHMTable2.domain [j]) {
                out.println ("compatible ERROR: tables disagree on domain " + j);
                return false;
            } // if
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Match the column and attribute names to determine the domains.
     *
     * @param column  the array of column names
     * @return  an array of column index positions
     */
    private int [] match (String [] column)
    {
        int [] colPos = new int [column.length];

        for (int j = 0; j < column.length; j++) {
            boolean matched = false;
            for (int k = 0; k < attribute.length; k++) {
                if (column [j].equals (attribute [k])) {
                    matched = true;
                    colPos [j] = k;
                } // for
            } // for
            if ( ! matched) {
                out.println ("match: domain not found for " + column [j]);
            } // if
        } // for

        return colPos;
    } // match

    /************************************************************************************
     * Extract the attributes specified by the column array from tuple t.
     *
     * @param t       the tuple to extract from
     * @param column  the array of column names
     * @return  a smaller tuple extracted from tuple t 
     */
    private Comparable [] extract (Comparable [] t, String [] column)
    {
        Comparable [] tup = new Comparable [column.length];
        int [] colPos = match (column);
        for (int j = 0; j < column.length; j++) tup [j] = t [colPos [j]];
        return tup;
    } // extract

    /************************************************************************************
     * Check the size of the tuple (number of elements in list) as well as the type of
     * each value to ensure it is from the right domain. 
     *
     * @param t  the tuple as a list of attribute values
     * @return  whether the tuple has the right size and values that comply
     *          with the given domains
     */
    private boolean typeCheck (Comparable [] t)
    {
        //check if the tuple has the correct amount of attributes
        if (t.length != attribute.length)
            return false;

        //checks if each attribute is of the correct class
        for (int i = 0; i < t.length; i++) {
            if (!(domain[i].isInstance(t[i])))
                return false;
        }

        return true;
    } // typeCheck

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  an array of Java classes
     */
    private static Class [] findClass (String [] className)
    {
        Class [] classArray = new Class [className.length];

        for (int i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param colPos the column positions to extract.
     * @param group  where to extract from
     * @return  the extracted domains
     */
    private Class [] extractDom (int [] colPos, Class [] group)
    {
        Class [] obj = new Class [colPos.length];

        for (int j = 0; j < colPos.length; j++) {
            obj [j] = group [colPos [j]];
        } // for

        return obj;
    } // extractDom

} // EHMTable class

