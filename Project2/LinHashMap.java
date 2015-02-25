
/************************************************************************************
 * @file LinHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides hash maps that use the Linear Hashing algorithm.
 * A hash table is created that is an array of buckets.
 */
public class LinHashMap <K, V>
    extends AbstractMap <K, V>
    implements Serializable, Cloneable, Map <K, V>
{
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    {
        int    nKeys;
        K []   key;
        V []   value;
        Bucket next;
        
        @SuppressWarnings("unchecked")
	    Bucket (Bucket n)
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = n;
        } // constructor
        
        public Bucket insert(K newKey, V newValue) {
            count++;
            if (this.nKeys == SLOTS) {
                if (next == null)
                    next = new Bucket(null);
                return next.insert(newKey,newValue);
            }
            this.key[nKeys] = newKey;
            this.value[nKeys++] = newValue;
            items++;
            return this;
        }//end insert()
        
        public Bucket remove(K keyToRemove, V valueToRemove) {
            count++;
            for (int i = 0; i < this.nKeys; i++) {
                if (key[i] == keyToRemove) {
                    this.key[i] = this.getLastKey();
                    this.value[i] = this.getLastValue();
                }//end if statement
            }//end for loop
            if (next == null)
                return null;
            return next.remove(keyToRemove,valueToRemove);
        }//end remove()
        
        public K getLastKey() {
            if (next == null)
                return this.key[nKeys--];
            return next.getLastKey();
        }//end getLastKey()
        
        public V getLastValue() {
            if (next == null)
                return this.value[nKeys--];
            return next.getLastValue();
        }//end getLastValue()
    } // end Bucket inner class

    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The index of the next bucket to split.
     */
    private int split = 0;
    
    private int items = 0;

    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * @param classK    the class for keys (K)
     * @param classV    the class for keys (V)
     * @param initSize  the initial number of home buckets (a power of 2, e.g., 4)
     */
    public LinHashMap (Class <K> _classK, Class <V> _classV, int initSize)
	{
	    classK = _classK;
	    classV = _classV;
	    hTable = new ArrayList <> ();
	    mod1   = initSize;
	    mod2   = 2 * mod1;
        for (int i = 0; i < initSize; i++)
            hTable.add(new Bucket(null));
	} // constructor
    
    public void splitBucket() {
        System.out.println("splitBucket(" + split + ")");
        hTable.add(new Bucket(null));
        
        Bucket b = hTable.get(split);
        while (b != null) {
            for (int i = 0; i < b.nKeys; i++) {
                if (h2(b.key[i]) != split) {
                    K key = b.key[i];
                    V value = b.value[i];
                    int newBucket = h2(key);
                    
                    b.remove(key,value);
                    System.out.println("key = " + key);
                    System.out.println("newBucket = " + newBucket);
                    hTable.get(newBucket).insert(key,value);
                }//end if
            }
            b = b.next;
        }
        
        if (++split == mod1) {
            split = 0;
            mod1 = mod2;
            mod2 *= 2;
            System.out.println("mod1 = " + mod1);
            System.out.println("mod2 = " + mod2);
        }
    }

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
	{
	    Set <Map.Entry <K, V>> enSet = new HashSet <> ();

	    //  T O   B E   I M P L E M E N T E D
        Bucket b;
        for (int i = 0; i < hTable.size(); i++) {
            b = hTable.get(i);
            if (b == null)
                continue;
            while (b != null) {
                for (int j = 0; j < b.nKeys; j++) {
                    enSet.add(new SimpleEntry(b.key[j],b.value[j]));
                }
                b = b.next;
            }//end while loop
        }

	    return enSet;
	} // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int i = h (key);

        //  T O   B E   I M P L E M E N T E D
        
        if (i < split)
            i = h2(key);
        
        Bucket b = hTable.get(i);
        count++;
        
        while (b != null) {
            for (i = 0; i < b.nKeys; i++) {
                if (b.key[i] == key)
                    return b.value[i];
            }
            b = b.next;
            count++;
        }

        return null;
    } // get

    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        out.println("put(" + key + "," + value + ")");
        int i = h (key);

        //  T O   B E   I M P L E M E N T E D
        
        if (i < split)
            i = h2(key);
        
        Bucket b = hTable.get(i);
        count++;
        if (b == null) {
            b = new Bucket(null);
        }
        b.insert(key,value);
        System.out.println("Put(" + key + ") into bucket " + i);

        if ((double)((double)items / (double)size()) > 0.75) {
            this.splitBucket();
        }
        
        
        return value;
    } // put

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table. 
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * (mod1 + split);
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Linear Hashing)");
        out.println ("-------------------------------------------");

        out.println("Key|Value");
        //  T O   B E   I M P L E M E N T E D
        for (int i = 0; i < hTable.size(); i++) {
            out.print(i + "\t");
            if (hTable.get(i) == null)
                continue;
            Bucket b = hTable.get(i);
            while (b != null) {
                for (int j = 0; j < b.nKeys; j++) {
                    out.print(b.key[j] + "|" + b.value[j]);
                    out.print("\t\t");
                }
                b = b.next;
                count++;
            }
            out.println();
        }

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h (Object key)
    {
        return key.hashCode () % mod1;
    } // h

    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h2 (Object key)
    {
        return key.hashCode () % mod2;
    } // h2

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class, 11);
        int nKeys = 30;
        if (args.length == 1)
            nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i += 2)
            ht.put (i, i * i);
        ht.print ();
        for (int i = 1; i < nKeys; i += 2) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main

} // LinHashMap class
