import java.util.*;
import java.io.*;

/** 
 * TicTacToe - Play Tic Tac Toe game.
 * <p>
 * This is an original design of playing Tic Tac Toe game with the computer.  The computer has the
 * capability of remembering its loses and never repeat the same lose again.
 * <p>
 * There are many Tic Tac Toe program codes on the internet.  But this is not a copy of any of those.  It
 * is the result of about 10 hours work.
 * <p>
 * It uses a file "loserecords.txt" (can be changed in the main function) to store the lose records.  By
 * loading the lose records at the start of the game, it then calls getLosePatterns() to analyze the lose
 * records and get all the lose patterns.  The lose patterns are used to direct the computer not to repeat
 * the same lose.
 * <p>
 * To simplify the registration of the game moves, it uses one digit to record a move:
 * 1,1==>1, 1,2==>2, 1,3==>3, ..., x,y==>(x-1)*3+y
 * So that all the moves of one game at any point is a string with length not longer than 9.  
 * When a move is expressed in two digits like (2,1), it stands 2nd row and first column.
 * <p>
 * Each time when the computer makes a move, it uses a random# to pick from the available moves and 
 * avoid to repeat the same lose from the lose patterns.
 * <p>
 * The program also has a function personMoveSimulated() to simulate the person move by picking a random
 * move only.  When this function is used inside nextMove() instead of personMove(), it can simulate a
 * person playing randomly with the computer.  By calling th program repeatedly with this simulation, I
 * can generate a lose records file "loserecords.txt" with many loses.  And I can use the lose records to
 * decrease the lose of the computer.
 */
public class TicTacToe {
  public static final int MAX_MOVE_NUMBER = 9;

  private String loseRecordsFileName;
  private String[] loseRecords;
  private String[] losePatterns;
  private String moveRecord;
  private boolean someoneWin;
  private String[] ticTacToeEmpty = 
                     {"   |   |   ",
                      "---|---|---",
                      "   |   |   ",
                      "---|---|---",
                      "   |   |   "};
  private String[] ticTacToeShape; 


  public static void main (String[] args) {
    //System.out.println("Hello World");

    String loseRecordsFile = "loserecords.txt";
    TicTacToe ttt = new TicTacToe(loseRecordsFile);
    ttt.play();
  }

/**
 * Init of the game.  
 * <p>
 * It calls init() for the following:<br>
 * loadLoseRecords(), Load the lose records from a text file.<br>
 * getLosePatterns(), Analyze the lose records, and get all the lose patterns for the computer to use.
 */
  public TicTacToe(String loseRecordsFile) {
    loseRecordsFileName = loseRecordsFile;
    loseRecords = new String[0];
    losePatterns = new String[0];
    if (loseRecordsFileName == null || loseRecordsFileName.trim().equals("")) {
      loseRecordsFileName = "loserecords.txt";
    }
    moveRecord = "";
    someoneWin = false;
    init();
  }

  private void init() {
    resetTicTacToeShape();
    loadLoseRecords();
    getLosePatterns();
  }

  private void resetTicTacToeShape() {
    ticTacToeShape = new String[ticTacToeEmpty.length];
    for (int ii=0; ii<ticTacToeEmpty.length;ii++) {
      ticTacToeShape[ii] = ticTacToeEmpty[ii];
    }
  }

  /** 
   * Play one game with the computer.
   * <p>
   * The game moves are recorded in String moveRecord.  It is analyzed by isGameEnded() after each move
   * to decide if one side wins.  The game also ends when 9 moves are played.
   * <p>
   * It calls nextMove() to do each move.  This function then calls computerMove() or personMove() to 
   * do the move by computer or by person.
   */
  public void play() {
    int ii = 0;

    System.out.println("****************************************************************");
    System.out.println("Play game: a move is expressed as \"row#,col#\", such as \"1,2\"");
    System.out.println("Computer: X   Person: O");
    System.out.println("****************************************************************");
    while(!isGameEnded()) {
      if (ii >= MAX_MOVE_NUMBER) {
	break;
      }

      nextMove();
      ii++;
    }

    if (isPersonWin()) {
      System.out.println("Person Wins!");
      saveLoseRecord();
    }
    else if (isComputerWin()) {
      System.out.println("Computer Wins!");
    }
    else {
      System.out.println("Tie!");
    }

  }

