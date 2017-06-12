// Developed for Amazon by Matt Lunde
//
// This is an algorithm that packs a box using the Less Flexibility First Principle.
// It takes inputs from an excel file and automatically runs through the whole set.
//
// Questions can be directed to mtl15@comcast.net
//
// Excel input file should have one sheet contianing all data and a second sheet containing the box suite. 
// All other sheets can be used for output results. Seen in lines 38-40.
// The first sheet (data sheet) must have fsi numbers in column B, item dimensions in columns F-H, recommended 
// box dimensions in columns J-L, and the number of units of the same item in an order in column M.
// An example is uploaded on Box.com
// 
// An example of how to write outputs to an excel file is shown in lines 163-178.

import java.io.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class LFFPFinalAuto { // all inputs must be in the same units
   
   public static final double CUBIT_SIZE = 0.25; // input cubit size
   public static final double E = 0; // input buffer size -- this increases the item dimesions by the set amount to accomodate robot placement accuracy issues
   
   public static void main(String[] args) throws FileNotFoundException {
      
      long totaltimestart = System.currentTimeMillis(); // starts the timer for the entire program
      long timerstart;
      long timerstop;
      
      // if uncommented, these two lines will change the output location from the console to a text file called output.txt
      //PrintStream OUT = new PrintStream(new FileOutputStream("output.txt"));
      //System.setOut(OUT);
      
      try {
         FileInputStream file = new FileInputStream(new File("10TestCases.xlsx")); // reads in orders from an excel file
         XSSFWorkbook workbook = new XSSFWorkbook(file);
         XSSFSheet sheet = workbook.getSheetAt(0); // the first sheet of the excel file must be the order/item data
         XSSFSheet sheet1 = workbook.getSheetAt(1); // the second sheet of the excel file must be the box suite data for the box suite being used
         //XSSFSheet sheet2 = workbook.getSheetAt(2); // other sheets are available for the output of data
         Iterator<Row> rowIterator = sheet.iterator();
         while (rowIterator.hasNext()) { // forces all fsi numbers to be strings so equality can be tested later in the program
            Row row = rowIterator.next();
            row.getCell(1).setCellType(Cell.CELL_TYPE_STRING); 
         }
         double[][] boxlist = new double[21][4];
         for (int i = 0; i < 21; i++) { // grabs box suite data from the second sheet in the excel file and puts it in an array
            boxlist[i][0] = sheet1.getRow(i).getCell(2).getNumericCellValue();
            boxlist[i][1] = sheet1.getRow(i).getCell(3).getNumericCellValue();
            boxlist[i][2] = sheet1.getRow(i).getCell(4).getNumericCellValue();
            boxlist[i][3] = sheet1.getRow(i).getCell(5).getNumericCellValue();
         }
         int index = 0;
         int counter = 0;
         int numofitems;
         int numoforders = 0;
         double[][] itemdata = new double[10][3];
         double[][] recboxdata = new double[1][3];
         int tally = 0;
         int totaltally = 0;
         int same = 0;
         int smaller = 0;
         int larger = 0;
         while (sheet.getRow(index) != null) { // while there is another order to process
            timerstart = System.currentTimeMillis(); // start timer for individual order
            numofitems = 0; // initialize the number of items in the order
            
            // while the fsi number is the same
            while (sheet.getRow(index).getCell(1).getStringCellValue() == sheet.getRow(counter).getCell(1).getStringCellValue()) {
               for (int i = 0; i < sheet.getRow(counter).getCell(12).getNumericCellValue(); i++) { // grab item dimension values
                  itemdata[i + numofitems][0] = sheet.getRow(counter).getCell(5).getNumericCellValue();
                  itemdata[i + numofitems][1] = sheet.getRow(counter).getCell(6).getNumericCellValue();
                  itemdata[i + numofitems][2] = sheet.getRow(counter).getCell(7).getNumericCellValue();
               }
               numofitems = numofitems + (int)sheet.getRow(counter).getCell(12).getNumericCellValue(); // update number of items in the order
               counter++;
               if (counter > sheet.getLastRowNum()) { // stop if there are no more items in the order
                  break;
               }
            }
            numoforders++;
            
            // grab the recommended box dimensions
            recboxdata[0][0] = sheet.getRow(index).getCell(9).getNumericCellValue();
            recboxdata[0][1] = sheet.getRow(index).getCell(10).getNumericCellValue();
            recboxdata[0][2] = sheet.getRow(index).getCell(11).getNumericCellValue();
            Item[] arr = new Item[numofitems]; // create an array of all item objects in the order
            for (int i = 0; i < numofitems; i++) { // create item objects
               arr[i] = new Item(Integer.toString((i + 1) * 11), (int)Math.ceil((itemdata[i][0] + E) / CUBIT_SIZE), (int)Math.ceil((itemdata[i][1] + E) / CUBIT_SIZE), (int)Math.ceil((itemdata[i][2] + E) / CUBIT_SIZE));
            }
            int[] useditems = new int [arr.length];
            int[][] usedcopms = new int[numofitems][10];
            int count = 0;
            double[][] rawboxdata = new double[1][3];
            int[][] boxdata = new int[1][3];
            double recboxvol = recboxdata[0][0] * recboxdata[0][1] * recboxdata[0][2]; // calculate recommended box volume
            double actboxvol = 0; // initialize the actual box volume
            
            // create the final box list including the recommended box if it is not in the box suite
            double[][] updatedboxlist = createUpdatedBoxList(boxlist, recboxdata[0][0], recboxdata[0][1], recboxdata[0][2]);
            
            for (int h = 0; h < updatedboxlist.length; h++) { // for each box in the final box list (starts from the smallest and goes up)
               count = 0;
               for (int i = 0; i < useditems.length; i++) { // make all items available for packing
                  arr[i].resetAvailable();
               }
               
               // grab the box dimensions to be used for this iteration
               rawboxdata[0][0] = updatedboxlist[h][0];
               rawboxdata[0][1] = updatedboxlist[h][1];
               rawboxdata[0][2] = updatedboxlist[h][2];
               
               // create a 3D array to represent the box in terms of cubits
               Grid3D x = new Grid3D(rawboxdata[0][1], rawboxdata[0][0], rawboxdata[0][2], CUBIT_SIZE);
               boxdata[0][0] = (int)(rawboxdata[0][1] / CUBIT_SIZE); boxdata[0][1] = (int)(rawboxdata[0][0] / CUBIT_SIZE); boxdata[0][2] = (int)(rawboxdata[0][2] / CUBIT_SIZE);
               
               for (int z = 0; z < numofitems; z++) { // for as many times as there are items in the order
                  for (int i = 0; i < useditems.length; i++) { // determine which items are available to be packed
                     useditems[i] = arr[i].getAvailable();
                  }
                  int[][] cornerdata = findCorners(x.getTable()); // finds all corners inside the box
                  if (cornerdata[0][0] == 0) { // stop if there are no corners found
                     break;
                  }
                  int[][] copmdata = createCOPMs(cornerdata, arr, numofitems); // creates COPMs
                  int[][] evaluatedcopms = evaluateCOPMs(copmdata, boxdata, x.getTable(), numofitems, useditems); // evaluates all COPMs
                  int selection = selectCOPM(evaluatedcopms, boxdata, x.getTable(), numofitems, arr, useditems); // selects which COPM to use
                                    
                  try {
                     
                     // checks the space to ensure the item will fit
                     boolean empty = checkArea(x.getTable(), evaluatedcopms[selection][0], evaluatedcopms[selection][1], evaluatedcopms[selection][2], evaluatedcopms[selection][3], evaluatedcopms[selection][4], evaluatedcopms[selection][5], evaluatedcopms[selection][6]);
                     if (empty == true) {
                        
                        // places an item based on the COPM selection
                        x.placeItem(x.getTable(), arr[evaluatedcopms[selection][8]].getItemNumber(), evaluatedcopms[selection][0], evaluatedcopms[selection][1], evaluatedcopms[selection][2], evaluatedcopms[selection][3], evaluatedcopms[selection][4], evaluatedcopms[selection][5], evaluatedcopms[selection][6]);
                        for (int i = 0; i < 10; i++) { // stores placed item information
                           usedcopms[count][i] = evaluatedcopms[selection][i];
                        }
                        count++;
                        arr[evaluatedcopms[selection][8]].changeAvailable(); // makes the placed item unavailable to be packed
                        
                        // if uncommented, this will print the current state of the box
                        //printBoxArray(x.getTable(), boxdata);
                     }
                  } catch (ArrayIndexOutOfBoundsException e) {
                     continue;
                  }  
               }
               if (numofitems == count) { // if all items in the order were packed in the current box
                  
                  // if uncommented, this will print the final state of the box
                  //printBoxArray(x.getTable(), boxdata);
                  System.out.print(numoforders + ".  "); // prints the order number in terms of the number of orders in the excel file
                  double[][] outputlocations = translateItemLocation(usedcopms, numofitems, count, itemdata); // translates item locations to coordinates that a robot would use
                  actboxvol = rawboxdata[0][0] * rawboxdata[0][1] * rawboxdata[0][2]; // calculates the volume of the box used
                  double utilization = computeVolumeUtilization(x.getTable(), boxdata); // calculates the volume utilization of the box used
                  System.out.print("Box Used: " + rawboxdata[0][0] + ", " + rawboxdata[0][1] + ", " + rawboxdata[0][2] + "\t\t");
                  System.out.print("Box Recommended: " + recboxdata[0][0] + ", " + recboxdata[0][1] + ", " + recboxdata[0][2]);
                  double voldiff = Math.round(1000.0 * (actboxvol - recboxvol)) / 1000.0; // calculates the volume difference between the box used and the box recommended
                  
                  // this is an example of how to print outputs to an excel sheet
                  /*Row r = sheet.getRow(index);
                  Cell c = r.getCell(15);
                  if (c == null) {
                     c = r.createCell(15, Cell.CELL_TYPE_NUMERIC);
                  }
                  c.setCellValue(voldiff);
                  Row r1 = sheet2.getRow(numoforders);
                  if (r1 == null) {
                     r1 = sheet2.createRow(numoforders);
                  }
                  Cell c1 = r1.getCell(0);
                  if (c1 == null) {
                     c1 = r1.createCell(0, Cell.CELL_TYPE_NUMERIC);
                  }
                  c1.setCellValue(voldiff); */
                  
                  System.out.println("    Volume +/- From Recommended Box: " + voldiff);
                  System.out.println("Box Used Volume: " + actboxvol + " in3");
                  timerstop = System.currentTimeMillis(); // stops the timer for the individual order
                  double executiontime = (timerstop - timerstart) / 1000.0; // calculates the execution time for the individual order
                  System.out.println("Execution Time: " + executiontime + " seconds");
                  System.out.println("-----------------------------------------------------------------------------------------------------------");
                  System.out.println();
                  break;
               }
            }
            if (numofitems != count) {
               actboxvol = 0;
            }
            if (actboxvol < 0.001) {
               /*Row r2 = sheet.getRow(index);
               Cell c2 = r2.getCell(15);
               if (c2 == null) {
                  c2 = r2.createCell(15, Cell.CELL_TYPE_STRING);
               }
               c2.setCellValue("Fail");*/
               System.out.println("-----------------------------------------------------------------------------------------------------------");
               System.out.println("Fail");
               System.out.println("-----------------------------------------------------------------------------------------------------------");
               System.out.println();
            } else if (actboxvol < recboxvol + 0.001 && actboxvol > recboxvol - 0.001) {
               same++;
            } else if (actboxvol < recboxvol) {
               smaller++;
            } else {
               larger++;
            }
            if (numofitems == count) {
               tally++;
               totaltally++;
            } else {
               totaltally++;
            }
            index = counter; // ensures that the program will go to the next order
         }
         System.out.println("Number of Orders: " + numoforders);
         double percent = 100.0 * tally / totaltally; // calculates the percent of orders accommodated
         System.out.println("Percent of orders accommodated: " + percent + "%  (" + tally + "/" + totaltally + ")");
         System.out.println("Same: " + same + "    Smaller: " + smaller + "    Larger: " + larger);
         System.out.println();
         long totaltimestop = System.currentTimeMillis(); // stops the timer for the whole program
         double totalexecutiontime = (totaltimestop - totaltimestart) / 1000.0; // calculates the execution time for the whole program (the entire set of orders run)
         System.out.println("Total Execution Time: " + totalexecutiontime + " seconds");
         System.out.println();
         file.close();
         FileOutputStream out = new FileOutputStream(new File("10TestCases.xlsx"));
         workbook.write(out);
         out.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   // prints the current state of the box
	public static void printBoxArray(String matrix[][][], int[][] boxdata) {
      for (int z = 0; z < boxdata[0][2] + 2; z++) {
         for (int row = 0; row < boxdata[0][0] + 2; row++) {
   	      for (int column = 0; column < boxdata[0][1] + 2; column++) {
   	         System.out.print(matrix[row][column][z] + " ");
   	      }
   	      System.out.println();
   	   }
         System.out.println();
      }
      System.out.println();
	}
   
   // finds the corners of the space remaining in the box and returns them in a 2D array
   // each row contains a corner location and the directions for which there is empty space
   public static int[][] findCorners(String matrix[][][]) {
      int[][] cornerdata = new int[50][5];
      cornerdata[0][0] = 0; // counts number of corners found
      for (int row = 1; row < matrix.length - 1; row++) {
         for (int column = 1; column < matrix[row].length - 1; column++) {
            for (int z = 1; z < matrix[row][column].length - 1; z++) {
               if (matrix[row][column][z] == "00" && matrix[row][column][z - 1] != "00") {
                  if (matrix[row + 1][column + 1][z] != "00" && matrix[row + 1][column][z] != "00" && matrix[row][column + 1][z] != "00") {
                     cornerdata[0][0]++;
                     cornerdata[cornerdata[0][0]][0] = row;
                     cornerdata[cornerdata[0][0]][1] = column;
                     cornerdata[cornerdata[0][0]][2] = z;
                     cornerdata[cornerdata[0][0]][3] = 1; // 1 means north is one possible orientation
                     cornerdata[cornerdata[0][0]][4] = 4; // 1 means west is one possible orientation
                  } else if (matrix[row + 1][column - 1][z] != "00" && matrix[row + 1][column][z] != "00" && matrix[row][column - 1][z] != "00") {
                     cornerdata[0][0]++;
                     cornerdata[cornerdata[0][0]][0] = row;
                     cornerdata[cornerdata[0][0]][1] = column;
                     cornerdata[cornerdata[0][0]][2] = z;
                     cornerdata[cornerdata[0][0]][3] = 1; // 1 means north
                     cornerdata[cornerdata[0][0]][4] = 2; // 2 means east is one possible orientation
                  } else if (matrix[row - 1][column + 1][z] != "00" && matrix[row - 1][column][z] != "00" && matrix[row][column + 1][z] != "00") {
                     cornerdata[0][0]++;
                     cornerdata[cornerdata[0][0]][0] = row;
                     cornerdata[cornerdata[0][0]][1] = column;
                     cornerdata[cornerdata[0][0]][2] = z;
                     cornerdata[cornerdata[0][0]][3] = 3; // 3 means south is one possible orientation
                     cornerdata[cornerdata[0][0]][4] = 4; // 2 means east
                  } else if (matrix[row - 1][column - 1][z] != "00" && matrix[row - 1][column][z] != "00" && matrix[row][column - 1][z] != "00") {
                     cornerdata[0][0]++;
                     cornerdata[cornerdata[0][0]][0] = row;
                     cornerdata[cornerdata[0][0]][1] = column;
                     cornerdata[cornerdata[0][0]][2] = z;
                     cornerdata[cornerdata[0][0]][3] = 3; // 3 means south
                     cornerdata[cornerdata[0][0]][4] = 2; // 4 means west
                  }
               }
            }
         }
      }
      return cornerdata;
   }
   
   // creates all possible COPMs and returns them in a 2D array
   // a COPM is defined as [longer item dimension][shorter item dimension][item z dimension][orientation][row-location][column-location][z-location][face number][item number]
   public static int[][] createCOPMs(int[][] cornerdata, Item[] itemdata, int numofitems) {
      int[][] copmdata = new int[(numofitems * cornerdata[0][0] * 6)][9];
      int count = 0; // counts number of COPMs created
      for (int i = 1; i <= cornerdata[0][0]; i++) { // for every corner
         for (int j = 0; j < itemdata.length; j++) { // for every item
            for (int m = 0; m < 3; m++) { // for every item face
               if (m == 0) {
                  copmdata[count][0] = Math.max(itemdata[j].getXDimen(), itemdata[j].getYDimen());
                  copmdata[count][1] = Math.min(itemdata[j].getXDimen(), itemdata[j].getYDimen());
                  copmdata[count][2] = itemdata[j].getZDimen();
                  copmdata[count][7] = 0;
               } else if (m == 1) {
                  copmdata[count][0] = Math.max(itemdata[j].getXDimen1(), itemdata[j].getYDimen1());
                  copmdata[count][1] = Math.min(itemdata[j].getXDimen1(), itemdata[j].getYDimen1());
                  copmdata[count][2] = itemdata[j].getZDimen1();
                  copmdata[count][7] = 1;
               } else {
                  copmdata[count][0] = Math.max(itemdata[j].getXDimen2(), itemdata[j].getYDimen2());
                  copmdata[count][1] = Math.min(itemdata[j].getXDimen2(), itemdata[j].getYDimen2());
                  copmdata[count][2] = itemdata[j].getZDimen2();
                  copmdata[count][7] = 2;
               }
               copmdata[count][3] = 1; // sets orientation to vertial (long dimension vertical)
               copmdata[count][6] = cornerdata[i][2];
               copmdata[count][8] = j;
               if (cornerdata[i][3] == 1) {
                  copmdata[count][4] = cornerdata[i][0] - (copmdata[count][0] - 1);
                  if (cornerdata[i][4] == 2) {
                     copmdata[count][5] = cornerdata[i][1];
                  } else { // cornerdata[i][4] == 4
                     copmdata[count][5] = cornerdata[i][1] - (copmdata[count][1] - 1);
                  }
               } else { // cornerdata[i][3] == 3
                  copmdata[count][4] = cornerdata[i][0];
                  if (cornerdata[i][4] == 2) {
                     copmdata[count][5] = cornerdata[i][1];
                  } else { // cornerdata[i][4] == 4
                     copmdata[count][5] = cornerdata[i][1] - (copmdata[count][1] - 1);
                  }
               }
               count++;
            }
         }
         for (int k = 0; k < itemdata.length; k++) {
            for (int n = 0; n < 3; n++) {
               if (n == 0) {
                  copmdata[count][0] = Math.max(itemdata[k].getXDimen(), itemdata[k].getYDimen());
                  copmdata[count][1] = Math.min(itemdata[k].getXDimen(), itemdata[k].getYDimen());
                  copmdata[count][2] = itemdata[k].getZDimen();
                  copmdata[count][7] = 0;
               } else if (n == 1) {
                  copmdata[count][0] = Math.max(itemdata[k].getXDimen1(), itemdata[k].getYDimen1());
                  copmdata[count][1] = Math.min(itemdata[k].getXDimen1(), itemdata[k].getYDimen1());
                  copmdata[count][2] = itemdata[k].getZDimen1();
                  copmdata[count][7] = 1;
               } else {
                  copmdata[count][0] = Math.max(itemdata[k].getXDimen2(), itemdata[k].getYDimen2());
                  copmdata[count][1] = Math.min(itemdata[k].getXDimen2(), itemdata[k].getYDimen2());
                  copmdata[count][2] = itemdata[k].getZDimen2();
                  copmdata[count][7] = 2;
               }
               copmdata[count][3] = 0; // sets orientation to horizontal (long dimension horizontal)
               copmdata[count][6] = cornerdata[i][2];
               copmdata[count][8] = k;
               if (cornerdata[i][4] == 2) {
                  copmdata[count][5] = cornerdata[i][1];
                  if (cornerdata[i][3] == 1) {
                     copmdata[count][4] = cornerdata[i][0] - (copmdata[count][1] - 1);
                  } else { // cornerdata[i][3] == 3
                     copmdata[count][4] = cornerdata[i][0];
                  }
               } else { // cornerdata[i][4] == 4
                  copmdata[count][5] = cornerdata[i][1] - (copmdata[count][0] - 1);
                  if (cornerdata[i][3] == 1) {
                     copmdata[count][4] = cornerdata[i][0] - (copmdata[count][1] - 1);
                  } else { // cornerdata[i][3] == 3
                     copmdata[count][4] = cornerdata[i][0];
                  }
               }
               count++;
            }
         }
      }
      Arrays.sort(copmdata, new COPMComparator()); // sorts the list of COPMs to be in the desired order
      return copmdata;
   }   
   
   // evaluates all COPMs that are passed to it
   // this currently does nothing of value but the program breaks if it is removed
   public static int[][] evaluateCOPMs(int[][] copmdata, int[][] boxdata, String[][][] table, int numofitems, int[] arr) {
      int[][] evaluatedcopms = new int[copmdata.length][10];
      
      // create a pseudo-box
      String[][][] grid = new String[boxdata[0][0] + 2][boxdata[0][1] + 2][boxdata[0][2] + 2];
      int[] remainingitems = new int[numofitems];
      for (int i = 0; i < copmdata.length; i++) { // for each COPM
   		for (int c = 0; c < numofitems; c++) { // determine which items are eligible to be packed
            remainingitems[c] = arr[c];
         }
         
         // populate the pseudo-box with the current state of the box
         for(int j = 0; j < boxdata[0][0] + 2; j++) {
			   for(int k = 0; k < boxdata[0][1] + 2; k++) {
               for (int l = 0; l < boxdata[0][2] + 2; l++) {
                  grid[j][k][l] = table[j][k][l];
               }
			   }
		   }
         if (remainingitems[copmdata[i][8]] == 0) { // if the item described by the COPM is eligible to be packed
            
            // check if the COPM is a feasible placement move
            boolean checkspace = checkArea(grid, copmdata[i][0], copmdata[i][1], copmdata[i][2], copmdata[i][3], copmdata[i][4], copmdata[i][5], copmdata[i][6]);
            if (checkspace == true) {
               
               // place the item described by the COPM
               grid = placeItem(grid, copmdata[i][0], copmdata[i][1], copmdata[i][2], copmdata[i][3], copmdata[i][4], copmdata[i][5], copmdata[i][6]);
               remainingitems[copmdata[i][8]] = 1; // make this item unavailable for future packing
               
               // goes through the list of COPMs and greedily places any item that is eligible and will fit
               for (int b = 0; b < numofitems - 1; b++) {
                  int c = 1;
                  for (int a = c; a < copmdata.length; a++) {
                     if (remainingitems[copmdata[a][8]] == 1) {
                        continue;
                     }
                     checkspace = checkArea(grid, copmdata[a][0], copmdata[a][1],copmdata[a][2], copmdata[a][3], copmdata[a][4], copmdata[a][5], copmdata[a][6]);
                     if (checkspace == true) {
                        grid = placeItem(grid, copmdata[a][0], copmdata[a][1], copmdata[a][2], copmdata[a][3], copmdata[a][4], copmdata[a][5], copmdata[a][6]);
                        remainingitems[copmdata[a][8]] = 1;
                        c = a;
                        break;
                     }
                  }
               }
            }
            evaluatedcopms[i][9] = computeFV(grid, remainingitems); // computes the fitness value for the current COPM
         } else {
            evaluatedcopms[i][9] = -1;
         }
         evaluatedcopms[i][0] = copmdata[i][0]; // ldim
         evaluatedcopms[i][1] = copmdata[i][1]; // sdim
         evaluatedcopms[i][2] = copmdata[i][2]; // zdim
         evaluatedcopms[i][3] = copmdata[i][3]; // orient
         evaluatedcopms[i][4] = copmdata[i][4]; // row location
         evaluatedcopms[i][5] = copmdata[i][5]; // column location
         evaluatedcopms[i][6] = copmdata[i][6]; // z location
         evaluatedcopms[i][7] = copmdata[i][7]; // face number
         evaluatedcopms[i][8] = copmdata[i][8]; // item number
         // evaluatedcopms[i][9] = fitness value
      }
      return evaluatedcopms;
   }
   
   // checks the area that an object will go and determines if it will fit there or not
   public static boolean checkArea(String[][][] mat, int ldim, int sdim, int zdim, int orient, int x, int y, int z) {
      if (orient == 0) { // 0 means long side goes horizontal
         try {
            for (int i = x; i < x + sdim; i++) {
               for (int j = y; j < y + ldim; j++) {
                  for (int k = z; k < z + zdim; k++) {
                     if (mat[i][j][k] != "00") {
                        return false;
                     }
                  }
               }
            }
            for (int i = x; i < x + sdim; i++) {
               for (int j = y; j < y + ldim; j++) {
                  if (mat[i][j][z - 1] == "00") {
                     return false;
                  }
               }
            }
         } catch (ArrayIndexOutOfBoundsException e) {
            return false;
         }
      } else { // (orient == 1) 1 means long side goes vertical
         try {
            for (int i = x; i < x + ldim; i++) {
               for (int j = y; j < y + sdim; j++) {
                  for (int k = z; k < z + zdim; k++) {
                     if (mat[i][j][k] != "00") {
                        return false;
                     }
                  }
               }
            }
            for (int i = x; i < x + ldim; i++) {
               for (int j = y; j < y + sdim; j++) {
                  if (mat[i][j][z - 1] == "00") {
                     return false;
                  }
               }
            }
         } catch (ArrayIndexOutOfBoundsException e) {
            return false;
         }
      }
      return true; 
   }
   
   // pseudo-places an item
   public static String[][][] placeItem(String[][][] mat, int ldim, int sdim, int zdim, int orient, int x, int y, int z) {
      if (orient == 0) { // 0 means long side goes horizontal
         for (int i = x; i < x + sdim; i++) {
            for (int j = y; j < y + ldim; j++) {
               for (int k = z; k < z + zdim; k++) {
                  mat[i][j][k] = "11";
               }
            }
         }
      } else { // (orient == 1) 1 means long side goes vertical
         for (int i = x; i < x + ldim; i++) {
            for (int j = y; j < y + sdim; j++) {
               for (int k = z; k < z + zdim; k++) {
                  mat[i][j][k] = "11";
               }
            }
         }
      }
      return mat;
   }
   
   // computes the fitness value of a fully packed box given that box as an input
   public static int computeFV(String[][][] matrix, int[] useditems) {
      int count1 = 0;
      for (int i = 0; i < useditems.length; i++) {
         if (useditems[i] == 1) {
            count1++;
         }
      }
      int fitnessvalue = count1;
      return fitnessvalue;
   }
      
   // finds the best COPMs based on their fitness value
   public static int[][] findBestCOPMs(int[][] evaluatedcopms) {
      int max = -1; // initialize max
      int count = 0;
      int count1 = 0;
      for (int i = 0; i < evaluatedcopms.length; i++) {
         max = Math.max(max, evaluatedcopms[i][9]);
      }
      for (int i = 0; i < evaluatedcopms.length; i++) {
         if (evaluatedcopms[i][9] == max) {
            count++;
         }
      }
      int[][] bestcopms = new int[count][11];
      for (int i = 0; i < evaluatedcopms.length; i++) {
         if (evaluatedcopms[i][9] == max) {
            bestcopms[count1][0] = evaluatedcopms[i][0];
            bestcopms[count1][1] = evaluatedcopms[i][1];
            bestcopms[count1][2] = evaluatedcopms[i][2];
            bestcopms[count1][3] = evaluatedcopms[i][3];
            bestcopms[count1][4] = evaluatedcopms[i][4];
            bestcopms[count1][5] = evaluatedcopms[i][5];
            bestcopms[count1][6] = evaluatedcopms[i][6];
            bestcopms[count1][7] = evaluatedcopms[i][7];
            bestcopms[count1][8] = evaluatedcopms[i][8];
            bestcopms[count1][9] = evaluatedcopms[i][9];
            bestcopms[count1][10] = i;
            count1++;
         }
      }
      return bestcopms;
   }
   
   // selects which of the evaluated COPMs to actually use based on the fitness values
   // returns the row of evaluatedcopms that main should use to place the next item
   public static int selectCOPM(int[][] bestcopms, int[][] boxdata, String[][][] table, int numofitems, Item[] arr, int[] itemdata) {
      
      // create a pseudo-box
      String[][][] grid = new String[boxdata[0][0] + 2][boxdata[0][1] + 2][boxdata[0][2] + 2];
      int row = -1; // initialize row output
      int[] useditems = new int[numofitems];
      int[] numberofuseditems = new int[bestcopms.length];
      int[] zlocations = new int[bestcopms.length];
      boolean checkspace;
      for (int i = 0; i < bestcopms.length; i++) { // for each COPM
         for (int l = 0; l < useditems.length; l++) { // determine which items are eligible to be packed
            useditems[l] = itemdata[l];
         }
         if (useditems[bestcopms[i][8]] == 0) { // if the COPM contains an item that is eligible to be packed
            
            // populate the pseudo-box with the current state of the box
            for(int j = 0; j < boxdata[0][0] + 2; j++) {
   			   for(int k = 0; k < boxdata[0][1] + 2; k++) {
                  for (int l = 0; l < boxdata[0][2] + 2; l++) {
                     grid[j][k][l] = table[j][k][l];
                  }
   			   }
   		   }
            
            // check if the placement move described by the COPM is feasible
            checkspace = checkArea(grid, bestcopms[i][0], bestcopms[i][1], bestcopms[i][2], bestcopms[i][3], bestcopms[i][4], bestcopms[i][5], bestcopms[i][6]);
            if (checkspace == true && useditems[bestcopms[i][8]] == 0) { // if COPM is feasible and item is eligible to be placed
               
               // pseudo-place the item described by the COPM
               placeItem(grid, bestcopms[i][0], bestcopms[i][1], bestcopms[i][2], bestcopms[i][3], bestcopms[i][4], bestcopms[i][5], bestcopms[i][6]);
               useditems[bestcopms[i][8]] = 1;
               zlocations[i] = bestcopms[i][6];
            } else {
               numberofuseditems[i] = -2;
               zlocations[i] = 1000;
               continue;
            }
            int[][] cornerdata = findCorners(grid);
            if (cornerdata[0][0] == 0) {
               break;
            }
            int[][] copmdata = createCOPMs(cornerdata, arr, numofitems);
            int[][] evaluatedcopms1 = evaluateCOPMs(copmdata, boxdata, grid, numofitems, useditems);
            int[][] bestcopms1 = findBestCOPMs(evaluatedcopms1);
            numberofuseditems[i] = bestcopms1[0][9];
         } else {
            numberofuseditems[i] = -2;
            zlocations[i] = 1000;
            continue;
         }
      }
      int max = -1;
      for (int n = 0; n < numberofuseditems.length; n++) { // find the maximum number of items placed
         max = Math.max(max, numberofuseditems[n]);
      }
      int min = 1000;
      for (int n = 0; n < zlocations.length; n++) { // find the minimum z location for a COPM
         min = Math.min(min, zlocations[n]);
      }
      for (int f = 0; f < numberofuseditems.length; f++) { // select COPM with max number of items, min z location, and occupies multiple corners
         try {
            if (bestcopms[f][3] == 0) {
               if (numberofuseditems[f] == max && bestcopms[f][6] == min && ((grid[bestcopms[f][4] + bestcopms[f][1]][bestcopms[f][5]][bestcopms[f][6]] != "00" && grid[bestcopms[f][4] - 1][bestcopms[f][5]][bestcopms[f][6]] != "00") || (grid[bestcopms[f][4]][bestcopms[f][5] + bestcopms[f][0]][bestcopms[f][6]] != "00" && grid[bestcopms[f][4]][bestcopms[f][5] - 1][bestcopms[f][6]] != "00"))) {
                  row = f;
                  return row;
               }
            } else { // (bestcopms[f][3] == 1)
               if (numberofuseditems[f] == max && bestcopms[f][6] == min && ((grid[bestcopms[f][4] + bestcopms[f][0]][bestcopms[f][5]][bestcopms[f][6]] != "00" && grid[bestcopms[f][4] - 1][bestcopms[f][5]][bestcopms[f][6]] != "00") || (grid[bestcopms[f][4]][bestcopms[f][5] + bestcopms[f][1]][bestcopms[f][6]] != "00" && grid[bestcopms[f][4]][bestcopms[f][5] - 1][bestcopms[f][6]] != "00"))) {
                  row = f;
                  return row;
               }
            }
         } catch (ArrayIndexOutOfBoundsException e) {
            continue;
         }
      }
      for (int q = 0; q < numberofuseditems.length; q++) { // select COPM with max number of items and occupies multiple corners
         try {
            if (bestcopms[q][3] == 0) {
               if (numberofuseditems[q] == max && ((grid[bestcopms[q][4] + bestcopms[q][1]][bestcopms[q][5]][bestcopms[q][6]] != "00" && grid[bestcopms[q][4] - 1][bestcopms[q][5]][bestcopms[q][6]] != "00") || (grid[bestcopms[q][4]][bestcopms[q][5] + bestcopms[q][0]][bestcopms[q][6]] != "00" && grid[bestcopms[q][4]][bestcopms[q][5] - 1][bestcopms[q][6]] != "00"))) {
                  row = q;
                  return row;
               }
            } else { // (bestcopms[q][3] == 1)
               if (numberofuseditems[q] == max && ((grid[bestcopms[q][4] + bestcopms[q][0]][bestcopms[q][5]][bestcopms[q][6]] != "00" && grid[bestcopms[q][4] - 1][bestcopms[q][5]][bestcopms[q][6]] != "00") || (grid[bestcopms[q][4]][bestcopms[q][5] + bestcopms[q][1]][bestcopms[q][6]] != "00" && grid[bestcopms[q][4]][bestcopms[q][5] - 1][bestcopms[q][6]] != "00"))) {               
                  row = q;
                  return row;
               }
            }
         } catch (ArrayIndexOutOfBoundsException e) {
            continue;
         }
      }
      for (int w = 0; w < numberofuseditems.length; w++) { // select COPM with max items and min z location
         if (numberofuseditems[w] == max && bestcopms[w][6] == min) {
            row = w;
            return row;
         }
      }
      for (int p = 0; p < numberofuseditems.length; p++) { // select COPM with max items
         if (numberofuseditems[p] == max) {
            row = p;
            break;
         }
      }
      return row;
   }
   
   // translates the item locations from the back bottom left corner of the item to the center of the top of the item
   public static double[][] translateItemLocation(int[][] usedcopms, int numofitems, int useditems, double[][] itemdata) {
      double[][] outputlocations = new double[numofitems][3];
      double[][] outputorientation = new double[numofitems][3];
      System.out.println("-----------------------------------------------------------------------------------------------------------");
      System.out.println("For an origin at the back left bottom corner of a box:");
      int[] placementorder = new int[useditems];
      for (int i = 0; i < useditems; i++) {
         placementorder[i] = usedcopms[i][8] + 1;
      }
      int[][] validusedcopms = new int[useditems][usedcopms[0].length];
      for (int i = 0; i < useditems; i++) {
         for (int j = 0; j < usedcopms[i].length; j++) {
            validusedcopms[i][j] = usedcopms[i][j];
         }
      }
      Arrays.sort(validusedcopms, new FinalComparator());
      for (int i = 0; i < validusedcopms.length; i++) {
         if (validusedcopms[i][3] == 0) {
            outputlocations[i][0] = (validusedcopms[i][4] + (validusedcopms[i][1] / 2.0) - 1) * CUBIT_SIZE * -1; // outputs center of filled cubits
            outputlocations[i][1] = (validusedcopms[i][5] + (validusedcopms[i][0] / 2.0) - 1) * CUBIT_SIZE; // outputs center of filled cubits
            if (validusedcopms[i][7] == 0) {
               outputorientation[i][0] = Math.max(itemdata[i][0], itemdata[i][1]);
               outputorientation[i][1] = Math.min(itemdata[i][0], itemdata[i][1]);
               outputorientation[i][2] = itemdata[i][2];
               //outputlocations[i][0] = (validusedcopms[i][4] + (outputorientation[i][1] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE * -1; // outputs center of actual object from where it is placed
               //outputlocations[i][1] = (validusedcopms[i][5] + (outputorientation[i][0] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
               //outputlocations[i][2] = (validusedcopms[i][6] + (outputorientation[i][2] / CUBIT_SIZE) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
            } else if (validusedcopms[i][7] == 1) {
               outputorientation[i][0] = Math.max(itemdata[i][0], itemdata[i][2]);
               outputorientation[i][1] = Math.min(itemdata[i][0], itemdata[i][2]);
               outputorientation[i][2] = itemdata[i][1];
               //outputlocations[i][0] = (validusedcopms[i][4] + (outputorientation[i][1] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE * -1; // outputs center of actual object from where it is placed
               //outputlocations[i][1] = (validusedcopms[i][5] + (outputorientation[i][0] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
               //outputlocations[i][2] = (validusedcopms[i][6] + (outputorientation[i][2] / CUBIT_SIZE) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
            } else { // (validusedcopms[i][7] == 2)
               outputorientation[i][0] = Math.max(itemdata[i][1], itemdata[i][2]);
               outputorientation[i][1] = Math.min(itemdata[i][1], itemdata[i][2]);
               outputorientation[i][2] = itemdata[i][0];
               //outputlocations[i][0] = (validusedcopms[i][4] + (outputorientation[i][1] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE * -1; // outputs center of actual object from where it is placed
               //outputlocations[i][1] = (validusedcopms[i][5] + (outputorientation[i][0] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
               //outputlocations[i][2] = (validusedcopms[i][6] + (outputorientation[i][2] / CUBIT_SIZE) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
            }
         } else { // (validusedcopms[i][3] == 1)
            outputlocations[i][0] = (validusedcopms[i][4] + (validusedcopms[i][0] / 2.0) - 1) * CUBIT_SIZE * -1; // outputs center of filled cubits
            outputlocations[i][1] = (validusedcopms[i][5] + (validusedcopms[i][1] / 2.0) - 1) * CUBIT_SIZE; // outputs center of filled cubits
            if (validusedcopms[i][7] == 0) {
               outputorientation[i][0] = Math.min(itemdata[i][0], itemdata[i][1]);
               outputorientation[i][1] = Math.max(itemdata[i][0], itemdata[i][1]);
               outputorientation[i][2] = itemdata[i][2];
               //outputlocations[i][0] = (validusedcopms[i][4] + (outputorientation[i][1] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE * -1; // outputs center of actual object from where it is placed
               //outputlocations[i][1] = (validusedcopms[i][5] + (outputorientation[i][0] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
               //outputlocations[i][2] = (validusedcopms[i][6] + (outputorientation[i][2] / CUBIT_SIZE) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
            } else if (validusedcopms[i][7] == 1) {
               outputorientation[i][0] = Math.min(itemdata[i][0], itemdata[i][2]);
               outputorientation[i][1] = Math.max(itemdata[i][0], itemdata[i][2]);
               outputorientation[i][2] = itemdata[i][1];
               //outputlocations[i][0] = (validusedcopms[i][4] + (outputorientation[i][1] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE * -1; // outputs center of actual object from where it is placed
               //outputlocations[i][1] = (validusedcopms[i][5] + (outputorientation[i][0] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
               //outputlocations[i][2] = (validusedcopms[i][6] + (outputorientation[i][2] / CUBIT_SIZE) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
            } else { // (validcopms[i][7] == 2)
               outputorientation[i][0] = Math.min(itemdata[i][1], itemdata[i][2]);
               outputorientation[i][1] = Math.max(itemdata[i][1], itemdata[i][2]);
               outputorientation[i][2] = itemdata[i][0];
               //outputlocations[i][0] = (validusedcopms[i][4] + (outputorientation[i][1] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE * -1; // outputs center of actual object from where it is placed
               //outputlocations[i][1] = (validusedcopms[i][5] + (outputorientation[i][0] / CUBIT_SIZE / 2.0) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
               //outputlocations[i][2] = (validusedcopms[i][6] + (outputorientation[i][2] / CUBIT_SIZE) - 1) * CUBIT_SIZE; // outputs center of actual object from where it is placed
            }
         }
         
         outputlocations[i][2] = (validusedcopms[i][6] + validusedcopms[i][2] - 1) * CUBIT_SIZE; // outputs center of filled cubits
         System.out.printf("%-17s", "Item " + (validusedcopms[i][8] + 1) + " Location: ");
         System.out.printf("%-12s", "x = " + (Math.round(100000.0 * outputlocations[i][1]) / 100000.0) + ", ");
         System.out.printf("%-12s", "y = " + (Math.round(100000.0 * outputlocations[i][0]) / 100000.0) + ", ");
         System.out.printf("%-20s", "z = " + (Math.round(100000.0 * outputlocations[i][2]) / 100000.0));
         System.out.printf("%-23s", "Orientation: " + "x = " + outputorientation[i][0] + ", ");
         System.out.printf("%-10s", "y = " + outputorientation[i][1] + ", ");
         System.out.println("z = " + outputorientation[i][2]);
      }
      if (useditems > 0) {
         System.out.println();
         System.out.print("Placement Order: ");
         for (int i = 0; i < useditems - 1; i++) {
            System.out.print("Item " + placementorder[i] + ", ");
         }
         System.out.println("Item " + placementorder[useditems - 1]);
      }
      System.out.println();
      System.out.println("Number of items in order: " + numofitems);
      System.out.println("Number of items packed: " + useditems);
      return outputlocations;
   }
   
   // computes the volume utilization of the box used based on filled cubit spaces
   public static double computeVolumeUtilization(String[][][] grid, int[][] boxdata) {
      int count = 0;
      int totalcount = 0;
      for (int z = 1; z < boxdata[0][2] + 1; z++) {
         for (int row = 1; row < boxdata[0][0] + 1; row++) {
   	      for (int column = 1; column < boxdata[0][1] + 1; column++) {
   	         totalcount++;
               if (grid[row][column][z] != "00") {
                  count++;
               }
            }
         }
      }
      double utilization = Math.round((100.0 * count / totalcount) * 1000.0) / 1000.0;
      System.out.println("Volume Utilization: " + utilization + "%");
      return utilization;
   }
   
   // takes the box list and updates it to include the recommended box if it is not already included in the box suite
   public static double[][] createUpdatedBoxList(double[][] boxlist, double box1, double box2, double box3) {
      double boxvol = box1 * box2 * box3;
      for (int i = 0; i < boxlist.length; i++) {
         if (boxvol < boxlist[i][3] + 0.00001 && boxvol > boxlist[i][3] - 0.00001) {
            return boxlist;
         }
      }
      double[][] updatedboxlist = new double[22][4];
      for (int i = 0; i < boxlist.length; i++) {
         updatedboxlist[i][0] = boxlist[i][0];
         updatedboxlist[i][1] = boxlist[i][1];
         updatedboxlist[i][2] = boxlist[i][2];
         updatedboxlist[i][3] = boxlist[i][3];
      }
      updatedboxlist[21][0] = box1;
      updatedboxlist[21][1] = box2;
      updatedboxlist[21][2] = box3;
      updatedboxlist[21][3] = boxvol;
      Arrays.sort(updatedboxlist, new BoxListComparator());
      return updatedboxlist;
   }

}