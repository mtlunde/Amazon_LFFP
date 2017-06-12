// Developed for Amazon by Matt Lunde
//
// Item Object
// Stores item information
//
// Questions can be directed to mtl15@comcast.net

public class Item {
   
   private String itemnumber;
   private int xdimen;
   private int ydimen;
   private int zdimen;
   private int xdimen1;
   private int ydimen1;
   private int zdimen1;
   private int xdimen2;
   private int ydimen2;
   private int zdimen2;

   private int available = 0;
   
   // creates item
   public Item(String itemnum, int xdim, int ydim, int zdim) {
      this.itemnumber = itemnum;
      this.xdimen = xdim;
      this.ydimen = ydim;
      this.zdimen = zdim;
      this.xdimen1 = xdim;
      this.ydimen1 = zdim;
      this.zdimen1 = ydim;
      this.xdimen2 = zdim;
      this.ydimen2 = ydim;
      this.zdimen2 = xdim;
   }
      
   // access xdimen
   public int getXDimen() {
      return xdimen;
   }
   
   // access ydimen
   public int getYDimen() {
      return ydimen;
   }
   
   // access zdimen
   public int getZDimen() {
      return zdimen;
   }
   
    // access xdimen1
   public int getXDimen1() {
      return xdimen1;
   }
   
   // access ydimen1
   public int getYDimen1() {
      return ydimen1;
   }
   
   // access zdimen1
   public int getZDimen1() {
      return zdimen1;
   }

    // access xdimen2
   public int getXDimen2() {
      return xdimen2;
   }
   
   // access ydimen2
   public int getYDimen2() {
      return ydimen2;
   }
   
   // access zdimen2
   public int getZDimen2() {
      return zdimen2;
   }

   // access itemnumber
   public String getItemNumber() {
      return itemnumber;
   }
   
   // access available
   public int getAvailable() {
      return available;
   }
   
   // change available from 0 (available) to 1 (not available/ used)
   public void changeAvailable() {
      available = 1;
   }
   
   // change available from 1 (not available/ used) to 0 (available)
   public void resetAvailable() {
      available = 0;
   }

}