  private void loadLoseRecords() {	  
    String myline, myMove;
    int moveX, moveY;
    String oneMoveRecord;
    int recordCount = 0;
    int idx = 0;

    loseRecords = new String[0];

    try {
      BufferedReader br = new BufferedReader(new FileReader(loseRecordsFileName));
      recordCount = 0;
      while((myline = br.readLine()) != null) {
        recordCount++;
      }
      br.close();
      loseRecords = new String[recordCount];

      br = new BufferedReader(new FileReader(loseRecordsFileName));
      idx = 0;
      while((myline = br.readLine()) != null) {
	oneMoveRecord = "";
	while(!(myline.trim().equals(""))) {
	  int bra, ket;
	  bra = myline.indexOf("(");
	  ket = myline.indexOf(")");
	  myMove = myline.substring(bra+1, ket);
	  myline = myline.substring(ket+1);

	  moveX=Integer.parseInt(myMove.substring(0, myMove.indexOf(",")).trim());
          moveY=Integer.parseInt(myMove.substring(myMove.indexOf(",") + 1).trim());

	  oneMoveRecord += String.valueOf((moveX-1)*3+moveY);
	}
	loseRecords[idx] = oneMoveRecord;
	idx++;
      }
      br.close();
    } catch (java.io.FileNotFoundException e) {
      //do nothing if the lose records file not existing.  It might be the first time to run it.
    } catch (Exception e) {
      e.printStackTrace();
    }

//    System.out.println("Lose Records retrieved:");
//    for (int ii=0; ii<loseRecords.length; ii++) {
//      System.out.println(loseRecords[ii]);
//    }
  }

