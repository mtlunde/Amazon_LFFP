// Developed for Amazon by Matt Lunde
//
// Sorts the final array of used items.
// Can be found in line 686 of LFFPFinalAuto and line 513 of LFFPFinalManual.
//
// Questions can be directed to mtl15@comcast.net

import java.util.*;

public class FinalComparator implements Comparator<int[]> {
   public int compare(int[] x, int[] y) {
      if (x[8] > y[8]) {
         return 11;
      } else if (x[8] < y[8]) {
         return -1;
      } else {
         return 0;
      }
   }
}