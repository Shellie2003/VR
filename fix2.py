import re

def fix(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
        
    for i, line in enumerate(lines):
        if 'Icon(androidx.compose.material.icons.Icons.Default.Star, contentDescription = null' in line:
            # We need to manually inspect these to fix them properly.
            print(f"{filename}:{i}: {line.strip()}")

fix('app/src/main/java/com/example/ui/screens/CalculatorScreen.kt')
fix('app/src/main/java/com/example/ui/screens/AddProductScreen.kt')
