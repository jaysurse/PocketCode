"""
Sample Python script for PocketCode app
"""

def hello_world():
    """Return a simple greeting"""
    return "Hello from Python!"

def add_numbers(a, b):
    """Add two numbers and return the result"""
    return a + b

def get_python_info():
    """Get Python version and system info"""
    import sys
    import platform
    
    info = {
        "python_version": sys.version,
        "platform": platform.platform(),
        "architecture": platform.architecture()[0]
    }
    return info

def test_requests():
    """Test the requests library"""
    try:
        import requests
        response = requests.get("https://httpbin.org/json")
        if response.status_code == 200:
            return {"status": "success", "data": response.json()}
        else:
            return {"status": "error", "message": f"HTTP {response.status_code}"}
    except Exception as e:
        return {"status": "error", "message": str(e)}

def calculator(operation, x, y):
    """Simple calculator function"""
    operations = {
        "add": lambda a, b: a + b,
        "subtract": lambda a, b: a - b,
        "multiply": lambda a, b: a * b,
        "divide": lambda a, b: a / b if b != 0 else "Error: Division by zero"
    }
    
    if operation in operations:
        return operations[operation](x, y)
    else:
        return "Error: Unknown operation"

# Test function for immediate execution
if __name__ == "__main__":
    print(hello_world())
    print(f"2 + 3 = {add_numbers(2, 3)}")
    print(f"Calculator: 10 * 5 = {calculator('multiply', 10, 5)}")