  /** 
   * With a lose record, remove the last person move, we get a LossPattern.
   * If the computer repeats the pattern, the person can win and duplicate the same record.
   * The computer should not take the action following the same LossPattern.
   * So with 8-step loss record ==> 7-step loss pattern, etc.
   *
   * If we have gathered 3 different 7-step loss patterns with the same first 6 steps, this
   * means that when the person takes the 6th step, the computer will definitely lose again
   * since the computer only has 3 choices at 7th step (only 3 spaces left).  Therefore, we can
   * treat the same 6-step as a lose record.  By dropping the last person move, we get another
   * 5-step lose pattern.  We add to lose pattern array if it does not exist.
   */
  private void getLosePatterns() {
    int losePatternsCount = 0;
    losePatterns = new String[loseRecords.length*2];
    for (int ii=0; ii<loseRecords.length;ii++) {
      losePatterns[ii] = loseRecords[ii].substring(0, loseRecords[ii].length() - 1);
    }
    losePatternsCount = loseRecords.length;

//    System.out.println("Lose Patterns retrieved:");
//    for (int ii=0; ii<losePatternsCount; ii++) {
//      System.out.println(losePatterns[ii]);
//    }

    //check patterns with fixed length for new pattern.
    //From losePatterns[] with length 7, we get the counts of all instances having the same first 6
    //char (moves).  If we have 3 of those, then the person can always repeat the win if the computer
    //take the moves up to move #5 with the same moves.  Therefore, the first 5 moves constitute a
    //lose pattern--which can lead to the same lose.  So we do:
    //losePatterns[] with length 7==>more losePatterns[] with length 5
    //losePatterns[] with length 5==>more losePatterns[] with length 3
    for (int ii=7; ii>=3; ii-=2) { 
      //Count lose patterns with a specific length to construct the array to be used
      int countSpecificLengthPattern = 0;
      for (int jj=0; jj<losePatternsCount; jj++) {
        if ( (losePatterns[jj] != null) &&
	     (losePatterns[jj].length() == ii) ) {
	  countSpecificLengthPattern++;
	}
      }
      if (countSpecificLengthPattern < 3) {
	continue;
      }	

      //newPattern is used to count how many lose patterns we have for a certain lose pattern
      //with the same moves up to the last person move
      //For example: for a lose pattern with 7-step, we count how many patterns we have for the 
      //same 6-steps, storing the 6-step pattern in newPattern[] and the count in newPatternCount[]
      String[] newPattern = new String[countSpecificLengthPattern];
      int[] newPatternCount = new int[countSpecificLengthPattern];
      countSpecificLengthPattern = 0;
      for (int jj=0; jj<losePatternsCount; jj++) {
        if ( (losePatterns[jj] != null) &&
	     (losePatterns[jj].length() == ii) ) {
	  String tmpPattern = losePatterns[jj].substring(0, ii-1);
	  boolean repeated = false;
	  for (int kk=0; kk < countSpecificLengthPattern; kk++) {
	    if (newPattern[kk].equals(tmpPattern)) {
	      newPatternCount[kk] = newPatternCount[kk] + 1;
	      repeated = true;
	      break;
	    }
	  }
	  if (!repeated) {
	    newPattern[countSpecificLengthPattern] = losePatterns[jj].substring(0, ii-1);
            newPatternCount[countSpecificLengthPattern] = 1;
	    countSpecificLengthPattern++;
	  }
	}
      }
      
      //adding new lose patterns
      //For a specific newPattern[], if newPatternCount[] equal to # of possibilities, we get a
      //new lose pattern.
      //For example: for a 7-step losePatterns[], we have a 6-step newPattern[], if the newPatternCount[]
      //for this newPattern[] is 3 (=9-7+1), we get a new losePatterns[] using the first 5 digits of
      //the newPattern[]
      for (int jj=0; jj<countSpecificLengthPattern; jj++) {
	//System.out.println("Pattern: " + newPattern[jj] + ", Count=" + newPatternCount[jj]);
	if (newPatternCount[jj] == (9-ii+1) ) {
	  //got a new lose pattern
          losePatterns[losePatternsCount] = newPattern[jj].substring(0,ii-2);
          //System.out.println("Add new lose pattern:" + losePatterns[losePatternsCount]);
          losePatternsCount++;
	}
      }
    }
  }

