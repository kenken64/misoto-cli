def solve_tower(n, src, temp, dst):
    if n > 1:
        solve_tower(n-1, src, dst, temp)
        solve_tower(1, src, temp, dst)
        solve_tower(n-1, temp, dst, src)

# Call the function to demonstrate tower of hanoi
solve_tower(3, 'A', 'B', 'C')