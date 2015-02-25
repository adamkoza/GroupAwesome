import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

import static java.lang.System.out;

public class ExtHashMap <K, V>
        extends AbstractMap<K, V>
        implements Serializable, Cloneable, Map<K, V> {
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    private int gd = 0;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    {
        int  nKeys;
        K [] key;
        V [] value;
        int ld;
        int bNum;
        @SuppressWarnings("unchecked")
        Bucket ()
        {
            ld = 1;
            nKeys = 0;
            key   = (K []) Array.newInstance(classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
        } // constructor
    } // Bucket inner class

    /** The hash table storing the buckets (buckets in physical order)
     */
    private final List<Bucket> hTable;

    /** The directory providing access paths to the buckets (buckets in logical oder)
     */
    private final List <Bucket> dir;

    /** The modulus for hashing (= 2^D) where D is the global depth
     */
    private int mod;

    /** The number of buckets
     */
    private int nBuckets;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /********************************************************************************
     * Construct a hash table that uses Extendable Hashing.
     * @param _classK    the class for keys (K)
     * @param _classV    the class for keys (V)
     * @param initSize  the initial number of buckets (a power of 2, e.g., 4)
     */
    public ExtHashMap (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList<> ();   // for bucket storage
        dir    = new ArrayList<> ();   // for bucket access
        mod    = nBuckets = initSize;
        gd     = 1;
        for(int i = 0; i < initSize; i++){

            hTable.add(new Bucket());
            hTable.get(i).ld = initSize/2;
            hTable.get(i).bNum = i;
            dir.add(hTable.get(i));

        }
    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set<Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet<>();
        Bucket b = null;

        //loop through buckets and get all keys and values
        for(int i = 0; i < this.nBuckets; i++) {
            b = hTable.get(i);
            this.count++;
            for(int j = 0; j < b.nKeys; j++){
                //add each key/value set to the total Set
                Map.Entry <K,V> ent = new AbstractMap.SimpleEntry<K,V>(b.key[j],b.value[j]);
                enSet.add(ent);
            }
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
        int    i = h (key);
        Bucket b = dir.get (i);

        //loop through bucket the key hashes to and find the value in it's list of members
        for(int j =0; j < b.key.length; j++){
            if(b.key[j] == key){
                return b.value[j];
            }
        }
        return null;
    } // get
    /********************************************************************************
     * Given the bucket, split and redistribute members
     * @param b  the bucket to split
     */
    public void splitBucket(Bucket b) {

        //if the local depth equals the global depth, double the directory
        if (b.ld > this.gd) {
            this.gd++;
            //duplicate directory and then set all members to correct hashed bucket
            List<Bucket> dir2 = dir;
            dir.addAll(dir2);
            for (int j = 0; j < dir.size(); j++) {
                for (int k = 0; k < hTable.size(); k++) {
                    Bucket c = hTable.get(k);
                    this.count++;
                    if(j%(Math.pow(2,c.ld)) == c.bNum) {
                        dir.set(j, c);
                    }
                }
            }
            mod *= 2;

        }
        //if the local depth is less than the global depth, reassign directory members to accommodate for added bucket
        else{
            for(int j = 0; j < dir.size(); j++) {
                for(int k = 0; k < hTable.size(); k++){
                    Bucket tempB = hTable.get(k);
                    this.count++;
                    if(j%(Math.pow(2,tempB.ld)) == tempB.bNum ){
                        dir.set(j, tempB);
                    }
                }

            }
        }
        //in the bucket to split, rehash the keys and put them in the new directory if needed
        for(int k = 0; k < b.nKeys; k++){
            int m = h(b.key[k]);
            Bucket c = dir.get(m);
            if(b != c){
                this.put(b.key[k],b.value[k]);
                b.key[k]=null;
                b.value[k]=null;
            }
        }
        int count = 0;
        //condense bucket to remove null entries moved to added bucket
        for(int k = 0; k < b.nKeys; k++){
            if(b.key[k] != null){
                b.key[count] = b.key[k];
                b.value[count] = b.value[k];
                count++;
            }
        }
        b.nKeys = count;

    }
    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value) {
        int    i = h (key);
        Bucket b = dir.get(i);

        //inserts key/value pair into the bucket if there is room
        if(b.nKeys < SLOTS) {
            out.println("Inserting key: " + key.toString() + " (hashed:" + i + ") value:" + value.toString() + " into bucket " + b.bNum);
            b.key[b.nKeys] = key;
            b.value[b.nKeys] = value;
            b.nKeys++;
        }
        //else adds a bucket and splits the full bucket, then tries to add the key/value pair again
        else{
            hTable.add(new Bucket());

            Double d = Math.pow(2,b.ld);

            Bucket c = hTable.get(hTable.size()-1);
            this.count++;

            c.bNum = b.bNum+d.intValue();
            b.ld++;
            c.ld = b.ld;
            splitBucket(b);

            this.put(key, value);

        }

        return null;
    } // put

    /********************************************************************************
     * Return the size (SLOTS * number of buckets) of the hash table.
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * nBuckets;
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("-------------------------------------------");
        out.println ("Hash Table (Extendible Hashing)");
        out.println ("-------------------------------------------");

        for(int i = 0; i < hTable.size(); i++){
            Bucket b = hTable.get(i);
            out.println("Bucket " + b.bNum + "\n------------------");
            out.println("-Key-\t-Value-\n");
            for(int j =0; j < b.nKeys; j++){
                out.print("  " + b.key[j].toString() + "        ");
                out.print(b.value[j].toString() + "\n");
            }
            out.println("------------------");
        }
        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the hash function.
     * @param key  the key to hash
     * @return  the location of the directory entry referencing the bucket
     */
    private int h (Object key)
    {
        return key.hashCode () % mod;
    } // h

    /********************************************************************************
     * The main method used for testing.
     * @param args -- the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        ExtHashMap <Integer, Integer> ht = new ExtHashMap<> (Integer.class, Integer.class, 2);
        int nKeys = 60;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 3; i < nKeys; i += 1) ht.put (i, i * i);
        ht.print();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main

} // ExtHashMap class
