def binary_search(arr, target):
    low = 0
    high = len(arr) - 1
    
    while low <= high:
        mid = (low + high) // 2
        
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            low = mid + 1
        else:
            high = mid - 1
            
    return None

# Example usage
arr = [1, 3, 5, 7, 9]
target = 5
result = binary_search(arr, target)
if result is not None:
    print(f"Element {target} found at index: {result}")
else:
    print(f"Element {target} not found in the array")