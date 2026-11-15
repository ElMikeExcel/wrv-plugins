# Guide note draft — ExcelBrainGames 0.3.4 (BB-02)

For the Guide knowledge (supervisor applies in the install pass):

- Brain Games navigation: the game picker (opened from the Server
  Menu, or with /games) has a "Back to Server Menu" button. Every
  game board (Word Search, Memory Match, Sudoku — including the
  Bedrock box picker) and the Quick Math / Word Scramble question
  cards have a Back button: it saves the round in progress and
  returns to the game picker, where the round shows as in progress
  and can be resumed. Back never discards a round and never banks
  a Word Scramble round (banking is Finish now). On the 9x9 Sudoku
  board, Back is at slot 51 and the Eraser sits on the 6–9 palette
  bank (switch the palette numbers to reach it). Closing a board
  with ESC also keeps the round saved, as before.
