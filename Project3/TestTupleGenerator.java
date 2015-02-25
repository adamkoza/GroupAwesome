/*****************************************************************************************
 * @file  TestTupleGenerator.java
 *
 * @author   Sadiq Charaniya, John Miller
 */

import static java.lang.System.out;
import java.util.*;

/*****************************************************************************************
 * This class tests the TupleGenerator on the Student Registration Database defined in the
 * Kifer, Bernstein and Lewis 2006 database textbook (see figure 3.6).  The primary keys
 * (see figure 3.6) and foreign keys (see example 3.2.2) are as given in the textbook.
 */
public class TestTupleGenerator
{
    /*************************************************************************************
     * The main method is the driver for TestGenerator.
     * @param args  the command-line arguments
     */
    public static void main (String [] args)
    {
        TupleGenerator test = new TupleGeneratorImpl ();

        test.addRelSchema ("Student",
                           "id name address status",
                           "Integer String String String",
                           "id",
                           null);
        
        test.addRelSchema ("Professor",
                           "id name deptId",
                           "Integer String String",
                           "id",
                           null);
        
        test.addRelSchema ("Course",
                           "crsCode deptId crsName descr",
                           "String String String String",
                           "crsCode",
                           null);
        
        test.addRelSchema ("Teaching",
                           "crsCode semester profId",
                           "String String Integer",
                           "crcCode semester",
                           new String [][] {{ "profId", "Professor", "id" },
                                            { "crsCode", "Course", "crsCode" }});
        
        test.addRelSchema ("Transcript",
                           "studId crsCode semester grade",
                           "Integer String String String",
                           "studId crsCode semester",
                           new String [][] {{ "studId", "Student", "id"},
                                            { "crsCode", "Course", "crsCode" },
                                            { "crsCode semester", "Teaching", "crsCode semester" }});

        String [] tables = { "Student", "Professor", "Course", "Transcript", "Teaching" };
        
        int tups [] = new int [] { 10000, 1000, 2000, 50000, 5000 };
    
        Comparable [][][] resultTest = test.generate (tups);

        Table ourTable = new Table ("Student", "id name address status", "Integer String String String", "id");

        ArrayList<Integer> ourIDs = new ArrayList<Integer>();
        
        for (int i = 0; i < 1/* i < resultTest.length */; i++) {
            out.println (tables [i]);
            for (int j = 0; j < resultTest [i].length; j++) {
                // for (int k = 0; k < resultTest [i][j].length; k++) {
                    // out.print (resultTest [i][j][k] + ",");
                // } // for
                if (j % 100 == 0) {
                    ourIDs.add((Integer) resultTest[i][j][0]);
                }
                ourTable.insert(resultTest[i][j]);
                out.println ();
            } // for
            out.println ();
        } // for

        for (int i = 0; i < ourIDs.size(); i++) {
            out.println("ID: " + ourIDs.get(i));
        }

        long startTime = System.nanoTime();
        ourTable.select(new KeyType (ourIDs.get(50)));
        long endTime = System.nanoTime();
        float duration = (float) (endTime - startTime) / 1000000;

        out.println("Tables Size: " + ourTable.numTuples());
        out.println("Select Time: " + duration);
    } // main

} // TestTupleGenerator
