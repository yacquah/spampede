package com.gradescope.hw10;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The SpampedeController manages all game interactions, including responding
 * to keyboard input and controlling the main game loop.
 * <p>
 * This class serves as the "controller" in the Model–View–Controller (MVC)
 * architecture. It updates the model, triggers view updates, and manages the
 * game's timer-based progression.
 * </p>
 *
 * @author HMC CS 60 Instructors
 * @version Fall 2025
 */
public class SpampedeController extends JPanel implements ActionListener, KeyListener, Runnable {
  
  /** The "view" responsible for rendering the game */
  private SpampedeView view;

  /** The "model" in MVC responsible for storing game state. */
  public SpampedeModel model;

  /* --------------------------------------------------------------- */
  /* Fields and methods are used to implement the Runnable interface */
  /* and to support pausing and resuming the game. */
  /* --------------------------------------------------------------- */

  /** The thread controlling the updates. */
  Thread thread;

  /** Tracks whether or not the thread is suspended. */
  boolean threadSuspended;

  /** Tracks whether or not the thread is stopped. */
  boolean running;

  // The mappings between direction (names) and keys.

  /** Key to reverse snake. */
  private static final char REVERSE = 'r';

  /** Key to move up. */
  private static final char UP = 'i';

  /** Key to move down. */
  private static final char DOWN = 'k';

  /** Key to move left. */
  private static final char LEFT = 'j';

  /** Key to move right. */
  private static final char RIGHT = 'l';

  /** Key to start AI mode. */
  private static final char AI_MODE = 'a';

  /** Key to play the spam noise. */
  private static final char PLAY_SPAM_NOISE = 's';

  /**
   * Constructs a new controller for managing gameplay
   */
  public SpampedeController() {
    // Initialize the view and start the game
    this.view = new SpampedeView(this, null, Preferences.BOARD_WIDTH, 600);
    this.startNewGame(); // Set up the game internals!
  }

  /**
   * Processes buttons and menu items.
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    String actionCommand = evt.getActionCommand();
    switch (actionCommand) {
    case "New Game":
      this.startNewGame();
      this.go();
      break;
    case "Pause":
      this.pause();
      break;
    case "Start":
      this.go();
      break;
    }
    this.requestFocus(); // makes sure this applet keeps keyboard focus
  }

  /**
   * Reacts to characters typed by the user.
   * 
   * <p>
   * SpampedeController registers as an "observer" for key presses on the
   * keyboard. So, whenever the user presses a key, Java automatically calls this
   * keyPressed method and passes it a KeyEvent describing the specific key press.
   * </p>
   *
   * <p>
   * Runs on Swing's event thread, so it takes the controller's lock to avoid
   * changing the snake while the game thread is in the middle of a cycle.
   * </p>
   */
  @Override
  public synchronized void keyPressed(KeyEvent evt) {
    switch (evt.getKeyChar()) { // get the char of the pressed key
        case REVERSE:
        this.model.reverseSnake();
        break;
        case AI_MODE:
            this.model.setAIMode();
            break;
        case PLAY_SPAM_NOISE:
            this.playSound_spam();
            break;
        case UP:
            this.model.setDirectionNorth();
            break;
        case DOWN:
            this.model.setDirectionSouth();
            break;
        case LEFT:
            this.model.setDirectionWest();
            break;
        case RIGHT:
            this.model.setDirectionEast();
            break;
        default:
            break;
    }
  }

  @Override
  public void keyReleased(KeyEvent evt) {
    // Not used
  }

  @Override
  public void keyTyped(KeyEvent evt) {
    // Not used
  }

  /**
   * Runs the game loop on the game thread.
   *
   * <p>
   * Each cycle runs while holding this controller's lock, the same lock taken by
   * {@link #keyPressed}, {@link #startNewGame}, {@link #go}, {@link #pause} and
   * {@link #gameOver}, so the game state is never changed by two threads at
   * once. The lock is not held while sleeping between cycles or while paused.
   * </p>
   */
  @Override
  public void run() {
    // calls the "model.cycle()" method every so often (every sleepTime
    // milliseconds)
    while (this.running) {
      try {
        Thread.sleep(Preferences.SLEEP_TIME); // sleep without holding the lock
        synchronized (this) {
          while (this.threadSuspended) {
            this.wait(); // releases the lock until notify() wakes it up
          }
          this.model.cycle(); // one update cycle, with key presses locked out
        }
      } catch (InterruptedException e) {
        ;
      }
    }
    this.thread = null;
  }

  /**
   * Called when the "Start" button is pressed. Does nothing once the game is
   * over; only "New Game" can restart it.
   */
  public synchronized void go() {
    if (this.model.getGameOver()) {
      return;
    }
    if (this.thread == null) {
      this.thread = new Thread(this);
      this.running = true;
      this.thread.start();
      this.threadSuspended = false;
    } else {
      this.threadSuspended = false;
    }
    this.notify(); // wakes up the call to wait(), above
  }

  /**
   * Called when the "Pause" button is pressed.
   */
  synchronized void pause() {
    if (this.thread != null)
      this.threadSuspended = true;
  }

  /**
   * Starts a new game. The model is swapped and drawn under the controller's
   * lock, so a cycle of the old game can never finish against the new one.
   */
  public void startNewGame() {
    synchronized (this) {
      this.model = new SpampedeModel(this, this.view);
      this.model.placeSnakeAtStartLocation();
      this.model.setStartDirection();

      this.view.updateModel(this.model);
    }

    /**
     * Hack because pictures have a delay in loading, and we do not redraw the
     * screen again until the game actually starts, which means we would not see the
     * image until the game does start. Wait a fraction of a second (200 ms), by
     * which time the picture should have been fetched from disk, and redraw.
     */
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    synchronized (this) {
      this.view.updateGraphics();
    }
    this.playSound_spam();
  }

  /** Declares the game over. */
  public synchronized void gameOver() {
    if (this.thread != null)
      this.threadSuspended = true;
    this.model.setGameOver(); // tell the model that the game is over
  }

  /* ------ */
  /* Sounds */
  /* ------ */

  /** Plays crunch noise. */
  public void playSound_spamEaten() {
    this.playSound("media/crunch.wav");
  }

  /** Plays spam noise. */
  public void playSound_spam() {
    this.playSound("media/spam.wav");
  }

  /**
   * Plays a sound from the given file name.
   * 
   * @param filename the file name of the sound to play
   */
  public void playSound(String filename) {
    AudioInputStream sound = null;
    File f = new File(filename).getAbsoluteFile();

    try {
      sound = AudioSystem.getAudioInputStream(f);
    } catch (Exception e) {
      System.out.println("Problem loading sound " + filename);
    }

    if (sound != null) {
      try (AudioInputStream stream = sound) {
        Clip clip = AudioSystem.getClip();
        // Free the audio line (and its thread) once the sound has finished
        clip.addLineListener(event -> {
          if (event.getType() == LineEvent.Type.STOP) {
            clip.close();
          }
        });
        clip.open(stream); // loads the whole sound, so the file can be closed
        clip.start();
      } catch (Exception e) {
        System.out.println("Problem playing sound " + filename);
      }
    }
  }

  /* ---------------------- */
  /* Main */
  /* ---------------------- */

  /**
   * Starts the Spampede game. WARNING: Make sure to close the last game before
   * running a new one!
   * 
   * @param args unused command-line arguments
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        new SpampedeController();
      }
    });
  }
}
