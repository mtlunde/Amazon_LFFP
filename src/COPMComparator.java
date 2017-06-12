// Developed for Amazon by Matt Lunde
//
// Sorts an array of COPMs putting items with larger dimensions towards the top of the list.
// Can be found in line 375 of LFFPFinalAuto and line 223 of LFFPFinalManual.
//
// Questions can be directed to mtl15@comcast.net

import java.util.*;

public class COPMComparator implements Comparator<int[]> {
   public int compare(int[] x, int[] y) {
      if (x[0] > y[0]) {
         return -1;
      } else if (x[0] < y[0]) {
         return 1;
      } else {
         if (x[1] > y[1]) {
            return -1;
         } else if (x[1] < y[1]) {
            return 1;
         } else {
            return 0;
         }
      }
   }
}