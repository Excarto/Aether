import static java.lang.Math.*;
import java.util.*;

/**
 * This work is non-copyrightable
 * @author Myriam Abramson
 * myriam.abramson@nrl.navy.mil
 */

public class HungarianAlg{
    
	public final double[][] matrix;
    private final int[] rowCover;
    private final int[] colCover;
    private final int[][] stars;
    private final int[][] path;
    private int size;
    
    public HungarianAlg(int maxSize){
		matrix = new double[maxSize][maxSize];
		stars = new int[maxSize][maxSize];
		rowCover = new int[maxSize];
		colCover = new int[maxSize];
		path = new int[maxSize*maxSize][2];
    }
    
    // Converts x,y to one dimension
    private int two2one(int x, int y){
    	return x*size + y;
    }
    private int one2col(int n){
    	return n%size;
    }
    private int one2row(int n){
    	return n/size;
    }

    // Find the minimum in each row and subtract it
    private void subRowMin(){
		for (int i=0; i<size; i++){
		    double minVal = matrix[i][0];
		    for (int j=1; j<size; j++){
				if (minVal > matrix[i][j])
				    minVal = matrix[i][j];
		    }
		    for (int j=0; j<size; j++)
		    	matrix[i][j] -= minVal;
		}
    }
    
    // Find the minimum in each column and subtract it
    private void subColMin(){
		for (int j=0; j<size; j++){
		    double minVal = matrix[0][j];
		    for (int i=1; i<size; i++){
				if (minVal > matrix[i][j]) 
				    minVal = matrix[i][j];
		    }
		    for (int i=0; i<size; i++)
		    	matrix[i][j] -= minVal;
		}
    }
	
    // Star the zeros
    private void starZeros(){
		for (int i=0; i<size; i++){
		    for (int j=0; j<size; j++){
				if (matrix[i][j] == 0 && colCover[j] == 0 && rowCover[i] == 0){
				    stars[i][j] = 1;
				    colCover[j] = 1;
				    rowCover[i] = 1;
				}
		    }
		}
		clearCovers();
    }
    
    // Check for solutions
    private int coveredColumns(){
		int k = 0;
		for (int i=0; i<size; i++){
		    for (int j=0; j<size; j++){
				if (stars[i][j] == 1)
				    colCover[j] = 1;
		    }
		}
		for (int j=0; j<size; j++)
		    k += colCover[j];
		return k;
    }
    
     // Returns a zero whose row or column is not covered or -1 if no uncovered zero is found
    private int findUncoveredZero(){
		for (int i=0; i<size; i++){
		    for (int j=0; j<size; j++){
				if (matrix[i][j] == 0 && rowCover[i] == 0 && colCover[j] == 0) 
				    return two2one(i,j);
		    }
		}
		return -1;
    }
    
    // Cover all the uncovered zeros one by one until no more are left
    // Cover the row and uncover the column
    private boolean coverZeros(){
		int zero = findUncoveredZero();
		while (zero >= 0){
		    int zeroCol = one2col(zero);
		    int zeroRow = one2row(zero);
		    stars[zeroRow][zeroCol] = 2; //prime it
		    int starCol = findStarInRow(zeroRow);
	
		    if (starCol >= 0){
				rowCover[zeroRow] = 1;
				colCover[starCol] = 0;
		    }
		    else{
				starZeroInRow(zero);
				return false;
		    }
		    zero = findUncoveredZero();
		}
		return true;
    }
    
    // Returns the column if found or -1 if not found
    private int findStarInRow(int zeroY){
		for (int j=0; j<size; j++){
		    if (stars[zeroY][j] == 1)
		    	return j;
		}
		return -1;
    }
    
    private int findStarInCol(int col){
		for (int i=0; i<size; i++){
			if (stars[i][col] == 1)
		    	return i;
		}
		return -1;
	}
    
	private void clearCovers(){
		for (int i=0; i<size; i++){
			rowCover[i] = 0;
			colCover[i] = 0;
		}
    }
    
    // Unstar stars and star primes
    private void erasePrimes(){
		for (int i=0; i<size; i++){
		    for (int j=0; j<size; j++){
				if (stars[i][j] == 2)
				    stars[i][j] = 0;
		    }
		}
    }
    
    private void convertPath(int[][] path, int count){
		for (int i=0; i<=count; i++){
		    int x = path[i][0];
		    int y = path[i][1];
		    if (stars[x][y] == 1){
		    	stars[x][y] = 0;
		    }else{
				if (stars[x][y] == 2)
				    stars[x][y] = 1;
		    }
		}
    }
    
    // Returns the column where a prime was found for a given row
    private int findPrimeInRow(int row){
		for (int j=0; j<size; j++){
		    if (stars[row][j] == 2)
		    	return j;
		}
		return -1;
    }
    
    // Augmenting path algorithm 
    private void starZeroInRow(int zero){
		boolean done = false;
		int zeroRow = one2row(zero);
		int zeroCol = one2col(zero);
		
		int count = 0;
		path[count][0] = zeroRow;
		path[count][1] = zeroCol;
		while (!done) {
		    int r = findStarInCol(path[count][1]);
		    if (r >= 0){
				count++;
				path[count][0] = r;
				path[count][1] = path[count-1][1];
		    }else{
				done = true;
				break;
		    }
		    int c = findPrimeInRow(path[count][0]);
		    count++;
		    path[count][0] = path[count-1][0];
		    path[count][1] = c;
		}
		convertPath(path, count);
		clearCovers();
		erasePrimes();
    }

    public int[] solve(int size){
    	this.size = size;
    	
    	Arrays.fill(rowCover, 0);
    	Arrays.fill(colCover, 0);
    	for (int i = 0; i < size; i++)
    		Arrays.fill(stars[i], 0);
    	
    	subRowMin(); //step 1
		subColMin();
		starZeros(); //step 2 
		boolean done = false;
		while (!done) {
		    int covCols = coveredColumns();//step 3
		    if (covCols >= size)
		    	break;
		    
		    done = coverZeros(); //step 4 (calls step 5)
		    while (done){
				double smallest = findSmallestUncoveredVal();
				uncoverSmallest(smallest); //step 6
				done = coverZeros();
		    }
		}
		
		int [] solutions = new int[size];
		for (int j = 0; j < size; j++){
		    solutions[j] = -1;
		    for (int i = 0; i < size; i++){
				if (stars[i][j] == 1 && (freeRow(i,j) || freeCol(i,j)))
				    solutions[j] = i;
			}
		}
		return solutions;
    }
    
    private boolean freeRow(int row, int col){
		for (int i=0;i<size;i++) 
		    if (i != row && stars[i][col] == 1)
		    	return false;
		return true;
    }

    private boolean freeCol(int row, int col){
		for (int j=0;j<size;j++) 
		    if (j != col && stars[row][j] == 1)
		    	return false;
		return true;
    }
    
    private double findSmallestUncoveredVal(){
		double minVal = Double.MAX_VALUE;
		for (int i = 0; i < size; i++){
		    for (int j = 0; j < size; j++){
				if (rowCover[i] == 0 && colCover[j] == 0)
					minVal = min(minVal, matrix[i][j]);
		    }
		}
		return minVal;
    }
    
    // If the row is covered, add the smallest value
    // If the column is not covered, subtract the smallest value
    private void uncoverSmallest(double smallest){
		for (int i = 0; i < size; i++){
		    for (int j = 0; j < size; j++){
				if (rowCover[i] == 1) 
				    matrix[i][j] += smallest;
				if (colCover[j] == 0)
				    matrix[i][j] -= smallest;
		    }
		}
    }
    
}