  private void saveLoseRecord() {
    int[] moveX = new int[9];
    int[] moveY = new int[9];
    String saveLine = "";
    String saveLineCombined = "";
    int tempNum = 0;

    //System.out.println("\n\n=========================\nSave lose record");
    //Get move coordinates from moveRecord
    for (int ii = 0; ii < moveRecord.length(); ii++) {
      int move = Integer.parseInt(moveRecord.substring(ii, ii+1));
      moveX[ii] = (move - ((move - 1) % 3)) / 3 + 1;
      moveY[ii] = (move - 1) % 3 + 1;
      //System.out.print("(" + moveX[ii] + "," + moveY[ii] + ") ");
    }

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(loseRecordsFileName, true));  //append
      for (int jj=0; jj < 4; jj++) {
	if (jj!=0) { //rotate 90 degree, with origin at center: (x,y)==>(-y,x), rotate 90 degree a time for 3 times
	  for (int ii = 0; ii < moveRecord.length(); ii++) {
	    tempNum = moveX[ii];
            moveX[ii] = -(moveY[ii]-2) + 2; //To get coordinate by center, need to "-2".  After rotate, shift back by "+2".
            moveY[ii] = (tempNum-2) + 2;
            //System.out.print("(" + moveX[ii] + "," + moveY[ii] + ") ");
          }
	}
//	else if (jj==2) { //180 degree, with origin at center: (x,y)==>(-x,-y)
//	  for (int ii = 0; ii < moveRecord.length(); ii++) {
//            moveX[ii] = -(moveX[ii]-2) + 2; //To get coordinate by center, need to "-2".  After rotate, shift back by "+2".
//            moveY[ii] = -(moveY[ii]-2) + 2;
//            System.out.print("(" + moveX[ii] + "," + moveY[ii] + ") ");
//          }
//	}
//	else if (jj==3) { //270 degree, with origin at center: (x,y)==>(y,-x)
//	  for (int ii = 0; ii < moveRecord.length(); ii++) {
//	    tempNum = moveX[ii];
//            moveX[ii] = (moveY[ii]-2) + 2; //To get coordinate by center, need to "-2".  After rotate, shift back by "+2".
//            moveY[ii] = -(tempNum-2) + 2;
//            System.out.print("(" + moveX[ii] + "," + moveY[ii] + ") ");
//          }
//	}
	//System.out.print("\n");

	//draw roated board
	if (jj!=0) {
	  updateMoveRecord(moveX, moveY, moveRecord.length());
	  resetTicTacToeShape();
	}
	//showTheMove();
        
	saveLine = "";
        for (int ii = 0; ii < moveRecord.length(); ii++) {
  	  if (ii != 0) {
	    saveLine += "-";
	  }
	  saveLine += "(" + moveX[ii] + "," + moveY[ii] + ")";
        }
	if (saveLineCombined.indexOf("**" + saveLine) >=0) {
	  //do not save duplicate move due to symmetry result
	  //System.out.println("Duplicate lose move, no save!");
	}
	else {
	  saveLineCombined += ("**" + saveLine);
	  bw.write(saveLine+"\n");
	}
      }
      bw.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }

  /**
   * Logic to play the computer move for the game.
   * <p>
   * It uses a random number to decide the next move.  It skips a position already occupied by either
   * computer of person, and it skips a position that will lead to the same lose record by comparing
   * to the losePatterns[] from the historical loses.
   */
  private void computerMove() {
    int move = 0;
    int tempMove = 0;
    String loseHistoricalMoves = "";
    
    loseHistoricalMoves = getLoseHistoricalMoves(); //Get all the next moves leading to existing loses

    Random rand = new Random();
    tempMove = rand.nextInt(MAX_MOVE_NUMBER - moveRecord.length() - loseHistoricalMoves.length()) + 1;

    move = 0;
    for(int ii=0; ii<tempMove; ii++) {
      move++;
      while ( (moveRecord.indexOf(String.valueOf(move)) >= 0) ||  //skip existing moves
	      (loseHistoricalMoves.indexOf(String.valueOf(move)) >= 0) ) {  //skip lose moves 
        if (loseHistoricalMoves.indexOf(String.valueOf(move)) >= 0) { 
	  System.out.println("Skip a historical lose move: " + move);
	}
	move++;
      }
    }

    System.out.println("Computer move: " + String.valueOf((move - ((move - 1) % 3)) / 3 + 1) + "," +
		       String.valueOf((move - 1) % 3 + 1));
    moveRecord = moveRecord + String.valueOf(move); 
  }

  private String getLoseHistoricalMoves() {
    String histMoves = "";

    for (int ii=1; ii < losePatterns.length; ii++) {
      if (losePatterns[ii] == null) {
        continue;
      }

      if ( (losePatterns[ii].length() == moveRecord.length()+1) &&
           (losePatterns[ii].startsWith(moveRecord)) ) {
	String loseMove = losePatterns[ii].substring(moveRecord.length(), moveRecord.length()+1);
	if (moveRecord.indexOf(loseMove)>=0) {
	  //the lose move is impossible to happen since the position is already occupied
	  ;
	}
	else {
	  histMoves += loseMove;
	}
      }
    }
    
    return histMoves;
  }

  private void updateTicTacToeShape() {
    int[] moveX = new int[9];
    int[] moveY = new int[9];

    //Get move coordinates from moveRecord
    System.out.print("Total Moves: ");
    for (int ii = 0; ii < moveRecord.length(); ii++) {
      int move = Integer.parseInt(moveRecord.substring(ii, ii+1));
      moveX[ii] = (move - ((move - 1) % 3)) / 3 + 1;
      moveY[ii] = (move - 1) % 3 + 1;
      System.out.print("(" + moveX[ii] + "," + moveY[ii] + ") ");
    }
    System.out.print("\n");

    resetTicTacToeShape();
    String moveMark = "";
    for (int ii = 0; ii < moveRecord.length(); ii++) {
      if (ii % 2 == 0) { //computer move
        moveMark = "X";
      }
      else { //person move
	moveMark = "O";
      }

      int rowNumber = moveX[ii] * 2 - 1 - 1;

      ticTacToeShape[rowNumber] = ticTacToeShape[rowNumber].substring(0, 2+(moveY[ii]-1)*4-1) + moveMark +
		ticTacToeShape[rowNumber].substring(2+(moveY[ii]-1)*4);
    }
  }

  private void updateMoveRecord(int[] moveX, int[] moveY, int recordSize) {
    //upddate moveRecord with moveX, moveY arrays
    moveRecord = "";
    for (int ii=0; ii<recordSize; ii++) {
      moveRecord += String.valueOf((moveX[ii]-1)*3+moveY[ii]);
    }
  }

  private String getInputMessage(String inputLine) {
    String msg = "";
    int moveX, moveY;

    if (inputLine.matches("^\\s*\\d\\s*,\\s*\\d\\s*$")) {
      moveX=Integer.parseInt(inputLine.substring(0, inputLine.indexOf(",")).trim());
      moveY=Integer.parseInt(inputLine.substring(inputLine.indexOf(",") + 1).trim());

      if ( (moveX>=1) && (moveX<=3) && (moveY>=1) && (moveY<=3) ) {
	if (moveRecord.indexOf(String.valueOf((moveX-1)*3+moveY))>=0) {
          msg = "Move (" + moveX + "," + moveY + ") already used, please re-enter:";
	}
	else {
          //msg = "Good Move: (" + moveX + "," + moveY + ")";
	  msg = String.valueOf((moveX-1)*3+moveY);
	}
      }
      else {
        msg = "Move (" + moveX + "," + moveY + ") out of range, please re-enter:";
      }
    }
    else {
      msg = "Bad move format, please re-enter:";
    }

    return msg;
  }

  private void personMove() {
    int move = 0;
    int tempMove = 0;
    String inputLine = "";

    String msg = "Please enter your move:";
    try {
      while(!msg.matches("^\\d$")) {
        System.out.print(msg);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        inputLine = br.readLine();
	msg = getInputMessage(inputLine);
      }
    } catch (Exception e) {}
    
    moveRecord = moveRecord + String.valueOf(msg); 
  }

  private void personMoveSimulated() {
    int move = 0;
    int tempMove = 0;

    Random rand = new Random();
    tempMove = rand.nextInt(MAX_MOVE_NUMBER - moveRecord.length()) + 1;

    move = 0;
    for(int ii=0; ii<tempMove; ii++) {
      move++;
      while (moveRecord.indexOf(String.valueOf(move)) >= 0) { //skip existing moves
	move++;
      }
    }

    System.out.println("Person move simulated: " + String.valueOf((move - ((move - 1) % 3)) / 3 + 1) + "," +
		       String.valueOf((move - 1) % 3 + 1));
    moveRecord = moveRecord + String.valueOf(move); 
  }

  private void showTheMove() {
    //System.out.println("The moves are: " + moveRecord);

    updateTicTacToeShape();
    for (int ii=0; ii<5; ii++) {
      System.out.println(ticTacToeShape[ii]);
    }
  }

  private void nextMove() {
    if (moveRecord.length() % 2 == 0) {
      computerMove();
    }
    else {
      personMove();
      //personMoveSimulated();
    }

    showTheMove();
  }

  private boolean isPersonWin() {
    boolean personWin = false;

    if (someoneWin && (moveRecord.length() % 2 == 0) ) {
      personWin = true;
    }

    return personWin;
  }

  private boolean isComputerWin() {
    boolean computerWin = false;

    if (someoneWin && (moveRecord.length() % 2 != 0) ) {
      computerWin = true;
    }

    return computerWin;
  }

  private boolean hasSameX(int[] moveX, int[] moveY, int aValue) {
    boolean hasSame = false;
    int countSameValue = 0;

    //check computer moves
    countSameValue = 0;
    for (int ii = 0; ii < moveRecord.length(); ii+=2) {
      if (moveX[ii] == aValue) {
	countSameValue++;
      }
    }

    if (countSameValue==3) {
      hasSame = true;
    }

    //check person moves
    if (!hasSame) {
      countSameValue = 0;
      for (int ii = 1; ii < moveRecord.length(); ii+=2) {
        if (moveX[ii] == aValue) {
  	countSameValue++;
        }
      }
  
      if (countSameValue==3) {
        hasSame = true;
      }
    }

    return hasSame;
  }

  private boolean hasSameY(int[] moveX, int[] moveY, int aValue) {
    boolean hasSame = false;
    int countSameValue = 0;

    //check computer moves
    countSameValue = 0;
    for (int ii = 0; ii < moveRecord.length(); ii+=2) {
      if (moveY[ii] == aValue) {
	countSameValue++;
      }
    }

    if (countSameValue==3) {
      hasSame = true;
    }

    //check person moves
    if (!hasSame) {
      countSameValue = 0;
      for (int ii = 1; ii < moveRecord.length(); ii+=2) {
        if (moveY[ii] == aValue) {
  	countSameValue++;
        }
      }
  
      if (countSameValue==3) {
        hasSame = true;
      }
    }

    return hasSame;
  }

  private boolean hasDiagonal(int[] moveX, int[] moveY) {
    boolean hasDiagonal = false;
    boolean has11, has22, has33, has13, has31;
    int computerOrPerson = 0; //computerOrPerson = 0 ==> computer, computerOrPerson = 1 ==> person
    
    hasDiagonal = false;
    for (computerOrPerson = 0; (computerOrPerson <= 1) && (!hasDiagonal); computerOrPerson++) {
    has11 = false;
    has22 = false;
    has33 = false;
    has13 = false;
    has31 = false;
    for (int ii = computerOrPerson; ii < moveRecord.length(); ii+=2) {
      if ( (moveX[ii] == 1) && (moveY[ii] == 1) ) {
	has11 = true;
      }
      if ( (moveX[ii] == 2) && (moveY[ii] == 2) ) {
	has22 = true;
      }
      if ( (moveX[ii] == 3) && (moveY[ii] == 3) ) {
	has33 = true;
      }
    }

    if (has11 && has22 && has33) {
      hasDiagonal = true;
    }
    else {
      for (int ii = computerOrPerson; ii < moveRecord.length(); ii+=2) {
        if ( (moveX[ii] == 1) && (moveY[ii] == 3) ) {
  	has13 = true;
        }
        if ( (moveX[ii] == 2) && (moveY[ii] == 2) ) {
  	has22 = true;
        }
        if ( (moveX[ii] == 3) && (moveY[ii] == 1) ) {
  	has31 = true;
        }
      }

      if (has13 && has22 && has31) {
        hasDiagonal = true;
      }
    }
    }

    return hasDiagonal;
  }

  private boolean isGameEnded() {
    boolean gameEnded = false;

    int[] moveX = new int[9];
    int[] moveY = new int[9];

    //Get move coordinates from moveRecord
    //System.out.print("Moves: ");
    for (int ii = 0; ii < moveRecord.length(); ii++) {
      int move = Integer.parseInt(moveRecord.substring(ii, ii+1));
      moveX[ii] = (move - ((move - 1) % 3)) / 3 + 1;
      moveY[ii] = (move - 1) % 3 + 1;
      //System.out.print("(" + moveX[ii] + "," + moveY[ii] + ") ");
    }
    //System.out.print("\n");

    for (int ii = 1; ii <= 3; ii++) {
      if (hasSameX(moveX, moveY, ii) ||  hasSameX(moveY, moveX, ii)) {
	gameEnded = true;
      }
    }

    if (!gameEnded) {
      if(hasDiagonal(moveX, moveY)) {
	gameEnded = true;
      }
    }

    if (gameEnded) {
      someoneWin = true;
    }

    return gameEnded;
  }
}
