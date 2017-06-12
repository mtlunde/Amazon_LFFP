// Developed for Amazon by Matt Lunde
//
// This is an algorithm that packs a box using the Less Flexibility First Principle.
// It takes manual inputs for item and box dimensions and only runs for the current box input.
//
// Questions can be directed to mtl15@comcast.net

import java.util.*;

public class LFFPFinalManual { // all inputs must be in the same units
   
   public static final double CUBIT_SIZE = 0.25; // input cubit size
   public static final double E = 0; // input buffer size -- this increases the item dimesions by the set amount to accomodate robot placement accuracy issues
   
   public static void main(String[] args) {
      
      long timerstart = System.currentTimeMillis();
      
      int numofitems = 5; // input number of items in order; this numer must match the number of uncommented item inputs
      double[][] itemdata = new double[numofitems][3];
      
      itemdata[0][0] = 2; itemdata[0][1] = 2; itemdata[0][2] = 10; // input item 1 dimensions
      itemdata[1][0] = 2; itemdata[1][1] = 2; itemdata[1][2] = 8; // input item 2 dimensions
      itemdata[2][0] = 2; itemdata[2][1] = 2; itemdata[2][2] = 6; // input item 3 dimensions
      itemdata[3][0] = 2; itemdata[3][1] = 2; itemdata[3][2] = 4; // input item 4 dimensions
      itemdata[4][0] = 2; itemdata[4][1] = 2; itemdata[4][2] = 2; // input item 5 dimensions
      //itemdata[5][0] = 7.2; itemdata[5][1] = 3.0; itemdata[5][2] = 0.3; // input item 6 dimensions
      //itemdata[6][0] = 4.7; itemdata[6][1] = 1.9; itemdata[6][2] = 0.8; // input item 7 dimensions
      
      Item[] arr = new Item[numofitems];
      for (int i = 0; i < numofitems; i++) {
         arr[i] = new Item(Integer.toString((i + 1) * 11), (int)Math.ceil((itemdata[i][0] + E) / CUBIT_SIZE), (int)Math.ceil((itemdata[i][1] + E) / CUBIT_SIZE), (int)Math.ceil((itemdata[i][2] + E) / CUBIT_SIZE));
      }
      int[] useditems = new int [arr.length];
      int[][] usedcopms = new int[numofitems][10];
      int count = 0;
      
      double[][] rawboxdata = new double[1][3];
      rawboxdata[0][0] = 10; rawboxdata[0][1] = 7; rawboxdata[0][2] = 1; // input box dimensions
      Grid3D x = new Grid3D(rawboxdata[0][1], rawboxdata[0][0], rawboxdata[0][2], CUBIT_SIZE);
      int[][] boxdata = new int[1][3];
      boxdata[0][0] = (int)(rawboxdata[0][1] / CUBIT_SIZE); boxdata[0][1] = (int)(rawboxdata[0][0] / CUBIT_SIZE); boxdata[0][2] = (int)(rawboxdata[0][2] / CUBIT_SIZE);
      
      for (int z = 0; z < numofitems; z++) {
         for (int i = 0; i < useditems.length; i++) {
            useditems[i] = arr[i].getAvailable();
         }
         int[][] cornerdata = findCorners(x.getTable());
         if (cornerdata[0][0] == 0) {
            break;
         }
         int[][] copmdata = createCOPMs(cornerdata, arr, numofitems);
         int[][] evaluatedcopms = evaluateCOPMs(copmdata, boxdata, x.getTable(), numofitems, useditems);
         int selection = selectCOPM1(evaluatedcopms, boxdata, x.getTable(), numofitems, arr, useditems);
         
         try {
            boolean empty = checkArea(x.getTable(), evaluatedcopms[selection][0], evaluatedcopms[selection][1], evaluatedcopms[selection][2], evaluatedcopms[selection][3], evaluatedcopms[selection][4], evaluatedcopms[selection][5], evaluatedcopms[selection][6]);
            if (empty == true) {
               x.placeItem(x.getTable(), arr[evaluatedcopms[selection][8]].getItemNumber(), evaluatedcopms[selection][0], evaluatedcopms[selection][1], evaluatedcopms[selection][2], evaluatedcopms[selection][3], evaluatedcopms[selection][4], evaluatedcopms[selection][5], evaluatedcopms[selection][6]);
               for (int i = 0; i < 10; i++) {
                  usedcopms[count][i] = evaluatedcopms[selection][i];
               }
               count++;
               arr[evaluatedcopms[selection][8]].changeAvailable();
               //printBoxArray(x.getTable(), boxdata); // if uncommented, this will print the state of the box after every time an item is placed
            }
         } catch (ArrayIndexOutOfBoundsException e) {
            continue;
         }
      }
      printBoxArray(x.getTable(), boxdata); // if uncommented, this will print the final state of the box
      double[][] outputlocations = translateItemLocation(usedcopms, numofitems, count, itemdata);
      double utilization = computeVolumeUtilization(x.getTable(), boxdata);
      long timerstop = System.currentTimeMillis();
      double executiontime = (timerstop - timerstart) / 1000.0;
      System.out.println("Execution Time: " + executiontime + " seconds");
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
   
   // finds the corners of the space remaining in the box and returns them in an int[][]
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
   
   // creates all possible COPMs and returns them in an int[][]
   // a COPM is defined as [longer item dimension][shorter item dimension][item z dimension][orientation][row-location][column-location][z-location][face number][item number]
   public static int[][] createCOPMs(int[][] cornerdata, Item[] itemdata, int numofitems) {
      int[][] copmdata = new int[(numofitems * cornerdata[0][0] * 6)][9];
      int count = 0; // counts number of COPMs created
      for (int i = 1; i <= cornerdata[0][0]; i++) {
         for (int j = 0; j < itemdata.length; j++) {
            for (int m = 0; m < 3; m++) {
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
      Arrays.sort(copmdata, new COPMComparator());
      return copmdata;
   }   
   
   // evaluates all COPMs that are passed to it
   public static int[][] evaluateCOPMs(int[][] copmdata, int[][] boxdata, String[][][] table, int numofitems, int[] arr) {
      int[][] evaluatedcopms = new int[copmdata.length][10];
      String[][][] grid = new String[boxdata[0][0] + 2][boxdata[0][1] + 2][boxdata[0][2] + 2];
      int[] remainingitems = new int[numofitems];
      for (int i = 0; i < copmdata.length; i++) {
   		for (int c = 0; c < numofitems; c++) {
            remainingitems[c] = arr[c];
         }
         for(int j = 0; j < boxdata[0][0] + 2; j++) {
			   for(int k = 0; k < boxdata[0][1] + 2; k++) {
               for (int l = 0; l < boxdata[0][2] + 2; l++) {
                  grid[j][k][l] = table[j][k][l];
               }
			   }
		   }
         if (remainingitems[copmdata[i][8]] == 0) {
            boolean checkspace = checkArea(grid, copmdata[i][0], copmdata[i][1], copmdata[i][2], copmdata[i][3], copmdata[i][4], copmdata[i][5], copmdata[i][6]);
            if (checkspace == true) {
               grid = placeItem(grid, copmdata[i][0], copmdata[i][1], copmdata[i][2], copmdata[i][3], copmdata[i][4], copmdata[i][5], copmdata[i][6]);
               remainingitems[copmdata[i][8]] = 1;
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
            evaluatedcopms[i][9] = computeFV(grid, remainingitems);
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
   public static int selectCOPM1(int[][] bestcopms, int[][] boxdata, String[][][] table, int numofitems, Item[] arr, int[] itemdata) {
      String[][][] grid = new String[boxdata[0][0] + 2][boxdata[0][1] + 2][boxdata[0][2] + 2];
      int row = -1;
      int[] useditems = new int[numofitems];
      int[] numberofuseditems = new int[bestcopms.length];
      int[] zlocations = new int[bestcopms.length];
      boolean checkspace;
      for (int i = 0; i < bestcopms.length; i++) {
         for (int l = 0; l < useditems.length; l++) {
            useditems[l] = itemdata[l];
         }
         if (useditems[bestcopms[i][8]] == 0) {
            for(int j = 0; j < boxdata[0][0] + 2; j++) {
   			   for(int k = 0; k < boxdata[0][1] + 2; k++) {
                  for (int l = 0; l < boxdata[0][2] + 2; l++) {
                     grid[j][k][l] = table[j][k][l];
                  }
   			   }
   		   }
            checkspace = checkArea(grid, bestcopms[i][0], bestcopms[i][1], bestcopms[i][2], bestcopms[i][3], bestcopms[i][4], bestcopms[i][5], bestcopms[i][6]);
            if (checkspace == true && useditems[bestcopms[i][8]] == 0) {
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
   
   public static double[][] translateItemLocation(int[][] usedcopms, int numofitems, int useditems, double[][] itemdata) {
      double[][] outputlocations = new double[numofitems][3];
      double[][] outputorientation = new double[numofitems][3];
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
            } else { // (validcopms[i][7] == 2)
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
      System.out.println();
      return utilization;
   }

}