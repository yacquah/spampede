package com.gradescope.hw10;

/**
 * The type of a board cell (wall, open, spam, head, body).
 *
 * <p>
 * Using an enum rather than a {@link String} code means a {@link BoardCell} can
 * never be given an invalid type. Each constant carries the character used to
 * display it in text form. Only {@link BoardCell} needs this type, so it is
 * package-private.
 * </p>
 *
 * @author HMC CS 60 Instructors
 * @version Fall 2025
 * 
 * @see <a href=
 *      "http://docs.oracle.com/javase/tutorial/java/javaOO/accesscontrol.html">
 *      Access Control</a>
 * 
 * @see BoardCell
 */
enum CellType {
  /** Indicates this cell is a wall (cannot be inhabited by the snake or spam). */
  WALL("*"),

  /** Indicates this cell is open (available for the snake or spam). */
  OPEN(" "),

  /** Indicates this cell contains spam. */
  SPAM("X"),

  /** Indicates this cell in inhabited by the snake's head. */
  HEAD("H"),

  /** Indicates this cell is inhabited by the snake's body. */
  BODY("B");

  /** The character used for display. */
  private final String displayChar;

  /**
   * Initializes this cell type.
   * 
   * @param displayChar the character to use for display
   */
  private CellType(String displayChar) {
    this.displayChar = displayChar;
  }

  /** {@return a {@link String} representing the {@link CellType}} */
  public String getDisplayChar() {
    return this.displayChar;
  }
}
