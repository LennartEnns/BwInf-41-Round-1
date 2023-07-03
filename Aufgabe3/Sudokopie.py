import numpy as np
from os import listdir
from itertools import permutations, product

class sudoku_solution:
    def __init__(self, rotation: int, rearrangements, reassignments, rotation_first = True):
        self.rotation = rotation
        self.rearrangements = list(rearrangements)
        self.reassignments = list(reassignments)
        self.rotation_first = rotation_first
    
    def get_data(self):
        return self.rotation, self.rearrangements.copy(), \
            self.reassignments.copy(), self.rotation_first

    def inversed(self):
        rot, rearr, reass, rot_first = self.get_data() # Initialisierung und Inversion der Rotation
        if rot in (1, 3):
            rot = (rot + 2) % 4
        
        rearr_row_blocks = rearr[0] = reverse_perm(rearr[0]) # Inversion der Permutationen
        rearr_col_blocks = rearr[1] = reverse_perm(rearr[1])
        rearr[2:5] = (rearr[i+2] for i in rearr_row_blocks)
        rearr[5:8] = (rearr[i+5] for i in rearr_col_blocks)
        for i in range(2, 8):
            rearr[i] = reverse_perm(rearr[i])
        
        reass = list(map(lambda i: reass.index(i) + 1, range(1, 10)))

        return sudoku_solution(rot, rearr, reass, not rot_first)

    def total_steps(self):
        nperms = 0
        if self.rotation:
            nperms += 1

        nperms += 8 - self.rearrangements.count((0,1,2))
        
        if self.reassignments != list(range(1, 10)):
            nperms += 1
        return nperms

    def instructions(self):
        rot, rearr, reass, rot_first = self.get_data()
        rearr_el1 = ["Zeilenblöcke", "Spaltenblöcke"] + 3*["Zeilen"] + 3*["Spalten"]
        rearr_el2 = 2*[""] + 2*[" 1-3", " 4-6", " 7-9"]
        instructions = []
        rot_instruction = ""
        rearr_instructions = []
        reass_instruction = ""

        if rot:
            rot_instruction = f"-> Das Sudoku {rot} mal um 90° im Uhrzeigersinn drehen"
            instructions.append(rot_instruction)
        
        for i, arr in enumerate(rearr):
            if arr != (0,1,2):
                inst = rearr_el1[i]
                inst += rearr_el2[i]
                inst += " umordnen zu: "
                if i in (0, 1, 2, 5):
                    inc = 1
                elif i in (3, 6):
                    inc = 4
                else:
                    inc = 7
                inst += " ".join(map(lambda i: str(i + inc), arr))
                rearr_instructions.append("-> " + inst)
        if rearr_instructions:
            instructions.append("\n".join(rearr_instructions))

        if reass != list(range(1, 10)):
            reass_instruction = "-> Ziffern 1 bis 9 umbenennen zu: " + " ".join(str(i) for i in reass)
            instructions.append(reass_instruction)
        
        if rot and rearr_instructions and not rot_first:
            instructions[:2] = instructions[1::-1]

        for i in range(len(instructions) - 1):
            instructions[i] += "\n"
        return "\n".join(instructions)

class sudoku:
    #################### Definieren der relevanten, mehrdimensionalen Indexe
    INDICES = [list(tuple([i]) for i in range(3)), list((rb, cb) for rb in range(3) for cb in range(3))] # Zeilenblock- und Sektor-Indexe
    for i in range(3):
        INDICES.append(list(zip(3*[i], 3*[slice(None)], range(3)))) # Zeilen-Indexe pro Zeilenblock
    for i in range(3):
        INDICES.append(list(zip(3*[slice(None)], 3*[i], 3*[slice(None)], range(3)))) # Spalten-Indexe pro Spaltenblock

    def __init__(self, data: np.ndarray):
        self.data = np.array(data) # Darstellung als 4D-Array
        self.FUNC_BY_DEPTH = (self.arrange_row_blocks, self.arrange_col_blocks) + \
        tuple(3 *[ self.arrange_rows]) + tuple(3 * [self.arrange_cols]) # Definition der verwendeten Methoden nach Rekursionstiefe
    
    def __eq__(self, other):
        return np.array_equal(self.data, other.data)

    def output(self, dig_spaces = 1, dig_breaks = 0, sec_spaces = 4, sec_breaks = 1):
        "Gibt das Sudoku gut lesbar als String zurück\n\ndig_spaces / sec_spaces = Anzahl von Leerzeichen \
        als Separator zwischen den Ziffern / Sektoren\n\ndig_breaks / sec_breaks = Anzahl von Leeren Zeilen \
        als Separator zwischen den Ziffern / Sektoren"
        string = []
        for row_block in range(3):
            for row in range(3):
                substring = []
                for sector in range(3):
                    string_array = np.char.mod("%i", self.data[row_block, sector, row, :])
                    substring.append((dig_spaces * " ").join(string_array))
                
                string.append((sec_spaces * " ").join(substring))
                if (row < 2) and dig_breaks + 1:
                    string.append((dig_breaks + 1) * "\n")
            if (row_block < 2) and sec_breaks + 1:
                string.append((sec_breaks + 1) * "\n")
        return "".join(string)
