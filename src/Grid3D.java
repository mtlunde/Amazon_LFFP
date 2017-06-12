
// 3D grid object
public class Grid3D {

	private String[][][] table;  // tb initialized
	private double xdimen;
	private double ydimen;
   private double zdimen;
   private double cubitsize;
	
	// creates grid
	public Grid3D(double xdim, double ydim, double zdim, double cb_size) {  // units and cb_size tbd
		this.xdimen = xdim;
		this.ydimen = ydim;
      this.zdimen = zdim;
      this.cubitsize = cb_size;
		table = new String[(int)(xdim / cb_size) + 2][(int)(ydim / cb_size) + 2][(int)(zdim / cb_size) + 2];
		populate(table, (int)(xdim / cb_size) + 2, (int)(ydim / cb_size) + 2, (int)(zdim / cb_size) + 2);
		
	}
	
   // populates grid
	public void populate(String[][][] mat, int x, int y, int z) {
      for(int i = 0; i < x; i++) {
			for(int j = 0; j < y; j++) {
				for (int k = 0; k < z; k++) {
               mat[i][j][k] = "-1";
            }
			}
		}
      for (int i = 1; i < x - 1; i++) {
         for (int j = 1; j < y - 1; j++) {
            for (int k = 1; k < z - 1; k++) {
               mat[i][j][k] = "00";
            }
         }
      }
	}
   
   // access table
   public String[][][] getTable() {
      return table;
   }
	
   // access xdimen
   public double getXDimen() {
      return xdimen;
   }
   
   // access ydimen
   public double getYDimen() {
      return ydimen;
   }
   
   // access zdimen
   public double getZDimen() {
      return zdimen;
   }
   
   // access cubitsize
   public double getCubitSize() {
      return cubitsize;
   }
   
   // places an item -- takes parameters (table, item number, longer dimension, shorter dimension, z dimension, orientation, x-location, y-location, z-location)
   public void placeItem(String[][][] mat, String itemnum, int ldim, int sdim, int zdim, int orient, int x, int y, int z) {
      if (orient == 0) { // 0 means long side goes horizontal
         for (int i = x; i < x + sdim; i++) {
            for (int j = y; j < y + ldim; j++) {
               for (int k = z; k < z + zdim; k++) {
                  mat[i][j][k] = itemnum;
               }
            }
         }
      } else if (orient == 1) { // 1 means long side goes vertical
         for (int i = x; i < x + ldim; i++) {
            for (int j = y; j < y + sdim; j++) {
               for (int k = z; k < z + zdim; k++) {
                  mat[i][j][k] = itemnum;
               }
            }
         }
      } else {
         System.out.println("Invalid orientation input.");
      }
   }

}