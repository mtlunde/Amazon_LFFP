
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