############################################################## Permutationen

    def arrange_row_blocks(self, indices):
        self.data[:] = np.array(list(map(lambda i: self.data[i], indices)))
    
    def arrange_col_blocks(self, indices):
        self.data[:, 0], self.data[:, 1], self.data[:, 2] = \
        np.array(list(map(lambda i: self.data[:, i], indices)))

    def arrange_rows(self, row_block, indices):
        self.data[row_block, :, range(3)] = \
        np.array(list(map(lambda i: self.data[row_block, :, i], indices)))

    def arrange_cols(self, col_block, indices):
        self.data[:, col_block, :, range(3)] = \
        np.array(list(map(lambda i: self.data[:, col_block, :, i], indices)))
    
    def rotate_90_clockwise(self, n=1):
        "Rotiert das Sudoku n-mal im Uhrzeigersinn"
        self.data = np.rot90(self.data, n, (1,0))
        self.data = np.rot90(self.data, n, (3,2))
    
    def rename_digits(self, new_digits):
        t_list = []
        for n in range(1, 10):
            t_list.append(self.data == n)
        for t, n in zip(t_list, new_digits):
            self.data[t] = n
    
    def do_solution(self, solution: sudoku_solution):
        rot, rearr, reass, rot_first = solution.get_data()

        if rot and rot_first:
            self.rotate_90_clockwise(rot)
        for i, p in enumerate(rearr):
            if p != (0,1,2):
                if i > 1:
                    self.FUNC_BY_DEPTH[i] ((i - 2) % 3, p)
                else:
                    self.FUNC_BY_DEPTH[i] (p)
        if rot and not rot_first:
            self.rotate_90_clockwise(rot)

        if reass != tuple(range(1, 10)):
            self.rename_digits(reass)

############################################### Methoden zur Erfassung von Daten über Ziffern

    def digit_distribution(self, area = None):
        "Gibt in einem Dictionary an, welche Ziffern im ggf. angegebenen Teilarray \
        die jeweilige Häufigkeit haben"
        if type(area) not in [np.ndarray, np.int32]:
            area = self.data
        abs_n_dig = dict()
        for n in range(1, 10):
            occ = np.count_nonzero(area == n)
            if occ not in abs_n_dig.keys():
                abs_n_dig[occ] = set()
            abs_n_dig[occ].add(n)

        return abs_n_dig

    def digit_assignments(self, target, indices = tuple()):
        """Vergleicht das Sudoku an den angegebenen Indexen mit dem Sudoku target und gibt
        anhand der Häufigkeitsverteilungen für jede Ziffer eine Menge mit ihren möglichen
        Umbenennungen zurück."""
        
        assignments = list(set(range(1, 10)) for _ in range(9))
        indices = tuple(indices)
        dist_self = self.digit_distribution(self.data[indices])
        dist_target = self.digit_distribution(target.data[indices])

        if dist_self.keys() != dist_target.keys():
            return None
        
        for k, v in dist_self.items():
            for i in v:
                assignments[i-1] = dist_target[k].copy()

        return assignments
    
    def positional_digit_assignments(self, target, assignments, indices):
        """Vergleicht das Sudoku an den angegebenen Indexen mit dem Sudoku target,
        \nindem geprüft wird, ob die Ziffern in Position und Umbennung übereinstimmen.
        \nIst das überall der Fall, wird für jede Ziffer eine Menge möglicher Umbennungen zurückgegeben."""
        for i in indices:
            data_self = np.reshape(self.data[i], 9)
            data_target = np.reshape(target.data[i], 9)
            for d1, d2 in zip(data_self, data_target):
                if d1 != 0:
                    if d2 not in assignments[d1 - 1]:
                        return None
                    else:
                        for i in range(9):
                            if i != d1 - 1:
                                assignments[i].discard(d2)
        return assignments

    def digit_assignment_product(assignments):
        "Bildet das kartesische Produkt aus den 9 von digit_assignments zurückgegebenen \
        Mengen.\n\nEs wird als Iterator zurückgegeben, um nicht ggf. die Laufzeit unnötig zu \
        verlängern."
        return filter(lambda i: len(set(i)) == len(i), product(*assignments))

def sudoku_from_lines(lines):
    for i in range(len(lines)):
        int_list = [int(n) for n in lines[i].split(" ")]
        lines[i] = [int_list[:3], int_list[3:6], int_list[6:9]]
    row_blocks = [lines[:3], lines[3:6], lines[6:9]]
    for i in range(len(row_blocks)):
        row_blocks[i] = list(zip(*row_blocks[i]))
    return sudoku(row_blocks)

def sudokus_to_textfile(s1: sudoku, s2: sudoku, name, path = ""):
    if path and path[-2:] != "\\":
        path += "\\"
    with open(path + name + ".txt", 'w', encoding = "utf-8-sig") as file:
        for s in (s1, s2):
            sudoku_string = []
            for i_block in range(3):
                for i_row in range(3):
                    row = np.reshape(s.data[i_block, :, i_row], 9)
                    row_string = " ".join(str(i) for i in row)
                    sudoku_string.append(row_string)
            file.write("\n".join(sudoku_string))
            if s is s1:
                file.write("\n\n")

