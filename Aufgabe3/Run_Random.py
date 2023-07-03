"""
Dieses Programm erstellt ein zufälliges Sudoku-Paar, das zu einer Wahrscheinlichkeit
von standardmäßig 0.5 ineinander umformbar ist, und versucht dann, dieses zu lösen.

Wenn ein Ordner Beispieleingaben auf demselben Pfad wie diese Datei existiert, wird
das Ergebnis dort gespeichert, ansonsten wird es auf dem aktuellen Pfad gespeichert.
"""
from random_sudoku import *
from os import listdir

s1, s2 = random_pair()

instructions, _, _ = related_sudoku_check(s1, s2)
print(instructions)

####################################################################### Sudokus werden evtl. gespeichert
if input("\nSoll dieses Sudoku-Paar gespeichert werden? (y/n) ") != "y":
    quit()

try:
    files = listdir("Beispieleingaben")
except FileNotFoundError:
    files = listdir()

file_indices = list(filter(lambda s: s[:6] == "zufall" and s[-4:] == ".txt", files))
for i, s in enumerate(file_indices):
    index = s[6:s.find(".")]
    if index.isnumeric():
        file_indices[i] = int(index)
    else:
        file_indices[i] = -1

if file_indices:
    new_index = max(0, max(file_indices) + 1)
else:
    new_index = 0
    
try:
    sudokus_to_textfile(s1, s2, "zufall" + str(new_index), "Beispieleingaben")
except FileNotFoundError:
    sudokus_to_textfile(s1, s2, "zufall" + str(new_index))

print("\nSudokus wurden gespeichert.")
