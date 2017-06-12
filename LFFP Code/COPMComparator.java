
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