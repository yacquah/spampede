package com.gradescope.hw10;

import java.awt.Color;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Collections;

/**
 * The "model" in MVC that is responsible for storing all the data for the
 * board.
 * 
 * @author HMC CS 60 Instructors
 * @version Fall 2025
 * 
 * @see SpampedeController
 * @see SpampedeView
 */



/**
 * The SpampedeModel stores all game state information, including the board,
 * snake position, spam location, movement direction, and game-over condition.
 * <p>
 * This class contains all of the logic necessary for updating the game's
 * state, such as moving the snake, detecting collisions, placing spam, and
 * computing BFS distances. It does not perform any rendering; all drawing is
 * handled by the {@link SpampedeView}.
 */
class SpampedeModel {
  /**
   * The model needs to call the controller to play sounds and to end the game.
   */
  private final SpampedeController controller;

  /**
   * The model needs to call the view to update graphics.
   */
  private final SpampedeView view;

  /**
   * The collection of all the {@link BoardCell} objects in the program, indexed
   * by row and column.
   * 
   * <p>
   * All {@link BoardCell} objects needed by the program are created by the
   * {@link SpampedeModel} constructor, so you do not need to create any new
   * {@link BoardCell} objects in your code. Instead, you will pass around
   * (references to) existing cells, and change the contents of some of these
   * cells.
   * </p>
   */
  private final BoardCell[][] boardCells2D;

  // TODO HW #10.0C Add final where appropriate to members below!

  /** The number of non-wall cells in the initial board. */
  private int freeSpots;

  /**
   * The current movement "mode" of the snake, i.e. whether it is headed in a
   * particular direction or in AI mode.
   */
  private SnakeMode currentMode;

  /**
   * A list of (references to) cells that currently contain spam, ordered from
   * oldest (first) to youngest (last).
   */
  private LinkedList<BoardCell> spamCells;

  /**
   * A list of (references to) the cells that contain the snake. The head is the
   * last element of the list.
   */
  private LinkedList<BoardCell> snakeCells;

  /** Whether the game is over. */
  private boolean gameOver;

  /** The number of animated frames displayed so far. */
  private int cycleNum;

  /* -------------------------------------- */
  /* Constructor and initialization methods */
  /* -------------------------------------- */

  /**
   * Initializes a new Spampede model.
   * 
   * @param controller the Spampede controller
   * @param view       the Spampede view
   * @param width      the board width in number of cells
   * @param height     the board height in number of cells
   * @throws IllegalArgumentException if the number of cells is less than 1
   */
  public SpampedeModel(SpampedeController controller, SpampedeView view, int width, int height)
      throws IllegalArgumentException {
    // check if valid value
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Width and height must be 1 or more.");
    }

    // initialize the game board
    this.boardCells2D = new BoardCell[height][width];

    // initialize MVC
    this.controller = controller;
    this.view = view;

    // initialize the other instance member variables
    this.freeSpots = 0;
    this.currentMode = SnakeMode.GOING_EAST;
    this.spamCells = new LinkedList<>();
    this.snakeCells = new LinkedList<>();
    this.gameOver = false;
    this.cycleNum = 0;

