"""
Erwartet eine Eingabe vom Benutzer als Textdatei-Name und überprüft die dort enthaltenen Sudokus.

Wenn ein Ordner Beispieleingaben auf demselben Pfad wie diese Datei existiert, wird
dort nach der angegebenen Datei gesucht, ansonsten auf dem aktuellen Pfad.
"""
from Sudokopie import *

try:
    s1, s2 = sudokus_from_input("Beispieleingaben")
except FileNotFoundError:
    s1, s2 = sudokus_from_input()

instructions, _, _ = related_sudoku_check(s1, s2)
print(instructions)

while True: pass
