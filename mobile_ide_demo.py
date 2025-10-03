# 🚀 PocketCode Mobile IDE - Sample Programs
# Copy these examples to test your mobile coding environment!

print("=== Welcome to PocketCode Mobile IDE! ===")
print("A powerful coding environment right in your pocket!")
print("")

# ========== EXAMPLE 1: Interactive Calculator ==========
print("📱 Interactive Calculator")
print("Enter two numbers and see the magic!")

try:
    # You can uncomment these lines to make it interactive
    # num1 = float(input("Enter first number: "))
    # num2 = float(input("Enter second number: "))
    
    # For demonstration, using sample values
    num1, num2 = 15, 7
    print(f"Using sample values: {num1} and {num2}")
    
    print(f"➕ Addition: {num1} + {num2} = {num1 + num2}")
    print(f"➖ Subtraction: {num1} - {num2} = {num1 - num2}")
    print(f"✖️ Multiplication: {num1} × {num2} = {num1 * num2}")
    print(f"➗ Division: {num1} ÷ {num2} = {num1 / num2:.2f}")
    print(f"📐 Power: {num1} ^ {num2} = {num1 ** num2}")
except ValueError:
    print("❌ Please enter valid numbers!")

print("\n" + "="*50 + "\n")

# ========== EXAMPLE 2: Smart List Manager ==========
print("📋 Smart List Manager")

# Create a sample task list
tasks = [
    {"task": "Learn Python", "priority": "High", "done": True},
    {"task": "Build mobile app", "priority": "High", "done": False},
    {"task": "Practice coding daily", "priority": "Medium", "done": False},
    {"task": "Read tech blogs", "priority": "Low", "done": True}
]

print("📝 Your Tasks:")
for i, task in enumerate(tasks, 1):
    status = "✅" if task["done"] else "⏳"
    priority_emoji = {"High": "🔴", "Medium": "🟡", "Low": "🟢"}
    emoji = priority_emoji.get(task["priority"], "⚪")
    
    print(f"{i}. {status} {task['task']} {emoji} ({task['priority']})")

# Statistics
total_tasks = len(tasks)
completed_tasks = sum(1 for task in tasks if task["done"])
completion_rate = (completed_tasks / total_tasks) * 100

print(f"\n📊 Progress: {completed_tasks}/{total_tasks} tasks completed ({completion_rate:.1f}%)")

print("\n" + "="*50 + "\n")

# ========== EXAMPLE 3: Pattern Generator ==========
print("🎨 Beautiful Pattern Generator")

def create_pattern(size=5):
    print("✨ Diamond Pattern:")
    # Upper half
    for i in range(size):
        spaces = " " * (size - i - 1)
        stars = "⭐" * (2 * i + 1)
        print(f"{spaces}{stars}")
    
    # Lower half
    for i in range(size - 2, -1, -1):
        spaces = " " * (size - i - 1)
        stars = "⭐" * (2 * i + 1)
        print(f"{spaces}{stars}")

create_pattern(4)

print("\n🌈 Number Triangle:")
for i in range(1, 6):
    numbers = " ".join(str(j) for j in range(1, i + 1))
    spaces = " " * (5 - i)
    print(f"{spaces}{numbers}")

print("\n" + "="*50 + "\n")

# ========== EXAMPLE 4: Data Analysis ==========
print("📈 Simple Data Analysis")

# Sample student data
students = [
    {"name": "Alice", "math": 95, "science": 88, "english": 92},
    {"name": "Bob", "math": 78, "science": 95, "english": 85},
    {"name": "Charlie", "math": 88, "science": 78, "english": 90},
    {"name": "Diana", "math": 95, "science": 92, "english": 88},
    {"name": "Eve", "math": 82, "science": 85, "english": 95}
]

print("🎓 Student Report Card:")
print(f"{'Name':<10} {'Math':<6} {'Science':<8} {'English':<8} {'Average':<8} {'Grade'}")
print("-" * 55)

for student in students:
    avg = (student["math"] + student["science"] + student["english"]) / 3
    
    if avg >= 90:
        grade = "A"
    elif avg >= 80:
        grade = "B"
    elif avg >= 70:
        grade = "C"
    else:
        grade = "D"
    
    print(f"{student['name']:<10} {student['math']:<6} {student['science']:<8} {student['english']:<8} {avg:<8.1f} {grade}")

# Class statistics
all_scores = []
for student in students:
    all_scores.extend([student["math"], student["science"], student["english"]])

class_avg = sum(all_scores) / len(all_scores)
highest = max(all_scores)
lowest = min(all_scores)

print(f"\n📊 Class Statistics:")
print(f"   Class Average: {class_avg:.1f}")
print(f"   Highest Score: {highest}")
print(f"   Lowest Score: {lowest}")

print("\n" + "="*50 + "\n")

# ========== EXAMPLE 5: Mini Game ==========
print("🎮 Number Guessing Game (Simulated)")

import random

secret_number = random.randint(1, 10)
print(f"🎯 I'm thinking of a number between 1 and 10...")

# Simulate some guesses
guesses = [3, 7, secret_number]
attempts = 0

for guess in guesses:
    attempts += 1
    print(f"Guess #{attempts}: {guess}")
    
    if guess == secret_number:
        print(f"🎉 Congratulations! You found it in {attempts} attempts!")
        break
    elif guess < secret_number:
        print("📈 Too low! Try higher.")
    else:
        print("📉 Too high! Try lower.")

print(f"🔢 The secret number was: {secret_number}")

print("\n" + "="*50 + "\n")

# ========== EXAMPLE 6: Web Request Demo ==========
print("🌐 Web Request Demo")

try:
    import requests
    print("🔄 Fetching data from the internet...")
    
    response = requests.get("https://httpbin.org/json", timeout=5)
    if response.status_code == 200:
        data = response.json()
        print("✅ Successfully connected to the internet!")
        print(f"📡 Response from server:")
        for key, value in data.items():
            print(f"   {key}: {value}")
    else:
        print(f"❌ Request failed with status: {response.status_code}")
        
except ImportError:
    print("📱 Requests library not available (this is normal on some devices)")
except Exception as e:
    print(f"🌐 Network error: {e}")

print("\n" + "="*50 + "\n")

# ========== EXAMPLE 7: File Operations Demo ==========
print("📁 File Operations Demo")

# Demonstrate working with file content
sample_content = """# My Python Notes
# Created with PocketCode Mobile IDE

## Key Concepts:
1. Variables and Data Types
2. Control Structures (if, for, while)
3. Functions and Classes
4. File Handling
5. Error Handling

## Remember:
- Practice coding daily
- Build real projects
- Learn from others
- Stay curious!
"""

print("📝 Sample file content:")
print(sample_content)

# Process the content
lines = sample_content.split('\n')
print(f"📊 File statistics:")
print(f"   Total lines: {len(lines)}")
print(f"   Non-empty lines: {len([line for line in lines if line.strip()])}")
print(f"   Comment lines: {len([line for line in lines if line.strip().startswith('#')])}")

print("\n🎯 PocketCode Mobile IDE Features Demonstrated:")
print("✅ Interactive console output")
print("✅ Mathematical calculations")
print("✅ Data structures and loops")
print("✅ Pattern generation")
print("✅ Data analysis and statistics")
print("✅ Conditional logic")
print("✅ Error handling")
print("✅ External library usage")
print("✅ String processing")
print("✅ Random number generation")

print("\n🚀 Ready to code your own programs!")
print("📱 PocketCode - Your coding companion anywhere, anytime!")