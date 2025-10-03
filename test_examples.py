# Example Python codes that users might paste into your code editor app
# Copy and paste these into your app to test!

# ========== EXAMPLE 1: Simple Hello World ==========
print("Hello, World!")
print("Welcome to PocketCode!")

# ========== EXAMPLE 2: Basic Math Operations ==========
a = 10
b = 5
print(f"Addition: {a} + {b} = {a + b}")
print(f"Subtraction: {a} - {b} = {a - b}")
print(f"Multiplication: {a} * {b} = {a * b}")
print(f"Division: {a} / {b} = {a / b}")

# ========== EXAMPLE 3: Loops and Conditions ==========
print("Numbers from 1 to 10:")
for i in range(1, 11):
    if i % 2 == 0:
        print(f"{i} is even")
    else:
        print(f"{i} is odd")

# ========== EXAMPLE 4: Functions ==========
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)

print("Fibonacci sequence:")
for i in range(10):
    print(f"F({i}) = {fibonacci(i)}")

# ========== EXAMPLE 5: Lists and Dictionaries ==========
fruits = ["apple", "banana", "orange", "grape"]
print("Fruits list:")
for i, fruit in enumerate(fruits):
    print(f"{i+1}. {fruit}")

student = {
    "name": "John",
    "age": 20,
    "grade": "A",
    "subjects": ["Math", "Physics", "Chemistry"]
}
print(f"\nStudent Info:")
print(f"Name: {student['name']}")
print(f"Age: {student['age']}")
print(f"Grade: {student['grade']}")
print(f"Subjects: {', '.join(student['subjects'])}")

# ========== EXAMPLE 6: Classes ==========
class Calculator:
    def __init__(self):
        self.result = 0
    
    def add(self, x):
        self.result += x
        return self
    
    def multiply(self, x):
        self.result *= x
        return self
    
    def get_result(self):
        return self.result

calc = Calculator()
result = calc.add(10).multiply(5).add(25).get_result()
print(f"Calculator result: {result}")

# ========== EXAMPLE 7: Error Handling ==========
try:
    number = int("not_a_number")
except ValueError as e:
    print(f"Error caught: {e}")
    print("This demonstrates error handling!")

# ========== EXAMPLE 8: List Comprehensions ==========
squares = [x**2 for x in range(1, 11)]
print(f"Squares: {squares}")

even_squares = [x**2 for x in range(1, 11) if x % 2 == 0]
print(f"Even squares: {even_squares}")

# ========== EXAMPLE 9: String Operations ==========
text = "Python Programming"
print(f"Original: {text}")
print(f"Uppercase: {text.upper()}")
print(f"Lowercase: {text.lower()}")
print(f"Reversed: {text[::-1]}")
print(f"Length: {len(text)}")

# ========== EXAMPLE 10: Working with requests (if available) ==========
try:
    import requests
    print("Testing HTTP request...")
    response = requests.get("https://httpbin.org/json")
    if response.status_code == 200:
        data = response.json()
        print(f"HTTP request successful!")
        print(f"Response keys: {list(data.keys())}")
    else:
        print(f"HTTP request failed with status: {response.status_code}")
except ImportError:
    print("Requests library not available")
except Exception as e:
    print(f"Request error: {e}")