    this.addWalls(); // place walls around the outside
    this.fillRemainingCells(); // fill the remaining cells not already filled!
  }

  /**
   * Creates a new Spampede model using {@link Preferences} as the default width
   * and height.
   * 
   * @param controller the Spampede controller
   * @param view       the Spampede view
   * 
   * @see Preferences#NUM_CELLS_WIDE
   * @see Preferences#NUM_CELLS_TALL
   */
  public SpampedeModel(SpampedeController controller, SpampedeView view) {
    this(controller, view, Preferences.NUM_CELLS_WIDE, Preferences.NUM_CELLS_TALL);
  }

  /**
   * Adds walls around the edges of this board.
   */
  private void addWalls() {
    int height = this.getNumRows();
    int width = this.getNumColumns();

    // Add left and right walls
    for (int row = 0; row < height; row++) {
      this.boardCells2D[row][0] = new BoardCell(row, 0, CellType.WALL);
      this.boardCells2D[row][width - 1] = new BoardCell(row, width - 1, CellType.WALL);
    }

    // Add top and bottom walls
    for (int column = 0; column < width; column++) {
      this.boardCells2D[0][column] = new BoardCell(0, column, CellType.WALL);
      this.boardCells2D[height - 1][column] = new BoardCell(height - 1, column, CellType.WALL);
    }
  }

  /**
   * Adds open cells to the interior of this board.
   */
  private void fillRemainingCells() {
    int height = this.getNumRows();
    int width = this.getNumColumns();
    this.freeSpots = 0;

    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        if (this.boardCells2D[row][column] == null) {
          this.boardCells2D[row][column] = new BoardCell(row, column, CellType.OPEN);
          this.freeSpots++;
        }
      }
    }
  }

  /**
   * Puts the snake in the upper-left corner of the walls, facing east.
   */
  public void placeSnakeAtStartLocation() {
    BoardCell body = this.getCell(1, 1);
    BoardCell head = this.getCell(1, 2);
    this.snakeCells.addLast(body);
    this.snakeCells.addLast(head);
    head.becomeHead();
    body.becomeBody();
  }

  /* -------- */
  /* Gameplay */
  /* -------- */

  /**
   * Moves the game forward one step. One step is one frame of animation, which
   * occurs every {@link Preferences#SLEEP_TIME} milliseconds.
   * 
   * @see Preferences#SLEEP_TIME
   */
  public void cycle() {
    // move the snake
    this.updateSnake();

    // update the list of spam
    this.updateSpam();

    // draw the board
    this.view.updateGraphics();

    // update the cycle counter
    this.cycleNum++;
  }

  /* ---------------------- */
  /* Snake movement methods */
  /* ---------------------- */

  /**
   * Moves the snake.
   * 
   * <p>
   * This method is called once every {@link Preferences#REFRESH_RATE} cycles,
   * either in the current direction, or as directed by the AI's breadth-first
   * search.
   * 
   * Called by {@link #cycle()}.
   * </p>
   */
  private void updateSnake() {
    if (this.cycleNum % Preferences.REFRESH_RATE == 0) {
      BoardCell nextCell;
      if (this.inAImode()) {
        nextCell = this.getNextCellFromBFS();
      } else {
        nextCell = this.getNextCellInDir();
      }
      this.advanceTheSnake(nextCell);
    }
  }

  /**
   * Moves the snake to the next cell (and possibly eat spam).
   * 
   * @param nextCell the new location of the snake head (which must be
   *                 horizontally or vertically adjacent to the old location of
   *                 the snake head)
   */
  private void advanceTheSnake(BoardCell nextCell) {
    // Note - do not modify provided code.
    if (nextCell.isWall() || nextCell.isBody()) {
      // Oops...we hit something.
      this.controller.gameOver();
      return;
    } else if (nextCell.isSpam()) {
      // the snake ate spam!
      this.controller.playSound_spamEaten();
    }
    this.moveSnakeForward(nextCell);
  }

  /**
   * Moves the snake forward
   * 
   * @param cell the cell to move the snake forward to
   */
  public void moveSnakeForward(BoardCell cell) {
    // Remember whether this cell contained spam before we move into it
    boolean wasSpam =cell.isSpam();

    this.updateHead(cell);    //move head into new cell

    //if new head cell is not spam, remove tail, snake does not grow
    // otherwise, we do not remove tail, so the snake grows
    if (!wasSpam){
        this.removeTail();
    }
  }

  // ---- REMOVE BEG ----
  /**
   * Updates the head of the snake.
   * 
   * @param cell the new head cell
   */
  private void updateHead(BoardCell cell) {
    if (cell.isSpam()) {
      this.spamCells.remove(cell);
    }
    BoardCell head = this.getSnakeHead();
    head.becomeBody();
    cell.becomeHead();
    this.snakeCells.addLast(cell);
  }

  /** Removes the snake's tail. */
  private void removeTail() {
    BoardCell tail = this.getSnakeTail();
    tail.becomeOpen();
    this.snakeCells.removeFirst();
  }
  // ---- REMOVE END ----

  /**
   * Adds more spam every SPAM_ADD_RATE cycles.
   */
  private void updateSpam() {
    if (this.noSpam()) {
      this.addSpam();
    } else if (this.cycleNum % Preferences.SPAM_ADD_RATE == 0) {
      this.addSpam();
    }
  }

  /* ---------------------------------------------- */
  /* Methods to access information about this board */
  /* ---------------------------------------------- */

  /**
   * {@return true if we are in AI mode}
   */
  private boolean inAImode() {
    return this.currentMode == SnakeMode.AI_MODE;
  }

  /**
   * {@return the height of this board (including walls) in cells}
   */
  public int getNumRows() {
    return this.boardCells2D.length;
  }

  /**
   * {@return the width of this board (including walls) in cells}
   */
  public int getNumColumns() {
    return this.boardCells2D[0].length;
  }

  /**
   * Accesses a cell at a particular location.
   * 
   * <p>
   * This method should really be private. We make it public to allow our unit
   * tests to use it, but it should not be called from SpampedeController or
   * SpampedeView.
   * </p>
   * 
   * @param row the row to access, between 0 and numRows-1 inclusive
   * @param col the column to access, between 0 and numCols-1 inclusive
   * @return the cell in row and col
   */
  protected BoardCell getCell(int row, int col) {
    if (row >= this.getNumRows() || col >= this.getNumColumns() || row < 0 || col < 0) {
      System.err.println("Trying to access cell outside of the Board:");
      System.err.println("row: " + row + " col: " + col);
      System.exit(0);
    }
    return this.boardCells2D[row][col];
  }

  /* ------------------------------ */
  /* Helper method used by the view */
  /* ------------------------------ */

  /**
   * Gets the color of the cell at a particular location.
   * 
   * @param row the row to access, between 0 and numRows-1 inclusive
   * @param col the column to access, between 0 and numCols-1 inclusive
   * @return the color of cell at row r and column c
   */
  public Color getCellColor(int row, int col) {
    BoardCell cell = this.getCell(row, col);
    return cell.getCellColor();
  }

  /* ---------------- */
  /* Game over status */
  /* ---------------- */

  /**
   * Sets the game status as game over.
   */
  public void setGameOver() {
    this.gameOver = true;
  }

  /**
   * {@return {@code true} if the game over message should be displayed}
   */
  public boolean getGameOver() {
    return this.gameOver;
  }

  /* -------------------- */
  /* Spam-related methods */
  /* -------------------- */

  /**
   * {@return {@code true} if there is zero spam}
   */
  private boolean noSpam() {
    return this.spamCells.isEmpty();
  }

  /**
   * Adds spam to a random open spot.
   */
  private void addSpam() {
    // Pick a random cell
    int row = (int) (this.getNumRows() * Math.random());
    int column = (int) (this.getNumColumns() * Math.random());
    BoardCell cell = this.getCell(row, column);

    if (cell.isOpen()) {
      // If the random cell is open, put spam there
      cell.becomeSpam();
      this.spamCells.addLast(cell);
    } else {
      // If the random cell is occupied and this board is not already
      // too full of spam, try to place spam again
      double totalSize = this.getNumColumns() * this.getNumRows();
      double currentFreeSpots = this.freeSpots - this.snakeCells.size() - this.spamCells.size();
      double ratioFree = currentFreeSpots / totalSize;
      if (ratioFree < 0.2) {
        System.err.println("Not adding more spam");
      } else {
        this.addSpam();
      }
    }
  }

  /**
   * Removes the oldest piece of un-eaten spam.
   * 
   * <p>
   * The function is not used in the given code, but it might be useful if you
   * want to extend the game.
   * </p>
   */
  @SuppressWarnings("unused")
  private void removeSpam() {
    if (!this.spamCells.isEmpty()) {
      this.spamCells.peekFirst().becomeOpen();
      this.spamCells.removeFirst();
    }
  }

  /* -------------------- */
  /* Snake access methods */
  /* -------------------- */

  /**
   * {@return the cell containing the snake's head}
   */
  private BoardCell getSnakeHead() {
    return this.snakeCells.peekLast();
  }

  /**
   * {@return the cell containing the snake's tail}
   */
  private BoardCell getSnakeTail() {
    return this.snakeCells.peekFirst();
  }

  /**
   * {@return the cell contains the snake body adjacent to the head}
   */
  private BoardCell getSnakeNeck() {
    int lastSnakeCellIndex = this.snakeCells.size() - 1;
    return this.snakeCells.get(lastSnakeCellIndex - 1);
  }

  /* ------------------------------------------ */
  /* Methods to set the snake's (movement) mode */
  /* ------------------------------------------ */

  /**
   * Makes the snake head north.
   */
  public void setDirectionNorth() {
    this.currentMode = SnakeMode.GOING_NORTH;
  }

  /**
   * Makes the snake head south.
   */
  public void setDirectionSouth() {
    this.currentMode = SnakeMode.GOING_SOUTH;
  }

  /**
   * Makes the snake head east.
   */
  public void setDirectionEast() {
    this.currentMode = SnakeMode.GOING_EAST;
  }

  /**
   * Makes the snake head west.
   */
  public void setDirectionWest() {
    this.currentMode = SnakeMode.GOING_WEST;
  }

  /**
   * Makes the snake switch to AI mode.
   */
  public void setAIMode() {
    this.currentMode = SnakeMode.AI_MODE;
  }

  /**
   * Picks an initial movement mode for the snake.
   */
  public void setStartDirection() {
    this.setDirectionEast();
  }

  /* -------------------------------------- */
  /* Methods to support movement without AI */
  /* -------------------------------------- */

  /*
   * NOTE: These methods should really be private. We make them public to allow
   * access by our unit tests, but the methods should not be called from
   * SpampedeController or SpampedeView.
   */


  /**
   * Returns the cell north of the specified cell, which must not be on the boundary.
   * 
   * @param cell the specified cell in the board */
  protected BoardCell getNorthNeighbor(BoardCell cell) {
    int row = cell.getRow();
    int col = cell.getColumn();
    return this.getCell(row-1, col);
  }

  /**
   * Returns the cell south of the specified cell, which must not be on the boundary.
   * 
   * @param cell the specified cell in the board  */ 
  protected BoardCell getSouthNeighbor(BoardCell cell) {
    int row = cell.getRow();
    int col = cell.getColumn();
    return this.getCell(row+1, col);
  }

  /** 
   * Returns the cell east of the specified cell, which must not be on the boundary.
   * 
   * @param cell the specified cell in the board */ 
  protected BoardCell getEastNeighbor(BoardCell cell) {
    int row = cell.getRow();
    int col = cell.getColumn();
    return getCell(row, col +1);
  }

  /**
   * Returns the cell west of the specified cell, which must not be on the boundary.
   * 
   * @param cell the specified cell in the board */ 
  protected BoardCell getWestNeighbor(BoardCell cell) {
    int row = cell.getRow();
    int col = cell.getColumn();
    return getCell(row, col-1);
  }

  /**
   * Returns the cell north, south, east, or west of the snake head based on the
   * current direction of travel. This method should not be called when in AI
   * mode, though Java requires the method to return a value regardless.
   * 
   * @return the cell north, south, east, or west of the snake head based on the
   *         current direction of travel
   */
  protected BoardCell getNextCellInDir() {
    switch (this.currentMode){
        case GOING_NORTH:
            return this.getNorthNeighbor();
        case GOING_SOUTH:
            return this.getSouthNeighbor();
        case GOING_EAST:
            return this.getEastNeighbor();
        case GOING_WEST:
            return this.getWestNeighbor();
        default:
            // Should never happen (AI mode uses BFS), but for safety retun the head
            return this.getSnakeHead();
    }
  }

  /**
   * {@return the cell north of the snake's head}
   */
  protected BoardCell getNorthNeighbor() {
    return this.getNorthNeighbor(this.getSnakeHead());
  }

  /**
   * {@return the cell south of the snake's head}
   */
  protected BoardCell getSouthNeighbor() {
    return this.getSouthNeighbor(this.getSnakeHead());
  }

  /**
   * {@return the cell east of the snake's head}
   */
  protected BoardCell getEastNeighbor() {
    return this.getEastNeighbor(this.getSnakeHead());
  }

  /**
   * {@return the cell west of the snake's head}
   */
  protected BoardCell getWestNeighbor() {
    return this.getWestNeighbor(this.getSnakeHead());
  }

  /* -------------------------------------------------- */
  /* Public methods to get all or one (random) neighbor */
  /* -------------------------------------------------- */

  /**
   * Returns an array of the four neighbors of the specified cell.
   * 
   * @param center the center cell to return neighbors around
   * @return an array of the four neighbors of the specified cell
   */
  private BoardCell[] getNeighbors(BoardCell center) {
    BoardCell[] neighborsArray = { this.getNorthNeighbor(center), this.getSouthNeighbor(center),
        this.getEastNeighbor(center), this.getWestNeighbor(center) };
    return neighborsArray;
  }

  /**
   * Returns a random open neighbor of the specified cell (or some other neighbor
   * if there are no open neighbors.
   * 
   * @param start the starting cell
   * @return a random open neighbor of the specified cell (or some other neighbor
   *         if there are no open neighbors
   */
  private BoardCell getRandomNeighboringCell(BoardCell start) {
    BoardCell[] neighborsArray = this.getNeighbors(start);
    for (BoardCell mc : neighborsArray) {
      if (mc.isOpen()) {
        return mc;
      }
    }
    // if we did not find an open space, return the first neighbor
    return neighborsArray[0];
  }

  /* ---------------------------- */
  /* Helper method(s) for reverse */
  /* ---------------------------- */

  /**
   * Reverses the snake.
   * Tail becomes the new head, and direction is updated solely on 
   * new head and new new body
   */
  public void reverseSnake() {
    
    // Snake must be long enough to reverse
    if (this.snakeCells.size() < 2){
        return;
    }
    
    // reverse the linkedlist of Cells
    Collections.reverse(this.snakeCells);

    // Update all cell types
    for (BoardCell cell : this.snakeCells){
        cell.becomeBody();
    }
    BoardCell newHead = this.snakeCells.getLast();   // head is last
    BoardCell newBody = this.snakeCells.get(this.snakeCells.size()-2);

    newHead.becomeHead();

    // Compute new direction from head to neck
    int headRow = newHead.getRow();
    int headColumn = newHead.getColumn();
    int bodyRow = newBody.getRow();
    int bodyColumn = newBody.getColumn();

    int dirRow = headRow - bodyRow;
    int dirColumn = headColumn - bodyColumn;

    if (dirRow == -1 && dirColumn == 0){
        this.currentMode = SnakeMode.GOING_NORTH;
    } else if (dirRow== 1 && dirColumn == 0){
        this.currentMode =SnakeMode.GOING_SOUTH;
    } else if (dirRow == 0 && dirColumn == 1){
        this.currentMode =SnakeMode.GOING_EAST;
    } else if (dirRow == 0 && dirColumn == -1){
        this.currentMode = SnakeMode.GOING_WEST;
    }
  }

  /* ------------------------------------- */
  /* Methods to reset the model for search */
  /* ------------------------------------- */

  /**
   * Clears the search-related fields in all the cells, in preparation for a new
   * breadth-first search.
   */
  private void resetCellsForNextSearch() {
    for (BoardCell[] row : this.boardCells2D) {
      for (BoardCell cell : row) {
        cell.resetSearch();
      }
    }
  }

  /**
   * Searches for the spam closest to the snake head using BFS.
   * 
   * @return the cell to move the snake head to, if the snake moves *one step*
   *         along the shortest path to (the nearest) spam cell
   */
  protected BoardCell getNextCellFromBFS() {
    // initialize the search
    this.resetCellsForNextSearch();

    // initialize the cellsToSearch queue with the snake head;
    // as with any cell, we mark the head cells as having been added
    // to the queue
    Queue<BoardCell> cellsToSearch = new LinkedList<BoardCell>();
    BoardCell snakeHead = this.getSnakeHead();
    snakeHead.addToSearch();
    cellsToSearch.add(snakeHead);

    // BFS search loop
    while (!cellsToSearch.isEmpty()) {
        BoardCell current = cellsToSearch.remove();

        // If current cell is spam, trace back to find the next move
        if (current.isSpam()) {
            return getFirstCellInPath(current);
        }

        // Get neighbors in required order: North, South, East, West
        BoardCell[] neighbors = this.getNeighbors(current);

        for (BoardCell nbr : neighbors) {

            // Skip walls and snake body (not open)
            if (!nbr.isOpen() && !nbr.isSpam()) {
                continue;
            }

            // Only add if not already in queue
            if (!nbr.alreadySearched()) {
                nbr.addToSearch();
                nbr.setParent(current);
                cellsToSearch.add(nbr);
            }
        }
    }

    // if the search fails, just move somewhere
    return this.getRandomNeighboringCell(snakeHead);
  }

  /**
   * Follows the traceback pointers from the closest spam cell to decide where the
   * head should move. Specifically, follows the parent pointers back from the
   * spam until we find the cell whose parent is the snake head (and which must
   * therefore be adjacent to the previous snake head location).
   * 
   * @param start - the cell from which to start following pointers, typically the
   *              location of the spam closest to the snake head
   * @return the cell to move the snake head to, which should be a neighbor of the
   *         head
   */
  private BoardCell getFirstCellInPath(BoardCell start) {
    BoardCell current = start;

    // If BFS returned the head itself (unlikely but safe guard)
    if (current.getParent() == null) {
        return current;
    }

    // Walk back until the parent is the head
    while (current.getParent() != null && !current.getParent().isHead()) {
        current = current.getParent();
    }

    return current; // This is the neighbor to move into
}
  

  /* --------------------------------------------------------------------- */
  /* Testing infrastructure - You do not need to understand these methods! */
  /* --------------------------------------------------------------------- */

  // Pictures of test boards at http://tinyurl.com/spampedeTestBoards

  // Constructor used exclusively for testing!
  
  /**
   * Initializes a model with no controller or view for testing purposes.
   * 
   * @param gameNum the game number to initialize
   */
  public SpampedeModel(TestGame gameNum) {
    this(null, null, 6, 6);

    if (gameNum.snakeAtStart()) {
      this.testingSnakeAtStartLocation(gameNum);
      this.setDirectionEast();
    } else {
      this.testingSnakeNotAtStartLocation(gameNum);
    }

  }

  /**
   * Adds cells when the snake is at the starting location.
   * 
   * @param gameNum the game number
   */
  private void testingSnakeAtStartLocation(TestGame gameNum) {
    this.placeSnakeAtStartLocation();

    if (gameNum == TestGame.G1) {
      this.getCell(1, 3).becomeSpam();
    } else if (gameNum == TestGame.G2) {
      this.getCell(2, 2).becomeSpam();
    } else if (gameNum == TestGame.G3) {
      this.getCell(1, 4).becomeSpam();
    } else if (gameNum == TestGame.G4) {
      this.getCell(2, 1).becomeSpam();
    } else if (gameNum == TestGame.G5) {
      this.getCell(4, 1).becomeSpam();
    } else if (gameNum == TestGame.G6) {
      this.getCell(1, 3).becomeSpam();
      this.getCell(3, 1).becomeSpam();
    } else if (gameNum == TestGame.G7) {
      this.getCell(2, 2).becomeSpam();
      this.getCell(1, 4).becomeSpam();
    } else if (gameNum == TestGame.G8) {
      this.getCell(1, 4).becomeSpam();
      this.getCell(4, 2).becomeSpam();
    } else if (gameNum == TestGame.G9) {
      this.getCell(2, 1).becomeSpam();
      this.getCell(2, 4).becomeSpam();
    } else if (gameNum == TestGame.G10) {
      this.getCell(4, 1).becomeSpam();
      this.getCell(4, 4).becomeSpam();
    } else if (gameNum == TestGame.G11) {
      // No spam :)
    }

    // Add all spam to the spam cells
    int height = this.getNumRows();
    int width = this.getNumColumns();

    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        BoardCell cell = this.getCell(row, column);
        if (cell.isSpam()) {
          this.spamCells.add(cell);
        }
      }
    }
  }

  /**
   * Adds cells when the snake is NOT at the starting location.
   * 
   * @param gameNum the game number
   */
  private void testingSnakeNotAtStartLocation(TestGame gameNum) {
    if (gameNum == TestGame.G12) {
      BoardCell body2 = this.getCell(2, 3);
      BoardCell body1 = this.getCell(2, 2);
      BoardCell head = this.getCell(2, 1);
      this.snakeCells.add(body2);
      this.snakeCells.add(body1);
      this.snakeCells.add(head);
      head.becomeHead();
      body2.becomeBody();
      body1.becomeBody();
    } else if (gameNum == TestGame.G13) {
      BoardCell body2 = this.getCell(3, 2);
      BoardCell body1 = this.getCell(2, 2);
      BoardCell head = this.getCell(2, 1);
      this.snakeCells.add(body2);
      this.snakeCells.add(body1);
      this.snakeCells.add(head);
      head.becomeHead();
      body2.becomeBody();
      body1.becomeBody();
    } else if (gameNum == TestGame.G14) {
      BoardCell body2 = this.getCell(2, 2);
      BoardCell body1 = this.getCell(3, 2);
      BoardCell head = this.getCell(3, 1);
      this.snakeCells.add(body2);
      this.snakeCells.add(body1);
      this.snakeCells.add(head);
      head.becomeHead();
      body2.becomeBody();
      body1.becomeBody();
    } else if (gameNum == TestGame.G15) {
      BoardCell body2 = this.getCell(3, 2);
      BoardCell body1 = this.getCell(3, 3);
      BoardCell head = this.getCell(3, 4);
      this.snakeCells.add(body2);
      this.snakeCells.add(body1);
      this.snakeCells.add(head);
      head.becomeHead();
      body2.becomeBody();
      body1.becomeBody();
    }
  }

  @Override
  public String toString() {
    String result = "";
    for (int r = 0; r < this.getNumRows(); r++) {
      for (int c = 0; c < this.getNumColumns(); c++) {
        BoardCell cell = this.getCell(r, c);
        result += cell.toStringType();
      }
      result += "\n";
    }
    return result;
  }

  /**
   * {@return the {@link String} representation of the cell parents}
   */
  public String toStringParents() {
    String result = "";
    for (int r = 0; r < this.getNumRows(); r++) {
      for (int c = 0; c < this.getNumColumns(); c++) {
        BoardCell cell = this.getCell(r, c);
        result += cell.toStringParent() + "\t";
      }
      result += "\n";
    }
    return result;
  }
}
