/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides B+Tree maps.  B+Trees are used as multi-level index structures
 * that provide efficient access for both point queries and range queries.
 */
public class BpTreeMap <K extends Comparable <K>, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The maximum fanout for a B+Tree node.
     */
    private static final int ORDER = 5;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines nodes that are stored in the B+tree map.
     */
    private class Node
    {
        boolean   isLeaf;
        int       nKeys;
        K []      key;
        Object [] ref;
        Node      parent;
        @SuppressWarnings("unchecked")
        Node (boolean _isLeaf, Node _parent)
        {
            isLeaf = _isLeaf;
            parent = _parent;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, ORDER - 1);
            if (isLeaf) {
                //ref = (V []) Array.newInstance (classV, ORDER);
                ref = new Object [ORDER];
            } else {
                ref = (Node []) Array.newInstance (Node.class, ORDER);
            } // if
        } // constructor
    } // Node inner class

    /** The root of the B+Tree
     */
    private Node root;

    /** The counter for the number nodes accessed (for performance testing).
     */
    private int count = 0;

    /********************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTreeMap (Class <K> _classK, Class <V> _classV)
    {
        classK = _classK;
        classV = _classV;
        root   = new Node (true, null);
    } // constructor

    /********************************************************************************
     * Return null to use the natural order based on the key type.  This requires the
     * key type to implement Comparable.
     */
    public Comparator <? super K> comparator () 
    {
        return null;
    } // comparator

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Entry <K, V>> entrySet ()
    {
        Set <Entry <K, V>> enSet = new HashSet <> ();

        for(Entry<K,V> entry: this.entrySet()) {
            enSet.add(entry);
        }

        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public V get (Object key)
    {
        return find ((K) key, root);
    } // get

    /********************************************************************************
     * Put the key-value pair in the B+Tree map.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        // Node[] node = new Node[2];
        // node = searchForCorrectNode (key, root, null); // node[0] is the correct node and node[1] is parent
        Node n = searchForCorrectNode(key, root);
        // System.out.println("AfterCorrectNode: " + (n == root));
        insert (key, value, n, n.parent);
        return null;
    } // put

    /********************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     */
    public K firstKey () 
    {
        K key = null;

        for(Entry<K,V> entry: this.entrySet()) {
            if(key.compareTo(entry.getKey()) < 0)
               key = entry.getKey();
        }

        return key;
    } // firstKey

    /********************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     */
    public K lastKey () 
    {
        K key = null;

        for(Entry<K,V> entry: this.entrySet()) {
            if(key.compareTo(entry.getKey()) >= 0)
                key = entry.getKey();
        }

        return key;
    } // lastKey

    /********************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     */
    public SortedMap <K,V> headMap (K toKey)
    {

        BpTreeMap<K,V> headMap = new BpTreeMap<>(this.classK,this.classV);

        K key;
        V value;

        for(Entry<K,V> entry: this.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if(!key.equals(toKey))
                headMap.put(key,value);
            else
                break;
        }

        return headMap;
    } // headMap

    /********************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {
        BpTreeMap<K,V> tailMap = new BpTreeMap<>(this.classK,this.classV);

        K key;
        V value;

        for(Entry<K,V> entry: this.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if(key.compareTo(fromKey) >= 0)
                tailMap.put(key,value);
        }

        return tailMap;
    } // tailMap

    /********************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {
        BpTreeMap<K,V> subMap = new BpTreeMap<>(this.classK,this.classV);

        K key;
        V value;

        for(Entry<K,V> entry: this.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if((key.compareTo(fromKey) >= 0) && (key.compareTo(toKey) < 0))
                subMap.put(key,value);
        }

        return subMap;
    } // subMap

    /********************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     */
    public int size ()
    {
        int sum = 0;

        for(Entry<K,V> entry: this.entrySet()) {
            sum++;
        }

        return  sum;
    } // size

    /********************************************************************************
     * Print the B+Tree using a pre-order traveral and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
        out.println ("BpTreeMap");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
        out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
        out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
        } // if

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param ney  the current node
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        for (int i = 0; i < n.nKeys; i++) {
            K k_i = n.key [i];
            if (key.compareTo (k_i) <= 0) {
                if (n.isLeaf) {
                    return (key.equals (k_i)) ? (V) n.ref [i] : null;
                } else {
                    return find (key, (Node) n.ref [i]);
                } // if
            } // if
        } // for
        return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]);
    } // find

    /********************************************************************************
     * Recursive helper function for finding the correct node to insert a key in.
     * @param key  the key to find
     * @param n  the current node
     * @param p  the parent node
     */
    private Node searchForCorrectNode(K key, Node n)
    {
        if (n.isLeaf) {
            return n;
        } else {
            for (int i = 0; i < n.nKeys; i++) {
                if (n.key[i].compareTo(key) < 0)
                    searchForCorrectNode(key, (Node) n.ref[i]);
                // else if (i == n.nKeys - 1)
                    // searchForCorrectNode(key, (Node) n.ref[i+1]);
            }
            return searchForCorrectNode(key, (Node) n.ref[n.nKeys]);
        }
    }

    /********************************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param p    the parent node
     */
    private void insert (K key, V ref, Node n, Node p)
    {
        boolean b = true;
        // System.out.println("Insert");
        if (n.nKeys < ORDER - 1) {
            // System.out.println("First if");
            for (int i = 0; i < n.nKeys; i++) {
                if (key.compareTo(n.key[i]) < 0) {
                    wedge (key, ref, n, i);
                    b = false;
                } 
                // else if (i == n.nKeys - 1) {
                    // wedge (key, ref, n, n.nKeys);
                // }
            }
            if (b) {
                wedge (key, ref, n, n.nKeys);
            }
            if (n.nKeys == 0) {
                wedge (key, ref, n, 0);
            }
        } else if ((p != null) && (p.nKeys < ORDER - 1)) {
            // System.out.println("First else if");
            Node sn = split(key, ref, n);
            for (int i = 0; i < p.nKeys; i++) { // Point parent to the new split node
                if (key.compareTo(p.key[i]) < 0) {
                    wedge (key, (V) sn, p, i);
                } else if (i == p.nKeys - 1) {
                    wedge (key, (V) sn, p, p.nKeys);
                }
            }
        } else if ((p != null) && (p != root)){
            // System.out.println("second else if");
            Node sn = split(key, ref, p);
            insert(sn.key[0], (V) sn, p, p.parent);
        } else {
            // System.out.println("else");
            Node newRoot = new Node(false, null);
            root = newRoot;
            n.parent = root;
            Node sn = split(key, ref, n);
            root.key[0] = sn.key[0];
            root.ref[0] = n;
            root.ref[1] = sn;
        }

        // if (n.isLeaf) { // leaf node
            // if (n.nKeys < ORDER - 1) {
                // for (int i = 0; i < n.nKeys; i++) {
                    // K k_i = n.key [i];
                    // if (key.compareTo (k_i) < 0) {
                        // wedge (key, ref, n, i);
                        // return;
                    // } else if (key.equals (k_i)) {
                        // out.println ("BpTreeMap:insert: attempt to insert duplicate key = " + key);
                    // } // if
                // } // for
                // wedge (key, ref, n, n.nKeys);
                // return;
            // } else if (p.nKeys < ORDER - 1) {
                // Node sib = split (key, ref, n);
                // insert(n.key[i], n.ref, p, null); // point parent to newly split key
                // return;
            // } else {
                // Node sib = split (key, ref, n);
            // }
        // } else { // internal node
            // for (int i = 0; i < n.nKeys; i++) { // find correct node
                // K k_i = n.key[i];
                // if (key.compareTo(k_i) <= 0) {
                    // insert(key, ref, (Node) n.ref[i], n);
                    // return;
                // }
            // }
        // }

    } // insert

    /********************************************************************************
     * Wedge the key-ref pair into node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     */
    private void wedge (K key, V ref, Node n, int i)
    {
        for (int j = n.nKeys; j > i; j--) {
            n.key [j] = n.key [j - 1];
            n.ref [j] = n.ref [j - 1];
        } // for
        n.key [i] = key;
        n.ref [i] = ref;
        n.nKeys++;
    } // wedge

    /********************************************************************************
     * Split node n and return the newly created node.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     */
    private Node split (K key, V ref, Node n)
    {
        K tempKeys[] = (K []) Array.newInstance (classK, ORDER);
        V tempRefs[] = (V []) Array.newInstance (classV, ORDER + 1);

        for (int i = 0; i < n.nKeys; i++) {
            tempKeys[i] = n.key[i];
        }
        for (int i = 0; i <= n.nKeys; i++) {
            tempRefs[i] = (V) n.ref[i];
        }
        // Arrays.sort(tempKeys);
        // Arrays.sort(tempRefs);

        int mid = ORDER / 2;
        Node sn = new Node(n.isLeaf, n.parent);
        sn = n;

        Arrays.fill(n.key, null);
        Arrays.fill(n.ref, null);

        n.key = Arrays.copyOfRange(tempKeys, 0, mid);
        sn.key = Arrays.copyOfRange(tempKeys, mid, tempKeys.length - 1);
        n.ref = Arrays.copyOfRange(tempRefs, 0, mid);
        sn.ref = Arrays.copyOfRange(tempRefs, mid, tempRefs.length - 1);
        // for (int i = 0; i < mid; i++) {
            // n.key[i] = tempKeys[i];
        // }
        // for (int i = 0; i < tempKeys.length; i++) {
            // sn.key[i] = tempKeys[i + mid];
        // }
        // for (int i = 0; i < mid + 1; i++) {
            // n.ref[i] = (V) tempRefs[i];
        // }
        // for (int i = 0; i < tempRefs.length; i++) {
            // sn.ref[i] = (V) tempRefs[i + mid];
        // }

        return sn;
    } // split

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        BpTreeMap <Integer, Integer> bpt = new BpTreeMap <> (Integer.class, Integer.class);
        int totKeys = 10;
        if (args.length == 1) totKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < totKeys; i += 2) bpt.put (i, i * i);
        bpt.print (bpt.root, 0);
        for (int i = 0; i < totKeys; i++) {
            out.println ("key = " + i + " value = " + bpt.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of nodes accessed = " + bpt.count / (double) totKeys);
    } // main

} // BpTreeMap class
