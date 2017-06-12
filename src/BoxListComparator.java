// Developed for Amazon by Matt Lunde
//
// Sorts the array of box dimensions putting the lowest volume box at the top of the list.
// Can be found in line 801 of LFFPFinalAuto.
//
// Questions can be directed to mtl15@comcast.net

import java.util.*;

public class BoxListComparator implements Comparator<double[]> {
   public int compare(double[] x, double[] y) {
      if (x[3] > y[3]) {
         return 1;
      } else if (x[3] < y[3]) {
         return -1;
      } else {
         return 0;
      }
   }
}