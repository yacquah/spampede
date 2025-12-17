# Spampede 

A Java implementation of **Spampede** (Snake-style gameplay) built with a clean **Model–View–Controller (MVC)** architecture using **Java Swing / Java2D**. Includes standard player controls, a **reverse** mechanic, and an **AI mode** powered by **Breadth-First Search (BFS)** pathfinding.

---

## Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [AI Mode](#ai-mode)
- [Rendering](#rendering)
- [Project Structure](#project-structure)
- [Controls](#controls)
- [How to Run](#how-to-run)
- [Testing](#testing)
- [Notes on Style & Design](#notes-on-style--design)

---

## Features
- Grid-based game board with typed cells (e.g., wall/open/spam/snake head/snake body)
- Keyboard movement
- Reverse functionality
- “Say Spam” action
- AI mode (BFS shortest-path steering)

---

## Architecture

This project follows **MVC** to keep responsibilities clean and code maintainable:

### Model (`SpampedeModel`)
- Owns all game state (board, snake, spam, modes)
- Enforces game rules and updates state each cycle
- Implements AI logic (BFS search + next-move selection)

### View (`SpampedeView`)
- Responsible only for drawing the current model state
- Renders the title/UI and board grid using Java2D

### Controller (`SpampedeController`)
- Handles keyboard/button events (listeners)
- Runs the game loop and triggers `model.cycle()` updates

---

## AI Mode

AI mode is implemented using **Breadth-First Search (BFS)** on the board grid:

1. Start BFS from the snake head.
2. Expand neighbors to find a reachable target (typically spam).
3. Track `parent` pointers so the shortest path can be reconstructed.
4. Use the first step of the reconstructed path to choose the next movement direction.

To support this cleanly, each `BoardCell` stores BFS bookkeeping (e.g., visited/added flag and parent pointer), allowing fast path reconstruction without extra external maps.

---

## Rendering

The board is drawn as a grid of rectangles:

- The view loops over the model’s board cells
- Each `CellType` maps to a visual style (color / tile)
- Each cell is drawn as a filled rectangle sized by a constant (e.g., `CELL_SIZE`)

This keeps drawing logic centralized in the view and prevents gameplay rules from leaking into UI code.

---

## Project Structure

Typical structure:

```text
.
├── src/
│   ├── SpampedeModel.java
│   ├── SpampedeView.java
│   ├── SpampedeController.java
│   ├── BoardCell.java
│   ├── CellType.java
│   └── Preferences.java
└── README.md

