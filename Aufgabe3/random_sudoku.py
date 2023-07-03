from Sudokopie import *
import numpy as np

def random_sudoku():
    rand_array = np.random.randint(1, 10, size = 3**4)

    for i in range(rand_array.size):
        rand_array[i] *= np.random.choice(2)

    s1 = sudoku(np.reshape(rand_array, (3, 3, 3, 3)))
    return s1

def related_pair():
    s1 = random_sudoku()
    s2 = sudoku(s1.data)

    if np.random.choice(2):
        s2.arrange_row_blocks(np.random.permutation(3))
    if np.random.choice(2):
        s2.arrange_col_blocks(np.random.permutation(3))
    for i in range(3):
        if np.random.choice(2):
            s2.arrange_rows(i, np.random.permutation(3))
        if np.random.choice(2):
            s2.arrange_cols(i, np.random.permutation(3))

    s2.rotate_90_clockwise(np.random.choice(4))

    if np.random.choice(2):
        s2.rename_digits(np.random.permutation(range(1, 10)))
    
    return s1, s2

def unrelated_pair():
    return random_sudoku(), random_sudoku()

def random_pair(relation_probability = 0.5):
    if not np.random.choice(round(1 / relation_probability)):
        return related_pair()
    return unrelated_pair()
