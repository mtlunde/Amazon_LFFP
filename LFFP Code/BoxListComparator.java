
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