def sudokus_from_textfile(path):
    with open(path, 'r', encoding = "utf-8-sig") as file:
        lines = file.readlines()
        lines = list(l.strip() for l in lines)
        lines = list(filter(lambda l: l != "", lines))

        l1 = lines[:9]
        l2 = lines[9:]
        return sudoku_from_lines(l1), \
               sudoku_from_lines(l2)

def sudokus_from_input(path = ""):
    if path:
        if path[-2:] != "\\":
            path += "\\"
        files = listdir(path)
    else:
        files = listdir()
    
    filename = input("Welche Textdatei soll verwendet werden? ").strip()
    while (filename + ".txt") not in files:
        print(f"Die Datei \"{filename + '.txt'}\" existiert nicht unter dem angegebenen Pfad.\n")
        filename = input("Welche Textdatei soll verwendet werden? ").strip()
    
    return sudokus_from_textfile(path + filename + ".txt")

def reverse_perm(p: tuple):
    return tuple(map(lambda i: p.index(i), range(3)))

def find_rearrangements(s1: sudoku, s2: sudoku, depth = 0,
assignments = list(set(range(1, 10)) for _ in range(9)), arr = None):
    if arr is None:
        arr = []
    IND = sudoku.INDICES
    PERMS = s1.FUNC_BY_DEPTH
    if depth < 2:
        add_arg = tuple()
    else:
        add_arg = tuple([(depth - 2) % 3])
    for p in permutations(range(3)):
        if p != (0,1,2):
            PERMS[depth] (*add_arg, p)

        if depth < 5:
            next_assignments = []
            for indices in IND[depth]:
                next_assignments.append(s1.digit_assignments(s2, indices))
            assignment_overlap = None
            if not None in next_assignments:
                assignment_overlap = list(set.intersection(*sets) for sets in
                zip(assignments, *next_assignments))
        else:
            assignment_overlap = s1.positional_digit_assignments(s2, assignments, IND[depth])

        if assignment_overlap:
            if not set() in assignment_overlap:
                if depth < 7:
                    next_depth_data = find_rearrangements(s1, s2, depth + 1, assignment_overlap.copy(), arr.copy() + [p])
                    if next_depth_data:
                        PERMS[depth] (*add_arg, reverse_perm(p))
                        return next_depth_data
                else:
                    arr.append(p)
                    PERMS[depth] (*add_arg, reverse_perm(p))
                    return arr, \
                        next(sudoku.digit_assignment_product(assignment_overlap))

        PERMS[depth] (*add_arg, reverse_perm(p))

def related_sudoku_check(s1: sudoku, s2: sudoku):
    print("\nLösungen werden ermittelt...\n")
    info = ["Sudoku 1:\n", s1.output(), "\nSudoku 2:\n", s2.output()]
    if s1 == s2:
        info.append("\nDiese Sudokus sind gleich, keine Umformungen nötig.")
        return "\n".join(info), None, None

    solutions: list[sudoku_solution] = []
    data_rot0 = find_rearrangements(s1, s2)
    if data_rot0:
        solutions.append(sudoku_solution(0, *data_rot0))
        s1.rotate_90_clockwise(2)
        solutions.append(sudoku_solution(2, *find_rearrangements(s1, s2)))
        s1.rotate_90_clockwise(2)

    s1.rotate_90_clockwise()
    data_rot1 = find_rearrangements(s1, s2)
    s1.rotate_90_clockwise(3)
    if data_rot1:
        solutions.append(sudoku_solution(1, *data_rot1))
        s1.rotate_90_clockwise(3)
        solutions.append(sudoku_solution(3, *find_rearrangements(s1, s2)))
        s1.rotate_90_clockwise()
    
    if solutions:
        nsteps_each = tuple(map(lambda s: s.total_steps(), solutions))

        solution: sudoku_solution = solutions[nsteps_each.index(min(nsteps_each))]
        solution_back: sudoku_solution = solution.inversed()

        info.append("\nDiese Sudokus sind Kopien voneinander.\nUmformungen von 1 zu 2:\n")
        info.append(solution.instructions())
        info.append("\nUmformungen von 2 zu 1:\n")
        info.append(solution_back.instructions())
    else:
        info.append("\nDiese Sudokus sind keine Kopien voneinander.")
        solution, solution_back = None, None

    return "\n".join(info), solution, solution_back

def check_solutions(s1: sudoku, s2: sudoku, solution, solution_back):
    print("\nLösungen werden geprüft:")
    if not (solution and solution_back):
        print("Keine Lösungen vorhanden")
        return

    s1.do_solution(solution)
    if s1 == s2:
        print("Lösung von 1 zu 2 funktioniert")
    else:
        print("Lösung von 1 zu 2 funktioniert nicht") # Sollte niemals passieren
    s1.do_solution(solution_back)

    s2.do_solution(solution_back)
    if s1 == s2:
        print("Lösung von 2 zu 1 funktioniert.")
    else:
        print("Lösung von 2 zu 1 funktioniert nicht") # Sollte auch niemals passieren
